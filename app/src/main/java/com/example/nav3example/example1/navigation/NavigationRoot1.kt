package com.example.nav3example.example1.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.nav3example.example1.screens.TodoDetailScreen
import com.example.nav3example.example1.screens.TodoListScreen


@Composable
fun NavigationRoot1(
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(AppRoute.TodoList)

    NavDisplay(
        modifier = modifier,
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<AppRoute.TodoList> {
                TodoListScreen(
                    onTodoClick = {
                        backStack.add(AppRoute.TodoDetail(it))
                    }
                )
            }
            entry<AppRoute.TodoDetail> {
                TodoDetailScreen(todo = it.todo)
            }
        }
    )
}
