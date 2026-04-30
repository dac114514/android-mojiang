package com.java.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.java.myapplication.data.LocalNovelStore
import com.java.myapplication.data.ModelProvider
import com.java.myapplication.network.ModelApiClient
import com.java.myapplication.ui.components.DotBadge
import com.java.myapplication.ui.components.SectionTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelsScreen() {
    val providers = LocalNovelStore.providers
    val scope = rememberCoroutineScope()
    val availableModels = remember {
        mutableStateListOf("deepseek-chat", "deepseek-reasoner", "openai/gpt-4o-mini", "claude-3.5-sonnet", "gemini-2.0-flash")
    }

    val defaultProvider = providers.firstOrNull { it.isDefault } ?: providers.firstOrNull() ?: ModelProvider(
        id = 1,
        profileName = "默认配置",
        providerName = "Custom",
        model = "custom-model",
        endpoint = "https://your-endpoint/v1",
        apiKeyMasked = "未保存",
        apiKey = "",
        connected = false,
        isDefault = true,
        lastSyncNote = "待配置"
    )

    var profileMenuExpanded by remember { mutableStateOf(false) }
    var profileName by remember { mutableStateOf(defaultProvider.profileName) }
    var providerName by remember { mutableStateOf(defaultProvider.providerName) }
    var endpoint by remember { mutableStateOf(defaultProvider.endpoint) }
    var modelName by remember { mutableStateOf(defaultProvider.model) }
    var apiKey by remember { mutableStateOf("") }
    var selectedProviderId by remember { mutableIntStateOf(defaultProvider.id) }
    var isCreatingNewProfile by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("已载入默认配置，可直接测试连接或拉取模型。") }

    fun maskKey(raw: String): String {
        if (raw.isBlank()) return "未保存"
        val prefix = raw.take(3)
        val suffix = raw.takeLast(minOf(2, raw.length))
        return prefix + "****" + suffix
    }

    fun currentDraftProvider(): ModelProvider {
        val existing = providers.firstOrNull { it.id == selectedProviderId }
        val rawKey = apiKey.ifBlank { existing?.apiKey.orEmpty() }
        return ModelProvider(
            id = if (selectedProviderId > 0) selectedProviderId else LocalNovelStore.nextProviderId(),
            profileName = profileName.ifBlank { "未命名配置" },
            providerName = providerName.ifBlank { "Custom" },
            model = modelName.ifBlank { "未设置模型" },
            endpoint = endpoint.ifBlank { "未设置地址" },
            apiKeyMasked = if (rawKey.isBlank()) existing?.apiKeyMasked ?: "未保存" else maskKey(rawKey),
            apiKey = rawKey,
            connected = existing?.connected ?: false,
            isDefault = existing?.isDefault ?: false,
            lastSyncNote = existing?.lastSyncNote ?: "尚未拉取模型"
        )
    }

    fun loadProvider(provider: ModelProvider) {
        selectedProviderId = provider.id
        profileName = provider.profileName
        providerName = provider.providerName
        endpoint = provider.endpoint
        modelName = provider.model
        apiKey = ""
        isCreatingNewProfile = false
        statusText = "已切换到配置：${provider.profileName}"
    }

    fun resetForNewProfile(copyCurrent: Boolean = true) {
        val source = providers.firstOrNull { it.id == selectedProviderId } ?: defaultProvider
        selectedProviderId = -1
        profileName = if (copyCurrent) "${source.profileName} 副本" else ""
        providerName = if (copyCurrent) source.providerName else ""
        endpoint = if (copyCurrent) source.endpoint else ""
        modelName = if (copyCurrent) source.model else ""
        apiKey = ""
        isCreatingNewProfile = true
        statusText = if (copyCurrent) "已进入新建配置模式，可基于当前配置另存。" else "已新建空白配置。"
    }

    LazyColumn(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionTitle("模型提供商配置", "支持保存、选择、设为默认，并保留真实接入所需字段；当前交互已按后续功能流程做顺。")
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = profileMenuExpanded,
                        onExpandedChange = { profileMenuExpanded = !profileMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = if (isCreatingNewProfile) "新建配置中" else (providers.firstOrNull { it.id == selectedProviderId }?.profileName ?: profileName),
                            onValueChange = {},
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            readOnly = true,
                            label = { Text("已保存配置") },
                            supportingText = {
                                Text(if (isCreatingNewProfile) "当前未绑定已有配置，保存后会新增一条。" else "切换后会将该配置载入编辑区。")
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = profileMenuExpanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = profileMenuExpanded,
                            onDismissRequest = { profileMenuExpanded = false }
                        ) {
                            providers.forEach { provider ->
                                DropdownMenuItem(
                                    text = { Text(provider.profileName) },
                                    onClick = {
                                        loadProvider(provider)
                                        profileMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { resetForNewProfile(copyCurrent = true) }, modifier = Modifier.weight(1f)) {
                            Text("另存为新配置")
                        }
                        OutlinedButton(onClick = { resetForNewProfile(copyCurrent = false) }, modifier = Modifier.weight(1f)) {
                            Text("新建空白")
                        }
                    }
                    OutlinedTextField(value = profileName, onValueChange = { profileName = it }, modifier = Modifier.fillMaxWidth(), label = { Text("配置名称") })
                    OutlinedTextField(value = providerName, onValueChange = { providerName = it }, modifier = Modifier.fillMaxWidth(), label = { Text("提供商名称") })
                    OutlinedTextField(value = endpoint, onValueChange = { endpoint = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Base URL") })
                    OutlinedTextField(value = modelName, onValueChange = { modelName = it }, modifier = Modifier.fillMaxWidth(), label = { Text("当前模型") })
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("API Key") },
                        supportingText = {
                            val masked = providers.firstOrNull { it.id == selectedProviderId }?.apiKeyMasked
                            if (apiKey.isBlank() && !isCreatingNewProfile && !masked.isNullOrBlank()) {
                                Text("当前已保存密钥：$masked")
                            } else {
                                Text("未输入新密钥时，将沿用已保存的掩码信息。")
                            }
                        }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                val existingIndex = providers.indexOfFirst { it.id == selectedProviderId }
                                val item = currentDraftProvider().copy(
                                    id = if (existingIndex >= 0 && !isCreatingNewProfile) selectedProviderId else LocalNovelStore.nextProviderId(),
                                    connected = existingIndex >= 0 && !isCreatingNewProfile && providers[existingIndex].connected,
                                    isDefault = existingIndex >= 0 && !isCreatingNewProfile && providers[existingIndex].isDefault,
                                    lastSyncNote = if (existingIndex >= 0 && !isCreatingNewProfile) providers[existingIndex].lastSyncNote else "尚未拉取模型"
                                )
                                if (existingIndex >= 0 && !isCreatingNewProfile) {
                                    providers[existingIndex] = item
                                } else {
                                    providers.add(0, item)
                                }
                                LocalNovelStore.saveQuietly()
                                loadProvider(item)
                                statusText = "配置已保存：${item.profileName}"
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(if (isCreatingNewProfile) "保存为新配置" else "保存配置") }
                        OutlinedButton(
                            onClick = {
                                val currentId = selectedProviderId
                                for (i in providers.indices) {
                                    providers[i] = providers[i].copy(isDefault = providers[i].id == currentId)
                                }
                                LocalNovelStore.saveQuietly()
                                statusText = "已设为默认配置：${providers.firstOrNull { it.id == currentId }?.profileName ?: profileName}"
                            },
                            modifier = Modifier.weight(1f),
                            enabled = selectedProviderId != -1
                        ) { Text("设为默认") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                statusText = "正在从真实接口拉取模型列表……"
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) { ModelApiClient.listModels(currentDraftProvider()) }
                                    result.onSuccess { models ->
                                        availableModels.clear()
                                        availableModels.addAll(models.ifEmpty { listOf(modelName.ifBlank { "custom-model" }) })
                                        val index = providers.indexOfFirst { it.id == selectedProviderId }
                                        if (index >= 0) {
                                            providers[index] = providers[index].copy(lastSyncNote = "已同步 ${availableModels.size} 个模型")
                                            LocalNovelStore.saveQuietly()
                                        }
                                        statusText = "真实模型列表拉取成功，共 ${availableModels.size} 个。"
                                    }.onFailure { e ->
                                        statusText = "拉取失败：${e.message ?: "未知错误"}"
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("拉取模型") }
                        OutlinedButton(
                            onClick = {
                                statusText = "正在进行真实连接测试……"
                                scope.launch {
                                    val draft = currentDraftProvider()
                                    val result = withContext(Dispatchers.IO) { ModelApiClient.testConnection(draft) }
                                    result.onSuccess { message ->
                                        val index = providers.indexOfFirst { it.id == selectedProviderId }
                                        if (index >= 0) {
                                            providers[index] = providers[index].copy(
                                                connected = true,
                                                lastSyncNote = message
                                            )
                                            LocalNovelStore.saveQuietly()
                                        }
                                        statusText = message
                                    }.onFailure { e ->
                                        val index = providers.indexOfFirst { it.id == selectedProviderId }
                                        if (index >= 0) {
                                            providers[index] = providers[index].copy(
                                                connected = false,
                                                lastSyncNote = "连接失败：${e.message ?: "未知错误"}"
                                            )
                                            LocalNovelStore.saveQuietly()
                                        }
                                        statusText = "连接失败：${e.message ?: "未知错误"}"
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = selectedProviderId != -1
                        ) { Text("测试连接") }
                    }
                    Text(statusText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            SectionTitle("可用模型", "点击模型名填入当前模型；DeepSeek/OpenRouter 等兼容 OpenAI 的 /models 接口会真实拉取。")
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    availableModels.chunked(2).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            rowItems.forEach { item ->
                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .clickable {
                                            modelName = item
                                            statusText = "已选择模型：$item"
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                )
                            }
                            if (rowItems.size == 1) {
                                Row(modifier = Modifier.weight(1f)) {}
                            }
                        }
                    }
                }
            }
        }
        item {
            SectionTitle("已保存配置", "这里负责摘要浏览与快速选择；编辑、另存、默认设定都在上方完成，逻辑更清晰。")
        }
        items(providers, key = { it.id }) { provider ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (provider.id == selectedProviderId && !isCreatingNewProfile) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { loadProvider(provider) }
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                        Text(provider.profileName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${provider.providerName} · ${provider.model}", style = MaterialTheme.typography.bodyMedium)
                        Text(provider.endpoint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("密钥：${provider.apiKeyMasked}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(provider.lastSyncNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DotBadge(
                        text = when {
                            provider.isDefault -> "默认"
                            provider.connected -> "已连接"
                            else -> "未测试"
                        },
                        color = when {
                            provider.isDefault -> MaterialTheme.colorScheme.primary
                            provider.connected -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.tertiary
                        }
                    )
                }
            }
        }
    }
}
