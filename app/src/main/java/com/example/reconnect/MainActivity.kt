package com.example.reconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reconnect.ui.screens.ContactsScreen
import com.example.reconnect.ui.theme.REConnectTheme
import com.example.reconnect.ui.viewmodel.ContactsViewModel
import com.example.reconnect.ui.viewmodel.ContactsViewModelFactory

class MainActivity : ComponentActivity() {
    //On app open and creation
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = (application as REConnectApplication).repository
        setContent { // Apply colour and font theme
            REConnectTheme {
                val viewModel: ContactsViewModel = viewModel(
                    factory = ContactsViewModelFactory(repository)
                )
                ContactsScreen(viewModel = viewModel)
            }
        }

        //Permission Runtime Check
    }
}
