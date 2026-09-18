package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.AppThemeMode
import com.example.ui.viewmodel.EditorViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val editorViewModel: EditorViewModel = viewModel()
            val uiState by editorViewModel.uiState.collectAsStateWithLifecycle()

            val isDark = when (uiState.themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            val fontPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    editorViewModel.importFont(uri)
                }
            }

            val textFilePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) editorViewModel.importTextDocument(uri)
            }

            MyApplicationTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when (uiState.currentScreen) {
                        AppScreen.HOME -> HomeScreen(
                            viewModel = editorViewModel,
                            uiState = uiState,
                            onImportFontClick = {
                                fontPickerLauncher.launch(
                                    arrayOf("font/*", "application/x-font-ttf", "application/x-font-otf", "*/*")
                                )
                            },
                            onImportTextClick = {
                                textFilePickerLauncher.launch(arrayOf("text/plain", "text/markdown", "text/*"))
                            }
                        )
                        AppScreen.EDITOR -> EditorScreen(
                            viewModel = editorViewModel
                        )
                    }
                }
            }
        }
    }
}
