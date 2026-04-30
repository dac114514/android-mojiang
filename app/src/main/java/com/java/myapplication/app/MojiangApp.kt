package com.java.myapplication.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.java.myapplication.data.LocalNovelStore
import com.java.myapplication.ui.theme.CardElevation
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
    val backStack = navController.currentBackStackEntryAsState()
    val currentRoute = backStack.value?.destination?.route

    val totalChapters by remember { derivedStateOf { LocalNovelStore.novels.sumOf { it.chapters.size } } }
    val rewrittenChapters by remember { derivedStateOf { LocalNovelStore.novels.sumOf { it.chapters.count { c -> c.rewrittenContent.isNotBlank() } } } }
    val progress by remember { derivedStateOf { if (totalChapters > 0) rewrittenChapters.toFloat() / totalChapters else 0f } }

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
                Column {
                    CenterAlignedTopAppBar(
                        title = { Text("墨匠 Rewrite") },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                    if (totalChapters > 0) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "改写进度：$rewrittenChapters / $totalChapters 章",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                    }
                }
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    NavigationBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .clip(RoundedCornerShape(34.dp)),
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        tonalElevation = CardElevation.level2
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
                composable(AppDestination.Project.route) { ProjectScreen(navController = navController) }
                composable(AppDestination.Rewrite.route) { RewriteScreen() }
                composable(AppDestination.Prompts.route) { PromptsScreen() }
                composable(AppDestination.Export.route) { ExportScreen() }
                composable(AppDestination.Settings.route) { SettingsScreen() }
                composable(
                    route = AppDestination.Reader.route,
                    arguments = listOf(navArgument("chapterId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val chapterId = backStackEntry.arguments?.getLong("chapterId") ?: return@composable
                    ReaderScreen(chapterId = chapterId, navController = navController)
                }
            }
        }
    }
}
