package com.example.nav3example.jetRouter

import android.net.Uri

// ============================================================================
// 3. ROUTE MATCHING
// ============================================================================

/**
 * A single matched route in the match chain.
 */
data class RouteMatch(
    val route: JetRoute,
    val matchedPath: String,
    val pathParameters: Map<String, String>,
    val fullPath: String,
)

/**
 * The full result of matching a URI against the route tree.
 */
data class RouteMatchList(
    val matches: List<RouteMatch>,
    val uri: Uri,
    val queryParameters: Map<String, String> = emptyMap(),
    val extra: Any? = null,
    val error: Exception? = null,
    val isTransitionEnabled: Boolean = true
) {
    val isEmpty get() = matches.isEmpty()
    val isNotEmpty get() = matches.isNotEmpty()
    val isError get() = error != null

    /** The last (leaf) match — this is the route that gets rendered. */
    val lastMatch: RouteMatch? get() = matches.lastOrNull()

    /** All path parameters merged from the entire match chain. */
    val allPathParameters: Map<String, String>
        get() = matches.fold(emptyMap()) { acc, m -> acc + m.pathParameters }

    /** Build a GoRouterState from this match list. */
    fun toState(): JetRouterState {
        val leaf = lastMatch
        return JetRouterState(
            uri = uri,
            path = leaf?.route?.path ?: "",
            matchedLocation = leaf?.matchedPath ?: "",
            name = leaf?.route?.name,
            pathParameters = allPathParameters,
            queryParameters = queryParameters,
            extra = extra,
            fullPath = leaf?.fullPath ?: "",
            error = error,
        )
    }
}

/**
 * The route matching engine.
 * Walks the route tree depth-first, matching URL segments against route path patterns.
 */

class RouteMatcher(private val routes: List<RouteBase>) {
    /**
     * Match a URI string against the route tree.
     * Returns a [RouteMatchList] with the chain of matches from root to leaf.
     */

    fun match(
        location: String,
        extra: Any? = null,
        isTransitionEnabled: Boolean = true
    ): RouteMatchList {
        val uri = Uri.parse(location)
        val path = uri.path?.trimEnd('/') ?: ""
        val segments = path.split("/").filter { it.isNotEmpty() }

        val queryParams = buildMap {
            uri.queryParameterNames.forEach { name ->
                uri.getQueryParameter(name)?.let { put(name, it) }
            }
        }

        val matches = mutableListOf<RouteMatch>()
        val found = matchRoutes(routes, segments, 0, "", matches)

        return if (found) {
            RouteMatchList(
                matches = matches.toList(),
                uri = uri,
                queryParameters = queryParams,
                extra = extra,
                isTransitionEnabled = isTransitionEnabled,
            )
        } else {
            RouteMatchList(
                matches = emptyList(),
                uri = uri,
                queryParameters = queryParams,
                extra = extra,
                error = GoRouterException("No match found for: $location"),
                isTransitionEnabled = isTransitionEnabled,
            )
        }
    }

    /**
     * Recursively match routes depth-first.
     *
     * @param routes Current level of routes to check
     * @param segments All URL path segments
     * @param startIndex Index into segments where we start matching
     * @param parentPath The accumulated matched path from parent routes
     * @param result Accumulator for matched routes
     * @return true if a complete match was found
     */
    private fun matchRoutes(
        routes: List<RouteBase>,
        segments: List<String>,
        startIndex: Int,
        parentPath: String,
        result: MutableList<RouteMatch>,
    ): Boolean {
        for (route in routes) {
            when (route) {
                is JetRoute -> {
                    val matchResult = matchGoRoute(route, segments, startIndex, parentPath)
                    if (matchResult != null) {
                        result.add(matchResult)
                        val consumedCount = matchResult.matchedPath
                            .trimStart('/').split("/").filter { it.isNotEmpty() }.size
                        val nextIndex = startIndex + consumedCount

                        // If we've consumed all segments, this is a leaf match
                        if (nextIndex >= segments.size) {
                            return true
                        }

                        // Try to match sub-routes
                        if (route.routes.isNotEmpty()) {
                            val fullPath = parentPath.trimEnd('/') + "/" +
                                    route.path.trimStart('/')
                            if (matchRoutes(
                                    route.routes, segments, nextIndex,
                                    fullPath, result
                                )
                            ) {
                                return true
                            }
                        }

                        // No sub-route matched; remove this partial match
                        result.removeLast()
                    }
                }

                is ShellRoute -> {
                    // Shell routes don't consume path segments; try children
                    if (matchRoutes(
                            route.routes, segments, startIndex,
                            parentPath, result
                        )
                    ) {
                        return true
                    }
                }

                is StatefulShellRoute -> {
                    // Try all branches
                    for (branch in route.branches) {
                        if (matchRoutes(
                                branch.routes, segments, startIndex,
                                parentPath, result
                            )
                        ) {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }

    /**
     * Try to match a single GoRoute against segments starting at [startIndex].
     *
     * Supports:
     *  - Literal segments: "users", "settings"
     *  - Path parameters: ":id", ":name"
     *  - Absolute paths starting with "/"
     */
    private fun matchGoRoute(
        route: JetRoute,
        segments: List<String>,
        startIndex: Int,
        parentPath: String,
    ): RouteMatch? {
        val routePath = route.path.trimStart('/')

        if (routePath.isEmpty()) {
            // Root route "/" — matches when startIndex == 0 and segments are empty
            // or when used as a catch-all at the current level
            if (startIndex == 0 && segments.isEmpty()) {
                return RouteMatch(
                    route = route,
                    matchedPath = "/",
                    pathParameters = emptyMap(),
                    fullPath = parentPath.trimEnd('/') + "/",
                )
            }
            // Also match root when we're at position 0
            if (startIndex == 0) {
                return RouteMatch(
                    route = route,
                    matchedPath = "/",
                    pathParameters = emptyMap(),
                    fullPath = parentPath.trimEnd('/') + "/",
                )
            }
            return null
        }

        val routeSegments = routePath.split("/").filter { it.isNotEmpty() }

        // Not enough segments remaining
        if (startIndex + routeSegments.size > segments.size) return null

        val pathParams = mutableMapOf<String, String>()
        val matchedParts = mutableListOf<String>()

        for (i in routeSegments.indices) {
            val routeSeg = routeSegments[i]
            val actualSeg = segments[startIndex + i]

            when {
                // Path parameter: ":id"
                routeSeg.startsWith(":") -> {
                    val paramName = routeSeg.removePrefix(":")
                    pathParams[paramName] = Uri.decode(actualSeg)
                    matchedParts.add(actualSeg)
                }
                // Wildcard
                routeSeg == "*" -> {
                    matchedParts.add(actualSeg)
                }
                // Literal match (case-insensitive)
                routeSeg.equals(actualSeg, ignoreCase = true) -> {
                    matchedParts.add(actualSeg)
                }
                // No match
                else -> return null
            }
        }

        val matchedPath = "/" + matchedParts.joinToString("/")
        return RouteMatch(
            route = route,
            matchedPath = matchedPath,
            pathParameters = pathParams,
            fullPath = parentPath.trimEnd('/') + matchedPath,
        )
    }
}

class GoRouterException(message: String, cause: Throwable? = null) :
    Exception(message, cause)