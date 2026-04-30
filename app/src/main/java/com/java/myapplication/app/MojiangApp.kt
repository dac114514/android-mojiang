package com.java.myapplication.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.java.myapplication.ui.screens.DashboardScreen
import com.java.myapplication.ui.screens.ExportScreen
import com.java.myapplication.ui.screens.ModelsScreen
import com.java.myapplication.ui.screens.ProjectScreen
import com.java.myapplication.ui.screens.PromptsScreen
import com.java.myapplication.ui.screens.RewriteScreen
import com.java.myapplication.ui.screens.SettingsScreen

@Composable
fun MojiangApp() {
    val navController = rememberNavController()
    val backStack = navController.currentBackStackEntryAsState()
    val currentRoute = backStack.value?.destination?.route

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    NavigationBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(32.dp)),
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                        tonalElevation = 0.dp
                    ) {
                        bottomDestinations.forEach { destination ->
                            val icon = when (destination) {
                                AppDestination.Dashboard -> Icons.Rounded.Dashboard
                                AppDestination.Project -> Icons.Rounded.AutoStories
                                AppDestination.Rewrite -> Icons.Rounded.Bolt
                                AppDestination.Models -> Icons.Rounded.Psychology
                                AppDestination.Export -> Icons.Rounded.FileUpload
                                AppDestination.Prompts -> Icons.Rounded.Psychology
                                AppDestination.Settings -> Icons.Rounded.Settings
                            }
                            NavigationBarItem(
                                selected = currentRoute == destination.route,
                                onClick = {
                                    navController.navigate(destination.route) {
                                        launchSingleTop = true
                                        restoreState = true
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                    }
                                },
                                icon = { Icon(icon, contentDescription = destination.label) },
                                label = { Text(destination.label) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = AppDestination.Dashboard.route,
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(top = 6.dp)
                    .padding(innerPadding)
            ) {
                composable(AppDestination.Dashboard.route) { DashboardScreen() }
                composable(AppDestination.Project.route) { ProjectScreen() }
                composable(AppDestination.Rewrite.route) { RewriteScreen() }
                composable(AppDestination.Models.route) { ModelsScreen() }
                composable(AppDestination.Prompts.route) { PromptsScreen() }
                composable(AppDestination.Export.route) { ExportScreen() }
                composable(AppDestination.Settings.route) { SettingsScreen() }
            }
        }
    }
}
