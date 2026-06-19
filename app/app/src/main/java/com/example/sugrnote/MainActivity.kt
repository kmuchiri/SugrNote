package com.example.sugrnote

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.sugrnote.data.local.AppDatabase
import com.example.sugrnote.data.repository.GlucoseRepository
import com.example.sugrnote.data.settings.SettingsRepository
import com.example.sugrnote.notification.GlucoseNotificationManager
import com.example.sugrnote.ui.navigation.AppNavHost
import com.example.sugrnote.ui.navigation.Routes
import com.example.sugrnote.ui.theme.SugrNoteTheme
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Whether granted or denied, we still try – the system silently drops
            // notifications when permission is missing.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create notification channel (idempotent)
        GlucoseNotificationManager.createChannel(this)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Observe latest entry + prefs and keep the notification in sync.
        val db = AppDatabase.getInstance(applicationContext)
        val glucoseRepo = GlucoseRepository(db.glucoseEntryDao())
        val settingsRepo = SettingsRepository(applicationContext)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    glucoseRepo.observeLatestEntry(),
                    settingsRepo.preferencesFlow
                ) { entry, prefs -> entry to prefs }
                    .collect { (entry, prefs) ->
                        if (entry != null) {
                            GlucoseNotificationManager.show(this@MainActivity, entry, prefs)
                        } else {
                            GlucoseNotificationManager.cancel(this@MainActivity)
                        }
                    }
            }
        }

        val initialRoute = if (intent?.action == "com.example.sugrnote.ACTION_ADD_ENTRY") {
            "${Routes.ENTRY}?entryId={entryId}"
        } else {
            Routes.OVERVIEW
        }

        setContent {
            val prefs by settingsRepo.preferencesFlow.collectAsState(
                initial = com.example.sugrnote.data.settings.UserPreferences()
            )
            SugrNoteTheme(
                themeMode = prefs.themeMode,
                darkThemeStyle = prefs.darkThemeStyle
            ) {
                MainScreen(initialRoute = initialRoute)
            }
        }
    }
}

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
private fun MainScreen(initialRoute: String = Routes.OVERVIEW) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = listOf(
        BottomNavItem(Routes.OVERVIEW, "Overview", Icons.Default.BarChart),
        BottomNavItem(Routes.LOGBOOK, "Logbook", Icons.AutoMirrored.Filled.List),
        BottomNavItem(Routes.YOU, "You", Icons.Default.Person)
    )

    // Only show bottom nav for top-level destinations
    val showBottomNav = currentDestination?.route in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any {
                                it.route == item.route
                            } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            startDestination = initialRoute
        )
    }
}
