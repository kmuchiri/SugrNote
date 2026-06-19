package com.example.sugrnote.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.sugrnote.data.local.AppDatabase
import com.example.sugrnote.data.repository.GlucoseRepository
import com.example.sugrnote.data.settings.SettingsRepository
import com.example.sugrnote.ui.entry.EntryScreen
import com.example.sugrnote.ui.entry.EntryViewModel
import com.example.sugrnote.ui.logbook.LogbookScreen
import com.example.sugrnote.ui.logbook.LogbookViewModel
import com.example.sugrnote.ui.overview.OverviewScreen
import com.example.sugrnote.ui.overview.OverviewViewModel
import com.example.sugrnote.ui.settings.SettingsScreen
import com.example.sugrnote.ui.settings.SettingsViewModel
import com.example.sugrnote.ui.you.YouScreen
import com.example.sugrnote.ui.you.YouViewModel

object Routes {
    const val OVERVIEW = "overview"
    const val LOGBOOK = "logbook"
    const val YOU = "you"
    const val SETTINGS = "settings"
    const val ENTRY = "entry"
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val database = AppDatabase.getInstance(context)
    val glucoseRepository = GlucoseRepository(database.glucoseEntryDao())
    val settingsRepository = SettingsRepository(context)

    NavHost(
        navController = navController,
        startDestination = Routes.OVERVIEW,
        modifier = modifier
    ) {
        composable(Routes.OVERVIEW) {
            val vm: OverviewViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        OverviewViewModel(glucoseRepository, settingsRepository) as T
                }
            )
            OverviewScreen(
                viewModel = vm,
                onAddEntry = {
                    navController.navigate(Routes.ENTRY)
                }
            )
        }

        composable(Routes.LOGBOOK) {
            val vm: LogbookViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        LogbookViewModel(glucoseRepository, settingsRepository) as T
                }
            )
            LogbookScreen(
                viewModel = vm,
                onEntryClick = { entryId ->
                    navController.navigate("${Routes.ENTRY}?entryId=$entryId")
                },
                onAddEntry = {
                    navController.navigate(Routes.ENTRY)
                }
            )
        }

        composable(Routes.YOU) {
            val vm: YouViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        YouViewModel(settingsRepository) as T
                }
            )
            YouScreen(
                viewModel = vm,
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        SettingsViewModel(settingsRepository) as T
                }
            )
            SettingsScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "${Routes.ENTRY}?entryId={entryId}",
            arguments = listOf(
                navArgument("entryId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getLong("entryId") ?: -1L
            val actualEntryId = if (entryId == -1L) null else entryId

            val vm: EntryViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        EntryViewModel(
                            glucoseRepository,
                            settingsRepository,
                            actualEntryId
                        ) as T
                }
            )
            EntryScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
