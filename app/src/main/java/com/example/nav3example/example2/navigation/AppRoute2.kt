package com.example.nav3example.example2.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute2 : NavKey {

    @Serializable
    data object FlowA : AppRoute2 {
        @Serializable
        data object FlowAFirstRoute : AppRoute2

        @Serializable
        data object FlowASecondRoute : AppRoute2
    }

    @Serializable
    data object FlowB : AppRoute2 {
        @Serializable
        data object FlowBFirstRoute : AppRoute2

        @Serializable
        data object FlowBSecondRoute : AppRoute2
    }
}