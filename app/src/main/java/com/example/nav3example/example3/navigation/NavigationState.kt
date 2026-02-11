package com.example.nav3example.example3.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavKeySerializer

data class NavigationState(
    val startRoute: NavKey,
    val topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
    val topRoute by topLevelRoute

    val stackInUse: List<NavKey>
        get() = if (topRoute == startRoute) {
            listOf(topRoute)
        } else {
            listOf(startRoute, topRoute)
        }
}

@Composable
fun rememberNavigationState(
    startRoute: () -> NavKey,
    topLevelRoutes: () -> Set<NavKey>,
): NavigationState {
    val topLevelRoute = rememberSerializable(
        startRoute(), topLevelRoutes(),
        stateSerializer = NavKeySerializer(),
    ) {
        mutableStateOf(startRoute())
    }

    val backStacks = topLevelRoutes().associateWith {
        rememberNavBackStack(it)
    }

    return remember(startRoute(), topLevelRoute) {
        NavigationState(
            startRoute = startRoute(),
            topLevelRoute = topLevelRoute,
            backStacks = backStacks,
        )
    }
}

@Composable
fun NavigationState.toEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>,
): SnapshotStateList<NavEntry<NavKey>> {
    val decoratedEntries = backStacks.mapValues { (key, stack) ->
        val decorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
            rememberViewModelStoreNavEntryDecorator(),
        )

        rememberDecoratedNavEntries(
            backStack = stack,
            entryDecorators = decorators,
            entryProvider = entryProvider,
        )
    }

    return stackInUse
        .flatMap { decoratedEntries[it] ?: emptyList() }
        .toMutableStateList()
}
