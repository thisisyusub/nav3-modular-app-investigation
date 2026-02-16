package com.example.nav3example.jetRouter

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
 *        NavDisplay.transitionSpec { }, NavDisplay.popTransitionSpec { }, etc.
 *        If null, global NavDisplay transitions apply.
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

/**
 * A shell route that wraps child routes in a persistent UI shell (e.g., bottom nav bar).
 */
data class ShellRoute(
    val builder: @Composable (JetRouterState, @Composable () -> Unit) -> Unit,
    override val routes: List<RouteBase> = emptyList(),
) : RouteBase

/**
 * A stateful shell route that maintains separate navigation state per branch.
 */
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
// ROUTE MODULES — Multi-module route splitting
//
// Allows feature teams to define their routes independently and merge them
// at the app level. This mirrors go_router's approach where route lists
// can be composed from multiple sources.
//
// Example usage:
//
//   // In :feature-users module
//   val usersRoutes = routeModule {
//       route("/users", name = "users", builder = { UsersScreen(it) }) {
//           route(":id", name = "user-profile", builder = { UserProfileScreen(it) }) {
//               route("edit", name = "user-edit", builder = { UserEditScreen(it) })
//           }
//       }
//   }
//
//   // In :feature-settings module
//   val settingsRoutes = routeModule {
//       route("/settings", name = "settings", builder = { SettingsScreen() }) {
//           route("notifications", builder = { NotificationsScreen() })
//       }
//   }
//
//   // In :app module — merge everything
//   val router = rememberJetRouter(
//       routes = listOf(
//           JetRoute("/login", builder = { LoginScreen() }),
//           ShellRoute(
//               builder = { state, child -> AppShell(state, child) },
//               routes = listOf(
//                   JetRoute("/", name = "home", builder = { HomeScreen() }),
//               ) + usersRoutes + settingsRoutes,
//           ),
//       ),
//   )
// ============================================================================

/**
 * DSL scope for building route modules.
 */
class RouteModuleScope {
    internal val routes = mutableListOf<RouteBase>()

    fun route(
        path: String,
        name: String? = null,
        builder: (@Composable (JetRouterState) -> Unit)? = null,
        redirect: (suspend (JetRouterState) -> String?)? = null,
        transitionMetadata: Map<String, Any>? = null,
        children: RouteModuleScope.() -> Unit = {},
    ) {
        val childScope = RouteModuleScope().apply(children)
        routes.add(
            JetRoute(
                path = path,
                name = name,
                builder = builder,
                redirect = redirect,
                transitionMetadata = transitionMetadata,
                routes = childScope.routes,
            )
        )
    }

    fun shell(
        builder: @Composable (JetRouterState, @Composable () -> Unit) -> Unit,
        children: RouteModuleScope.() -> Unit,
    ) {
        val childScope = RouteModuleScope().apply(children)
        routes.add(
            ShellRoute(
                builder = builder,
                routes = childScope.routes,
            )
        )
    }

    /** Include routes from another module */
    fun include(moduleRoutes: List<RouteBase>) {
        routes.addAll(moduleRoutes)
    }
}

/**
 * Build a list of routes using the DSL.
 * Returns List<RouteBase> that can be used in JetRouter or combined with other modules.
 */
fun routeModule(block: RouteModuleScope.() -> Unit): List<RouteBase> {
    return RouteModuleScope().apply(block).routes
}