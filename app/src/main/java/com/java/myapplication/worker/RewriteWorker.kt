package com.java.myapplication.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.java.myapplication.data.LocalNovelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 稳定优先的后台改写队列 Worker。
 * 每次只处理少量章节，避免长时间占用内存导致 Android 杀进程；若仍有队列，会自动续排下一轮。
 */
class RewriteWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        runCatching {
            LocalNovelStore.init(applicationContext)
            val hasMore = LocalNovelStore.processNextRewriteBatch(maxItems = 3)
            if (hasMore) {
                val next = OneTimeWorkRequestBuilder<RewriteWorker>().build()
                WorkManager.getInstance(applicationContext).enqueue(next)
            }
            Result.success()
        }.getOrElse { e ->
            LocalNovelStore.statusMessage.value = "后台处理异常：${e.message ?: "未知错误"}"
            LocalNovelStore.saveQuietly()
            Result.retry()
        }
    }
}
