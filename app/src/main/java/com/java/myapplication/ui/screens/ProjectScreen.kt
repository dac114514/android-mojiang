package com.java.myapplication.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.java.myapplication.data.ChapterData
import com.java.myapplication.data.LocalNovelStore
import com.java.myapplication.ui.components.DotBadge
import com.java.myapplication.ui.components.SectionTitle

@Composable
fun ProjectScreen() {
    val context = LocalContext.current
    val novels = LocalNovelStore.novels
    val activeNovel = LocalNovelStore.activeNovel
    val status = LocalNovelStore.statusMessage.value
    var selectedChapter by remember { mutableStateOf<ChapterData?>(null) }
    var showRewritten by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { LocalNovelStore.importTxt(context, it) }
    }

    LazyColumn(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { SectionTitle("项目工作台", "导入 TXT 后自动分章，点击章节可查看原文和加料后内容。") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = activeNovel?.let { "当前小说：${it.fileName}" } ?: "当前小说：未导入",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { filePicker.launch("*/*") }) { Text("导入 TXT") }
                        OutlinedButton(onClick = { LocalNovelStore.statusMessage.value = "已自动分章，章节列表见下方" }) { Text("自动分章") }
                    }
                    activeNovel?.let {
                        Text("共 ${it.chapters.size} 章 · ${it.chapters.sumOf { c -> c.wordCount }} 字", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (novels.size > 1) {
            item { SectionTitle("已导入小说", "点击切换当前项目") }
            items(novels) { novel ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (novel.id == LocalNovelStore.activeNovelId.value) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    onClick = { LocalNovelStore.selectNovel(novel.id) }
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(novel.title, fontWeight = FontWeight.SemiBold)
                        Text("${novel.chapters.size} 章 · ${novel.chapters.sumOf { it.wordCount }} 字 · ${novel.importedAt}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { LocalNovelStore.selectNovel(novel.id) }) { Text("切换") }
                            OutlinedButton(onClick = { LocalNovelStore.deleteNovel(novel.id) }) { Text("删除") }
                        }
                    }
                }
            }
        }
        activeNovel?.let { novel ->
            item { SectionTitle("章节列表", "点击章节查看内容；已加料章节可切换查看改写结果。") }
            items(novel.chapters) { chapter ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    onClick = {
                        selectedChapter = chapter
                        showRewritten = chapter.rewrittenContent.isNotBlank()
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                            Text("第 ${chapter.index} 章 · ${chapter.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("${chapter.wordCount} 字", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    selectedChapter = chapter
                                    showRewritten = false
                                }) { Text("看原文") }
                                OutlinedButton(
                                    onClick = {
                                        selectedChapter = chapter
                                        showRewritten = true
                                    },
                                    enabled = chapter.rewrittenContent.isNotBlank()
                                ) { Text("看加料后") }
                                OutlinedButton(
                                    onClick = { LocalNovelStore.clearRewrite(chapter.id) },
                                    enabled = chapter.rewrittenContent.isNotBlank()
                                ) { Text("清空结果") }
                            }
                        }
                        DotBadge(
                            text = chapter.status,
                            color = if (chapter.status == "已加料") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }

    selectedChapter?.let { chapter ->
        AlertDialog(
            onDismissRequest = { selectedChapter = null },
            title = { Text("第 ${chapter.index} 章 · ${chapter.title}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showRewritten = false }) { Text("原文") }
                        OutlinedButton(
                            onClick = { showRewritten = true },
                            enabled = chapter.rewrittenContent.isNotBlank()
                        ) { Text("加料后") }
                    }
                    Text(
                        text = if (showRewritten && chapter.rewrittenContent.isNotBlank()) chapter.rewrittenContent else chapter.originalContent,
                        modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = { TextButton(onClick = { selectedChapter = null }) { Text("关闭") } }
        )
    }
}
