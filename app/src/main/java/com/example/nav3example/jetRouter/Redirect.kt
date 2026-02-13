package com.example.nav3example.jetRouter

// ============================================================================
// 4. REDIRECTION
// ============================================================================

/**
 * Runs the redirect pipeline: top-level redirect first, then per-route redirects.
 * Mirrors go_router's redirect behavior, including loop detection.
 *
 * @param topLevelRedirect The global redirect function from GoRouter config
 * @param matcher The route matcher to re-match after a redirect
 * @param initialMatchList The initial match result before redirects
 * @param maxRedirects Maximum number of redirect hops (prevents infinite loops)
 * @return The final RouteMatchList after all redirects have been applied
 */
suspend fun runRedirectPipeline(
    topLevelRedirect: (suspend (JetRouterState) -> String?)?,
    matcher: RouteMatcher,
    initialMatchList: RouteMatchList,
    maxRedirects: Int = 10,
): RouteMatchList {
    var currentMatch = initialMatchList
    val visitedLocations = mutableSetOf<String>()
    var redirectCount = 0

    while (redirectCount < maxRedirects) {
        val state = currentMatch.toState()
        var redirectTarget: String? = null

        // 1. Run the top-level redirect
        if (topLevelRedirect != null) {
            redirectTarget = topLevelRedirect(state)
        }

        // 2. If no top-level redirect, run per-route redirect
        if (redirectTarget == null && currentMatch.lastMatch != null) {
            val routeRedirect = currentMatch.lastMatch!!.route.redirect
            if (routeRedirect != null) {
                redirectTarget = routeRedirect(state)
            }
        }

        // 3. No redirect needed — we're done
        if (redirectTarget == null) {
            return currentMatch
        }

        // 4. Loop detection
        if (redirectTarget in visitedLocations) {
            return currentMatch.copy(
                error = GoRouterException(
                    "Redirect loop detected: $redirectTarget has already been visited. " +
                            "Visited: $visitedLocations"
                )
            )
        }

        visitedLocations.add(redirectTarget)
        redirectCount++

        // 5. Re-match with the redirected location
        currentMatch = matcher.match(
            location = redirectTarget,
            extra = currentMatch.extra,
            isTransitionEnabled = currentMatch.isTransitionEnabled,
        )

        // If matching itself failed, return the error
        if (currentMatch.isError) {
            return currentMatch
        }
    }

    return currentMatch.copy(
        error = GoRouterException(
            "Too many redirects (max=$maxRedirects). Last location: ${currentMatch.uri}"
        )
    )
}