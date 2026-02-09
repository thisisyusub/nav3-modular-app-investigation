package com.example.nav3example.example1.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute : NavKey {
    @Serializable
    data object TodoList : AppRoute

    @Serializable
    data class TodoDetail(val todo: String) : AppRoute
}