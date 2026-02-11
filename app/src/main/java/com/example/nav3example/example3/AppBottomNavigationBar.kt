package com.example.nav3example.example3

import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import com.example.nav3example.example3.navigation.TOP_LEVEL_DESTINATIONS

@Composable
fun AppBottomNavigationBar(
    modifier: Modifier = Modifier,
    selectedKey: () -> NavKey,
    onTabSelected: (NavKey) -> Unit,
) {
    BottomAppBar(modifier = modifier) {
        TOP_LEVEL_DESTINATIONS.forEach { (destination, data) ->
            NavigationBarItem(
                selected = destination == selectedKey(),
                onClick = { onTabSelected(destination as NavKey) },
                label = {
                    Text(data.title)
                },
                icon = {
                    Icon(
                        imageVector = data.icon,
                        contentDescription = data.title,
                    )
                }

            )
        }
    }
}