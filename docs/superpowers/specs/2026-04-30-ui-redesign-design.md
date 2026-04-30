# 墨匠 Rewrite UI 精简与规范化设计

## 概述

对墨匠 Rewrite Android 应用的 UI 进行全面精简与规范化改造，移除冗余控件和信息，统一文字风格，并将进度展示从界面内迁移至系统通知栏。

## 改动项

### 1. 底部导航栏标准化

**文件**: `app/src/main/java/com/java/myapplication/app/MojiangApp.kt`

- 移除 `NavigationBar` 的自定义修饰：`.height(68.dp)`、`.clip(RoundedCornerShape(34.dp))`、自定义 `containerColor` 和 `tonalElevation`
- 回退到 Material3 NavigationBar 默认样式（默认高度、默认背景色、默认圆角）
- 保留图标、标签、导航逻辑不变

### 2. 进度条迁移至前台服务通知

**新建文件**: `app/src/main/java/com/java/myapplication/worker/RewriteForegroundService.kt`

- 继承 `Service`，在启动时调用 `startForeground()` 发布通知
- 创建通知渠道 `mojiang_rewrite`（渠道 ID: `rewrite_progress`）
- 使用 `NotificationCompat.Builder.setProgress(max, progress, false)` 显示进度的通知
- 通知内容：`"改写进度：X/Y 章"` + 进度条
- 处理完成时：更新通知为"改写完成"，移除前台状态，30 秒后自动取消通知
- 启动方式：通过 Intent 传入章节 ID 列表、提示词、强度等参数
- 注：由于用户选择了"前台服务+通知"，将改写处理逻辑从 WorkManager 迁移至该 Service

**修改文件**: `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<application>
    <service
        android:name=".worker.RewriteForegroundService"
        android:exported="false"
        android:foregroundServiceType="dataSync" />
</application>
```

**修改文件**: `app/src/main/java/com/java/myapplication/data/LocalNovelStore.kt`

- `enqueueRewrite()` 不再调用 `scheduleRewriteWork()`（WorkManager），改为启动 `RewriteForegroundService`
- 其他方法：`retryFailedJobs()` 同样改为启动 Service
- `RewriteWorker.kt` 保留但不活跃（可后续作为备选）

**修改文件**: `app/src/main/java/com/java/myapplication/app/MojiangApp.kt`

- 删除 `LinearProgressIndicator` 及其相关状态变量（`totalChapters`、`rewrittenChapters`、`progress` 的计算和显示）

### 3. 项目工作台精简为仅章节列表

**文件**: `app/src/main/java/com/java/myapplication/ui/screens/ProjectScreen.kt`

- 删除导入 TXT 按钮（`filePicker` 及相关代码）
- 删除已导入小说列表切换卡片（`if (novels.size > 1)` 块）
- 删除搜索栏（`OutlinedTextField` 和 `searchQuery` 状态）
- 删除 SectionTitle "项目工作台" 和 "已导入小说"
- 保留：章节列表（`filteredChapters` → 改为直接使用 `activeNovel.chapters`）
- 保留：当前项目基本信息的简短文字

### 4. 改写策略区简化

**文件**: `app/src/main/java/com/java/myapplication/ui/screens/RewriteScreen.kt`

- 在"改写策略"卡片中，删除 `SettingRow("保留原剧情", ...)` 和 `SettingRow("保留角色名称与称呼", ...)` 两行
- 保留"改文强度 XX%" 标签和 `Slider`
- `LocalNovelStore.enqueueRewrite()` 参数保持不变（`keepPlot`、`preserveNames` 传默认值 true）
- 以简洁为主

### 5. 删除自定义提示词板块

**文件**: `app/src/main/java/com/java/myapplication/ui/screens/RewriteScreen.kt`

- 移除包含 `promptOverride` 输入框和保存按钮的整个 Card 块
- 删除相关状态变量：`promptOverride`、`savePromptTitle`、`effectivePrompt` 的计算
- 提示词使用所选模板的内容（不再支持临时覆盖）

### 6. 删除长篇一致性记忆板块

**文件**: `app/src/main/java/com/java/myapplication/ui/screens/RewriteScreen.kt`

- 移除带 Bolt 图标的"长篇一致性记忆"Card
- 删除 `longMemorySummary` 引用

### 7. 删除概览界面渐变色卡片

**文件**: `app/src/main/java/com/java/myapplication/ui/screens/DashboardScreen.kt`

- 删除第 1 个 item 中的 `GradientHeroCard` 调用
- `GradientHeroCard.kt` 组件文件保留，可供后续复用

### 8. 文字规范化

全应用范围内的文字归一。以下为逐文件的改动计划：

#### DashboardScreen.kt
| 位置 | 原文字 | 改后 |
|------|--------|------|
| SectionTitle subtitle | "真实读取本地项目、章节、模型和导出状态。" | 删除 subtitle |
| MetricCard caption | 保留 | 保留 |

#### ProjectScreen.kt
| 位置 | 原文字 | 改后 |
|------|--------|------|
| 章节状态 | "已加料" / "待加料" | 保留 |
| 顶部文字 | "当前小说：xxx · 共 X 章 · X 字" | 保留 |

#### RewriteScreen.kt
| 位置 | 原文字 | 改后 |
|------|--------|------|
| SectionTitle | "加料与改写" | "改写" |
| SectionTitle subtitle | "选择章节范围和自定义提示词..." | 删除 |
| "改写策略" title | "改写策略" | "强度" |
| 按钮 "后台处理范围" | "后台处理范围" | "开始改写" |
| 按钮 "真实改写首章" | "真实改写首章" | 删除（简化） |
| 提示词模板 title | "提示词模板" | "提示词" |
| 状态文字 | 各种状态消息 | 统一为简洁格式 |

#### ExportScreen.kt
| 位置 | 原文字 | 改后 |
|------|--------|------|
| SectionTitle subtitle | "真实生成 TXT 文件..." | 删除 |
| 说明文字 | "默认导出加料后内容；未加料章节自动回退原文。" | 保留（信息性） |

#### SettingsScreen.kt
| 位置 | 原文字 | 改后 |
|------|--------|------|
| SectionTitle subtitle | "界面、数据管理、模型配置集中管理。" | 删除 |
| 提示词模板区域 | 保留（功能上需要的） | 保留 |

## 受影响文件清单

| 文件 | 改动类型 |
|------|----------|
| `app/.../app/MojiangApp.kt` | 修改 |
| `app/.../ui/screens/DashboardScreen.kt` | 修改 |
| `app/.../ui/screens/ProjectScreen.kt` | 修改 |
| `app/.../ui/screens/RewriteScreen.kt` | 修改 |
| `app/.../ui/screens/ExportScreen.kt` | 修改 |
| `app/.../ui/screens/SettingsScreen.kt` | 修改 |
| `app/.../worker/RewriteForegroundService.kt` | 新建 |
| `app/.../data/LocalNovelStore.kt` | 修改 |
| `AndroidManifest.xml` | 修改 |

## 非改动项

- `PromptsScreen.kt`（提示词单独页面）— 保留不动，但 RewriteScreen 中删除自定义提示词后不影响此页
- `ReaderScreen.kt` — 保留不动
- `GradientHeroCard.kt` 组件 — 保留不动（仅不在 Dashboard 中引用）
- `RewriteWorker.kt` — 保留不动（作为备选处理方案）
- 数据模型、主题色、网络层 — 不动
