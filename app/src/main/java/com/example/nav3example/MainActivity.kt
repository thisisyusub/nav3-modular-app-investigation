package com.example.nav3example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.nav3example.example1.navigation.NavigationRoot1
import com.example.nav3example.example2.navigation.NavigationRoot2
import com.example.nav3example.example3.navigation.NavigationRoot3
import com.example.nav3example.ui.theme.Nav3ExampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Nav3ExampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavigationRoot3(
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}
