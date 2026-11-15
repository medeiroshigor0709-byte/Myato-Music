package com.example.core.common

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.R
import com.example.core.database.AppPreferencesStore
import com.example.core.database.MyatoDatabase
import com.example.core.storage.BackupManager
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class BackupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefsStore = AppPreferencesStore(applicationContext)
        val prefs = prefsStore.preferencesFlow.first()
        val uriStr = prefs.autoBackupDirectoryUri
        if (uriStr.isBlank()) return Result.success()

        val db = MyatoDatabase.getInstance(applicationContext)
        val manager = BackupManager(applicationContext, db, prefsStore)
        val outcome = manager.exportBackupToDirectory(Uri.parse(uriStr), isAutomatic = true)
        notifyBackupResult(outcome.isSuccess, outcome.exceptionOrNull()?.localizedMessage)
        return if (outcome.isSuccess) Result.success() else Result.retry()
    }

    private fun notifyBackupResult(success: Boolean, errorMessage: String?) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.notification_channel_backup),
                NotificationManager.IMPORTANCE_LOW,
            )
            nm.createNotificationChannel(channel)
        }
        val text = if (success) {
            "Backup automático concluído sem sobrescrever backups anteriores."
        } else {
            "Falha no backup automático: ${errorMessage ?: "verifique a pasta de destino"}"
        }
        val notif = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("MYATO MUSIC Backup")
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        nm.notify(9401, notif)
    }

    companion object {
        private const val WORK_NAME = "myato_auto_backup_work"
        private const val CHANNEL_ID = "myato_backup_channel"

        fun schedule(context: Context, intervalDays: Int) {
            val wm = WorkManager.getInstance(context)
            if (intervalDays <= 0) {
                wm.cancelUniqueWork(WORK_NAME)
                return
            }
            val request = PeriodicWorkRequestBuilder<BackupWorker>(
                intervalDays.toLong(),
                TimeUnit.DAYS,
            ).build()
            wm.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}
