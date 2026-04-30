package com.java.myapplication.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.java.myapplication.app.AppDestination
import com.java.myapplication.data.LocalNovelStore
import com.java.myapplication.ui.components.DotBadge
import com.java.myapplication.ui.components.SectionTitle

@Composable
fun ProjectScreen(navController: NavController? = null) {
    val activeNovel = LocalNovelStore.activeNovel

    LazyColumn(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        activeNovel?.let { novel ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "当前小说：${novel.fileName}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text("共 ${novel.chapters.size} 章 · ${novel.chapters.sumOf { c -> c.wordCount }} 字", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { SectionTitle("章节列表") }
            items(novel.chapters) { chapter ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    onClick = {
                        navController?.navigate(AppDestination.Reader.createRoute(chapter.id))
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("第 ${chapter.index} 章 · ${chapter.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("${chapter.wordCount} 字", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DotBadge(
                                text = chapter.status,
                                color = if (chapter.status == "已加料") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = "阅读",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
