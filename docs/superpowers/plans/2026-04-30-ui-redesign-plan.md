# 墨匠 Rewrite UI 重构实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use subagent-driven-development (recommended) or executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标：** 对墨匠 Rewrite 的 Jetpack Compose UI 进行视觉重构，核心改造为悬浮式底部导航栏，增加小说阅读器、全局进度条，全面提升视觉层次和交互体验。

**架构：** 纯 UI 层改造，不涉及业务逻辑和数据层变更。底部导航栏重组为：概览 → 项目 → 加料 → 导出 → 设置（模型配置合并入设置）。

**技术栈：** Jetpack Compose, Material 3, Kotlin, Navigation Compose

**设计原则：**
- 修改只涉及 `ui/` 包和 `app/` 包中的 UI 代码，不触碰 `data/`、`network/`、`worker/`
- 保持所有现有功能完整，不改变业务逻辑
- 色彩的运用需要匹配 `Color.kt` 中已定义但未被充分利用的调色板

---

## 文件变更一览

| 文件 | 操作 | 说明 |
|------|------|------|
| `ui/theme/Color.kt` | 修改 | 增加语义色值的便捷引用 |
| `ui/theme/Theme.kt` | 修改 | 增加 elevation 层级常量 |
| `app/MojiangApp.kt` | 修改 | 重写底部栏为悬浮式；底部栏改为 5 项（设置替代模型）；增加全局进度条；添加阅读器路由 |
| `app/AppDestination.kt` | 修改 | 增加 Reader 路由；`bottomDestinations` 改为 概览/项目/加料/导出/设置 |
| `ui/components/AppComponents.kt` | 修改 | 增加 MetricCard、StatusBadge、LoadingOverlay 等共享组件 |
| `ui/screens/DashboardScreen.kt` | 修改 | 重写布局，**删除快捷入口板块**，仅保留指标卡片 |
| `ui/screens/ProjectScreen.kt` | 修改 | **删除"自动分章"按钮**；增加章节搜索/筛选；简化章节列表项；点击章节跳转阅读器 |
| `ui/screens/RewriteScreen.kt` | 修改 | 重新组织表单分组，增加操作反馈 |
| `ui/screens/ModelsScreen.kt` | 删除 | 内容合并到 SettingsScreen |
| `ui/screens/ExportScreen.kt` | 修改 | **删除合并导出/UTF-8/优先加料后标签**，默认导出改写后内容 |
| `ui/screens/SettingsScreen.kt` | 修改 | 整合模型配置；增加设置分组；修复暗色模式 |
| `ui/screens/ReaderScreen.kt` | **新建** | 小说阅读器界面，支持章节切换、原文/改写切换 |

---

### Task 1: 设计系统增强 — 主题与色彩

**目标：** 在已有 `Color.kt` 调色板基础上，建立清晰的语义色使用规范和 Card 层级体系。

**Files:**
- Modify: `ui/theme/Color.kt`
- Modify: `ui/theme/Theme.kt`

- [ ] **Step 1: Color.kt — 增加语义色引用对象**

```kotlin
object InkColors {
    val success get() = Color(0xFF20C997)
    val warning get() = Color(0xFFFFB020)
    val error get() = Color(0xFFE74C3C)
    val accent get() = Color(0xFFFF7D5C)
}
```

- [ ] **Step 2: Theme.kt — 增加 Card 层级 elevation 常量**

```kotlin
object CardElevation {
    val level0 get() = 0.dp
    val level1 get() = 2.dp
    val level2 get() = 6.dp
    val level3 get() = 12.dp
}
```

---

### Task 2: 导航架构重构 — 底部栏重组 + 新路由

**目标：** 底部栏改为 概览/项目/加料/导出/设置；新增阅读器路由。

**Files:**
- Modify: `app/AppDestination.kt`
- Modify: `app/MojiangApp.kt`

- [ ] **Step 1: AppDestination.kt — 增加 Reader 路由，更新底部栏列表**

```kotlin
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
```

- [ ] **Step 2: MojiangApp.kt — 为底部栏添加 Settings 图标**

```kotlin
// 底部导航栏逻辑中更新图标映射
val icon = when (destination) {
    AppDestination.Dashboard -> Icons.Rounded.Dashboard
    AppDestination.Project -> Icons.Rounded.AutoStories
    AppDestination.Rewrite -> Icons.Rounded.Bolt
    AppDestination.Export -> Icons.Rounded.FileUpload
    AppDestination.Settings -> Icons.Rounded.Settings
    else -> Icons.Rounded.Dashboard
}
```

- [ ] **Step 3: MojiangApp.kt — NavHost 中添加 Reader 路由**

```kotlin
composable(
    route = AppDestination.Reader.route,
    arguments = listOf(navArgument("chapterId") { type = NavType.LongType })
) { backStackEntry ->
    val chapterId = backStackEntry.arguments?.getLong("chapterId") ?: return@composable
    ReaderScreen(chapterId = chapterId, navController = navController)
}
```

---

### Task 3: 悬浮式底部导航栏

**目标：** 将标准 Material 3 `NavigationBar` 替换为胶囊形悬浮导航栏。

**Files:**
- Modify: `app/MojiangApp.kt`

- [ ] **Step 1: 重写底部栏为悬浮式**

```kotlin
bottomBar = {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(34.dp)),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = CardElevation.level2,
            shadowElevation = 8.dp
        ) {
            bottomDestinations.forEach { destination ->
                NavigationBarItem(
                    selected = currentRoute == destination.route,
                    onClick = {
                        navController.navigate(destination.route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                        }
                    },
                    icon = { Icon(icon, contentDescription = destination.label, modifier = Modifier.size(24.dp)) },
                    label = { Text(destination.label, style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
```

---

### Task 4: 全局改写进度条

**目标：** 在 TopAppBar 下方增加一条全局进度条，实时显示改写完成进度。

**Files:**
- Modify: `app/MojiangApp.kt`

- [ ] **Step 1: 计算改写进度**

```kotlin
// 在 MojiangApp 中
val totalChapters = LocalNovelStore.novels.sumOf { it.chapters.size }
val rewrittenChapters = LocalNovelStore.novels.sumOf { it.chapters.count { c -> c.rewrittenContent.isNotBlank() } }
val progress = if (totalChapters > 0) rewrittenChapters.toFloat() / totalChapters else 0f
```

- [ ] **Step 2: 在 Scaffold 的 topBar 中添加进度条**

```kotlin
topBar = {
    Column {
        // 现有的 TopAppBar 内容
        CenterAlignedTopAppBar(
            title = { ... },
            navigationIcon = { ... },
            actions = { ... },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
        )
        // 全局进度条
        if (totalChapters > 0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "改写进度：$rewrittenChapters / $totalChapters 章",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}
```

---

### Task 5: 共享组件体系增强

**目标：** 创建可复用的 UI 组件，统一各屏幕视觉风格。

**Files:**
- Modify: `ui/components/AppComponents.kt`

- [ ] **Step 1: 增加 `MetricCard`**

```kotlin
@Composable
fun MetricCard(
    title: String, value: String, caption: String,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation.level1)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = accentColor)
            Text(caption, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
```

- [ ] **Step 2: 增加 `StatusBadge`**

```kotlin
@Composable
fun StatusBadge(text: String, type: StatusType = StatusType.INFO) {
    val (bgColor, textColor) = when (type) {
        StatusType.SUCCESS -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        StatusType.WARNING -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onPrimary
        StatusType.ERROR -> Color(0xFFE74C3C) to Color.White
        StatusType.INFO -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = RoundedCornerShape(100.dp), color = bgColor) {
        Text(text = text, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}
enum class StatusType { SUCCESS, WARNING, ERROR, INFO }
```

- [ ] **Step 3: 增加 `LoadingOverlay`**

```kotlin
@Composable
fun LoadingOverlay(isLoading: Boolean, message: String = "处理中…") {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(elevation = CardDefaults.cardElevation(defaultElevation = CardElevation.level2), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text(message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
```

- [ ] **Step 4: 优化 `GradientHeroCard`**

调整内边距和阴影，突出视觉层次（代码同原 Task 4 Step 4）。

---

### Task 6: DashboardScreen — 删除"快捷入口"，仅保留概览卡片

**目标：** Dashboard 只显示 Hero 卡片和 4 个指标卡片，删除 FeatureRow/InfoRow 板块。

**Files:**
- Modify: `ui/screens/DashboardScreen.kt`

- [ ] **Step 1: 删除快捷入口板块**

```kotlin
// 删除以下代码块（约原文件第 53-57 行）
// item { SectionTitle("快捷入口", ...) }
// item { FeatureRow(...) }
// item { FeatureRow(...) }
// item { FeatureRow(...) }
// item { FeatureRow(...) }
```

- [ ] **Step 2: 替换 StatCard 为带颜色标识的 MetricCard**

```kotlin
// 使用 MetricCard 替代 StatCard
val cardColors = listOf(
    MaterialTheme.colorScheme.primary,
    MaterialTheme.colorScheme.tertiary,
    InkColors.success,
    InkColors.warning
)
items(stats.indices.toList()) { index ->
    MetricCard(
        title = stats[index].title,
        value = stats[index].value,
        caption = stats[index].caption,
        accentColor = cardColors[index % cardColors.size]
    )
}
```

最终 DashboardScreen 只包含：GradientHeroCard + SectionTitle("工作概览") + 4 个 MetricCard。

---

### Task 7: ProjectScreen — 删除自动分章 + 增加章节搜索 + 跳转阅读器

**目标：** 删除"自动分章"按钮；简化章节列表；增加章节搜索/筛选；点击章节跳转 ReaderScreen。

**Files:**
- Modify: `ui/screens/ProjectScreen.kt`

- [ ] **Step 1: 删除"自动分章"按钮和状态文本**

当前项目卡片中，删除：
```kotlin
// 删除
OutlinedButton(onClick = { LocalNovelStore.statusMessage.value = "已自动分章..." }) { Text("自动分章") }
// 以及独立的 Text(status, ...) 行
```

仅保留：当前小说名称、章节数字数统计、"导入 TXT" 按钮。

- [ ] **Step 2: 增加章节搜索/筛选栏**

在章节列表 SectionTitle 下方添加搜索框：

```kotlin
var searchQuery by remember { mutableStateOf("") }

// 在章节列表之前
item {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "搜索") },
        placeholder = { Text("按章节标题或编号筛选") },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

// 过滤章节列表
val filteredChapters = remember(novel, searchQuery) {
    novel?.chapters?.filter { chapter ->
        searchQuery.isBlank() ||
        chapter.title.contains(searchQuery, ignoreCase = true) ||
        chapter.index.toString().contains(searchQuery)
    }.orEmpty()
}
```

- [ ] **Step 3: 简化章节列表项**

每项简化为：标题 + 状态 Badge + 右箭头，点击跳转阅读器：

```kotlin
items(filteredChapters) { chapter ->
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = {
            navController.navigate(AppDestination.Reader.createRoute(chapter.id))
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("第 ${chapter.index} 章 · ${chapter.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${chapter.wordCount} 字", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DotBadge(
                    text = chapter.status,
                    color = if (chapter.status == "已加料") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = "阅读",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

- [ ] **Step 4: 删除原有的 AlertDialog / BottomSheet 代码**

不再在 ProjectScreen 中展示章节内容，点击直接跳转 ReaderScreen。

---

### Task 8: RewriteScreen 重组织 — 折叠式提示词模板 + 弹窗编辑

**目标：** 使用视觉分组划分表单区域；提示词模板改为折叠式：折叠时仅显示下拉选择器，展开后查看模板列表；模板编辑通过 AlertDialog 弹窗进行。

**Files:**
- Modify: `ui/screens/RewriteScreen.kt`

- [ ] **Step 1: 表单范围 + 改写策略区域增加分组标题**

```kotlin
// 处理范围增加图标标题
item {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Rounded.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text("处理范围", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
// 改写策略增加图标标题
item {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Rounded.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text("改写策略", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
```

- [ ] **Step 2: 提示词模板改为折叠式 + 下拉选择**

```kotlin
var templatesExpanded by remember { mutableStateOf(false) }
var showTemplateDialog by remember { mutableStateOf(false) }
var editingTemplateId by remember { mutableStateOf<Long?>(null) }

// 折叠式提示词模板区域
item {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // 可点击的折叠标题行
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

            // 第一行：下拉选择器 + 管理按钮（始终显示）
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
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedButton(onClick = { showTemplateDialog = true }) {
                    Text("管理")
                }
            }

            // 展开后显示已保存模板列表
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
                // 新增模板按钮
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
```

- [ ] **Step 3: 模板编辑弹窗（AlertDialog）**

```kotlin
// 模板编辑弹窗
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
```

- [ ] **Step 4: Snackbar + 进度指示器**

```kotlin
// 在 RewriteScreen 中增加 Snackbar 反馈和队列进度指示
val snackbarHostState = remember { SnackbarHostState() }
val scope = rememberCoroutineScope()

// 操作成功后调用
scope.launch { snackbarHostState.showSnackbar("已加入改写队列") }

// 在组件树中添加
Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(...) { ... }
    SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
}
```

---

### Task 9: 模型配置合并入设置 + 导出精简

**目标：** ModelsScreen 内容移入 SettingsScreen 的"模型配置"板块。ExportScreen 删除标签。

**Files:**
- Delete: `ui/screens/ModelsScreen.kt`
- Modify: `ui/screens/SettingsScreen.kt`
- Modify: `ui/screens/ExportScreen.kt`
- Modify: `app/MojiangApp.kt` (移除 Models 路由/导入)

- [ ] **Step 1: 删除 ModelsScreen.kt 和 MojiangApp.kt 中的 Models 路由**

删除文件，并在 MojiangApp.kt 中移除 `composable(AppDestination.Models.route) { ModelsScreen() }`。

- [ ] **Step 2: SettingsScreen — 整合模型配置**

在 SettingsScreen 中增加"模型配置"板块，复用原 ModelsScreen 的核心代码：

```kotlin
// SettingsScreen 结构：
// 1. 界面（深色模式、动态颜色）
// 2. 数据管理（自动保存、导出偏好、立即保存）
// 3. 模型配置（从原 ModelsScreen 移植：
//    - 配置选择器（ExposedDropdownMenu）
//    - 编辑字段（名称、提供商、URL、模型、API Key）
//    - 操作按钮（保存、设为默认、拉取模型、测试连接）
//    - 可用模型列表
//    - 已保存配置列表)
// 4. 数据摘要
```

- [ ] **Step 3: SettingsScreen — 增加提示词模板管理板块**

在 SettingsScreen 中增加"提示词模板"板块，与模型配置同级：

```kotlin
// SettingsScreen 结构（更新后）：
// 1. 界面（深色模式、动态颜色）
// 2. 数据管理（自动保存、立即保存）
// 3. 模型配置（提供商、端点、密钥等）
// 4. 提示词模板（列表 + 弹窗编辑）
// 5. 数据摘要

// 提示词模板板块代码：
Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("提示词模板", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        
        templates.forEach { template ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(template.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        template.content.take(50) + if (template.content.length > 50) "…" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { /* 打开编辑弹窗 */ }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = { LocalNovelStore.deletePrompt(template.id) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "删除")
                    }
                }
            }
        }
        
        OutlinedButton(
            onClick = { /* 打开新建弹窗 */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Text("新增模板")
        }
    }
}
```

- [ ] **Step 4: ExportScreen — 删除标签行**

删除：
```kotlin
// 删除这三行 Badge 标签
Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
    DotBadge("合并导出", MaterialTheme.colorScheme.primary)
    DotBadge("UTF-8", MaterialTheme.colorScheme.secondary)
    DotBadge(if (LocalNovelStore.preferRewrittenExport.value) "优先加料后" else "优先原文", MaterialTheme.colorScheme.tertiary)
}
```

以及删除"优先导出加料后内容"的 Switch 行（默认行为就是导出改写后内容）。同时精简描述文本，默认即为导出加料后内容。

---

### Task 10: 新增小说阅读器 ReaderScreen

**目标：** 创建全屏小说阅读器，支持章节切换、原文/改写后切换、阅读进度提示。

**Files:**
- New: `ui/screens/ReaderScreen.kt`
- Modify: `app/MojiangApp.kt`（已添加路由）

- [ ] **Step 1: 创建 ReaderScreen.kt**

```kotlin
package com.java.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.java.myapplication.data.LocalNovelStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    chapterId: Long,
    navController: NavController
) {
    val novel = LocalNovelStore.activeNovel
    val chapters = novel?.chapters?.sortedBy { it.index }.orEmpty()
    val currentIndex = chapters.indexOfFirst { it.id == chapterId }
    val chapter = chapters.getOrNull(currentIndex)
    var showRewritten by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            chapter?.let { "第 ${it.index} 章 · ${it.title}" } ?: "阅读器",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "${chapter?.wordCount ?: 0} 字",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 原文/改写切换
                    if (chapter?.rewrittenContent?.isNotBlank() == true) {
                        Row(
                            modifier = Modifier.padding(end = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = !showRewritten,
                                onClick = { showRewritten = false },
                                label = { Text("原文", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.height(32.dp)
                            )
                            FilterChip(
                                selected = showRewritten,
                                onClick = { showRewritten = true },
                                label = { Text("改文", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.height(32.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            // 上下章导航
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            if (currentIndex > 0) {
                                val prevChapter = chapters[currentIndex - 1]
                                navController.popBackStack()
                                navController.navigate(AppDestination.Reader.createRoute(prevChapter.id))
                            }
                        },
                        enabled = currentIndex > 0
                    ) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = null)
                        Text("上一章")
                    }

                    Text(
                        "第 ${chapter?.index ?: 0} 章",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )

                    TextButton(
                        onClick = {
                            if (currentIndex < chapters.size - 1) {
                                val nextChapter = chapters[currentIndex + 1]
                                navController.popBackStack()
                                navController.navigate(AppDestination.Reader.createRoute(nextChapter.id))
                            }
                        },
                        enabled = currentIndex < chapters.size - 1
                    ) {
                        Text("下一章")
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 正文内容 — 使用阅读优化的排版
            Text(
                text = when {
                    showRewritten && chapter?.rewrittenContent?.isNotBlank() == true -> chapter.rewrittenContent
                    else -> chapter?.originalContent.orEmpty()
                },
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.6f
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
```

- [ ] **Step 2: 在 AppDestination 中添加 Reader 的 createRoute 方法**（已在 Task 2 中完成）

- [ ] **Step 3: 给 ProjectScreen 添加 navController 参数**

```kotlin
@Composable
fun ProjectScreen(navController: NavController? = null) {
    // ...
    // 章节点击时导航到阅读器
    Card(
        onClick = {
            navController?.navigate(AppDestination.Reader.createRoute(chapter.id))
        }
    ) { ... }
}
```

在 MojiangApp.kt 中传递 `navController`：
```kotlin
composable(AppDestination.Project.route) { ProjectScreen(navController = navController) }
```

---

### Task 11: 全面校验与调试

**目标：** 确保所有 UI 修改正确编译且运行正常。

- [ ] **Step 1: 编译检查**

```bash
./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 视觉回归检查**

逐页确认：
- 所有页面正常导航，底部栏为悬浮胶囊样式
- 概览页无快捷入口板块
- 项目页无"自动分章"按钮，搜索筛选正常，点击章节跳转阅读器
- 阅读器上下章切换正常，原文/改文切换正常
- 设置页包含界面/数据/模型配置三大板块
- 导出页无标签行，无 Switch
- 全局进度条正确反映改写进度

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/java/myapplication/
git commit -m "feat: redesign UI with floating nav, reader, global progress, and settings merge"
```
