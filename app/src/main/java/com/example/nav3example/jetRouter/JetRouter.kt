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
    val navigationLogger: NavigationLogger? = null,
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

    val currentLocation: String?
        get() = locationStack.lastOrNull()?.location

    // ── go() — Declarative navigation ─────────────────────────────────

    suspend fun go(location: String, extra: Any? = null) {
        val previousLocation = currentLocation
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
            val currentTabRoot = currentLeaf?.let {
                findTabRoot(currentShell!!, it)
            }

            if (currentTabRoot != null) {
                while (backStack.size > 1) {
                    val topLeaf = backStack.last().lastMatch?.route ?: break
                    val topTabRoot = findTabRoot(currentShell!!, topLeaf)

                    if (topTabRoot === currentTabRoot) {
                        val topIsTabRoot = isDirectTabRoute(currentShell, topLeaf)
                        if (topIsTabRoot) {
                            break
                        } else {
                            backStack.removeLast()
                            locationStack.removeLast()
                        }
                    } else {
                        break
                    }
                }
            }

            backStack.add(matchList)
            locationStack.add(LocationEntry(location, extra))
        } else {
            backStack.clear()
            locationStack.clear()
            backStack.add(matchList)
            locationStack.add(LocationEntry(location, extra))
        }

        logEvent(NavigationType.GO, location, previousLocation)
    }

    // ── push() — Imperative navigation ────────────────────────────────

    suspend fun push(location: String, extra: Any? = null) {
        val previousLocation = currentLocation
        val matchList = resolveWithRedirects(location, extra)
        if (matchList.isError) {
            handleError(matchList)
            return
        }
        backStack.add(matchList)
        locationStack.add(LocationEntry(location, extra))

        logEvent(NavigationType.PUSH, location, previousLocation)
    }

    // ── pushReplacement() ─────────────────────────────────────────────

    suspend fun pushReplacement(location: String, extra: Any? = null) {
        val previousLocation = currentLocation
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

        logEvent(NavigationType.PUSH_REPLACEMENT, location, previousLocation)
    }

    // ── pop() ─────────────────────────────────────────────────────────

    fun pop(): Boolean {
        if (backStack.size <= 1) return false
        val poppedLocation = currentLocation
        backStack.removeLast()
        locationStack.removeLast()

        logEvent(NavigationType.POP, currentLocation ?: "/", poppedLocation)
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
        logEvent(NavigationType.RESTORE, currentLocation ?: initialLocation, null)
    }

    internal fun getLocationStrings(): List<String> =
        locationStack.map { it.location }

    // ── Shell & tab lookup helpers ────────────────────────────────────

    private fun findOwnerShell(
        searchRoutes: List<RouteBase>,
        target: JetRoute,
    ): ShellRoute? {
        for (route in searchRoutes) {
            when (route) {
                is ShellRoute -> {
                    if (containsJetRoute(route.routes, target)) return route
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

    private fun isDirectTabRoute(shell: ShellRoute, leaf: JetRoute): Boolean {
        for (route in shell.routes) {
            if (route is JetRoute && route === leaf) return true
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

        val resolved = runRedirectPipeline(
            topLevelRedirect = redirect,
            matcher = matcher,
            initialMatchList = initialMatch,
        )

        // Log redirect if the resolved location differs from the original
        if (resolved.uri.toString() != initialMatch.uri.toString() && !resolved.isError) {
            logEvent(NavigationType.REDIRECT, resolved.uri.toString(), location)
        }

        return resolved
    }

    private fun handleError(matchList: RouteMatchList) {
        val state = matchList.toState()
        if (onException != null) {
            onException.invoke(state, this)
        } else {
            backStack.add(matchList)
            locationStack.add(LocationEntry(matchList.uri.toString(), null))
        }
        logEvent(NavigationType.ERROR, matchList.uri.toString(), null)
    }

    private fun logEvent(
        type: NavigationType,
        location: String,
        previousLocation: String?,
    ) {
        val event = NavigationLogEvent(
            type = type,
            location = location,
            previousLocation = previousLocation,
            stackDepth = backStack.size,
        )

        // Always log to stdout when debugLogDiagnostics is on
        if (debugLogDiagnostics) {
            println("[JetRouter] $event | stack: ${locationStack.map { it.location }}")
        }

        // Call the developer's custom logger
        navigationLogger?.onNavigate(event)
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
    navigationLogger: NavigationLogger? = null,
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
            navigationLogger = navigationLogger,
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