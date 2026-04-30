# 墨匠 Rewrite UI 精简与规范化 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 对墨匠 Rewrite Android 应用进行 UI 精简、文字规范化和进度通知迁移

**Architecture:** 纯 UI 层改动（Jetpack Compose + Material3），新增一个前台服务替代 WorkManager 用于改写任务进度通知，其余均为删除/简化现有 UI 组件

**Tech Stack:** Android, Jetpack Compose, Material3, Kotlin, Foreground Service

---

## 文件结构

### 新建
| 文件 | 职责 |
|------|------|
| `app/src/main/java/com/java/myapplication/worker/RewriteForegroundService.kt` | 前台服务，处理改写任务并发布系统通知进度 |

### 修改
| 文件 | 职责 |
|------|------|
| `app/src/main/AndroidManifest.xml` | 添加 FOREGROUND_SERVICE 权限和服务声明 |
| `app/src/main/java/com/java/myapplication/app/MojiangApp.kt` | 底部导航标准化 + 删除进度条 |
| `app/src/main/java/com/java/myapplication/data/LocalNovelStore.kt` | 启用前台服务替换 WorkManager |
| `app/src/main/java/com/java/myapplication/ui/screens/DashboardScreen.kt` | 删除渐变色卡片 + 文字简化 |
| `app/src/main/java/com/java/myapplication/ui/screens/ProjectScreen.kt` | 精简为仅章节列表 + 文字简化 |
| `app/src/main/java/com/java/myapplication/ui/screens/RewriteScreen.kt` | 策略简化 + 删除自定义提示词 + 删除记忆板块 + 文字简化 |
| `app/src/main/java/com/java/myapplication/ui/screens/ExportScreen.kt` | 文字简化 |
| `app/src/main/java/com/java/myapplication/ui/screens/SettingsScreen.kt` | 文字简化 |
| `app/src/main/java/com/java/myapplication/ui/components/AppComponents.kt` | SectionTitle 支持可选 subtitle |

### 不变
| 文件 | 原因 |
|------|------|
| `PromptsScreen.kt` | 独立页面，功能保留 |
| `ReaderScreen.kt` | 功能保留，无改动需求 |
| `RewriteWorker.kt` | 保留文件但不再被调用 |
| 数据模型/主题色/网络层 | 无变动 |

---

### Task 1: 底部导航栏标准化 + 删除进度条

**Files:**
- Modify: `app/src/main/java/com/java/myapplication/app/MojiangApp.kt`

- [ ] **Step 1: 删除进度条相关代码**

删除以下内容：
1. `derivedStateOf` 中 `totalChapters`、`rewrittenChapters`、`progress` 的 3 个计算
2. `topBar` 中 `if (totalChapters > 0)` 块内所有代码（Row + Spacer + LinearProgressIndicator）
3. 对应的 import

删除后 topBar 变为：
```kotlin
topBar = {
    CenterAlignedTopAppBar(
        title = { Text("墨匠 Rewrite") },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        )
    )
}
```

并删除对应的 `Row`、`Spacer`、`Arrangement`（仅用于进度条的话）、`LinearProgressIndicator`、`RoundedCornerShape`、`clip`、`derivedStateOf` 等不再使用的 import。

- [ ] **Step 2: 底部导航栏标准化**

修改 bottomBar 中的 NavigationBar，移除以下修饰：
- 移除 `.height(68.dp)`
- 移除 `.clip(RoundedCornerShape(34.dp))`
- 移除 `tonalElevation = CardElevation.level2`

保留 `containerColor` 和 `navigationBarsPadding`。

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/java/myapplication/app/MojiangApp.kt
git commit -m "refactor: 标准化底部导航栏并移除顶部进度条"
```

---

### Task 2: 创建 RewriteForegroundService

**Files:**
- Create: `app/src/main/java/com/java/myapplication/worker/RewriteForegroundService.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: 创建 RewriteForegroundService**

```kotlin
package com.java.myapplication.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.java.myapplication.data.LocalNovelStore

class RewriteForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "mojiang_rewrite"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.java.myapplication.action.START_REWRITE"
        const val ACTION_CANCEL = "com.java.myapplication.action.CANCEL_REWRITE"

        fun start(context: Context) {
            val intent = Intent(context, RewriteForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun cancel(context: Context) {
            val intent = Intent(context, RewriteForegroundService::class.java).apply {
                action = ACTION_CANCEL
            }
            context.startService(intent)
        }
    }

    private lateinit var notificationManager: NotificationManager
    private var isProcessing = false

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (!isProcessing) {
                    isProcessing = true
                    processRewriteQueue()
                }
            }
            ACTION_CANCEL -> {
                isProcessing = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun processRewriteQueue() {
        LocalNovelStore.init(this)

        val total = LocalNovelStore.queuedJobs() + LocalNovelStore.completedJobs()
        startForeground(NOTIFICATION_ID, buildProgressNotification(0, total))

        thread {
            while (isProcessing) {
                val hasMore = LocalNovelStore.processNextRewriteBatch(maxItems = 1)
                val completed = LocalNovelStore.completedJobs()
                val totalJobs = completed + LocalNovelStore.queuedJobs()
                updateProgressNotification(completed, totalJobs)

                if (!hasMore) break
            }

            if (isProcessing) {
                val done = LocalNovelStore.completedJobs()
                val failed = LocalNovelStore.rewriteQueue.count { it.state == "失败" }
                showCompletionNotification(done, failed)
            }

            isProcessing = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "改写进度",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示后台改写任务的实时进度"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildProgressNotification(current: Int, total: Int): Notification {
        val max = total.coerceAtLeast(1)
        val progress = current.coerceAtMost(max)
        val text = if (total > 0) "改写进度：$current / $total 章" else "正在准备改写任务…"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("墨匠 Rewrite")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setProgress(max, progress, false)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateProgressNotification(current: Int, total: Int) {
        val notification = buildProgressNotification(current, total)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification(done: Int, failed: Int) {
        val text = buildString {
            append("已完成 $done 章")
            if (failed > 0) append("，$failed 章失败")
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("改写完成")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setSilent(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
```

- [ ] **Step 2: 更新 AndroidManifest.xml**

在 `</application>` 前的 `<application>` 块内添加 service 声明。在 `manifest` 下的现有权限之后添加 FOREGROUND_SERVICE 和 POST_NOTIFICATIONS 权限：

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<application ...>
    ...
    <service
        android:name=".worker.RewriteForegroundService"
        android:exported="false"
        android:foregroundServiceType="dataSync" />
</application>
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/java/myapplication/worker/RewriteForegroundService.kt app/src/main/AndroidManifest.xml
git commit -m "feat: 创建 RewriteForegroundService 用于系统通知栏进度"
```

---

### Task 3: LocalNovelStore 启用前台服务

**Files:**
- Modify: `app/src/main/java/com/java/myapplication/data/LocalNovelStore.kt`

- [ ] **Step 1: 修改 enqueueRewrite**

将 `enqueueRewrite` 方法末尾的 `scheduleRewriteWork(context)` 替换为：

```kotlin
RewriteForegroundService.start(context)
```

并在文件顶部 imports 区域添加：

```kotlin
import com.java.myapplication.worker.RewriteForegroundService
```

- [ ] **Step 2: 修改 retryFailedJobs**

将 `retryFailedJobs` 方法末尾的 `scheduleRewriteWork(context)` 同样替换为：

```kotlin
RewriteForegroundService.start(context)
```

- [ ] **Step 3: 删除不再使用的代码和 import**

删除以下不再使用的 import：
```kotlin
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.java.myapplication.worker.RewriteWorker
```

删除 `scheduleRewriteWork` 私有方法。

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/java/myapplication/data/LocalNovelStore.kt
git commit -m "feat: 改写任务改用前台服务通知"
```

---

### Task 4: 精简 DashboardScreen + 修改 SectionTitle

**Files:**
- Modify: `app/src/main/java/com/java/myapplication/ui/screens/DashboardScreen.kt`
- Modify: `app/src/main/java/com/java/myapplication/ui/components/AppComponents.kt`

- [ ] **Step 1: 修改 SectionTitle 支持可选 subtitle**

在 AppComponents.kt 中：

```kotlin
@Composable
fun SectionTitle(title: String, subtitle: String = "") {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (subtitle.isNotBlank()) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
```

- [ ] **Step 2: 删除 DashboardScreen 中的 GradientHeroCard 并简化文字**

删除第 39-44 行的 `GradientHeroCard` item，删除对应的 import `com.java.myapplication.ui.components.GradientHeroCard`。

SectionTitle 从 `SectionTitle("工作概览", "真实读取本地项目、章节、模型和导出状态。")` 改为 `SectionTitle("工作概览")`。

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/java/myapplication/ui/screens/DashboardScreen.kt app/src/main/java/com/java/myapplication/ui/components/AppComponents.kt
git commit -m "refactor: 精简概览界面，删除渐变色卡片"
```

---

### Task 5: 精简 ProjectScreen

**Files:**
- Modify: `app/src/main/java/com/java/myapplication/ui/screens/ProjectScreen.kt`

- [ ] **Step 1: 删除文件导入和搜索控件**

删除：
1. `filePicker` launcher
2. `searchQuery` 状态变量
3. `filteredChapters` 计算（直接使用 `activeNovel?.chapters.orEmpty()`）
4. "导入 TXT" 的 Card
5. 已导入小说列表切换
6. 搜索栏
7. 相关 SectionTitle 和 import

精简后仅保留章节列表，顶部显示文件名和字数概览。

- [ ] **Step 2: 清理无用 import**

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/java/myapplication/ui/screens/ProjectScreen.kt
git commit -m "refactor: 项目工作台精简为仅章节列表"
```

---

### Task 6: 简化 RewriteScreen

**Files:**
- Modify: `app/src/main/java/com/java/myapplication/ui/screens/RewriteScreen.kt`

- [ ] **Step 1: 删除状态变量**

删除 `keepPlot`、`preserveNames`、`promptOverride`、`savePromptTitle` 4 个状态变量。删除 `effectivePrompt` 计算。

- [ ] **Step 2: 删除策略区开关**

在"改写策略" Card 中删除两行 SettingRow：
```kotlin
SettingRow("保留原剧情", keepPlot) { keepPlot = it }
SettingRow("保留角色名称与称呼", preserveNames) { preserveNames = it }
```

- [ ] **Step 3: 删除自定义提示词 Card**

删除包含 promptOverride 输入框和保存按钮的整个 Card 块。

- [ ] **Step 4: 删除长篇一致性记忆 Card**

删除带 Bolt 图标和 longMemorySummary 的 Card。

- [ ] **Step 5: 删除"真实改写首章"按钮**

简化后只保留一个"开始改写"按钮。

- [ ] **Step 6: 调整按钮调用**

将 enqueueRewrite 调用中的 keepPlot/preserveNames 参数删除。

- [ ] **Step 7: 清理无用 import**

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/java/myapplication/ui/screens/RewriteScreen.kt
git commit -m "refactor: 简化改写界面，删除多余控件和文字"
```

---

### Task 7: ExportScreen 文字简化

**Files:**
- Modify: `app/src/main/java/com/java/myapplication/ui/screens/ExportScreen.kt`

- [ ] **Step 1: 简化 SectionTitle**

```kotlin
item { SectionTitle("导出") }
item { SectionTitle("导出记录") }
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/java/myapplication/ui/screens/ExportScreen.kt
git commit -m "refactor: 简化导出界面文字"
```

---

### Task 8: SettingsScreen 文字简化

**Files:**
- Modify: `app/src/main/java/com/java/myapplication/ui/screens/SettingsScreen.kt`

- [ ] **Step 1: 简化 SectionTitle**

```kotlin
item { SectionTitle("设置") }
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/java/myapplication/ui/screens/SettingsScreen.kt
git commit -m "refactor: 简化设置界面文字"
```
