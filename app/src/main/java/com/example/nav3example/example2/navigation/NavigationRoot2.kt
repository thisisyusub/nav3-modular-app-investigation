package com.example.nav3example.example2.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.nav3example.example2.navigation.graphs.FlowAGraph
import com.example.nav3example.example2.navigation.graphs.FlowBGraph

@Composable
fun NavigationRoot2(
    modifier: Modifier = Modifier,
) {
    val rootBackStack = rememberNavBackStack(AppRoute2.FlowA)

    NavDisplay(
        modifier = modifier,
        backStack = rootBackStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<AppRoute2.FlowA> {
                FlowAGraph(
                    onNavigationToBFlow = {
                        rootBackStack.remove(AppRoute2.FlowA)
                        rootBackStack.add(AppRoute2.FlowB)
                    }
                )
            }
            entry<AppRoute2.FlowB> {
                FlowBGraph()
            }
        }
    )
}