package com.example.nav3example.jetRouter

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent

// ============================================================================
// 7. JetRouterDisplay — Bridges JetRouter to Nav3's NavDisplay
//
// Delegates entirely to NavDisplay which gives us:
//   - transitionSpec (forward animation)
//   - popTransitionSpec (back animation)
//   - predictivePopTransitionSpec (swipe-back gesture preview)
//   - Per-entry metadata overrides via NavDisplay.transitionSpec { } etc.
//   - Entry decorators (savedState, viewModel scoping)
//   - onBack handler (system back button/gesture + predictive back)
// ============================================================================

val LocalGoRouter = compositionLocalOf<JetRouter> {
    error("No JetRouter provided. Wrap your content in JetRouterDisplay.")
}

/**
 * The main composable that renders the router's current route via Nav3's NavDisplay.
 *
 * All three transition spec parameters have the same signature — they are lambdas
 * on AnimatedContentTransitionScope that return a ContentTransform.
 *
 * Per-route animation overrides are set via [JetRoute.transitionMetadata] using
 * NavDisplay.transitionSpec { }, NavDisplay.popTransitionSpec { }, etc.
 */

// 1. Define the "Kill Switch" metadata
// This tells Nav3 to use None for Enter, Exit, and Predictive Back
private val ImmediateTransitionMetadata =
    NavDisplay.transitionSpec { EnterTransition.None togetherWith ExitTransition.None } +
            NavDisplay.popTransitionSpec { EnterTransition.None togetherWith ExitTransition.None } +
            NavDisplay.predictivePopTransitionSpec { EnterTransition.None togetherWith ExitTransition.None }

@Composable
fun JetRouterDisplay(
    router: JetRouter,
    modifier: Modifier = Modifier,
    entryDecorators: List<NavEntryDecorator<RouteDisplayKey>> = emptyList(),
    transitionSpec: AnimatedContentTransitionScope<Scene<RouteDisplayKey>>.() -> ContentTransform = {
        slideInHorizontally(tween(300)) { it } togetherWith
                slideOutHorizontally(tween(300)) { -it / 3 }
    },

    popTransitionSpec: AnimatedContentTransitionScope<Scene<RouteDisplayKey>>.() -> ContentTransform = {
        slideInHorizontally(tween(300)) { -it } togetherWith
                slideOutHorizontally(tween(300)) { it }
    },
    predictivePopTransitionSpec:
    AnimatedContentTransitionScope<Scene<RouteDisplayKey>>.(
        @NavigationEvent.SwipeEdge Int
    ) -> ContentTransform = {
        slideInHorizontally(tween(300)) { -it } togetherWith
                slideOutHorizontally(tween(300)) { it }
    },
) {
    // Initialize with the initial location on first composition
    LaunchedEffect(router) {
        if (router.backStack.isEmpty()) {
            router.go(router.initialLocation)
        }
    }

    CompositionLocalProvider(LocalGoRouter provides router) {
        // Map our RouteMatchList backStack to a list of RouteDisplayKey
        // that NavDisplay can observe.
        val nav3BackStack: List<RouteDisplayKey> =
            router.backStack.mapIndexed { index, matchList ->
                RouteDisplayKey(index = index, matchList = matchList)
            }

        if (nav3BackStack.isEmpty()) return@CompositionLocalProvider

        NavDisplay(
            backStack = nav3BackStack,
            modifier = modifier.fillMaxSize(),
            onBack = { router.pop() },
            entryDecorators = entryDecorators,
            transitionSpec = transitionSpec,
            popTransitionSpec = popTransitionSpec,
            predictivePopTransitionSpec = predictivePopTransitionSpec,
            entryProvider = { key: RouteDisplayKey ->
                val matchList = key.matchList

                if (matchList.isError) {
                    NavEntry(key) {
                        val state = matchList.toState()
                        if (router.errorBuilder != null) {
                            router.errorBuilder.invoke(state)
                        } else {
                            DefaultErrorScreen(state)
                        }
                    }
                } else {
                    val leafMatch = matchList.lastMatch
                    val leafRoute = leafMatch?.route

                    // Per-route Nav3 transition metadata (if any).
                    // Created via NavDisplay.transitionSpec { } + NavDisplay.popTransitionSpec { } etc.
                    val metadata = leafRoute?.transitionMetadata ?: emptyMap()

                    // If the state says "No Animation", overlay the Immediate metadata
                    val finalMetadata = if (!matchList.isTransitionEnabled) {
                        metadata + ImmediateTransitionMetadata
                    } else {
                        metadata
                    }

                    NavEntry(
                        key = key,
                        metadata = finalMetadata,
                    ) {
                        val state = JetRouterState(
                            uri = matchList.uri,
                            path = leafMatch?.route?.path ?: "",
                            matchedLocation = leafMatch?.matchedPath ?: "",
                            name = leafMatch?.route?.name,
                            pathParameters = matchList.allPathParameters,
                            queryParameters = matchList.queryParameters,
                            extra = matchList.extra,
                            fullPath = leafMatch?.fullPath ?: "",
                        )

                        // Shells wrap the leaf content (stable, not animated)
                        val shells = leafRoute?.let {
                            findAncestorShells(router.routes, it)
                        } ?: emptyList()

                        RenderShellChain(shells, 0, state) {
                            leafRoute?.builder?.invoke(state)
                        }
                    }
                }
            },
        )
    }
}

// ============================================================================
// RouteDisplayKey — wrapper for NavDisplay's back stack
// ============================================================================

data class RouteDisplayKey(
    val index: Int,
    val matchList: RouteMatchList,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RouteDisplayKey) return false
        return index == other.index &&
                matchList.uri == other.matchList.uri &&
                matchList.lastMatch?.fullPath == other.matchList.lastMatch?.fullPath
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + matchList.uri.hashCode()
        result = 31 * result + (matchList.lastMatch?.fullPath?.hashCode() ?: 0)
        return result
    }
}

// ============================================================================
// Shell rendering
// ============================================================================

@Composable
private fun RenderShellChain(
    shells: List<ShellRoute>,
    index: Int,
    state: JetRouterState,
    leafContent: @Composable () -> Unit,
) {
    if (index >= shells.size) {
        leafContent()
        return
    }
    shells[index].builder(state) {
        RenderShellChain(shells, index + 1, state, leafContent)
    }
}

// ============================================================================
// Shell lookup helpers
// ============================================================================

internal fun findAncestorShells(
    routes: List<RouteBase>,
    target: JetRoute,
): List<ShellRoute> {
    val result = mutableListOf<ShellRoute>()
    findShellsRecursive(routes, target, result)
    return result
}

private fun findShellsRecursive(
    routes: List<RouteBase>,
    target: JetRoute,
    result: MutableList<ShellRoute>,
): Boolean {
    for (route in routes) {
        when (route) {
            is ShellRoute -> {
                if (containsRoute(route.routes, target)) {
                    result.add(route)
                    findShellsRecursive(route.routes, target, result)
                    return true
                }
            }

            is JetRoute -> {
                if (route === target) return true
                if (findShellsRecursive(route.routes, target, result)) return true
            }

            is StatefulShellRoute -> {
                for (branch in route.branches) {
                    if (findShellsRecursive(branch.routes, target, result)) return true
                }
            }
        }
    }
    return false
}

private fun containsRoute(routes: List<RouteBase>, target: JetRoute): Boolean {
    for (route in routes) {
        when (route) {
            is JetRoute -> {
                if (route === target) return true
                if (containsRoute(route.routes, target)) return true
            }

            is ShellRoute -> {
                if (containsRoute(route.routes, target)) return true
            }

            is StatefulShellRoute -> {
                for (branch in route.branches) {
                    if (containsRoute(branch.routes, target)) return true
                }
            }
        }
    }
    return false
}

@Composable
private fun DefaultErrorScreen(state: JetRouterState) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text("Page not found: ${state.uri}\n${state.error?.message ?: ""}")
    }
}