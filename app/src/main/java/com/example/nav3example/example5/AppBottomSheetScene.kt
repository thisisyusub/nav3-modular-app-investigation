package com.example.nav3example.example5

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope


@OptIn(ExperimentalMaterial3Api::class)
class AppBottomSheetScene<T : Any>(
    override val key: T,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
    private val entry: NavEntry<T>,
    private val onDismiss: () -> Unit,
) : OverlayScene<T> {
    override val entries: List<NavEntry<T>> = listOf(entry)

    override val content: @Composable (() -> Unit) = {
        ModalBottomSheet(onDismissRequest = onDismiss) {
            entry.Content()
        }
    }

    companion object {
        fun bottomSheet(id: String) = mapOf(BOTTOM_SHEET to id)
        internal const val BOTTOM_SHEET = "bottom_sheet"
    }
}

@Suppress("UNCHECKED_CAST")
class AppBottomSheetStrategy<T : Any>(
    private val onDismiss: () -> Unit,
) : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val lastEntry = entries.lastOrNull() ?: return null
        val remainEntries = entries.dropLast(1)

        if (remainEntries.isEmpty()) {
            return null
        }


        return lastEntry.metadata.ifEmpty { null }.let {
            AppBottomSheetScene(
                key = lastEntry.contentKey as T,
                previousEntries = remainEntries,
                overlaidEntries = remainEntries,
                entry = lastEntry,
                onDismiss = onDismiss,
            )
        }
    }
}