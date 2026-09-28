package com.familyguardian.app.capture

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.familyguardian.app.data.repository.CaptureRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Reads WhatsApp's own notifications and turns them into a capture log.
 *
 * Scope is deliberately limited to what WhatsApp itself chooses to publish: the
 * sender and the notification preview. It cannot read message history, archived
 * chats, or anything WhatsApp does not surface as a notification.
 */
class WhatsAppNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository by lazy { CaptureRepository.getInstance(applicationContext) }

    override fun onListenerConnected() {
        super.onListenerConnected()
        scope.launch { repository.onListenerStateChanged(isConnected = true) }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        scope.launch { repository.onListenerStateChanged(isConnected = false) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        if (!WhatsAppNotificationParser.isWatched(notification.packageName)) return

        val extras = notification.notification?.extras ?: return
        val raw = RawNotification(
            packageName = notification.packageName,
            notificationKey = notification.key.orEmpty(),
            title = extras.read("android.title"),
            text = extras.read("android.text"),
            bigText = extras.read("android.bigText"),
            subText = extras.read("android.subText"),
            postTime = notification.postTime
        )

        val parsed = WhatsAppNotificationParser.parse(raw) ?: return
        scope.launch { repository.record(parsed, raw) }
    }

    private fun android.os.Bundle.read(key: String): String? =
        if (containsKey(key)) getCharSequence(key)?.toString() else null

    override fun onDestroy() {
        scope.launch { repository.onListenerStateChanged(isConnected = false) }
        super.onDestroy()
    }
}

object NotificationAccess {

    fun isGranted(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        val expected = ComponentName(context, WhatsAppNotificationListener::class.java)
        val flat = enabled.split(":").mapNotNull { ComponentName.unflattenFromString(it) }
        return flat.any { it == expected }
    }

    fun settingsIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * Some OEM builds hide the listener list behind the app info page, which is where
     * the user lands if the generic settings screen does not list our service.
     */
    fun appNotificationSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun logState(context: Context) {
        Log.i(TAG, "notification access granted=${isGranted(context)}")
    }

    private const val TAG = "NotificationAccess"
}
