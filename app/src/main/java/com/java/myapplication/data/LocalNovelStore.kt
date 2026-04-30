package com.java.myapplication.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.java.myapplication.network.ModelApiClient
import com.java.myapplication.worker.RewriteWorker
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * 应用级本地仓库。
 * - 使用 app 私有目录 JSON 持久化，避免重启闪退/数据丢失。
 * - 所有入口 runCatching 包裹，错误只进入状态栏，不抛到 UI。
 * - 大文本只在章节详情展示，列表只读摘要字段，降低 Compose 卡顿风险。
 */
object LocalNovelStore {
    private val idGen = AtomicLong(1)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false; encodeDefaults = true }
    private var appContext: Context? = null
    private var loaded = false

    val novels = mutableStateListOf<NovelProject>()
    val promptTemplates = mutableStateListOf<PromptTemplate>()
    val providers = mutableStateListOf<ModelProvider>()
    val exportRecords = mutableStateListOf<ExportRecordData>()
    val rewriteQueue = mutableStateListOf<RewriteJob>()
    val activeNovelId = mutableStateOf<Long?>(null)
    val statusMessage = mutableStateOf("尚未导入小说")
    val longMemorySummary = mutableStateOf("尚未生成长篇记忆")
    val autoSaveEnabled = mutableStateOf(true)
    val preferRewrittenExport = mutableStateOf(true)

    val activeNovel: NovelProject?
        get() = novels.firstOrNull { it.id == activeNovelId.value }

    fun init(context: Context) {
        if (loaded) return
        appContext = context.applicationContext
        loaded = true
        loadFromDisk()
        ensureDefaults()
        saveQuietly()
    }

    fun totalWords(): Int = novels.sumOf { novel -> novel.chapters.sumOf { it.wordCount } }
    fun rewrittenChapters(): Int = novels.sumOf { novel -> novel.chapters.count { it.rewrittenContent.isNotBlank() } }
    fun pendingChapters(): Int = novels.sumOf { novel -> novel.chapters.count { it.rewrittenContent.isBlank() } }
    fun queuedJobs(): Int = rewriteQueue.count { it.state == "排队中" || it.state == "处理中" }
    fun completedJobs(): Int = rewriteQueue.count { it.state == "完成" }

    fun importTxt(context: Context, uri: Uri) {
        init(context)
        runCatching {
            val name = queryDisplayName(context, uri).sanitizeFileName().ifBlank { "未命名.txt" }
            val text = readTextRobust(context, uri)
            require(text.isNotBlank()) { "文件内容为空" }
            require(text.length <= 3_000_000) { "文件过大：当前稳定版建议单次不超过约300万字，请先拆分后导入" }

            val novelId = idGen.getAndIncrement()
            val chapters = splitChapters(text).mapIndexed { index, chapter ->
                ChapterData(
                    id = idGen.getAndIncrement(),
                    novelId = novelId,
                    index = index + 1,
                    title = chapter.first.ifBlank { "第 ${index + 1} 章" }.take(100),
                    originalContent = chapter.second,
                    rewrittenContent = "",
                    status = "待加料"
                )
            }
            require(chapters.isNotEmpty()) { "未解析到有效正文" }
            val novel = NovelProject(
                id = novelId,
                title = name.removeSuffix(".txt").removeSuffix(".TXT").ifBlank { "未命名小说" },
                fileName = name,
                importedAt = nowText(),
                chapters = chapters.toMutableList()
            )
            novels.add(0, novel)
            activeNovelId.value = novelId
            statusMessage.value = "导入完成：${novel.title} · ${chapters.size} 章"
            saveQuietly()
        }.onFailure {
            statusMessage.value = "导入失败：${it.message ?: "未知错误"}"
        }
    }

    fun deleteNovel(id: Long) {
        novels.removeAll { it.id == id }
        if (activeNovelId.value == id) activeNovelId.value = novels.firstOrNull()?.id
        statusMessage.value = "已删除项目"
        saveQuietly()
    }

    fun selectNovel(id: Long) {
        activeNovelId.value = id
        statusMessage.value = "已切换项目：${activeNovel?.title.orEmpty()}"
        saveQuietly()
    }

    fun upsertPrompt(id: Long?, title: String, content: String) {
        val safeTitle = title.trim().ifBlank { "未命名模板" }.take(60)
        val safeContent = content.trim().ifBlank { "请在这里填写提示词内容。" }
        val index = id?.let { promptTemplates.indexOfFirst { p -> p.id == it } } ?: -1
        if (index >= 0) {
            promptTemplates[index] = promptTemplates[index].copy(title = safeTitle, content = safeContent)
            statusMessage.value = "提示词已更新：$safeTitle"
        } else {
            promptTemplates.add(0, PromptTemplate(idGen.getAndIncrement(), safeTitle, safeContent))
            statusMessage.value = "提示词已保存：$safeTitle"
        }
        saveQuietly()
    }

    fun deletePrompt(id: Long) {
        if (promptTemplates.size <= 1) {
            statusMessage.value = "至少保留一个提示词模板"
            return
        }
        promptTemplates.removeAll { it.id == id }
        statusMessage.value = "提示词模板已删除"
        saveQuietly()
    }

    fun upsertProvider(item: ModelProvider) {
        val safe = item.copy(
            profileName = item.profileName.ifBlank { "未命名配置" }.take(60),
            providerName = item.providerName.ifBlank { "Custom" }.take(40),
            model = item.model.ifBlank { "未设置模型" }.take(120),
            endpoint = item.endpoint.ifBlank { "未设置地址" }.trim(),
            apiKeyMasked = item.apiKeyMasked.ifBlank { "未保存" },
            apiKey = item.apiKey.trim()
        )
        val index = providers.indexOfFirst { it.id == safe.id }
        if (index >= 0) providers[index] = safe else providers.add(0, safe.copy(id = nextProviderId()))
        saveQuietly()
    }

    fun replaceProviders(newProviders: List<ModelProvider>) {
        providers.clear()
        providers.addAll(newProviders.ifEmpty { defaultProviders() })
        saveQuietly()
    }

    fun setDefaultProvider(id: Int) {
        for (i in providers.indices) {
            providers[i] = providers[i].copy(isDefault = providers[i].id == id)
        }
        statusMessage.value = "已设为默认模型配置"
        saveQuietly()
    }

    fun nextProviderId(): Int = ((providers.maxOfOrNull { it.id } ?: 0) + 1).coerceAtLeast(1)

    fun enqueueRewrite(context: Context, chapterIds: List<Long>, prompt: String, intensity: Int, keepPlot: Boolean = true, preserveNames: Boolean = true) {
        init(context)
        val novel = activeNovel ?: run {
            statusMessage.value = "请先导入小说"
            return
        }
        val selectedIds = chapterIds.toSet()
        if (selectedIds.isEmpty()) {
            statusMessage.value = "没有匹配到要处理的章节"
            return
        }
        val cleanPrompt = prompt.trim().ifBlank { promptTemplates.firstOrNull()?.content.orEmpty() }
        val existingPending = rewriteQueue.filter { it.novelId == novel.id && it.state in setOf("排队中", "处理中") }.map { it.chapterId }.toSet()
        val targets = novel.chapters.filter { it.id in selectedIds && it.id !in existingPending }
        if (targets.isEmpty()) {
            statusMessage.value = "所选章节已在后台队列中"
            return
        }
        targets.forEach { chapter ->
            rewriteQueue.add(
                RewriteJob(
                    id = idGen.getAndIncrement(),
                    novelId = novel.id,
                    chapterId = chapter.id,
                    chapterIndex = chapter.index,
                    prompt = cleanPrompt.take(4000),
                    intensity = intensity.coerceIn(1, 100),
                    keepPlot = keepPlot,
                    preserveNames = preserveNames,
                    state = "排队中",
                    createdAt = nowText()
                )
            )
            updateChapterStatus(novel.id, chapter.id, "排队中")
        }
        refreshLongMemory(novel.id)
        statusMessage.value = "已加入后台队列：${targets.size} 章。退出应用后仍会尽力继续处理。"
        saveQuietly()
        scheduleRewriteWork(context)
    }

    fun applyLocalRewrite(chapterIds: List<Long>, prompt: String, intensity: Int, keepPlot: Boolean = true, preserveNames: Boolean = true) {
        val context = appContext
        if (context == null) {
            statusMessage.value = "真实模型改写需要应用初始化完成后执行"
            return
        }
        enqueueRewrite(context, chapterIds, prompt, intensity, keepPlot, preserveNames)
    }

    fun processNextRewriteBatch(maxItems: Int = 3): Boolean {
        init(appContext ?: return false)
        val jobs = rewriteQueue.filter { it.state == "排队中" }.sortedBy { it.chapterIndex }.take(maxItems)
        if (jobs.isEmpty()) return false
        jobs.forEach { job ->
            runCatching {
                updateJobState(job.id, "处理中", null)
                val novel = novels.firstOrNull { it.id == job.novelId } ?: error("项目不存在")
                val index = novel.chapters.indexOfFirst { it.id == job.chapterId }
                require(index >= 0) { "章节不存在" }
                val chapter = novel.chapters[index]
                val memory = buildContextForChapter(novel, chapter.index)
                val provider = providers.firstOrNull { it.isDefault } ?: providers.firstOrNull() ?: error("请先配置模型提供商")
                val rewritten = ModelApiClient.rewriteChapter(
                    provider = provider,
                    title = chapter.title,
                    original = chapter.originalContent,
                    userPrompt = job.prompt,
                    memoryContext = memory,
                    intensity = job.intensity,
                    keepPlot = job.keepPlot,
                    preserveNames = job.preserveNames
                ).getOrElse { error(it.message ?: "模型改写失败") }
                novel.chapters[index] = chapter.copy(
                    rewrittenContent = rewritten,
                    status = "已加料"
                )
                updateJobState(job.id, "完成", null)
                refreshLongMemory(novel.id)
            }.onFailure { e ->
                updateJobState(job.id, "失败", e.message ?: "未知错误")
                updateChapterStatus(job.novelId, job.chapterId, "失败")
            }
        }
        statusMessage.value = "后台处理进度：完成 ${completedJobs()}，队列剩余 ${queuedJobs()}"
        saveQuietly()
        return rewriteQueue.any { it.state == "排队中" }
    }

    fun retryFailedJobs(context: Context) {
        init(context)
        var count = 0
        for (i in rewriteQueue.indices) {
            if (rewriteQueue[i].state == "失败") {
                rewriteQueue[i] = rewriteQueue[i].copy(state = "排队中", error = null)
                updateChapterStatus(rewriteQueue[i].novelId, rewriteQueue[i].chapterId, "排队中")
                count++
            }
        }
        statusMessage.value = if (count > 0) "已重新加入失败任务：$count 个" else "没有失败任务"
        saveQuietly()
        if (count > 0) scheduleRewriteWork(context)
    }

    fun clearFinishedJobs() {
        rewriteQueue.removeAll { it.state == "完成" }
        statusMessage.value = "已清理完成任务"
        saveQuietly()
    }

    private fun scheduleRewriteWork(context: Context) {
        val request = OneTimeWorkRequestBuilder<RewriteWorker>().build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork("mojiang_rewrite_queue", ExistingWorkPolicy.KEEP, request)
    }

    fun clearRewrite(chapterId: Long) {
        val novel = activeNovel ?: return
        val index = novel.chapters.indexOfFirst { it.id == chapterId }
        if (index >= 0) {
            novel.chapters[index] = novel.chapters[index].copy(rewrittenContent = "", status = "待加料")
            statusMessage.value = "已清空该章加料结果"
            saveQuietly()
        }
    }

    fun exportRange(context: Context, start: Int, end: Int, currentOnly: Boolean = false): File? {
        init(context)
        return runCatching {
            val novel = activeNovel ?: error("请先导入小说")
            val safeStart = start.coerceAtLeast(1)
            val safeEnd = end.coerceAtLeast(safeStart)
            val selected = if (currentOnly) {
                novel.chapters.filter { it.index == safeStart }
            } else {
                novel.chapters.filter { it.index in safeStart..safeEnd }
            }
            require(selected.isNotEmpty()) { "没有可导出的章节" }
            val dir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
            val fileName = "${novel.title.sanitizeFileName()}_${safeStart}-${if (currentOnly) safeStart else safeEnd}_${System.currentTimeMillis()}.txt"
            val outFile = File(dir, fileName)
            val body = buildString {
                appendLine(novel.title)
                appendLine("导出时间：${nowText()}")
                appendLine("章节范围：第 $safeStart - ${if (currentOnly) safeStart else safeEnd} 章")
                appendLine("说明：优先导出加料后内容；没有加料结果的章节自动使用原文。")
                appendLine()
                selected.forEach { chapter ->
                    appendLine(chapter.title)
                    appendLine()
                    val content = if (preferRewrittenExport.value && chapter.rewrittenContent.isNotBlank()) chapter.rewrittenContent else chapter.originalContent
                    appendLine(content.trim())
                    appendLine()
                    appendLine("----------------------------------------")
                    appendLine()
                }
            }
            outFile.writeText(body, Charsets.UTF_8)
            exportRecords.add(0, ExportRecordData(fileName, outFile.absolutePath, "第 $safeStart-${if (currentOnly) safeStart else safeEnd} 章 · ${selected.size} 章", "成功", nowText()))
            statusMessage.value = "导出成功：$fileName"
            saveQuietly()
            outFile
        }.onFailure {
            statusMessage.value = "导出失败：${it.message ?: "未知错误"}"
        }.getOrNull()
    }

    fun saveQuietly() {
        if (!autoSaveEnabled.value && loaded) return
        runCatching {
            val context = appContext ?: return
            ensureDefaults()
            val state = PersistedState(
                nextId = idGen.get(),
                activeNovelId = activeNovelId.value,
                novels = novels.toList(),
                prompts = promptTemplates.toList(),
                providers = providers.toList(),
                exports = exportRecords.take(50),
                queue = rewriteQueue.takeLast(300),
                preferRewrittenExport = preferRewrittenExport.value,
                longMemorySummary = longMemorySummary.value
            )
            storeFile(context).writeText(json.encodeToString(state), Charsets.UTF_8)
        }
    }

    private fun loadFromDisk() {
        runCatching {
            val context = appContext ?: return
            val file = storeFile(context)
            if (!file.exists()) return
            val state = json.decodeFromString<PersistedState>(file.readText(Charsets.UTF_8))
            idGen.set(state.nextId.coerceAtLeast(1))
            activeNovelId.value = state.activeNovelId
            novels.clear(); novels.addAll(state.novels)
            promptTemplates.clear(); promptTemplates.addAll(state.prompts)
            providers.clear(); providers.addAll(state.providers)
            exportRecords.clear(); exportRecords.addAll(state.exports)
            rewriteQueue.clear(); rewriteQueue.addAll(state.queue)
            preferRewrittenExport.value = state.preferRewrittenExport
            longMemorySummary.value = state.longMemorySummary
            statusMessage.value = activeNovel?.let { "已恢复项目：${it.title} · ${it.chapters.size} 章" } ?: "已恢复本地数据"
        }.onFailure {
            statusMessage.value = "本地数据恢复失败，已进入安全默认模式"
        }
    }

    private fun ensureDefaults() {
        if (promptTemplates.isEmpty()) promptTemplates.addAll(defaultPrompts())
        if (providers.isEmpty()) providers.addAll(defaultProviders())
        if (providers.none { it.isDefault }) providers[0] = providers[0].copy(isDefault = true)
    }

    private fun storeFile(context: Context): File = File(context.filesDir, "mojiang_state.json")

    private fun updateChapterStatus(novelId: Long, chapterId: Long, status: String) {
        val novel = novels.firstOrNull { it.id == novelId } ?: return
        val index = novel.chapters.indexOfFirst { it.id == chapterId }
        if (index >= 0) novel.chapters[index] = novel.chapters[index].copy(status = status)
    }

    private fun updateJobState(jobId: Long, state: String, error: String?) {
        val index = rewriteQueue.indexOfFirst { it.id == jobId }
        if (index >= 0) rewriteQueue[index] = rewriteQueue[index].copy(state = state, error = error, updatedAt = nowText())
    }

    private fun refreshLongMemory(novelId: Long) {
        val novel = novels.firstOrNull { it.id == novelId } ?: return
        val finished = novel.chapters.filter { it.rewrittenContent.isNotBlank() }.sortedBy { it.index }
        val last = finished.takeLast(8).joinToString("\n") { chapter ->
            "第${chapter.index}章 ${chapter.title}：" + (chapter.rewrittenContent.ifBlank { chapter.originalContent }).replace(Regex("\\s+"), " ").take(180)
        }
        val names = Regex("[\\u4e00-\\u9fa5]{2,4}").findAll(novel.chapters.take(12).joinToString("\n") { it.originalContent.take(1200) })
            .map { it.value }
            .filterNot { it.startsWith("第") || it in setOf("他们", "自己", "什么", "一个", "没有", "只是", "已经") }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(18)
            .joinToString("、") { it.key }
            .ifBlank { "待自动识别" }
        longMemorySummary.value = buildString {
            appendLine("【长篇记忆摘要】")
            appendLine("项目：${novel.title}；总章数：${novel.chapters.size}；已处理：${finished.size}。")
            appendLine("【高频角色/称呼候选】$names")
            appendLine("【近期剧情锚点】")
            append(last.ifBlank { "暂无已处理章节，首章将仅依据原文与提示词处理。" })
        }.take(5000)
    }

    private fun buildContextForChapter(novel: NovelProject, chapterIndex: Int): String {
        val previous = novel.chapters.filter { it.index < chapterIndex }.takeLast(3).joinToString("\n") {
            "第${it.index}章 ${it.title}：" + (it.rewrittenContent.ifBlank { it.originalContent }).replace(Regex("\\s+"), " ").take(220)
        }
        return buildString {
            appendLine(longMemorySummary.value)
            appendLine("【当前章前文窗口】")
            append(previous.ifBlank { "当前为开篇或缺少前文窗口。" })
            appendLine()
            appendLine("【稳定性约束】不得改名、不得跳章、不得突然切换人称/世界观；补充细节必须服务原剧情。")
        }.take(6500)
    }

    private fun queryDisplayName(context: Context, uri: Uri): String {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "未命名.txt"
    }

    private fun readTextRobust(context: Context, uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
        require(bytes.isNotEmpty()) { "无法读取文件" }
        val utf8 = runCatching { bytes.toString(Charsets.UTF_8) }.getOrDefault("")
        return if ('�' in utf8.take(2000)) {
            runCatching { bytes.toString(charset("GB18030")) }.getOrDefault(utf8)
        } else utf8
    }

    private fun splitChapters(text: String): List<Pair<String, String>> {
        val normalized = text.replace("\uFEFF", "").replace("\r\n", "\n").replace("\r", "\n").trim()
        if (normalized.isBlank()) return emptyList()
        val regex = Regex("""(?m)^\s*((?:第\s*[零〇一二两三四五六七八九十百千万\d]+\s*[章节回卷集部篇].{0,60})|(?:Chapter\s+\d+.{0,60})|(?:CHAPTER\s+\d+.{0,60})|(?:番外.{0,60}))\s*$""")
        val markers = regex.findAll(normalized).toList()
        if (markers.size < 2) {
            return normalized.chunked(7000).mapIndexed { i, part -> "自动分块 ${i + 1}" to part.trim() }.filter { it.second.isNotBlank() }
        }
        return markers.mapIndexedNotNull { i, m ->
            val start = m.range.first
            val end = if (i + 1 < markers.size) markers[i + 1].range.first else normalized.length
            val content = normalized.substring(start, end).trim()
            if (content.isBlank()) null else m.groupValues[1].trim().take(100) to content
        }.ifEmpty { listOf("全文" to normalized) }
    }
    private fun defaultPrompts() = listOf(
        PromptTemplate(idGen.getAndIncrement(), "通用加料模板", "在不破坏原剧情主线的前提下，加强环境、动作、心理、氛围和情绪推进。保持角色性格稳定，保留原有人名、地名和关键事件。"),
        PromptTemplate(idGen.getAndIncrement(), "文风一致模板", "模仿原文叙述节奏和用词习惯，不要突然改变视角、人设和时代背景。补充细节时优先服务剧情连贯性。"),
        PromptTemplate(idGen.getAndIncrement(), "细节扩写模板", "扩写时优先增加感官细节、人物微动作、场景压迫感和对话间停顿。不要水文，不要改变结局，不要新增无关角色。")
    )

    private fun defaultProviders() = listOf(
        ModelProvider(1, "主力 DeepSeek", "DeepSeek", "deepseek-chat", "https://api.deepseek.com/v1", "未保存", "", connected = false, isDefault = true, lastSyncNote = "待测试连接"),
        ModelProvider(2, "OpenRouter 备用", "OpenRouter", "openai/gpt-4o-mini", "https://openrouter.ai/api/v1", "未保存", "", connected = false, isDefault = false, lastSyncNote = "待测试连接"),
        ModelProvider(3, "自定义中转", "Custom", "your-model-name", "https://your-endpoint/v1", "未保存", "", connected = false, isDefault = false, lastSyncNote = "待配置")
    )

    private fun String.sanitizeFileName(): String = replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifBlank { "未命名" }
    private fun nowText(): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
}

@Serializable
data class PersistedState(
    val nextId: Long = 1,
    val activeNovelId: Long? = null,
    val novels: List<NovelProject> = emptyList(),
    val prompts: List<PromptTemplate> = emptyList(),
    val providers: List<ModelProvider> = emptyList(),
    val exports: List<ExportRecordData> = emptyList(),
    val queue: List<RewriteJob> = emptyList(),
    val preferRewrittenExport: Boolean = true,
    val longMemorySummary: String = "尚未生成长篇记忆"
)

@Serializable
data class NovelProject(
    val id: Long,
    val title: String,
    val fileName: String,
    val importedAt: String = "",
    val chapters: MutableList<ChapterData>
)

@Serializable
data class ChapterData(
    val id: Long,
    val novelId: Long,
    val index: Int,
    val title: String,
    val originalContent: String,
    val rewrittenContent: String,
    val status: String
) {
    val wordCount: Int get() = originalContent.length
}

@Serializable
data class PromptTemplate(
    val id: Long,
    val title: String,
    val content: String
)

@Serializable
data class ModelProvider(
    val id: Int,
    val profileName: String,
    val providerName: String,
    val model: String,
    val endpoint: String,
    val apiKeyMasked: String,
    val apiKey: String = "",
    val connected: Boolean,
    val isDefault: Boolean = false,
    val lastSyncNote: String = "未拉取模型"
)

@Serializable
data class RewriteJob(
    val id: Long,
    val novelId: Long,
    val chapterId: Long,
    val chapterIndex: Int,
    val prompt: String,
    val intensity: Int,
    val keepPlot: Boolean,
    val preserveNames: Boolean,
    val state: String,
    val createdAt: String,
    val updatedAt: String = createdAt,
    val error: String? = null
)

@Serializable
data class ExportRecordData(
    val fileName: String,
    val path: String,
    val detail: String,
    val state: String,
    val createdAt: String
)
