package com.java.myapplication.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

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
    var dropdownExpanded by remember { mutableStateOf(false) }
    var promptOverride by remember { mutableStateOf("") }
    var savePromptTitle by remember { mutableStateOf("") }
    var templatesExpanded by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var editingTemplateId by remember { mutableStateOf<Long?>(null) }

    val selectedTemplate = templates.firstOrNull { it.id == selectedTemplateId } ?: templates.firstOrNull()
    val effectivePrompt = promptOverride.ifBlank { selectedTemplate?.content.orEmpty() }
    val rangeSummary = buildRangeSummary(startChapter, endChapter)
    val failedJobs = LocalNovelStore.rewriteQueue.count { it.state == "失败" }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SectionTitle("加料与改写", "选择章节范围和自定义提示词，处理后可回到项目页查看加料后内容。") }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("处理范围", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("改写策略", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text("加料强度 ${(intensity * 100).toInt()}%", style = MaterialTheme.typography.titleMedium)
                        Slider(value = intensity, onValueChange = { intensity = it })
                        SettingRow("保留原剧情", keepPlot) { keepPlot = it }
                        SettingRow("保留角色名称与称呼", preserveNames) { preserveNames = it }
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { templatesExpanded = !templatesExpanded },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("提示词模板", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Icon(
                                if (templatesExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = if (templatesExpanded) "折叠" else "展开"
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            ExposedDropdownMenuBox(
                                expanded = dropdownExpanded,
                                onExpandedChange = { dropdownExpanded = !dropdownExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = selectedTemplate?.title ?: "选择模板",
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                    singleLine = true
                                )
                                ExposedDropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                                    templates.forEach { template ->
                                        DropdownMenuItem(
                                            text = { Text(template.title) },
                                            onClick = {
                                                selectedTemplateId = template.id
                                                promptOverride = ""
                                                dropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            OutlinedButton(onClick = {
                                editingTemplateId = null
                                showTemplateDialog = true
                            }) {
                                Text("管理")
                            }
                        }

                        if (templatesExpanded) {
                            HorizontalDivider()
                            Text("已保存模板", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            templates.forEach { template ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(template.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            template.content.take(60) + if (template.content.length > 60) "…" else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = {
                                            editingTemplateId = template.id
                                            showTemplateDialog = true
                                        }) {
                                            Icon(Icons.Rounded.Edit, contentDescription = "编辑", modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { LocalNovelStore.deletePrompt(template.id) }) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "删除", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    editingTemplateId = null
                                    showTemplateDialog = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Text("新增模板")
                            }
                        }
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                                    scope.launch { snackbarHostState.showSnackbar("已加入改写队列") }
                                },
                                enabled = novel != null,
                                modifier = Modifier.weight(1f)
                            ) { Text("后台处理范围") }
                            OutlinedButton(
                                onClick = {
                                    val chapter = novel?.chapters?.firstOrNull { it.index == (startChapter.toIntOrNull() ?: 1) }
                                    LocalNovelStore.enqueueRewrite(context, listOfNotNull(chapter?.id), effectivePrompt, (intensity * 100).toInt(), keepPlot, preserveNames)
                                    scope.launch { snackbarHostState.showSnackbar("已加入改写队列") }
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("长篇一致性记忆", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
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

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (showTemplateDialog) {
        val editingTemplate = editingTemplateId?.let { id -> templates.firstOrNull { it.id == id } }
        var dialogTitle by remember(editingTemplate) { mutableStateOf(editingTemplate?.title ?: "") }
        var dialogContent by remember(editingTemplate) { mutableStateOf(editingTemplate?.content ?: "") }

        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = { Text(if (editingTemplateId == null) "新建模板" else "编辑模板") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = dialogTitle,
                        onValueChange = { dialogTitle = it },
                        label = { Text("模板名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dialogContent,
                        onValueChange = { dialogContent = it },
                        label = { Text("提示词内容") },
                        minLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    LocalNovelStore.upsertPrompt(editingTemplateId, dialogTitle, dialogContent)
                    showTemplateDialog = false
                }) { Text("保存") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (editingTemplateId != null) {
                        OutlinedButton(onClick = {
                            LocalNovelStore.deletePrompt(editingTemplateId!!)
                            showTemplateDialog = false
                        }) { Text("删除") }
                    }
                    TextButton(onClick = { showTemplateDialog = false }) { Text("取消") }
                }
            }
        )
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
