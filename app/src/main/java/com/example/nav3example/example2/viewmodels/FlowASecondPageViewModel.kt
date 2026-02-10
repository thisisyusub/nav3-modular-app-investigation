package com.example.nav3example.example2.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel

class FlowASecondPageViewModel: ViewModel() {
    init {
        Log.d("ROCKY", "FlowASecondPageViewModel.init() called")
    }

    override fun onCleared() {
        Log.d("ROCKY", "FlowASecondPageViewModel.onCleared() called")
        super.onCleared()
    }
}