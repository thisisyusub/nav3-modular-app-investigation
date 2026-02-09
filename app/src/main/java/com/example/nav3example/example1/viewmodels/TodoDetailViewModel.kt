package com.example.nav3example.example1.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel

class TodoDetailViewModel(val id: String) : ViewModel() {
    init {
        Log.d("ROCKY", "TodoDetailViewModel.init() called with: id = $id")
    }

    override fun onCleared() {
        Log.d("ROCKY", "TodoDetailViewModel.onCleared() called with: id = $id")
        super.onCleared()
    }
}