# MD3 标准化与滑动动画 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 对墨匠 Rewrite 的 UI 进行 Material Design 3 标准化，重构导航架构并增加 Tab 切换滑动动画

**Architecture:**
- 顶层新增 NavHost 管理"主界面(main)" vs "全屏页面(reader/prompts)"
- 主界面内用 AnimatedContent 驱动 5 个 Tab 页面的位置感知滑动切换
- 阅读器和提示词页面全屏独立，不受主 Scaffold 约束
- 底部导航栏移除外层 Box，改为圆角悬浮半透明样式

**Tech Stack:** Jetpack Compose, Material3, AnimatedContent, Navigation Compose

---

### Task 1: MojiangApp.kt - 完整重构

**文件:** `app/src/main/java/com/java/myapplication/app/MojiangApp.kt`

将此文件从单级 Scaffold+NavHost 重构为双层架构：
- 外层：NavHost("main" / "reader/{chapterId}" / "prompts")
- 内层 MainScreen 组合函数：Scaffold(SmallTopAppBar + AnimatedContent + 悬浮NavigationBar)

- [ ] **Step 1: 添加所需 import 语句**

在当前 import 基础上，添加以下 import：
```kotlin
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
```

- [ ] **Step 2: 将 MojiangApp 函数改为双层架构**

用外层 NavHost 包裹，提取 MainScreen 组合函数：

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MojiangApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen(
                onNavigateToReader = { chapterId ->
                    navController.navigate(AppDestination.Reader.createRoute(chapterId))
                }
            )
        }
        composable(
            route = AppDestination.Reader.route,
            arguments = listOf(navArgument("chapterId") { type = NavType.LongType })
        ) { backStackEntry ->
            val chapterId = backStackEntry.arguments?.getLong("chapterId") ?: return@composable
            ReaderScreen(chapterId = chapterId, navController = navController)
        }
        composable(AppDestination.Prompts.route) { PromptsScreen() }
    }
}
```

- [ ] **Step 3: 实现 MainScreen 组合函数 - 顶栏 + 底栏**

```kotlin
@Composable
fun MainScreen(onNavigateToReader: (Long) -> Unit) {
    var selectedTab by remember { mutableStateOf(AppDestination.Dashboard) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                SmallTopAppBar(
                    title = { Text("墨匠 Rewrite") },
                    colors = TopAppBarDefaults.smallTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, bottom = 8.dp)
                        .navigationBarsPadding(),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 3.dp,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    for (destination in bottomDestinations) {
                        val icon = when (destination) {
                            AppDestination.Dashboard -> Icons.Rounded.Dashboard
                            AppDestination.Project -> Icons.Rounded.AutoStories
                            AppDestination.Rewrite -> Icons.Rounded.Bolt
                            AppDestination.Export -> Icons.Rounded.FileUpload
                            AppDestination.Settings -> Icons.Rounded.Settings
                            else -> Icons.Rounded.Dashboard
                        }
                        NavigationBarItem(
                            selected = selectedTab == destination,
                            onClick = { selectedTab = destination },
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
        ) { innerPadding ->

            AnimatedContent(
                targetState = selectedTab,
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(top = 6.dp)
                    .padding(innerPadding),
                transitionSpec = {
                    val currentIndex = bottomDestinations.indexOf(initialState)
                    val targetIndex = bottomDestinations.indexOf(targetState)
                    val slideForward = targetIndex > currentIndex

                    if (slideForward) {
                        slideInHorizontally(
                            animationSpec = tween(300)
                        ) { fullWidth -> fullWidth } +
                        fadeIn(animationSpec = tween(300)) togetherWith
                        slideOutHorizontally(
                            animationSpec = tween(300)
                        ) { fullWidth -> -fullWidth } +
                        fadeOut(animationSpec = tween(300))
                    } else {
                        slideInHorizontally(
                            animationSpec = tween(300)
                        ) { fullWidth -> -fullWidth } +
                        fadeIn(animationSpec = tween(300)) togetherWith
                        slideOutHorizontally(
                            animationSpec = tween(300)
                        ) { fullWidth -> fullWidth } +
                        fadeOut(animationSpec = tween(300))
                    }
                },
                label = "TabContent"
            ) { tab ->
                when (tab) {
                    AppDestination.Dashboard -> DashboardScreen()
                    AppDestination.Project -> ProjectScreen(
                        onNavigateToReader = onNavigateToReader
                    )
                    AppDestination.Rewrite -> RewriteScreen()
                    AppDestination.Export -> ExportScreen()
                    AppDestination.Settings -> SettingsScreen()
                }
            }
        }
    }
}
```

- [ ] **Step 4: 验证编译**

```bash
cd D:/开发/Android开发/MojiangRewrite-main/MojiangRewrite-main
./gradlew :app:compileDebugKotlin --no-daemon 2>&1 | tail -30
```

Expected: BUILD SUCCESSFUL（可能需要修复 import 和类型错误）

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/java/myapplication/app/MojiangApp.kt
git commit -m "refactor: MD3标准化 - 顶栏缩至48dp，底栏改圆角悬浮，AnimatedContent切换"
```

---

### Task 2: ProjectScreen.kt - navController 改为回调

**文件:** `app/src/main/java/com/java/myapplication/ui/screens/ProjectScreen.kt`

因 MojiangApp 不再通过 NavHost 管理 Tab 路由，ProjectScreen 不再直接持有 NavController。改为接收 `onNavigateToReader` 回调函数。

- [ ] **Step 1: 修改函数签名和 import**

删除 `import androidx.navigation.NavController`，修改函数签名：

```kotlin
@Composable
fun ProjectScreen(onNavigateToReader: (Long) -> Unit = {}) {
```

- [ ] **Step 2: 修改导航调用**

将：
```kotlin
onClick = {
    navController?.navigate(AppDestination.Reader.createRoute(chapter.id))
}
```
改为：
```kotlin
onClick = {
    onNavigateToReader(chapter.id)
}
```

完整改动后文件关键部分：
```kotlin
package com.java.myapplication.ui.screens

// 删除: import androidx.navigation.NavController
// 其他 imports 保持不变

@Composable
fun ProjectScreen(onNavigateToReader: (Long) -> Unit = {}) {
    val activeNovel = LocalNovelStore.activeNovel

    LazyColumn(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        activeNovel?.let { novel ->
            // ... 保持现有代码不变 ...
            items(novel.chapters) { chapter ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    onClick = {
                        onNavigateToReader(chapter.id)  // 改动点
                    }
                ) {
                    // ... 保持现有代码不变 ...
                }
            }
        }
    }
}
```

- [ ] **Step 3: 验证编译**

```bash
cd D:/开发/Android开发/MojiangRewrite-main/MojiangRewrite-main
./gradlew :app:compileDebugKotlin --no-daemon 2>&1 | tail -30
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 提交**

```bash
git add app/src/main/java/com/java/myapplication/ui/screens/ProjectScreen.kt
git commit -m "refactor: ProjectScreen navController改为onNavigateToReader回调"
```

---

### Task 3: 构建验证

- [ ] **Step 1: 完整构建**

```bash
cd D:/开发/Android开发/MojiangRewrite-main/MojiangRewrite-main
./gradlew assembleDebug --no-daemon 2>&1 | tail -50
```

Expected: BUILD SUCCESSFUL，生成 APK

- [ ] **Step 2: 检查代码中不再有废弃 import**

```bash
cd D:/开发/Android开发/MojiangRewrite-main/MojiangRewrite-main
grep -rn "import androidx.navigation.NavController" app/src/main/java/ 2>/dev/null || echo "OK: 无残留 NavController 引用"
```

Expected: ProjectScreen.kt 不再引用 NavController
