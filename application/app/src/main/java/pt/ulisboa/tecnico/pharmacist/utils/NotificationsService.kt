package pt.ulisboa.tecnico.pharmacist.utils

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class NotificationsService : FirebaseMessagingService() {

    // Send the FCM token to the backend when it's refreshed
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        sendFcmTokenToBackend(token)
    }

    // Handle incoming FCM messages here
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
    }

    // Code to send FCM token to the backend
    private fun sendFcmTokenToBackend(token: String) {
    }
}

class FcmService : Service() {
    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}