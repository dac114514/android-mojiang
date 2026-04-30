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
