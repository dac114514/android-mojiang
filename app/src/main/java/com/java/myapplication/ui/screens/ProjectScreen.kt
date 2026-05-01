package com.java.myapplication.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.java.myapplication.data.LocalNovelStore
import com.java.myapplication.ui.components.DotBadge
import com.java.myapplication.ui.components.SectionTitle

@Composable
fun ProjectScreen(
    onNavigateToReader: (Long) -> Unit = {},
    onImportTxt: () -> Unit = {}
) {
    val novel = LocalNovelStore.activeNovel

    // Empty state
    if (novel == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Rounded.AutoStories,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    "尚未导入小说",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "导入 TXT 文件，自动分章",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Button(onClick = onImportTxt) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("导入 TXT 文件")
                }
            }
        }
        return
    }

    // State for search, filter, and delete dialog
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("全部") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val filteredChapters = remember(novel, searchQuery, statusFilter) {
        novel.chapters.filter { chapter ->
            val matchesSearch = searchQuery.isBlank() ||
                chapter.title.contains(searchQuery, ignoreCase = true)
            val matchesStatus = when (statusFilter) {
                "待加料" -> chapter.status == "待加料"
                "已加料" -> chapter.status == "已加料"
                else -> true
            }
            matchesSearch && matchesStatus
        }.sortedBy { it.index }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除项目") },
            text = {
                Text("确定删除「${novel.title}」吗？章节内容和改文数据将一并删除，此操作不可撤销。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        LocalNovelStore.deleteNovel(novel.id)
                        showDeleteDialog = false
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step 3: Project summary card
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = novel.fileName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "共 ${novel.chapters.size} 章 · ${novel.chapters.sumOf { it.wordCount }} 字",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "已加料 ${novel.chapters.count { it.rewrittenContent.isNotBlank() }} 章 · 待处理 ${novel.chapters.count { it.rewrittenContent.isBlank() }} 章",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("删除项目")
                    }
                }
            }
        }

        // Step 5: Search bar and filter chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索章节名…") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = statusFilter == "全部",
                        onClick = { statusFilter = "全部" },
                        label = { Text("全部") }
                    )
                    FilterChip(
                        selected = statusFilter == "待加料",
                        onClick = { statusFilter = "待加料" },
                        label = { Text("待加料") }
                    )
                    FilterChip(
                        selected = statusFilter == "已加料",
                        onClick = { statusFilter = "已加料" },
                        label = { Text("已加料") }
                    )
                }
            }
        }

        // Step 8: Section title for chapter list
        item {
            SectionTitle(
                "章节列表",
                subtitle = if (searchQuery.isNotBlank() || statusFilter != "全部")
                    "找到 ${filteredChapters.size} 章" else ""
            )
        }

        // Step 6: Chapter list
        items(filteredChapters) { chapter ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                onClick = {
                    onNavigateToReader(chapter.id)
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "第 ${chapter.index} 章 · ${chapter.title}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${chapter.wordCount} 字",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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

        // Step 7: Bottom import button
        item {
            OutlinedButton(
                onClick = onImportTxt,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Text("添加更多项目")
            }
        }
    }
}
