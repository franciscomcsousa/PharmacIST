package pt.ulisboa.tecnico.pharmacist.utils

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import pt.ulisboa.tecnico.pharmacist.R

class NotificationsService : FirebaseMessagingService() {

    // Create the notification channel if it doesn't exist
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    // Send the FCM token to the backend when it's refreshed
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        sendFcmTokenToBackend(token)
    }

    // Handle incoming FCM messages here
    @SuppressLint("MissingPermission")
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // TODO - ask permission for notifications in the navDrawer
        // Check if the message contains a notification payload
        remoteMessage.notification?.let {
            // Create a notification ID
            val notificationId = 1

            // Build the notification
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(it.title)
                .setContentText(it.body)
                .setSmallIcon(R.drawable.pill)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            // Show the notification
            with(NotificationManagerCompat.from(this)) {
                notify(notificationId, notification)
            }
        }
    }

    private fun createNotificationChannel() {
        val name = "notification"/*getString(R.string.channel_name)*/
        val descriptionText = "description"/*getString(R.string.channel_description)*/
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "notification_channel"
    }


    // Code to send FCM token to the backend
    private fun sendFcmTokenToBackend(token: String) {
        // TODO
    }
}

class FcmService : Service() {
    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}