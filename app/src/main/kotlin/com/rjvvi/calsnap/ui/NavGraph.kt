package com.rjvvi.calsnap.ui

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.rjvvi.calsnap.ui.screens.*
import com.rjvvi.calsnap.viewmodel.*

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Main : Screen("main")
}

@Composable
fun NavGraph() {
    val context = LocalContext.current
    val app = context.applicationContext as android.app.Application
    val prefs = remember { (app as com.rjvvi.calsnap.CalSnapApp).prefs }
    val onboardingDone by prefs.onboardingDone.collectAsStateWithLifecycle(initialValue = null)

    // Show nothing while loading
    if (onboardingDone == null) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background))
        return
    }

    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = if (onboardingDone == true) Screen.Main.route else Screen.Onboarding.route
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Main.route) {
            MainScreen()
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val navController = rememberNavController()

    var showAddSheet by remember { mutableStateOf(false) }

    val homeVm: HomeViewModel = viewModel(factory = HomeViewModel.Factory(app))
    val progressVm: ProgressViewModel = viewModel(factory = ProgressViewModel.Factory(app))
    val aiVm: AiViewModel = viewModel(factory = AiViewModel.Factory(app))
    val settingsVm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(app))
    val addVm: AddFoodViewModel = viewModel(factory = AddFoodViewModel.Factory(app))

    val tabs = listOf(
        Triple("Дом", Icons.Default.Home, "home"),
        Triple("Прогресс", Icons.Default.BarChart, "progress"),
        Triple("AI", Icons.Default.Chat, "ai"),
        Triple("Настройки", Icons.Default.Settings, "settings")
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 0.dp
            ) {
                tabs.forEach { (label, icon, route) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 11.sp) },
                        selected = currentRoute == route,
                        onClick = {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить еду")
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") { HomeScreen(homeVm, onAddClick = { showAddSheet = true }) }
            composable("progress") { ProgressScreen(progressVm) }
            composable("ai") { AiScreen(aiVm) }
            composable("settings") { SettingsScreen(settingsVm) }
        }
    }

    if (showAddSheet) {
        AddFoodSheet(
            viewModel = addVm,
            onDismiss = { showAddSheet = false },
            onAddToJournal = { result, mealType ->
                val entry = addVm.addToJournal(result, mealType)
                homeVm.addFood(entry)
                showAddSheet = false
                addVm.reset()
            }
        )
    }
}
