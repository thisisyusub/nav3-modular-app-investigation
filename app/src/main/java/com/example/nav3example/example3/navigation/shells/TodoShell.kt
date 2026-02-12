package com.example.nav3example.example3.navigation.shells

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.nav3example.example1.screens.TodoDetailScreen
import com.example.nav3example.example1.screens.TodoListScreen
import com.example.nav3example.example3.navigation.AppRoute3
import com.example.nav3example.example4.scenes.ListDetailScene
import com.example.nav3example.example4.scenes.rememberListDetailSceneStrategy
import com.example.nav3example.example5.AppBottomSheetScene
import com.example.nav3example.example5.AppBottomSheetStrategy

@Composable
fun TodoShell(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(AppRoute3.Todo.TodoList)

//    val bottomSheetStrategy = remember {
//        AppBottomSheetStrategy<NavKey>(
//            onDismiss = {
//                backStack.removeLast()
//            },
//        )
//    }


    NavDisplay(
        modifier = modifier,
        backStack = backStack,
        sceneStrategy = rememberListDetailSceneStrategy<NavKey>(),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
//        transitionSpec = {
//            slideInHorizontally { it } togetherWith
//                    slideOutHorizontally { -it }
//        },
//        popTransitionSpec = {
//            slideInHorizontally { -it } togetherWith
//                    slideOutHorizontally { it }
//        },
//        predictivePopTransitionSpec = {
//            slideInHorizontally { -it } togetherWith
//                    slideOutHorizontally { it }
//        },
        entryProvider = entryProvider {
            entry<AppRoute3.Todo.TodoList>(
                metadata = ListDetailScene.listPane(),
            ) {
                TodoListScreen(
                    onTodoClick = {
                        backStack.add(AppRoute3.Todo.TodoDetail(it))
                    }
                )
            }
            entry<AppRoute3.Todo.TodoDetail> {
                TodoDetailScreen(todo = it.todo)
            }
        }
    )
}