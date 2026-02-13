package com.example.nav3example.jetRouter

import android.net.Uri

// ============================================================================
// 5. NAMED ROUTE RESOLUTION
// ============================================================================

/**
 * Resolves named routes to their full URL paths.
 *
 * go_router lets you do: context.goNamed('profile', pathParameters: {'id': '42'})
 * This class provides the same functionality.
 */
class NamedRouteResolver(routes: List<RouteBase>) {

    // Map of name -> (full path pattern, GoRoute)
    private val namedRoutes: Map<String, Pair<String, JetRoute>> = buildMap {
        fun collect(routes: List<RouteBase>, parentPath: String) {
            for (route in routes) {
                when (route) {
                    is JetRoute -> {
                        val fullPath = if (route.path.startsWith("/")) {
                            route.path
                        } else {
                            "${parentPath.trimEnd('/')}/${route.path.trimStart('/')}"
                        }
                        if (route.name != null) {
                            if (containsKey(route.name)) {
                                throw GoRouterException(
                                    "Duplicate route name: '${route.name}'"
                                )
                            }
                            put(route.name, fullPath to route)
                        }
                        collect(route.routes, fullPath)
                    }
                    is ShellRoute -> collect(route.routes, parentPath)
                    is StatefulShellRoute -> {
                        for (branch in route.branches) {
                            collect(branch.routes, parentPath)
                        }
                    }
                }
            }
        }
        collect(routes, "")
    }

    /**
     * Resolve a named route to a full URL path.
     *
     * @param name The route name
     * @param pathParameters Values for path parameters (e.g., mapOf("id" to "42"))
     * @param queryParameters Optional query parameters
     * @return The resolved URL string
     */
    fun resolve(
        name: String,
        pathParameters: Map<String, String> = emptyMap(),
        queryParameters: Map<String, String> = emptyMap(),
    ): String {
        val (pathPattern, _) = namedRoutes[name]
            ?: throw GoRouterException("Unknown route name: '$name'")

        // Replace :param placeholders with actual values
        var resolvedPath = pathPattern
        val paramRegex = Regex(":([^/]+)")
        val requiredParams = paramRegex.findAll(pathPattern).map { it.groupValues[1] }.toSet()

        // Validate all required params are provided
        val missing = requiredParams - pathParameters.keys
        if (missing.isNotEmpty()) {
            throw GoRouterException(
                "Missing path parameters for route '$name': $missing"
            )
        }

        for ((param, value) in pathParameters) {
            resolvedPath = resolvedPath.replace(":$param", Uri.encode(value))
        }

        // Append query parameters
        if (queryParameters.isNotEmpty()) {
            val queryString = queryParameters.entries.joinToString("&") { (k, v) ->
                "${Uri.encode(k)}=${Uri.encode(v)}"
            }
            resolvedPath = "$resolvedPath?$queryString"
        }

        return resolvedPath
    }

    fun hasRoute(name: String): Boolean = name in namedRoutes
}