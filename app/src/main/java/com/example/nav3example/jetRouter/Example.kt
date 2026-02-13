package com.example.nav3example.jetRouter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// ── Simulated Auth State ──────────────────────────────────────────────

object AuthState {
    var isLoggedIn by mutableStateOf(true) // toggle to test redirect
}

// ── Router Configuration ──────────────────────────────────────────────

@Composable
fun ExampleApp() {
    // *** FIX: Use rememberJetRouter instead of remember { JetRouter(...) }
    // This saves the location stack via rememberSaveable so it survives rotation.
    val router = rememberJetRouter(
        routes = listOf(
            // ┌─────────────────────────────────────────────────┐
            // │  Login Route (outside the shell)                │
            // └─────────────────────────────────────────────────┘
            JetRoute(
                path = "/login",
                name = "login",
                builder = { state -> LoginScreen() },
            ),

            // ┌─────────────────────────────────────────────────┐
            // │  Shell Route — persistent bottom nav bar        │
            // └─────────────────────────────────────────────────┘
            ShellRoute(
                builder = { state, child ->
                    AppShell(state = state, content = child)
                },
                routes = listOf(
                    // ── Home Tab ──────────────────────────────
                    JetRoute(
                        path = "/",
                        name = "home",
                        builder = { state -> HomeScreen() },
                    ),

                    // ── Users Tab with sub-routes ────────────
                    JetRoute(
                        path = "/users",
                        name = "users",
                        builder = { state -> UsersScreen(state) },
                        routes = listOf(
                            JetRoute(
                                path = ":id",
                                name = "user-profile",
                                builder = { state -> UserProfileScreen(state) },
                                routes = listOf(
                                    JetRoute(
                                        path = "edit",
                                        name = "user-edit",
                                        builder = { state -> UserEditScreen(state) },
                                    ),
                                ),
                            ),
                        ),
                    ),

                    // ── Settings Tab ─────────────────────────
                    JetRoute(
                        path = "/settings",
                        name = "settings",
                        builder = { state -> SettingsScreen() },
                        routes = listOf(
                            JetRoute(
                                path = "notifications",
                                name = "notifications",
                                builder = { state -> NotificationsScreen() },
                            ),
                        ),
                    ),
                ),
            ),
        ),
        initialLocation = "/",

        // ── Global Redirect (Auth Guard) ──────────────────
        redirect = { state ->
            val isLoggingIn = state.matchedLocation == "/login"
            if (!AuthState.isLoggedIn && !isLoggingIn) {
                "/login"
            } else if (AuthState.isLoggedIn && isLoggingIn) {
                "/"
            } else {
                null
            }
        },

        // ── Error Page ────────────────────────────────────
        errorBuilder = { state ->
            ErrorScreen(state)
        },

        debugLogDiagnostics = true,
    )

    JetRouterDisplay(router = router)
}

// ============================================================================
// SHELL — Persistent UI wrapper
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    state: JetRouterState,
    content: @Composable () -> Unit,
) {
    val nav = rememberGoRouterNav()

    // Use the FULL path from router state to determine active tab.
    // This works for both top-level (/users) and nested (/users/42/edit).
    val currentPath = state.fullPath

    val selectedIndex = when {
        currentPath.startsWith("/settings") -> 2
        currentPath.startsWith("/users") -> 1
        else -> 0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("JetRouter + Nav3") },
                navigationIcon = {
                    if (nav.canPop()) {
                        IconButton(onClick = { nav.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedIndex == 0,
                    onClick = { nav.go("/") },
                    icon = { Icon(Icons.Default.Home, "Home") },
                    label = { Text("Home") },
                )
                NavigationBarItem(
                    selected = selectedIndex == 1,
                    onClick = { nav.go("/users") },
                    icon = { Icon(Icons.Default.Person, "Users") },
                    label = { Text("Users") },
                )
                NavigationBarItem(
                    selected = selectedIndex == 2,
                    onClick = { nav.go("/settings") },
                    icon = { Icon(Icons.Default.Settings, "Settings") },
                    label = { Text("Settings") },
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}

// ============================================================================
// SCREENS
// ============================================================================

@Composable
fun LoginScreen() {
    val nav = rememberGoRouterNav()
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Login Screen", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            AuthState.isLoggedIn = true
            nav.go("/")
        }) {
            Text("Log In")
        }
    }
}

@Composable
fun HomeScreen() {
    val nav = rememberGoRouterNav()
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Home Screen", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        Button(onClick = { nav.go("/users") }) {
            Text("Go to Users (go)")
        }
        Spacer(Modifier.height(8.dp))

        Button(onClick = { nav.push("/users/42") }) {
            Text("Push User #42 (push)")
        }
        Spacer(Modifier.height(8.dp))

        Button(onClick = {
            nav.goNamed(
                name = "user-profile",
                pathParameters = mapOf("id" to "99"),
            )
        }) {
            Text("Go to User #99 (named)")
        }
        Spacer(Modifier.height(8.dp))

        Button(onClick = { nav.go("/users?sort=name&limit=10") }) {
            Text("Users sorted by name")
        }
    }
}

@Composable
fun UsersScreen(state: JetRouterState) {
    val nav = rememberGoRouterNav()
    val sort = state.queryParam("sort")
    val limit = state.queryParam("limit")?.toIntOrNull() ?: 20

    val users = remember(sort) {
        val list = (1..50).map { "User #$it" }
        if (sort == "name") list.sorted() else list
    }.take(limit)

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Users (sort=$sort, limit=$limit)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
        )
        LazyColumn {
            items(users) { user ->
                val id = user.removePrefix("User #")
                ListItem(
                    headlineContent = { Text(user) },
                    modifier = Modifier.clickable {
                        nav.push("/users/$id")
                    },
                )
            }
        }
    }
}

@Composable
fun UserProfileScreen(state: JetRouterState) {
    val nav = rememberGoRouterNav()
    val userId = state.pathParam("id")

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "User Profile: #$userId",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text("Path: ${state.fullPath}")
        Text("All params: ${state.pathParameters}")
        Spacer(Modifier.height(16.dp))

        Button(onClick = { nav.push("/users/$userId/edit") }) {
            Text("Edit User")
        }
        Spacer(Modifier.height(8.dp))

        Button(onClick = {
            nav.push("/users/$userId", extra = mapOf("from" to "profile"))
        }) {
            Text("Refresh with extra data")
        }
        Spacer(Modifier.height(8.dp))

        Button(onClick = { nav.pop() }) {
            Text("Go Back (pop)")
        }
    }
}

@Composable
fun UserEditScreen(state: JetRouterState) {
    val nav = rememberGoRouterNav()
    val userId = state.pathParam("id")

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Edit User #$userId",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text("Full path: ${state.fullPath}")
        Spacer(Modifier.height(16.dp))

        Button(onClick = { nav.pop() }) {
            Text("Save & Back")
        }
    }
}

@Composable
fun SettingsScreen() {
    val nav = rememberGoRouterNav()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        Button(onClick = { nav.push("/settings/notifications") }) {
            Text("Notification Settings")
        }
        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            AuthState.isLoggedIn = false
            nav.go("/login")
        }) {
            Text("Log Out")
        }
    }
}

@Composable
fun NotificationsScreen() {
    val nav = rememberGoRouterNav()
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Notifications", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = { nav.pop() }) {
            Text("Back to Settings")
        }
    }
}

@Composable
fun ErrorScreen(state: JetRouterState) {
    val nav = rememberGoRouterNav()
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("404 — Page Not Found", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Could not find: ${state.uri}")
        state.error?.message?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { nav.go("/") }) {
            Text("Go Home")
        }
    }
}