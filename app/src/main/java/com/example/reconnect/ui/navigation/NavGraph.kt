package com.example.reconnect.ui.navigation

// Create a new file called NavDestinations.kt or put this in MainActivity
// ui/navigation/NavGraph.kt
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.reconnect.data.local.REConnectRepository
import com.example.reconnect.ui.screens.ContactsScreen
import com.example.reconnect.ui.screens.Screen
import com.example.reconnect.ui.viewmodel.ContactsViewModel
import com.example.reconnect.ui.viewmodel.ContactsViewModelFactory

@Composable
fun NavGraph(
    navController: NavHostController,
    repository: REConnectRepository          // passed down from MainActivity
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Contacts
    ) {

        // ── Route 1: Contacts List
        composable(Screen.Contacts) {
            val vm: ContactsViewModel = viewModel(factory = ContactsViewModelFactory(repository))
            ContactsScreen(
                viewModel = vm,
                onNavigateToDetail = { contactId ->
                    // Push "contact_detail/42" onto the back stack
                    navController.navigate(Screen.contactDetailRoute(contactId))
                }
            )
        }

        // ── Route 2: Contact Detail  (we'll build this screen next)
        composable(
            route = Screen.ContactDetail,
            arguments = listOf(
                // Tell the nav system: contactId is a Long, not a String
                navArgument("contactId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            // Extract the contactId from the route — like req.params.id in Express
            val contactId = backStackEntry.arguments?.getLong("contactId") ?: return@composable

            // ContactDetailScreen goes here — we'll add it in the next step
        }
    }
}