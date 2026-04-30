package com.java.myapplication.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.java.myapplication.app.AppDestination
import com.java.myapplication.data.LocalNovelStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    chapterId: Long,
    navController: NavController
) {
    val novel = LocalNovelStore.activeNovel
    val chapters = novel?.chapters?.sortedBy { it.index }.orEmpty()
    val currentIndex = chapters.indexOfFirst { it.id == chapterId }
    val chapter = chapters.getOrNull(currentIndex)
    var showRewritten by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            chapter?.let { "第 ${it.index} 章 · ${it.title}" } ?: "阅读器",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "${chapter?.wordCount ?: 0} 字",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (chapter?.rewrittenContent?.isNotBlank() == true) {
                        Row(
                            modifier = Modifier.padding(end = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = !showRewritten,
                                onClick = { showRewritten = false },
                                label = { Text("原文", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.height(32.dp)
                            )
                            FilterChip(
                                selected = showRewritten,
                                onClick = { showRewritten = true },
                                label = { Text("改文", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.height(32.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            if (currentIndex > 0) {
                                val prevChapter = chapters[currentIndex - 1]
                                navController.popBackStack()
                                navController.navigate(AppDestination.Reader.createRoute(prevChapter.id))
                            }
                        },
                        enabled = currentIndex > 0
                    ) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = null)
                        Text("上一章")
                    }

                    Text(
                        "第 ${chapter?.index ?: 0} 章",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )

                    TextButton(
                        onClick = {
                            if (currentIndex < chapters.size - 1) {
                                val nextChapter = chapters[currentIndex + 1]
                                navController.popBackStack()
                                navController.navigate(AppDestination.Reader.createRoute(nextChapter.id))
                            }
                        },
                        enabled = currentIndex < chapters.size - 1
                    ) {
                        Text("下一章")
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when {
                    showRewritten && chapter?.rewrittenContent?.isNotBlank() == true -> chapter.rewrittenContent
                    else -> chapter?.originalContent.orEmpty()
                },
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.6f
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
