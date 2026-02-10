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
import com.example.nav3example.example2.screens.FlowBFirstScreen
import com.example.nav3example.example2.screens.FlowBSecondScreen
import com.example.nav3example.example2.viewmodels.FlowBSharedViewModel

@Composable
fun FlowBGraph(
    modifier: Modifier = Modifier
) {
    val backStack = rememberNavBackStack(AppRoute2.FlowB.FlowBFirstRoute)
    val sharedBViewModel = viewModel { FlowBSharedViewModel() }

    NavDisplay(
        modifier = modifier,
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<AppRoute2.FlowB.FlowBFirstRoute> {
                FlowBFirstScreen(
                    flowBSharedViewModel = sharedBViewModel,
                    onNavigateToFlowBSecondScreen = {
                        backStack.add(AppRoute2.FlowB.FlowBSecondRoute)
                    }
                )
            }
            entry<AppRoute2.FlowB.FlowBSecondRoute> {
                FlowBSecondScreen(flowBSharedViewModel = sharedBViewModel)
            }
        }
    )
}