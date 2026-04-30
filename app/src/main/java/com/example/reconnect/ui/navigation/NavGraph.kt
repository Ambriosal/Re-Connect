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
import com.example.reconnect.ui.viewmodel.ContactDetailViewModel
import com.example.reconnect.ui.viewmodel.ContactsViewModel
import com.example.reconnect.ui.viewmodel.ContactsViewModelFactory
import com.example.reconnect.ui.screens.ContactDetailsScreen
import com.example.reconnect.ui.viewmodel.ContactDetailViewModelFactory


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
            arguments = listOf(navArgument("contactId") { type = NavType.LongType })

        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getLong("contactId") ?: return@composable
            android.util.Log.d("NAV", "Navigating to contactId = $contactId")

            // Build the ViewModel with the factory — same pattern as ContactsScreen
            val vm: ContactDetailViewModel = viewModel(
                factory = ContactDetailViewModelFactory(repository, contactId)
            )

            ContactDetailsScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }  // ← pop = go back
            )
        }
    }
}