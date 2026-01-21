package com.example.gharbato.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.gharbato.R
import com.example.gharbato.view.NotificationActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    /**
     * Called when FCM token is refreshed or created
     * This token is used to send push notifications to this device
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        Log.d("FCM", "🔑 New FCM Token: $token")

        // Save FCM token to Firebase for current user
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseDatabase.getInstance().reference
                .child("Users")
                .child(userId)
                .child("fcmToken")
                .setValue(token)
                .addOnSuccessListener {
                    Log.d("FCM", "✅ Token saved successfully for user: $userId")
                }
                .addOnFailureListener { e ->
                    Log.e("FCM", "❌ Failed to save token: ${e.message}")
                }
        } else {
            Log.d("FCM", "⚠️ No user logged in, token not saved")
        }
    }

    /**
     * Called when a message is received from Firebase Cloud Messaging
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d("FCM", "📨 Message received from: ${message.from}")

        // Handle notification payload (when sent from Firebase Console)
        message.notification?.let {
            val title = it.title ?: "Gharbato"
            val body = it.body ?: ""
            val type = message.data["type"] ?: "system"
            val imageUrl = message.data["imageUrl"] ?: ""

            Log.d("FCM", "📬 Notification: $title - $body")
            sendNotification(title, body, type, imageUrl)
        }

        // Handle data payload (when sent from your backend/server)
        if (message.data.isNotEmpty()) {
            Log.d("FCM", "📦 Message data payload: ${message.data}")

            val title = message.data["title"] ?: "Gharbato"
            val body = message.data["message"] ?: message.data["body"] ?: ""
            val type = message.data["type"] ?: "system"
            val imageUrl = message.data["imageUrl"] ?: ""

            // Only send notification if there was no notification payload
            // (to avoid duplicate notifications)
            if (message.notification == null) {
                Log.d("FCM", "📬 Data-only message: $title - $body")
                sendNotification(title, body, type, imageUrl)
            }
        }
    }

    /**
     * Show notification on device
     */
    private fun sendNotification(title: String, messageBody: String, type: String, imageUrl: String) {
        // Intent to open NotificationActivity when user clicks notification
        val intent = Intent(this, NotificationActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("notification_type", type)
            putExtra("from_push", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(), // Unique request code
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "gharbato_notifications"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Build the notification
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.outline_notifications_24) // Your notification icon
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true) // Dismiss notification when clicked
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody)) // Show full message

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android 8.0 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Gharbato Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for property updates and messages"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Show the notification
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())

        Log.d("FCM", "🔔 Notification shown: $title")
    }
}