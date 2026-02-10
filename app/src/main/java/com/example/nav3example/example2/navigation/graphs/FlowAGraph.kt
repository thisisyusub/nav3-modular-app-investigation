package com.example.nav3example.example2.navigation.graphs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.nav3example.example2.navigation.AppRoute2
import com.example.nav3example.example2.screens.FlowAFirstScreen
import com.example.nav3example.example2.screens.FlowASecondScreen
import com.example.nav3example.example2.viewmodels.FlowASecondPageViewModel
import com.example.nav3example.example2.viewmodels.SharedCounterViewModel

@Composable
fun FlowAGraph(
    modifier: Modifier = Modifier,
    onNavigationToBFlow: () -> Unit,
) {
    val backStack = rememberNavBackStack(AppRoute2.FlowA.FlowAFirstRoute)
    val sharedCounterViewModel = viewModel { SharedCounterViewModel() }

    NavDisplay(
        modifier = modifier,
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<AppRoute2.FlowA.FlowAFirstRoute> {
                FlowAFirstScreen(
                    counterViewModel = sharedCounterViewModel,
                    onNavigateToFlowBScreen = onNavigationToBFlow,
                    onNavigationToASecondPage = {
                        backStack.add(AppRoute2.FlowA.FlowASecondRoute)
                    }
                )
            }
            entry<AppRoute2.FlowA.FlowASecondRoute> {
                FlowASecondScreen(
                    counterViewModel = sharedCounterViewModel,
                    flowASecondPageViewModel = viewModel { FlowASecondPageViewModel()},
                )
            }
        }
    )
}