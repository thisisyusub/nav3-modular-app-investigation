package com.example.nav3example.example2.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel

class FlowBSharedViewModel: ViewModel() {
    init {
        Log.d("ROCKY", "FlowBSharedViewModel.init() called")
    }

    override fun onCleared() {
        Log.d("ROCKY", "FlowBSharedViewModel.onCleared() called")
        super.onCleared()
    }
}