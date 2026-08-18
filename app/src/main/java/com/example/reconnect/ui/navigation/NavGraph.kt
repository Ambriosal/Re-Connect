package com.example.reconnect.ui.navigation

// Create a new file called NavDestinations.kt or put this in MainActivity
// ui/navigation/NavGraph.kt
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.reconnect.REConnectApplication
import com.example.reconnect.data.local.REConnectRepository
import com.example.reconnect.ui.screens.CalendarScreen
import com.example.reconnect.ui.screens.ContactsScreen
import com.example.reconnect.ui.screens.HomeScreen
import com.example.reconnect.ui.screens.Screen
import com.example.reconnect.ui.screens.StatsScreen
import com.example.reconnect.ui.viewmodel.ContactDetailViewModel
import com.example.reconnect.ui.viewmodel.ContactsViewModel
import com.example.reconnect.ui.viewmodel.ContactsViewModelFactory
import com.example.reconnect.ui.viewmodel.CalendarViewModel
import com.example.reconnect.ui.viewmodel.CalendarViewModelFactory
import com.example.reconnect.ui.viewmodel.HomeViewModel
import com.example.reconnect.ui.viewmodel.HomeViewModelFactory
import com.example.reconnect.ui.viewmodel.StatsViewModel
import com.example.reconnect.ui.viewmodel.StatsViewModelFactory
import com.example.reconnect.ui.screens.ContactDetailsScreen
import com.example.reconnect.ui.screens.ContactSettingsScreen
import com.example.reconnect.ui.screens.SettingsScreen
import com.example.reconnect.ui.viewmodel.ContactDetailViewModelFactory
import com.example.reconnect.ui.viewmodel.SettingsViewModel
import com.example.reconnect.ui.viewmodel.SettingsViewModelFactory

// The four destinations that live behind the bottom nav bar — the bar only
// shows while the user is on one of these, not on detail/sub screens.
private data class BottomNavDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val bottomNavDestinations = listOf(
    BottomNavDestination(Screen.Home, "Home", Icons.Default.Home),
    BottomNavDestination(Screen.Contacts, "Contacts", Icons.Default.People),
    BottomNavDestination(Screen.Calendar, "Calendar", Icons.Default.DateRange),
    BottomNavDestination(Screen.Settings, "Settings", Icons.Default.Settings)
)

@Composable
fun NavGraph(
    navController: NavHostController,
    repository: REConnectRepository,          // passed down from MainActivity
    application: REConnectApplication,        // gives Settings access to AppPreferences + worker scheduling
    startContactId: Long? = null,             // set when launched from a reminder/follow-up notification
    startPromptInteractionId: Long? = null,
    onStartTargetConsumed: () -> Unit = {}
) {
    // Remembers which interaction (if any) should auto-open the follow-up dialog once
    // we land on ContactDetailsScreen for the deep-linked contact.
    var promptInteractionId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(startContactId, startPromptInteractionId) {
        if (startContactId != null) {
            promptInteractionId = startPromptInteractionId
            navController.navigate(Screen.contactDetailRoute(startContactId)) {
                launchSingleTop = true
            }
            onStartTargetConsumed()
        }
    }

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        bottomBar = {
            if (bottomNavDestinations.any { it.route == currentRoute }) {
                NavigationBar {
                    bottomNavDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    // Standard bottom-nav behavior: don't stack duplicate
                                    // destinations, and preserve each tab's own back stack.
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { scaffoldPadding ->

    NavHost(
        navController = navController,
        startDestination = Screen.Home,
        modifier = androidx.compose.ui.Modifier.padding(bottom = scaffoldPadding.calculateBottomPadding())
    ) {

        // ── Route 0: Home — overdue / due-this-week digest + stats widget
        composable(Screen.Home) {
            val vm: HomeViewModel = viewModel(factory = HomeViewModelFactory(repository))
            val statsVm: StatsViewModel = viewModel(
                factory = StatsViewModelFactory(repository, application.appPreferences)
            )
            HomeScreen(
                viewModel = vm,
                statsViewModel = statsVm,
                onNavigateToDetail = { contactId ->
                    navController.navigate(Screen.contactDetailRoute(contactId))
                },
                onNavigateToStats = { navController.navigate(Screen.Stats) }
            )
        }

        // ── Route 0c: Stats — full stats page reached from the Home widget
        composable(Screen.Stats) {
            val vm: StatsViewModel = viewModel(
                factory = StatsViewModelFactory(repository, application.appPreferences)
            )
            StatsScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Route 0b: Calendar
        composable(Screen.Calendar) {
            val vm: CalendarViewModel = viewModel(factory = CalendarViewModelFactory(repository))
            CalendarScreen(
                viewModel = vm,
                onNavigateToDetail = { contactId ->
                    navController.navigate(Screen.contactDetailRoute(contactId))
                }
            )
        }

        // ── Route 1: Contacts List
        composable(Screen.Contacts) {
            val vm: ContactsViewModel = viewModel(factory = ContactsViewModelFactory(repository))
            ContactsScreen(
                viewModel = vm,
                onNavigateToDetail = { contactId ->
                    // Push "contact_detail/42" onto the back stack
                    navController.navigate(Screen.contactDetailRoute(contactId))
                },
                onNavigateToSettings = {             // ← add this
                    navController.navigate(Screen.Settings)
                }
            )
        }

        // ── Route 2: Contact Detail
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

            // Only surface the follow-up prompt if we actually navigated here for this
            // contact via a deep link — otherwise leave it null for a normal visit.
            val promptForThisContact = promptInteractionId.takeIf { contactId == startContactId }

            ContactDetailsScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() },  // ← pop = go back
                onNavigateToContactSettings = {
                    navController.navigate(Screen.contactSettingsRoute(contactId))
                },
                promptInteractionId = promptForThisContact,
                onPromptConsumed = { promptInteractionId = null }
            )
        }

        // ── Route 2b: Per-contact Settings (reminder frequency, notification toggle)
        composable(
            route = Screen.ContactSettings,
            arguments = listOf(navArgument("contactId") { type = NavType.LongType })
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getLong("contactId") ?: return@composable
            val vm: ContactDetailViewModel = viewModel(
                factory = ContactDetailViewModelFactory(repository, contactId)
            )
            ContactSettingsScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        // ── Route 3: Settings
        composable(Screen.Settings) {
            val vm: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(repository, application)
            )
            SettingsScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
    }
}