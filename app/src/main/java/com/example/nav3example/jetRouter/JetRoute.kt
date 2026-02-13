package com.example.nav3example.jetRouter

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable

// ============================================================================
// 1. ROUTE DEFINITIONS
// ============================================================================

sealed interface RouteBase {
    val routes: List<RouteBase>
}

/**
 * A single navigable route definition.
 *
 * @param path URL path pattern, e.g. "/users/:id" or "/settings"
 * @param name Optional named route for navigation by name
 * @param builder Composable builder that receives the matched state
 * @param redirect Optional per-route redirect logic
 * @param onExit Optional callback when leaving this route
 * @param transitionMetadata Per-route Nav3 transition metadata. Created using
 *        [routeTransition] helper. If null, global NavDisplay transitions apply.
 * @param routes Nested child routes (sub-routes)
 */
data class JetRoute(
    val path: String,
    val name: String? = null,
    val builder: (@Composable (JetRouterState) -> Unit)? = null,
    val redirect: (suspend (JetRouterState) -> String?)? = null,
    val onExit: (suspend (JetRouterState) -> Boolean)? = null,
    val transitionMetadata: Map<String, Any>? = null,
    override val routes: List<RouteBase> = emptyList(),
) : RouteBase

data class ShellRoute(
    val builder: @Composable (JetRouterState, @Composable () -> Unit) -> Unit,
    override val routes: List<RouteBase> = emptyList(),
) : RouteBase

data class StatefulShellRoute(
    val branches: List<ShellBranch>,
    val builder: @Composable (
        currentIndex: Int,
        onBranchSelected: (Int) -> Unit,
        child: @Composable () -> Unit,
    ) -> Unit,
    override val routes: List<RouteBase> = emptyList(),
) : RouteBase

data class ShellBranch(
    val routes: List<RouteBase>,
    val initialPath: String? = null,
)

// ============================================================================
// Per-route transition helpers
//
// These produce metadata maps that Nav3's NavDisplay recognizes natively.
// Usage in route definition:
//
//   JetRoute(
//       path = "/modal",
//       transitionMetadata = routeTransition(
//           enter = { slideInVertically { it } togetherWith ExitTransition.KeepUntilTransitionsFinished },
//           popExit = { EnterTransition.None togetherWith slideOutVertically { it } },
//           predictivePop = { EnterTransition.None togetherWith slideOutVertically { it } },
//       ),
//       builder = { ... },
//   )
//
// Or use the presets:
//
//   JetRoute(path = "/page", transitionMetadata = slideHorizontalTransition(), ...)
//   JetRoute(path = "/modal", transitionMetadata = slideUpTransition(), ...)
// ============================================================================

// These keys must match what NavDisplay looks for internally.
// NavDisplay.transitionSpec, NavDisplay.popTransitionSpec, NavDisplay.predictivePopTransitionSpec
// are helper functions that produce Map<String, Any> entries with these exact keys.

/**
 * Build a per-route transition metadata map.
 * Pass any combination of enter/popExit/predictivePop.
 * Null parameters inherit the global NavDisplay defaults.
 */
/* NOTE: The actual metadata keys are created using NavDisplay.transitionSpec { },
   NavDisplay.popTransitionSpec { }, and NavDisplay.predictivePopTransitionSpec { }
   which return Map<String, Any>. We combine them with the + operator.

   This helper just wraps them for convenience. See the preset functions below
   for how to use them directly with NavDisplay's API.
*/

/**
 * Preset: horizontal slide (iOS-style push/pop).
 * Use NavDisplay's own helpers to ensure key compatibility.
 */
/* Example usage — these are called in the route definition:

   import androidx.navigation3.ui.NavDisplay

   JetRoute(
       path = "/details/:id",
       transitionMetadata = NavDisplay.transitionSpec {
           slideInHorizontally { it } togetherWith slideOutHorizontally { -it / 3 }
       } + NavDisplay.popTransitionSpec {
           slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
       } + NavDisplay.predictivePopTransitionSpec {
           slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
       },
       builder = { state -> DetailsScreen(state) },
   )

   // Or for a modal-style route:
   JetRoute(
       path = "/modal",
       transitionMetadata = NavDisplay.transitionSpec {
           slideInVertically { it } togetherWith ExitTransition.KeepUntilTransitionsFinished
       } + NavDisplay.popTransitionSpec {
           EnterTransition.None togetherWith slideOutVertically { it }
       },
       builder = { state -> ModalScreen(state) },
   )
*/