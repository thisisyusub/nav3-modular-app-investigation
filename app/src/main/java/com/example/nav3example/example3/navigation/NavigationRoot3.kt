package com.example.nav3example.example3.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.nav3example.example3.AppBottomNavigationBar
import com.example.nav3example.example3.navigation.shells.TodoShell

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NavigationRoot3(modifier: Modifier = Modifier) {
    val navigationState = rememberNavigationState(
        startRoute = { AppRoute3.Todo },
        topLevelRoutes = { TOP_LEVEL_DESTINATIONS.keys },
    )

    val navigator = remember { Navigator(navigationState) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            AppBottomNavigationBar(
                selectedKey = { navigationState.topRoute },
                onTabSelected = { navigator.navigate(it) },
            )
        },
    ) { innerPadding ->
        NavDisplay(
            modifier = modifier
                .fillMaxSize(),
            onBack = navigator::goBack,
            entries = navigationState.toEntries(
                entryProvider {
                    entry<AppRoute3.Todo> {
                        TodoShell()
                    }
                    entry<AppRoute3.Favorites> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Favorites")
                        }
                    }
                    entry<AppRoute3.Settings> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Settings")
                        }
                    }
                },
            ),
        )
    }
}