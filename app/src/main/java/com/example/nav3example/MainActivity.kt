package com.example.nav3example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.nav3example.datepicker.BasicSingleDatePickerExample
import com.example.nav3example.ui.theme.Nav3ExampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Nav3ExampleTheme {
                BasicSingleDatePickerExample()
            }
        }
    }
}
