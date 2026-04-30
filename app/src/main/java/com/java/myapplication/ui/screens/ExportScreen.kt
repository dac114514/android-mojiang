package com.java.myapplication.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.java.myapplication.data.LocalNovelStore
import com.java.myapplication.ui.components.ChapterRangeFields
import com.java.myapplication.ui.components.DotBadge
import com.java.myapplication.ui.components.SectionTitle
import com.java.myapplication.ui.components.buildRangeSummary

@Composable
fun ExportScreen() {
    val context = LocalContext.current
    val novel = LocalNovelStore.activeNovel
    val records = LocalNovelStore.exportRecords
    var startChapter by remember { mutableStateOf("1") }
    var endChapter by remember { mutableStateOf((novel?.chapters?.size ?: 1).toString()) }
    val rangeSummary = buildRangeSummary(startChapter, endChapter)
    val status = LocalNovelStore.statusMessage.value

    LazyColumn(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { SectionTitle("导出与交付", "真实生成 TXT 文件，优先导出加料后内容；未加料章节自动回退原文。") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("导出目标：TXT", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(novel?.let { "当前小说：${it.fileName} · ${it.chapters.size} 章" } ?: "请先在项目页导入 TXT", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ChapterRangeFields(
                        startChapter = startChapter,
                        endChapter = endChapter,
                        onStartChange = { startChapter = it.filter(Char::isDigit) },
                        onEndChange = { endChapter = it.filter(Char::isDigit) }
                    )
                    Text("当前导出范围：$rangeSummary", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        DotBadge("合并导出", MaterialTheme.colorScheme.primary)
                        DotBadge("UTF-8", MaterialTheme.colorScheme.secondary)
                        DotBadge(if (LocalNovelStore.preferRewrittenExport.value) "优先加料后" else "优先原文", MaterialTheme.colorScheme.tertiary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("优先导出加料后内容")
                        Switch(
                            checked = LocalNovelStore.preferRewrittenExport.value,
                            onCheckedChange = {
                                LocalNovelStore.preferRewrittenExport.value = it
                                LocalNovelStore.saveQuietly()
                            }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                val start = startChapter.toIntOrNull() ?: 1
                                val end = endChapter.toIntOrNull() ?: start
                                LocalNovelStore.exportRange(context, start, end, currentOnly = false)
                            },
                            enabled = novel != null,
                            modifier = Modifier.weight(1f)
                        ) { Text("导出范围") }
                        OutlinedButton(
                            onClick = {
                                val start = startChapter.toIntOrNull() ?: 1
                                LocalNovelStore.exportRange(context, start, start, currentOnly = true)
                            },
                            enabled = novel != null,
                            modifier = Modifier.weight(1f)
                        ) { Text("导出当前章") }
                    }
                    Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { SectionTitle("导出记录", "文件保存在应用外部私有目录 Android/data/.../files/exports。") }
        if (records.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text("暂无导出记录", modifier = Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(records, key = { it.path }) { record ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                            Text(record.fileName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("${record.detail} · ${record.createdAt}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(record.path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DotBadge(record.state, if (record.state == "成功") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }
    }
}
