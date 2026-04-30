package com.example.reconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.reconnect.ui.navigation.NavGraph
import com.example.reconnect.ui.screens.ContactsScreen
import com.example.reconnect.ui.theme.REConnectTheme
import com.example.reconnect.ui.viewmodel.ContactsViewModel
import com.example.reconnect.ui.viewmodel.ContactsViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Grab the repository from the Application class (same as before)
        val repository = (application as REConnectApplication).repository

        setContent {
            REConnectTheme {
                val navController = rememberNavController()

                // MainActivity now has ONE job: hand the controller to NavGraph
                NavGraph(
                    navController = navController,
                    repository = repository
                )
            }
        }
    }
}