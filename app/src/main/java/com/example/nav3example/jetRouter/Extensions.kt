package com.example.nav3example.jetRouter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// ============================================================================
// 8. EXTENSIONS
// ============================================================================

/**
 * Access the JetRouter from any Composable.
 */
@Composable
fun rememberGoRouter(): JetRouter = LocalGoRouter.current

/**
 * Convenience class providing coroutine-aware navigation helpers.
 * Use [rememberGoRouterNav] to obtain an instance.
 *
 * Usage:
 *   val nav = rememberGoRouterNav()
 *   Button(onClick = { nav.go("/details/42") }) { Text("Go") }
 */
class GoRouterNav internal constructor(
    private val router: JetRouter,
    private val scope: CoroutineScope,
) {
    /** Declarative navigation — replaces the entire stack. */
    fun go(location: String, extra: Any? = null) {
        scope.launch { router.go(location, extra) }
    }

    /** Imperative push — adds to the stack. */
    fun push(location: String, extra: Any? = null) {
        scope.launch { router.push(location, extra) }
    }

    /** Replace the current top of the stack. */
    fun pushReplacement(location: String, extra: Any? = null) {
        scope.launch { router.pushReplacement(location, extra) }
    }

    /** Pop the top of the stack. */
    fun pop(): Boolean = router.pop()

    /** Check if pop is possible. */
    fun canPop(): Boolean = router.canPop()

    /** Navigate by name — declarative. */
    fun goNamed(
        name: String,
        pathParameters: Map<String, String> = emptyMap(),
        queryParameters: Map<String, String> = emptyMap(),
        extra: Any? = null,
    ) {
        scope.launch { router.goNamed(name, pathParameters, queryParameters, extra) }
    }

    /** Navigate by name — imperative push. */
    fun pushNamed(
        name: String,
        pathParameters: Map<String, String> = emptyMap(),
        queryParameters: Map<String, String> = emptyMap(),
        extra: Any? = null,
    ) {
        scope.launch { router.pushNamed(name, pathParameters, queryParameters, extra) }
    }

    /** The current router state. */
    val currentState: JetRouterState get() = router.currentState
}

/**
 * Remember a [GoRouterNav] that wraps the nearest [GoRouter]
 * with a coroutine scope for safe async navigation.
 *
 * This is the primary API for navigating from Composables:
 *
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val nav = rememberGoRouterNav()
 *     Button(onClick = { nav.go("/home") }) { Text("Home") }
 *     Button(onClick = { nav.push("/profile/42") }) { Text("Profile") }
 *     Button(onClick = { nav.pop() }) { Text("Back") }
 *     Button(onClick = {
 *         nav.goNamed("user", pathParameters = mapOf("id" to "42"))
 *     }) { Text("User by name") }
 * }
 * ```
 */
@Composable
fun rememberGoRouterNav(): GoRouterNav {
    val router = LocalGoRouter.current
    val scope = rememberCoroutineScope()
    return GoRouterNav(router, scope)
}