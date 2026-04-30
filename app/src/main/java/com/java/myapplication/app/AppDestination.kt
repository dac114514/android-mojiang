package com.java.myapplication.app

sealed class AppDestination(val route: String, val label: String) {
    data object Dashboard : AppDestination("dashboard", "概览")
    data object Project : AppDestination("project", "项目")
    data object Rewrite : AppDestination("rewrite", "加料")
    data object Prompts : AppDestination("prompts", "提示词")
    data object Export : AppDestination("export", "导出")
    data object Settings : AppDestination("settings", "设置")
    data object Reader : AppDestination("reader/{chapterId}", "阅读器") {
        fun createRoute(chapterId: Long) = "reader/$chapterId"
    }
}

val bottomDestinations = listOf(
    AppDestination.Dashboard,
    AppDestination.Project,
    AppDestination.Rewrite,
    AppDestination.Export,
    AppDestination.Settings
)