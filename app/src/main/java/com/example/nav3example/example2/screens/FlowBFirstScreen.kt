package com.example.nav3example.example2.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nav3example.example2.viewmodels.FlowBSharedViewModel

@Composable
fun FlowBFirstScreen(
    flowBSharedViewModel: FlowBSharedViewModel,
    onNavigateToFlowBSecondScreen: () -> Unit
) {
    Column {
        Text(text = "Flow B First Screen")
        Button(onClick = onNavigateToFlowBSecondScreen) {
            Text(text = "Go to Flow B Second Screen")
        }
    }
}