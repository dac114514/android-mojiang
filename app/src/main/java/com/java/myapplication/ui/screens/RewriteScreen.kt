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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewriteScreen() {
    val novel = LocalNovelStore.activeNovel
    val context = LocalContext.current
    val templates = LocalNovelStore.promptTemplates
    var intensity by remember { mutableFloatStateOf(0.65f) }
    var keepPlot by remember { mutableStateOf(true) }
    var preserveNames by remember { mutableStateOf(true) }
    var startChapter by remember { mutableStateOf("1") }
    var endChapter by remember { mutableStateOf("1") }
    var selectedTemplateId by remember { mutableStateOf(templates.firstOrNull()?.id) }
    var expanded by remember { mutableStateOf(false) }
    var promptOverride by remember { mutableStateOf("") }
    var savePromptTitle by remember { mutableStateOf("") }

    val selectedTemplate = templates.firstOrNull { it.id == selectedTemplateId } ?: templates.firstOrNull()
    val effectivePrompt = promptOverride.ifBlank { selectedTemplate?.content.orEmpty() }
    val rangeSummary = buildRangeSummary(startChapter, endChapter)
    val failedJobs = LocalNovelStore.rewriteQueue.count { it.state == "失败" }

    LazyColumn(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { SectionTitle("加料与改写", "选择章节范围和自定义提示词，处理后可回到项目页查看加料后内容。") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("处理范围", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(novel?.let { "当前小说：${it.fileName} · 共 ${it.chapters.size} 章" } ?: "请先在项目页导入 TXT", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ChapterRangeFields(
                        startChapter = startChapter,
                        endChapter = endChapter,
                        onStartChange = { startChapter = it.filter(Char::isDigit) },
                        onEndChange = { endChapter = it.filter(Char::isDigit) }
                    )
                    Text("当前范围：$rangeSummary", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("改写策略", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("加料强度 ${(intensity * 100).toInt()}%", style = MaterialTheme.typography.titleMedium)
                    Slider(value = intensity, onValueChange = { intensity = it })
                    SettingRow("保留原剧情", keepPlot) { keepPlot = it }
                    SettingRow("保留角色名称与称呼", preserveNames) { preserveNames = it }

                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(
                            value = selectedTemplate?.title ?: "暂无模板",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            label = { Text("提示词模板") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            templates.forEach { template ->
                                DropdownMenuItem(
                                    text = { Text(template.title) },
                                    onClick = {
                                        selectedTemplateId = template.id
                                        promptOverride = ""
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = promptOverride.ifBlank { selectedTemplate?.content.orEmpty() },
                        onValueChange = { promptOverride = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 7,
                        label = { Text("自定义提示词：本次会按这里的内容执行") },
                        supportingText = { Text("可直接改；留空则使用上方模板。建议写清：加料方向、保留内容、禁改规则、输出格式。") }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = savePromptTitle,
                            onValueChange = { savePromptTitle = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text("保存为模板名称") }
                        )
                        OutlinedButton(
                            onClick = {
                                val prompt = promptOverride.ifBlank { selectedTemplate?.content.orEmpty() }
                                LocalNovelStore.upsertPrompt(null, savePromptTitle.ifBlank { "自定义模板" }, prompt)
                                selectedTemplateId = LocalNovelStore.promptTemplates.firstOrNull()?.id
                            LocalNovelStore.saveQuietly()
                                savePromptTitle = ""
                                LocalNovelStore.statusMessage.value = "已保存自定义提示词模板"
                            },
                            enabled = effectivePrompt.isNotBlank()
                        ) { Text("保存模板") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                val start = startChapter.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                val end = endChapter.toIntOrNull()?.coerceAtLeast(start) ?: start
                                val chapters = novel?.chapters?.filter { it.index in start..end }.orEmpty()
                                LocalNovelStore.enqueueRewrite(context, chapters.map { it.id }, effectivePrompt, (intensity * 100).toInt(), keepPlot, preserveNames)
                            },
                            enabled = novel != null,
                            modifier = Modifier.weight(1f)
                        ) { Text("后台处理范围") }
                        OutlinedButton(
                            onClick = {
                                val chapter = novel?.chapters?.firstOrNull { it.index == (startChapter.toIntOrNull() ?: 1) }
                                LocalNovelStore.enqueueRewrite(context, listOfNotNull(chapter?.id), effectivePrompt, (intensity * 100).toInt(), keepPlot, preserveNames)
                            },
                            enabled = novel != null,
                            modifier = Modifier.weight(1f)
                        ) { Text("真实改写首章") }
                    }
                    Text(LocalNovelStore.statusMessage.value, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("后台队列：剩余 ${LocalNovelStore.queuedJobs()} · 已完成 ${LocalNovelStore.completedJobs()} · 失败 $failedJobs", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { LocalNovelStore.retryFailedJobs(context) }, modifier = Modifier.weight(1f)) { Text("重试失败") }
                        OutlinedButton(onClick = { LocalNovelStore.clearFinishedJobs() }, modifier = Modifier.weight(1f)) { Text("清理完成") }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("长篇一致性记忆", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(LocalNovelStore.longMemorySummary.value, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        novel?.let {
            item { SectionTitle("处理状态", "已加料章节可以在项目页点开查看。") }
            items(it.chapters) { chapter ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("第 ${chapter.index} 章 · ${chapter.title}", modifier = Modifier.weight(1f))
                        DotBadge(chapter.status, if (chapter.status == "已加料") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}