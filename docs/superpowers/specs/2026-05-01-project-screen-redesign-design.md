# 项目界面重构与状态栏修复

## 概述

修复主界面状态栏遮蔽问题（改用 Material3 TopAppBar），并完全重构项目（Project）界面，增加导入 TXT、项目切换、章节搜索与状态筛选功能。

## 改动项

### 1. 主界面 TopAppBar 更换

**文件**: `app/src/main/java/com/java/myapplication/app/MojiangApp.kt`

- 移除自定义 `Box` 实现的顶部标题栏
- 改为 Material3 `TopAppBar`（利用其内置 `windowInsets` 自动适配状态栏）
- 标题文本「墨匠 Rewrite」左对齐
- transparent 背景

```kotlin
topBar = {
    TopAppBar(
        title = { Text("墨匠 Rewrite") },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        windowInsets = TopAppBarDefaults.windowInsets
    )
},
```

- `actions` 区域根据 `selectedTab` 条件渲染：
  - `selectedTab == AppDestination.Project` 时显示：项目切换下拉框 + 导入按钮
  - 其他 Tab 时：不显示（`actions = {}`）
- 项目切换下拉框使用 `ExposedDropdownMenuBox`，展示所有 `novels` 列表
- 导入按钮触发 `ActivityResultContracts.OpenDocument` 选择 TXT 文件

### 2. 项目界面重设计

**文件**: `app/src/main/java/com/java/myapplication/ui/screens/ProjectScreen.kt`

#### 布局结构（从上到下）

1. **空状态** — `activeNovel == null` 时：
   - 居中显示大号「导入 TXT 文件」按钮 + 说明文字
   - 点击触发系统文件选择器（`ActivityResultContracts.OpenDocument`）

2. **项目摘要卡片** — `activeNovel != null` 时显示：
   - 文件名 + 导入时间
   - 章节总数 / 总字数
   - 已加料 / 待处理统计
   - 「删除项目」按钮（带确认对话框）

3. **搜索 + 状态筛选栏**（sticky，不随列表滚动）：
   - `OutlinedTextField` 搜索框，placeholder "搜索章节名…"
   - 三个 `FilterChip`：全部 / 待加料 / 已加料
   - 搜索词 + 状态筛选 AND 组合

4. **章节列表**（`LazyColumn` + `items`）：
   - 每项显示：「第 N 章 · 标题」「字数」「DotBadge 状态」「右箭头」
   - 点击跳转 `ReaderScreen(chapterId)`

5. **底部**：
   - 「添加更多项目」`OutlinedButton` → 触发文件导入

#### 项目切换

- 通过 `MainScreen` 传入的回调控制 TopAppBar 右侧操作区
- 使用 `ExposedDropdownMenuBox` 展示所有项目列表
- 切换时调用 `LocalNovelStore.selectNovel(id)`
- 只有 1 个项目时下拉隐藏

### 3. 交互逻辑

- **导入流程**: `rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(arrayOf("text/plain")))` 选择 TXT → `LocalNovelStore.importTxt()` 自动分章 → 切换到新项目
- **搜索/筛选**: 使用 `derivedStateOf` 或 `remember` + `snapshotFlow` 对 `novel.chapters` 做实时过滤，不触发重组
- **删除项目**: 确认对话框 → `LocalNovelStore.deleteNovel(id)` → 列表自动清空或显示空状态

## 涉及文件

| 文件 | 改动类型 |
|------|---------|
| `MojiangApp.kt` | 修改：TopAppBar 替换 |
| `ProjectScreen.kt` | 重写：完整布局重设计 |
| 无新文件创建 | |

## 不变部分

- `LocalNovelStore` 的 `importTxt()`、`deleteNovel()`、`selectNovel()` 逻辑保持不变
- `ReaderScreen` 导航逻辑不变
- `AppDestination` 路由结构不变
- 所有现有数据持久化不变
