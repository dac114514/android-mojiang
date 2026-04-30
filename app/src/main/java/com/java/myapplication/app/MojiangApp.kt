package com.java.myapplication.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.java.myapplication.ui.screens.DashboardScreen
import com.java.myapplication.ui.screens.ExportScreen
import com.java.myapplication.ui.screens.ProjectScreen
import com.java.myapplication.ui.screens.PromptsScreen
import com.java.myapplication.ui.screens.ReaderScreen
import com.java.myapplication.ui.screens.RewriteScreen
import com.java.myapplication.ui.screens.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MojiangApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen(
                onNavigateToReader = { chapterId ->
                    navController.navigate(AppDestination.Reader.createRoute(chapterId))
                }
            )
        }
        composable(
            route = AppDestination.Reader.route,
            arguments = listOf(navArgument("chapterId") { type = NavType.LongType })
        ) { backStackEntry ->
            val chapterId = backStackEntry.arguments?.getLong("chapterId") ?: return@composable
            ReaderScreen(chapterId = chapterId, navController = navController)
        }
        composable(AppDestination.Prompts.route) { PromptsScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    onNavigateToReader: (Long) -> Unit
) {
    var selectedTab: AppDestination by remember { mutableStateOf(AppDestination.Dashboard) }

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
            topBar = {
                SmallTopAppBar(
                    title = { Text("墨匠 Rewrite") },
                    modifier = Modifier.height(48.dp),
                    colors = TopAppBarDefaults.smallTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, bottom = 8.dp)
                        .navigationBarsPadding(),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 3.dp,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    for (destination in bottomDestinations) {
                        val icon = when (destination) {
                            AppDestination.Dashboard -> Icons.Rounded.Dashboard
                            AppDestination.Project -> Icons.Rounded.AutoStories
                            AppDestination.Rewrite -> Icons.Rounded.Bolt
                            AppDestination.Export -> Icons.Rounded.FileUpload
                            AppDestination.Settings -> Icons.Rounded.Settings
                            else -> Icons.Rounded.Dashboard
                        }
                        NavigationBarItem(
                            selected = selectedTab == destination,
                            onClick = { selectedTab = destination },
                            icon = { Icon(icon, contentDescription = destination.label, modifier = Modifier.size(24.dp)) },
                            label = { Text(destination.label, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            AnimatedContent(
                targetState = selectedTab,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 6.dp)
                    .padding(innerPadding),
                transitionSpec = {
                    val currentIndex = bottomDestinations.indexOf(initialState)
                    val targetIndex = bottomDestinations.indexOf(targetState)
                    val slideForward = targetIndex > currentIndex

                    if (slideForward) {
                        slideInHorizontally(animationSpec = tween(300)) { fullWidth -> fullWidth } +
                        fadeIn(animationSpec = tween(300)) togetherWith
                        slideOutHorizontally(animationSpec = tween(300)) { fullWidth -> -fullWidth } +
                        fadeOut(animationSpec = tween(300))
                    } else {
                        slideInHorizontally(animationSpec = tween(300)) { fullWidth -> -fullWidth } +
                        fadeIn(animationSpec = tween(300)) togetherWith
                        slideOutHorizontally(animationSpec = tween(300)) { fullWidth -> fullWidth } +
                        fadeOut(animationSpec = tween(300))
                    }
                },
                label = "TabContent"
            ) { tab ->
                when (tab) {
                    AppDestination.Dashboard -> DashboardScreen()
                    AppDestination.Project -> ProjectScreen(onNavigateToReader = onNavigateToReader)
                    AppDestination.Rewrite -> RewriteScreen()
                    AppDestination.Export -> ExportScreen()
                    AppDestination.Settings -> SettingsScreen()
                }
            }
        }
    }
}
