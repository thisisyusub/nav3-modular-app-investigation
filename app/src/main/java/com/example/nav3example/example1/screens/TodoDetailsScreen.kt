package com.example.nav3example.example1.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nav3example.example1.viewmodels.TodoDetailViewModel

@Composable
fun TodoDetailScreen(
    modifier: Modifier = Modifier,
    todo: String,
    viewModel: TodoDetailViewModel = viewModel {
        TodoDetailViewModel(todo)
    }
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Cyan),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = todo)
    }
}