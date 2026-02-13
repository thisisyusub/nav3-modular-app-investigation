package com.example.nav3example.jetRouter


import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable

// ============================================================================
// 6. JetRouter — The main router class
// ============================================================================

@Stable
class JetRouter(
    val routes: List<RouteBase>,
    val initialLocation: String = "/",
    val redirect: (suspend (JetRouterState) -> String?)? = null,
    val errorBuilder: (@Composable (JetRouterState) -> Unit)? = null,
    val debugLogDiagnostics: Boolean = false,
    val onException: ((JetRouterState, JetRouter) -> Unit)? = null,
) {
    internal val matcher = RouteMatcher(routes)
    internal val namedRouteResolver = NamedRouteResolver(routes)

    // ── Navigation state ──────────────────────────────────────────────

    internal val backStack = mutableStateListOf<RouteMatchList>()
    internal val locationStack = mutableStateListOf<LocationEntry>()

    val currentMatch: RouteMatchList?
        get() = backStack.lastOrNull()

    val currentState: JetRouterState
        get() = currentMatch?.toState() ?: JetRouterState()

    // ── go() — Declarative navigation ─────────────────────────────────

    /**
     * Navigate to a location declaratively.
     *
     * When both current and target are in the same shell:
     *   - Finds the top-level JetRoute (direct child of the shell) that the
     *     current leaf belongs to — this is the "current tab root".
     *   - Pops all entries that descend from that same tab root.
     *   - Keeps the tab root entry itself so back returns to it.
     *   - Pushes the new destination.
     *
     * Example:
     *   Start:              ["/"]
     *   go("/users"):       ["/", "/users"]
     *   push("/users/42"):  ["/", "/users", "/users/42"]
     *   go("/settings"):    ["/", "/users", "/settings"]
     *     ↑ popped /users/42 (deep push under /users tab)
     *     ↑ kept /users (tab root)
     *     ↑ pushed /settings
     *   push("/settings/notifications"): ["/", "/users", "/settings", "/settings/notifications"]
     *   Back (pop):         ["/", "/users", "/settings"]
     *   Back (pop):         ["/", "/users"]
     *   Back (pop):         ["/"]
     *
     * When target is outside any shell (e.g. /login): replaces entire stack.
     */
    suspend fun go(location: String, extra: Any? = null) {
        val matchList = resolveWithRedirects(location, extra, forceAnimationOff = true)
        if (matchList.isError) {
            handleError(matchList)
            return
        }

        val leafRoute = matchList.lastMatch?.route
        val currentLeaf = currentMatch?.lastMatch?.route

        val targetShell = leafRoute?.let { findOwnerShell(routes, it) }
        val currentShell = currentLeaf?.let { findOwnerShell(routes, it) }
        val sameShell = targetShell != null && targetShell === currentShell

        if (sameShell && backStack.size > 0) {
            // Find the top-level tab route (direct child of shell) that the
            // CURRENT leaf belongs to.
            val currentTabRoot = currentLeaf?.let {
                findTabRoot(currentShell!!, it)
            }

            if (currentTabRoot != null) {
                // Pop all entries whose leaf descends from the same tab root,
                // EXCEPT the tab root entry itself.
                while (backStack.size > 1) {
                    val topLeaf = backStack.last().lastMatch?.route ?: break
                    val topTabRoot = findTabRoot(currentShell!!, topLeaf)

                    if (topTabRoot === currentTabRoot) {
                        // This entry is in the same tab branch
                        // Check if this IS the tab root entry itself
                        val topIsTabRoot = isDirectTabRoute(currentShell, topLeaf)
                        if (topIsTabRoot) {
                            // This is the tab root — keep it, stop popping
                            break
                        } else {
                            // This is a deep push within the tab — pop it
                            backStack.removeLast()
                            locationStack.removeLast()
                        }
                    } else {
                        // Different tab or outside shell — stop
                        break
                    }
                }
            }

            // Push the new destination
            backStack.add(matchList)
            locationStack.add(LocationEntry(location, extra))
        } else {
            // Outside shell or first entry — replace entire stack
            backStack.clear()
            locationStack.clear()
            backStack.add(matchList)
            locationStack.add(LocationEntry(location, extra))
        }

        if (debugLogDiagnostics) {
            log("go($location) -> stack: ${locationStack.map { it.location }}")
        }
    }

    // ── push() — Imperative navigation ────────────────────────────────

    suspend fun push(location: String, extra: Any? = null) {
        val matchList = resolveWithRedirects(location, extra)
        if (matchList.isError) {
            handleError(matchList)
            return
        }
        backStack.add(matchList)
        locationStack.add(LocationEntry(location, extra))

        if (debugLogDiagnostics) {
            log("push($location) -> stack size: ${backStack.size}")
        }
    }

    // ── pushReplacement() ─────────────────────────────────────────────

    suspend fun pushReplacement(location: String, extra: Any? = null) {
        val matchList = resolveWithRedirects(location, extra, forceAnimationOff = true)
        if (matchList.isError) {
            handleError(matchList)
            return
        }
        if (backStack.isNotEmpty()) {
            backStack.removeLast()
            locationStack.removeLast()
        }
        backStack.add(matchList)
        locationStack.add(LocationEntry(location, extra))

        if (debugLogDiagnostics) {
            log("pushReplacement($location)")
        }
    }

    // ── pop() ─────────────────────────────────────────────────────────

    fun pop(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeLast()
        locationStack.removeLast()

        if (debugLogDiagnostics) {
            log("pop() -> stack: ${locationStack.map { it.location }}")
        }
        return true
    }

    fun canPop(): Boolean = backStack.size > 1

    // ── Named route navigation ────────────────────────────────────────

    suspend fun goNamed(
        name: String,
        pathParameters: Map<String, String> = emptyMap(),
        queryParameters: Map<String, String> = emptyMap(),
        extra: Any? = null,
    ) {
        val location = namedRouteResolver.resolve(name, pathParameters, queryParameters)
        go(location, extra)
    }

    suspend fun pushNamed(
        name: String,
        pathParameters: Map<String, String> = emptyMap(),
        queryParameters: Map<String, String> = emptyMap(),
        extra: Any? = null,
    ) {
        val location = namedRouteResolver.resolve(name, pathParameters, queryParameters)
        push(location, extra)
    }

    // ── Restore from saved locations ──────────────────────────────────

    internal suspend fun restoreFromLocations(locations: List<String>) {
        locationStack.clear()
        backStack.clear()
        for (loc in locations) {
            val matchList = matcher.match(loc, null, isTransitionEnabled = false)
            val resolved = runRedirectPipeline(
                topLevelRedirect = redirect,
                matcher = matcher,
                initialMatchList = matchList,
            )
            if (!resolved.isError) {
                locationStack.add(LocationEntry(loc, null))
                backStack.add(resolved)
            }
        }
        if (debugLogDiagnostics) {
            log("restored ${backStack.size} entries: ${locationStack.map { it.location }}")
        }
    }

    internal fun getLocationStrings(): List<String> =
        locationStack.map { it.location }

    // ── Shell & tab lookup helpers ────────────────────────────────────

    /**
     * Find which ShellRoute directly owns a given JetRoute.
     */
    private fun findOwnerShell(
        searchRoutes: List<RouteBase>,
        target: JetRoute,
    ): ShellRoute? {
        for (route in searchRoutes) {
            when (route) {
                is ShellRoute -> {
                    if (containsJetRoute(route.routes, target)) {
                        return route
                    }
                }

                is JetRoute -> {
                    findOwnerShell(route.routes, target)?.let { return it }
                }

                is StatefulShellRoute -> {
                    for (branch in route.branches) {
                        findOwnerShell(branch.routes, target)?.let { return it }
                    }
                }
            }
        }
        return null
    }

    /**
     * Given a ShellRoute and a leaf JetRoute that's somewhere inside it,
     * find the top-level JetRoute (direct child of the shell) that the
     * leaf descends from. This is the "tab root".
     *
     * For example, if shell has children ["/", "/users", "/settings"]
     * and leaf is the "edit" route under /users/:id/edit,
     * this returns the "/users" JetRoute.
     */
    private fun findTabRoot(shell: ShellRoute, leaf: JetRoute): JetRoute? {
        for (route in shell.routes) {
            if (route is JetRoute) {
                if (route === leaf || containsJetRoute(route.routes, leaf)) {
                    return route
                }
            }
        }
        return null
    }

    /**
     * Check if [leaf] is a direct top-level tab route of the shell
     * (i.e., a direct child JetRoute of the shell, not a sub-route).
     */
    private fun isDirectTabRoute(shell: ShellRoute, leaf: JetRoute): Boolean {
        for (route in shell.routes) {
            if (route is JetRoute && route === leaf) {
                return true
            }
        }
        return false
    }

    private fun containsJetRoute(routes: List<RouteBase>, target: JetRoute): Boolean {
        for (route in routes) {
            when (route) {
                is JetRoute -> {
                    if (route === target) return true
                    if (containsJetRoute(route.routes, target)) return true
                }

                is ShellRoute -> {
                    if (containsJetRoute(route.routes, target)) return true
                }

                is StatefulShellRoute -> {
                    for (branch in route.branches) {
                        if (containsJetRoute(branch.routes, target)) return true
                    }
                }
            }
        }
        return false
    }

    // ── Internal helpers ──────────────────────────────────────────────

    private suspend fun resolveWithRedirects(
        location: String,
        extra: Any?,
        forceAnimationOff: Boolean = false,
    ): RouteMatchList {
        val initialMatch = matcher.match(
            location, extra,
            isTransitionEnabled = !forceAnimationOff,
        )

        return runRedirectPipeline(
            topLevelRedirect = redirect,
            matcher = matcher,
            initialMatchList = initialMatch,
        )
    }

    private fun handleError(matchList: RouteMatchList) {
        val state = matchList.toState()
        if (onException != null) {
            onException.invoke(state, this)
        } else {
            backStack.add(matchList)
            locationStack.add(LocationEntry(matchList.uri.toString(), null))
        }
        if (debugLogDiagnostics) {
            log("ERROR: ${matchList.error?.message}")
        }
    }

    private fun log(message: String) {
        println("[JetRouter] $message")
    }
}

data class LocationEntry(
    val location: String,
    val extra: Any? = null,
)

// ============================================================================
// rememberJetRouter — survives rotation
// ============================================================================

@Composable
fun rememberJetRouter(
    routes: List<RouteBase>,
    initialLocation: String = "/",
    redirect: (suspend (JetRouterState) -> String?)? = null,
    errorBuilder: (@Composable (JetRouterState) -> Unit)? = null,
    debugLogDiagnostics: Boolean = false,
    onException: ((JetRouterState, JetRouter) -> Unit)? = null,
): JetRouter {
    val savedLocations = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { it.toMutableList() },
        )
    ) {
        mutableListOf<String>()
    }

    val router = androidx.compose.runtime.remember {
        JetRouter(
            routes = routes,
            initialLocation = initialLocation,
            redirect = redirect,
            errorBuilder = errorBuilder,
            debugLogDiagnostics = debugLogDiagnostics,
            onException = onException,
        )
    }

    androidx.compose.runtime.LaunchedEffect(router) {
        if (savedLocations.isNotEmpty() && router.backStack.isEmpty()) {
            router.restoreFromLocations(savedLocations)
        } else if (router.backStack.isEmpty()) {
            router.go(initialLocation)
        }
    }

    androidx.compose.runtime.LaunchedEffect(router.locationStack.size) {
        val current = router.getLocationStrings()
        savedLocations.clear()
        savedLocations.addAll(current)
    }

    return router
}