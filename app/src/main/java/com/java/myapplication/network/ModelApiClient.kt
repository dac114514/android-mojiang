package com.java.myapplication.network

import com.java.myapplication.data.ModelProvider
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

object ModelApiClient {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    fun testConnection(provider: ModelProvider): Result<String> = runCatching {
        validateProvider(provider)
        val prompt = "只回复 OK，用于连接测试。"
        val reply = chat(provider, prompt, maxTokens = 16, timeoutMs = 20_000)
        if (reply.isBlank()) error("模型返回为空")
        "连接成功：${reply.take(40)}"
    }

    fun listModels(provider: ModelProvider): Result<List<String>> = runCatching {
        validateProvider(provider, requireModel = false)
        val endpoint = provider.endpoint.trim().trimEnd('/')
        val url = if (endpoint.endsWith("/models")) endpoint else "$endpoint/models"
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20_000
            readTimeout = 20_000
            setRequestProperty("Authorization", "Bearer ${provider.apiKey}")
            setRequestProperty("HTTP-Referer", "https://github.com/hongquangphung7044-oss/MojiangRewrite")
            setRequestProperty("X-Title", "MojiangRewrite")
        }
        val code = conn.responseCode
        val body = if (code in 200..299) {
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } else {
            val err = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            error("HTTP $code：${err.take(300)}")
        }
        val response = json.decodeFromString(ModelsResponse.serializer(), body)
        response.data.mapNotNull { it.id }.filter { it.isNotBlank() }.distinct()
    }

    fun rewriteChapter(
        provider: ModelProvider,
        title: String,
        original: String,
        userPrompt: String,
        memoryContext: String,
        intensity: Int,
        keepPlot: Boolean,
        preserveNames: Boolean
    ): Result<String> = runCatching {
        validateProvider(provider)
        val clippedOriginal = original.take(18_000)
        val prompt = buildString {
            appendLine("你是专业中文长篇小说改写助手。请直接输出改写后的章节正文，不要解释，不要输出 Markdown 标题。")
            appendLine("章节标题：$title")
            appendLine("加料强度：${intensity.coerceIn(1, 100)}%")
            appendLine("保留主线：${if (keepPlot) "是" else "允许小幅重构，但不得破坏核心剧情"}")
            appendLine("保留人名/称呼：${if (preserveNames) "必须保留" else "可轻微润色，但不得改名"}")
            appendLine("用户提示词：")
            appendLine(userPrompt.take(4000))
            appendLine("长篇一致性上下文：")
            appendLine(memoryContext.take(6500))
            appendLine("原文章节：")
            appendLine(clippedOriginal)
            appendLine("输出要求：保持原剧情、人设、世界观连续；补充环境、动作、心理、氛围；不要新增无关角色；不要遗漏原文关键事件。")
        }
        val reply = chat(provider, prompt, maxTokens = 4096, timeoutMs = 90_000).trim()
        require(reply.length >= 40) { "模型返回过短或为空" }
        reply
    }

    private fun validateProvider(provider: ModelProvider, requireModel: Boolean = true) {
        require(provider.endpoint.startsWith("http")) { "Base URL 必须以 http/https 开头" }
        require(provider.apiKey.isNotBlank()) { "请先保存 API Key" }
        if (requireModel) {
            require(provider.model.isNotBlank() && provider.model != "未设置模型") { "请填写模型名" }
        }
    }

    private fun chat(provider: ModelProvider, prompt: String, maxTokens: Int, timeoutMs: Int): String {
        val endpoint = provider.endpoint.trim().trimEnd('/')
        val url = if (endpoint.endsWith("/chat/completions")) endpoint else "$endpoint/chat/completions"
        val request = ChatRequest(
            model = provider.model.trim(),
            messages = listOf(
                ChatMessage("system", "你是稳定、严谨的中文长篇小说改写引擎。"),
                ChatMessage("user", prompt)
            ),
            temperature = 0.7,
            maxTokens = maxTokens
        )
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer ${provider.apiKey}")
            setRequestProperty("HTTP-Referer", "https://github.com/hongquangphung7044-oss/MojiangRewrite")
            setRequestProperty("X-Title", "MojiangRewrite")
        }
        conn.outputStream.use { it.write(json.encodeToString(request).toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val body = if (code in 200..299) {
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } else {
            val err = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            error("HTTP $code：${err.take(300)}")
        }
        val response = json.decodeFromString(ChatResponse.serializer(), body)
        return response.choices.firstOrNull()?.message?.content.orEmpty()
    }
}

@Serializable
private data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7,
    @SerialName("max_tokens") val maxTokens: Int = 2048
)

@Serializable
private data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
private data class ChatResponse(
    val choices: List<ChatChoice> = emptyList()
)

@Serializable
private data class ChatChoice(
    val message: ChatMessage? = null
)

@Serializable
private data class ModelsResponse(
    val data: List<ModelInfo> = emptyList()
)

@Serializable
private data class ModelInfo(
    val id: String? = null
)