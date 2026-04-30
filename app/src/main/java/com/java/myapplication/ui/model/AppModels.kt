package com.java.myapplication.ui.model

data class DashboardStat(
    val title: String,
    val value: String,
    val caption: String
)

data class ChapterItem(
    val title: String,
    val words: String,
    val status: String
)

data class ProviderItem(
    val id: Int,
    val profileName: String,
    val providerName: String,
    val model: String,
    val endpoint: String,
    val apiKeyMasked: String,
    val connected: Boolean,
    val isDefault: Boolean = false,
    val lastSyncNote: String = "未拉取模型"
)

data class PromptTemplateItem(
    val title: String,
    val summary: String
)

data class ExportRecord(
    val fileName: String,
    val detail: String,
    val state: String
)

data class RewriteMode(
    val title: String,
    val description: String
)
