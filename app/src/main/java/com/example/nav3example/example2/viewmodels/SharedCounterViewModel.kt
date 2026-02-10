package com.example.nav3example.example2.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SharedCounterViewModel : ViewModel() {
    init {
        Log.d("ROCKY", "SharedCounterViewModel.init() called")
    }

    val count: StateFlow<Int>
        field = MutableStateFlow(0)

    fun increase() {
        count.value = ++count.value
    }

    override fun onCleared() {
        Log.d("ROCKY", "SharedCounterViewModel.onCleared() called")
        super.onCleared()
    }
}