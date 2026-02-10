package com.example.nav3example.example2.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.nav3example.example2.viewmodels.FlowBSharedViewModel

@Composable
fun FlowBSecondScreen(
    flowBSharedViewModel: FlowBSharedViewModel,
) {
    Column {
        Text(text = "Flow B Second Screen")
    }
}