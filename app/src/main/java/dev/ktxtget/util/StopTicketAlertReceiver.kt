package dev.ktxtget.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

/**
 * Lets the user silence [NotificationHelper]'s looping strong alert (alarm sound + repeating
 * vibration) directly from the notification's action button, without switching away from
 * whatever app (e.g. 코레일톡) they're completing the booking in.
 */
class StopTicketAlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper.stopUserAlert(context)
        NotificationManagerCompat.from(context).cancel(NotificationHelper.NOTIFICATION_ID_TICKET_ALERT)
    }
}
