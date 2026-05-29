package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ExpenseViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.main.MainScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: ExpenseViewModel = viewModel()
        val currentUser by viewModel.currentUser.collectAsState()

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          if (currentUser == null) {
            AuthScreen(
              viewModel = viewModel,
              onAuthSuccess = {
                // Success is handled reactively by changing the view state
              },
              modifier = Modifier.padding(innerPadding)
            )
          } else {
            MainScreen(
              viewModel = viewModel,
              onLogout = {
                // Return to login screen reactively
              },
              modifier = Modifier.padding(innerPadding)
            )
          }
        }
      }
    }
  }
}

