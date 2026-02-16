package com.example.nav3example.jetRouter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// ============================================================================
// 8. EXTENSIONS — Navigation helpers for Composables
// ============================================================================

/**
 * Convenience wrapper that provides non-suspending navigation calls
 * from composable contexts (Button onClick, etc.)
 */
class GoRouterNav internal constructor(
    private val router: JetRouter,
    private val scope: CoroutineScope,
) {
    fun go(location: String, extra: Any? = null) {
        scope.launch { router.go(location, extra) }
    }

    fun push(location: String, extra: Any? = null) {
        scope.launch { router.push(location, extra) }
    }

    fun pushReplacement(location: String, extra: Any? = null) {
        scope.launch { router.pushReplacement(location, extra) }
    }

    fun pop(): Boolean = router.pop()

    fun canPop(): Boolean = router.canPop()

    fun goNamed(
        name: String,
        pathParameters: Map<String, String> = emptyMap(),
        queryParameters: Map<String, String> = emptyMap(),
        extra: Any? = null,
    ) {
        scope.launch { router.goNamed(name, pathParameters, queryParameters, extra) }
    }

    fun pushNamed(
        name: String,
        pathParameters: Map<String, String> = emptyMap(),
        queryParameters: Map<String, String> = emptyMap(),
        extra: Any? = null,
    ) {
        scope.launch { router.pushNamed(name, pathParameters, queryParameters, extra) }
    }
}

/**
 * Get a [GoRouterNav] from the current composition.
 * Must be called within a [JetRouterDisplay] subtree.
 */
@Composable
fun rememberGoRouterNav(): GoRouterNav {
    val router = LocalGoRouter.current
    val scope = rememberCoroutineScope()
    return remember(router, scope) {
        GoRouterNav(router, scope)
    }
}