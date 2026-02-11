package com.example.nav3example.example3.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute3 : NavKey {

    @Serializable
    data object Todo : AppRoute3 {
        @Serializable
        data object TodoList : AppRoute3

        @Serializable
        data class TodoDetail(val todo: String) : AppRoute3
    }

    @Serializable
    data object Favorites : AppRoute3

    @Serializable
    data object Settings : AppRoute3
}