package com.example.nav3example.example4.scenes

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND

class ListDetailSceneStrategy<T : Any>(
    val windowSizeClass: WindowSizeClass,
) : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        if (!windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)) {
            return null
        }

        val detailEntry = entries
            .lastOrNull()
            ?.takeIf { it.metadata.containsKey(ListDetailScene.DETAIL_KEY) }

        val listEntry = entries
            .findLast { it.metadata.containsKey(ListDetailScene.LIST_KEY) }


        return ListDetailScene(
            list = listEntry,
            detail = detailEntry,
            key = listEntry.contentKey,
        )
    }
}