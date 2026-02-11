package com.example.nav3example.example3.navigation

import androidx.navigation3.runtime.NavKey

class Navigator(val state: NavigationState) {
    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) {
            state.topLevelRoute.value = route
        } else {
            state.backStacks[state.topRoute]?.add(route)
        }
    }

    fun goBack() {
        val currentStack = state.backStacks[state.topRoute] ?: error("No stack found")
        val currentRoute = currentStack.last()

        if (currentRoute == state.topRoute) {
            state.topLevelRoute.value = state.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }
}