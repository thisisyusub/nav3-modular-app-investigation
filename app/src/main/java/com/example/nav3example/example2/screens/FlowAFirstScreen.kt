package com.example.nav3example.example2.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nav3example.example2.viewmodels.SharedCounterViewModel

@Composable
fun FlowAFirstScreen(
    counterViewModel: SharedCounterViewModel,
    onNavigationToASecondPage: () -> Unit,
    onNavigateToFlowBScreen: () -> Unit
) {
    val count by counterViewModel.count.collectAsStateWithLifecycle()

    Column {
        Text(text = "Flow A First Screen")
        Text(text = "Count: $count")
        Button(
            onClick = { counterViewModel.increase() }
        ) {
            Text(text = "Increase")
        }

        Button(onClick = onNavigationToASecondPage) {
            Text("Navigate to A Flow Second Page")
        }

        Button(onClick = onNavigateToFlowBScreen) {
            Text("Navigate to Flow B")
        }
    }
}