package com.java.myapplication.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Settings
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
import com.java.myapplication.data.LocalNovelStore
import com.java.myapplication.ui.components.GradientHeroCard
import com.java.myapplication.ui.components.SectionTitle
import com.java.myapplication.ui.model.DashboardStat

@Composable
fun DashboardScreen() {
    val active = LocalNovelStore.activeNovel
    val stats = listOf(
        DashboardStat("当前项目", "${LocalNovelStore.novels.size} 个", active?.title ?: "尚未导入"),
        DashboardStat("章节总量", "${LocalNovelStore.novels.sumOf { it.chapters.size }} 章", "待处理 ${LocalNovelStore.pendingChapters()} 章 · 已加料 ${LocalNovelStore.rewrittenChapters()} 章"),
        DashboardStat("模型配置", "${LocalNovelStore.providers.size} 套", LocalNovelStore.providers.firstOrNull { it.isDefault }?.profileName ?: "未设置默认"),
        DashboardStat("导出记录", "${LocalNovelStore.exportRecords.size} 次", LocalNovelStore.exportRecords.firstOrNull()?.fileName ?: "暂无导出")
    )

    LazyColumn(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            GradientHeroCard(
                title = "墨匠 Rewrite",
                subtitle = "长篇小说改写与加料工作台",
                footnote = LocalNovelStore.statusMessage.value
            )
        }
        item { SectionTitle("工作概览", "真实读取本地项目、章节、模型和导出状态。") }
        items(stats) { stat -> StatCard(stat) }
        item { SectionTitle("快捷入口", "最终阶段已形成导入、分章、模板、加料、查看、导出闭环。") }
        item { FeatureRow(Icons.Rounded.AutoStories, "导入原文", "TXT 导入、编码兼容、自动分章、重启恢复") }
        item { FeatureRow(Icons.Rounded.Bolt, "开始加料", "范围处理、提示词自定义、结果可查看/清空") }
        item { FeatureRow(Icons.Rounded.Hub, "模型配置", "多配置保存、默认选择、真实连接测试与模型列表拉取") }
        item { FeatureRow(Icons.Rounded.Settings, "导出结果", "按章节范围生成 UTF-8 TXT 文件") }
    }
}

@Composable
private fun StatCard(stat: DashboardStat) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                Text(stat.title, style = MaterialTheme.typography.titleMedium)
                Text(stat.caption, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(stat.value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}