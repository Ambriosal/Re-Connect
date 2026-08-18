package com.example.reconnect

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.reconnect.ui.navigation.NavGraph
import com.example.reconnect.ui.screens.ContactsScreen
import com.example.reconnect.ui.theme.REConnectTheme
import com.example.reconnect.ui.viewmodel.ContactsViewModel
import com.example.reconnect.ui.viewmodel.ContactsViewModelFactory
import com.example.reconnect.util.NotificationHelper

class MainActivity : ComponentActivity() {

    // Holds the pending deep-link target from a notification tap. mutableStateOf so
    // Compose recomposes NavGraph's LaunchedEffect when onNewIntent updates it while
    // the app is already running.
    private val pendingContactId = mutableStateOf<Long?>(null)
    private val pendingPromptInteractionId = mutableStateOf<Long?>(null)

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op — reminders just won't show a notification if denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Grab the repository from the Application class (same as before)
        val repository = (application as REConnectApplication).repository

        readDeepLinkExtras(intent)

        setContent {
            REConnectTheme {
                val navController = rememberNavController()

                // MainActivity now has ONE job: hand the controller to NavGraph
                NavGraph(
                    navController = navController,
                    repository = repository,
                    application = application as REConnectApplication,
                    startContactId = pendingContactId.value,
                    startPromptInteractionId = pendingPromptInteractionId.value,
                    onStartTargetConsumed = {
                        pendingContactId.value = null
                        pendingPromptInteractionId.value = null
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readDeepLinkExtras(intent)
    }

    private fun readDeepLinkExtras(intent: Intent) {
        val contactId = intent.getLongExtra(NotificationHelper.EXTRA_CONTACT_ID, -1L)
        if (contactId != -1L) {
            pendingContactId.value = contactId
            val promptId = intent.getLongExtra(NotificationHelper.EXTRA_PROMPT_INTERACTION_ID, -1L)
            pendingPromptInteractionId.value = if (promptId != -1L) promptId else null
        }
    }
}