package com.example.uptmtransportation.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.uptmtransportation.MainActivity
import com.example.uptmtransportation.R
import com.example.uptmtransportation.student.MyBookingActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

object NotificationHelper {

    private const val TAG = "NotificationHelper"
    private const val CHANNEL_ID = "uptm_transportation_channel"
    private const val CHANNEL_NAME = "UPTM Transportation"

    // Save FCM token for user
    fun saveUserToken(userId: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d(TAG, "FCM Token for user $userId: $token")

                // Save to Firestore
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .update("fcmToken", token)
                    .addOnSuccessListener {
                        Log.d(TAG, "Token saved successfully")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error saving token", e)
                    }
            }
        }
    }

    // Send notification to specific user (both in-app and push)
    fun sendNotificationToUser(userId: String, title: String, message: String, type: String, bookingId: String = "") {
        Log.d(TAG, "Sending notification to user: $userId, type: $type")

        // 1. Save notification to Firestore for in-app display
        saveInAppNotification(userId, title, message, type, bookingId)

        // 2. Get user's FCM token and send push notification
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val token = document.getString("fcmToken")
                if (token != null && token.isNotEmpty()) {
                    // In real implementation, you would call Firebase Cloud Function here
                    // For now, we'll just log it
                    Log.d(TAG, "Would send push notification to token: $token")

                    // You can also send local notification if app is in foreground
                    sendLocalNotification(title, message, type, bookingId)
                }
            }
    }

    // Save in-app notification to Firestore
    private fun saveInAppNotification(userId: String, title: String, message: String, type: String, bookingId: String) {
        val notification = hashMapOf(
            "userId" to userId,
            "title" to title,
            "message" to message,
            "type" to type,
            "bookingId" to bookingId,
            "timestamp" to System.currentTimeMillis(),
            "read" to false
        )

        FirebaseFirestore.getInstance()
            .collection("notifications")
            .add(notification)
            .addOnSuccessListener {
                Log.d(TAG, "In-app notification saved for user $userId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving in-app notification", e)
            }
    }

    // Send local notification (when app is open)
    fun sendLocalNotification(title: String, message: String, type: String, bookingId: String = "", context: Context? = null) {
        val ctx = context ?: return

        val intent = Intent(ctx, MyBookingActivity::class.java)
        intent.putExtra("notification_type", type)
        intent.putExtra("booking_id", bookingId)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

        val pendingIntent = PendingIntent.getActivity(
            ctx, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(bookingId.hashCode(), notificationBuilder.build())
        Log.d(TAG, "Local notification sent: $title")
    }
}