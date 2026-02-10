package com.example.nav3example.example2.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nav3example.example2.viewmodels.FlowASecondPageViewModel
import com.example.nav3example.example2.viewmodels.SharedCounterViewModel

@Composable
fun FlowASecondScreen(
    counterViewModel: SharedCounterViewModel,
    flowASecondPageViewModel: FlowASecondPageViewModel,
) {
    val count by counterViewModel.count.collectAsStateWithLifecycle()

    Column {
        Text(text = "Flow A Second Screen")
        Text(text = "Count: $count")
        Button(
            onClick = { counterViewModel.increase() }
        ) {
            Text(text = "Increase")
        }
    }
}