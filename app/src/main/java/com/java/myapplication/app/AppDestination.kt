package com.java.myapplication.app

sealed class AppDestination(val route: String, val label: String) {
    data object Dashboard : AppDestination("dashboard", "概览")
    data object Project : AppDestination("project", "项目")
    data object Rewrite : AppDestination("rewrite", "加料")
    data object Models : AppDestination("models", "模型")
    data object Prompts : AppDestination("prompts", "提示词")
    data object Export : AppDestination("export", "导出")
    data object Settings : AppDestination("settings", "设置")
}

val bottomDestinations = listOf(
    AppDestination.Dashboard,
    AppDestination.Project,
    AppDestination.Rewrite,
    AppDestination.Models,
    AppDestination.Export
)