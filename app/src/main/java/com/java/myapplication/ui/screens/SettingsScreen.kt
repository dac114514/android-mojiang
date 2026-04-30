package com.java.myapplication.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.java.myapplication.data.LocalNovelStore
import com.java.myapplication.ui.components.SectionTitle

@Composable
fun SettingsScreen() {
    var darkMode by remember { mutableStateOf(true) }
    val active = LocalNovelStore.activeNovel

    LazyColumn(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionTitle("设置", "最终版以稳定为优先：自动保存、重启恢复、导出保护、状态可见。") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("界面与体验", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    SettingSwitchRow("深色界面预设", darkMode) { darkMode = it }
                    SettingSwitchRow("自动保存本地数据", LocalNovelStore.autoSaveEnabled.value) {
                        LocalNovelStore.autoSaveEnabled.value = it
                        if (it) LocalNovelStore.saveQuietly()
                    }
                    SettingSwitchRow("导出时优先使用加料后内容", LocalNovelStore.preferRewrittenExport.value) {
                        LocalNovelStore.preferRewrittenExport.value = it
                        LocalNovelStore.saveQuietly()
                    }
                    Button(onClick = { LocalNovelStore.saveQuietly(); LocalNovelStore.statusMessage.value = "已手动保存当前数据" }) {
                        Text("立即保存")
                    }
                    Text(
                        text = "当前项目：${active?.title ?: "无"}\n项目数：${LocalNovelStore.novels.size}\n总字数：${LocalNovelStore.totalWords()}\n提示词：${LocalNovelStore.promptTemplates.size} 个\n模型配置：${LocalNovelStore.providers.size} 套\n导出记录：${LocalNovelStore.exportRecords.size} 条\n状态：${LocalNovelStore.statusMessage.value}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}