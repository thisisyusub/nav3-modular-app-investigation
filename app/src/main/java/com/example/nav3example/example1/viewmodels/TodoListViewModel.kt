package com.example.nav3example.example1.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TodoListViewModel : ViewModel() {
    val uiState: StateFlow<List<String>>
        field = MutableStateFlow(
            (1..100).map { "Todo $it" }
        )
}