package pt.ulisboa.tecnico.pharmacist.utils

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class NotificationWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    @SuppressLint("MissingPermission")
    override fun doWork(): Result {
        val notification = NotificationCompat.Builder(applicationContext, "default")
            .setSmallIcon(
                androidx.loader.R.drawable.notification_bg
            )
            .setContentTitle("Task completed")
            .setContentText("The background task has completed successfully.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        //NotificationManagerCompat.from(applicationContext).notify(1, notification)
        //println("OLAAAAAAAAA")
        return Result.success()
    }
}