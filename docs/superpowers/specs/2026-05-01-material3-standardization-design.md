# 墨匠 Rewrite Material Design 3 标准化与动画改造

## 概述

对墨匠 Rewrite Android 应用的 UI 控件进行 Material Design 3 标准化，包括顶部标题栏改标准紧凑尺寸、底部导航栏改圆角悬浮半透明样式、Tab 切换增加滑动动画。同时将阅读器从主框架中分离，作为独立全屏界面。

## 改动项

### 1. 顶部标题栏标准化

**文件**: `app/src/main/java/com/java/myapplication/app/MojiangApp.kt`

- `CenterAlignedTopAppBar` → `SmallTopAppBar`
- 标题左对齐（SmallTopAppBar 默认行为）
- 高度从 64dp 降至 48dp（MD3 紧凑尺寸标准）
- 保留 `containerColor = Color.Transparent` 透明背景

```kotlin
SmallTopAppBar(
    title = { Text("墨匠 Rewrite") },
    colors = TopAppBarDefaults.smallTopAppBarColors(
        containerColor = Color.Transparent
    )
)
```

### 2. 底部导航栏重写（圆角悬浮半透明）

**文件**: `app/src/main/java/com/java/myapplication/app/MojiangApp.kt`

- 移除包裹 NavigationBar 的 `Box` + `padding(horizontal = 20.dp, vertical = 8.dp)` + `navigationBarsPadding()`
- NavigationBar 直接作为 Scaffold.bottomBar 的内容
- 通过 Modifier 实现悬浮效果：
  - `Modifier.padding(horizontal = 16.dp, bottom = 8.dp)` — 悬浮边距
  - `Modifier.navigationBarsPadding()` — 避开系统导航栏
  - `Modifier.clip(RoundedCornerShape(12.dp))` — 圆角剪裁
  - `shape = RoundedCornerShape(12.dp)` — NavigationBar 形状
- 保留半透明背景色 `MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)`

移除外层 Box 后，键盘弹出时底部栏不再上跳遮挡输入区域。

### 3. 架构重构：两级导航

**文件**: `app/src/main/java/com/java/myapplication/app/MojiangApp.kt`

当前架构（单级 NavHost 包含所有路由）改为两级路由：

```
顶层 NavHost（主框架 vs 阅读器）
├── composable("main") → Scaffold 主框架
│   ├── SmallTopAppBar ("墨匠 Rewrite")
│   ├── AnimatedContent (5 个 Tab 页面切换)
│   │   ├── DashboardScreen (概览)
│   │   ├── ProjectScreen (项目)
│   │   ├── RewriteScreen (加料)
│   │   ├── ExportScreen (导出)
│   │   └── SettingsScreen (设置)
│   └── NavigationBar (悬浮圆角半透明)
│
├── composable("reader/{chapterId}") → ReaderScreen 全屏独立
│   └── 完全自控，不受主 Scaffold 约束
│
└── composable("prompts") → PromptsScreen 全屏独立
    └── 不受底部 Tab 栏影响
```

### 4. Tab 切换滑动动画（AnimatedContent）

**文件**: `app/src/main/java/com/java/myapplication/app/MojiangApp.kt`

- 使用 Compose `AnimatedContent` 驱动 5 个 Tab 页面的切换
- `selectedTab` 状态控制当前显示的 Tab 页面
- 滑动方向根据 Tab 在 `bottomDestinations` 列表中的位置索引计算：

```kotlin
val bottomDestinations = listOf(
    AppDestination.Dashboard,   // index 0
    AppDestination.Project,     // index 1
    AppDestination.Rewrite,     // index 2
    AppDestination.Export,      // index 3
    AppDestination.Settings     // index 4
)
```

- 目标 index > 当前 index → 新页面从右侧滑入（前向滑动）
- 目标 index < 当前 index → 新页面从左侧滑入（后向滑动）
- 同一页面 → 无动画

过渡规格：

```kotlin
AnimatedContent(
    targetState = selectedTab,
    transitionSpec = {
        val direction = // 根据 index 差值判断
        if (targetIndex > initialStateIndex) {
            // 向前：新内容从右滑入，旧内容向左滑出
            slideInHorizontally(animationSpec = tween(300)) { it } +
            fadeIn(animationSpec = tween(300)) togetherWith
            slideOutHorizontally(animationSpec = tween(300)) { -it } +
            fadeOut(animationSpec = tween(300))
        } else {
            // 向后：新内容从左滑入，旧内容向右滑出
            slideInHorizontally(animationSpec = tween(300)) { -it } +
            fadeIn(animationSpec = tween(300)) togetherWith
            slideOutHorizontally(animationSpec = tween(300)) { it } +
            fadeOut(animationSpec = tween(300))
        }
    }
) { tab ->
    when (tab) {
        AppDestination.Dashboard -> DashboardScreen()
        AppDestination.Project -> ProjectScreen(navController = ...)
        AppDestination.Rewrite -> RewriteScreen()
        AppDestination.Export -> ExportScreen()
        AppDestination.Settings -> SettingsScreen()
    }
}
```

### 5. 阅读器和提示词独立全屏

**文件**: `app/src/main/java/com/java/myapplication/app/MojiangApp.kt`

- 顶层 NavHost 包含 "main"、"reader/{chapterId}"、"prompts" 三个路由
- "main" 路由显示主框架（Scaffold + TopAppBar + AnimatedContent + NavigationBar）
- "reader/{chapterId}" 和 "prompts" 路由直接显示对应的全屏页面，完全不受主 Scaffold 约束
- 阅读器/提示词页面有各自的 Scaffold，独立控制返回按钮

#### 导航传参

- 顶层 NavController 通过回调传递到子页面，松耦合
- `ProjectScreen` 需要导航到阅读器 → 通过 `onNavigateToReader: (Long) -> Unit` 回调传入
- 其他 Screen 不直接需要顶层导航能力

### 6. 底部导航栏交互保留

进入阅读器或提示词页面时，底部导航栏自动隐藏（位于不同的路由层级，主框架不显示）。

当从阅读器按返回回到主框架时，底部导航栏恢复显示，Tab 保持在离开前的状态。

## 受影响文件清单

| 文件 | 改动类型 |
|------|----------|
| `app/.../app/MojiangApp.kt` | 重构 |
| `app/.../ui/screens/ProjectScreen.kt` | 微小调整（navController 传参变化） |

## 非改动项

- `ReaderScreen.kt` — 保持现有的独立 Scaffold 实现不变
- `PromptsScreen.kt` — 从 MojiangApp.kt 路由中移入顶层 NavHost，自身代码不变
- 所有 Screen 文件的内部实现不变
- 数据层、主题、Model API 客户端 — 不动
