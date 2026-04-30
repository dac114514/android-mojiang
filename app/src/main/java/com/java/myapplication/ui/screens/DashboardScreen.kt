package com.java.myapplication.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.java.myapplication.data.LocalNovelStore
import com.java.myapplication.ui.components.GradientHeroCard
import com.java.myapplication.ui.components.MetricCard
import com.java.myapplication.ui.components.SectionTitle
import com.java.myapplication.ui.model.DashboardStat
import com.java.myapplication.ui.theme.InkColors

@Composable
fun DashboardScreen() {
    val active = LocalNovelStore.activeNovel
    val stats = listOf(
        DashboardStat("当前项目", "${LocalNovelStore.novels.size} 个", active?.title ?: "尚未导入"),
        DashboardStat("章节总量", "${LocalNovelStore.novels.sumOf { it.chapters.size }} 章", "待处理 ${LocalNovelStore.pendingChapters()} 章 · 已加料 ${LocalNovelStore.rewrittenChapters()} 章"),
        DashboardStat("模型配置", "${LocalNovelStore.providers.size} 套", LocalNovelStore.providers.firstOrNull { it.isDefault }?.profileName ?: "未设置默认"),
        DashboardStat("导出记录", "${LocalNovelStore.exportRecords.size} 次", LocalNovelStore.exportRecords.firstOrNull()?.fileName ?: "暂无导出")
    )

    val cardColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        InkColors.success,
        InkColors.warning
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
        items(stats.indices.toList()) { index ->
            MetricCard(
                title = stats[index].title,
                value = stats[index].value,
                caption = stats[index].caption,
                accentColor = cardColors[index % cardColors.size]
            )
        }
    }
}
