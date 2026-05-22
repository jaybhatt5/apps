package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.database.SudokuDatabase
import com.example.data.repository.SudokuRepository
import com.example.ui.GameViewModel
import com.example.ui.GameViewModelFactory
import com.example.ui.SudokuApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Initialize Local Persistence Layer
    val database = SudokuDatabase.getDatabase(applicationContext)
    val repository = SudokuRepository(database.sudokuDao)
    
    // Inject Repository into the ViewModel via custom platform Factory patterns
    val viewModelFactory = GameViewModelFactory(repository)
    val viewModel = ViewModelProvider(this, viewModelFactory)[GameViewModel::class.java]

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        SudokuApp(viewModel = viewModel)
      }
    }
  }
}
