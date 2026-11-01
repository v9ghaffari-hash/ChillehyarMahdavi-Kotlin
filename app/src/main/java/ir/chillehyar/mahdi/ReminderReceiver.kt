package ir.chillehyar.mahdi

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver as AndroidBroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import java.util.Calendar

class ReminderReceiver : AndroidBroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "یادآورهای چله‌یار", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "یادآوری‌های برنامهٔ چله‌یار مهدوی"
            })
        }
        val permissionGranted = Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (permissionGranted) {
            val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(context, CHANNEL) else Notification.Builder(context)
            manager.notify(4002, builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("چله‌یار مهدوی")
                .setContentText("وقت عهد امروز و یک قدم به سوی بهتر شدن است.")
                .setAutoCancel(true).build())
        }
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (preferences.contains("reminder_hour")) {
            val next = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, preferences.getInt("reminder_hour", 9))
                set(Calendar.MINUTE, preferences.getInt("reminder_minute", 0))
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
            }
            val pending = PendingIntent.getBroadcast(context, REQUEST_CODE, Intent(context, ReminderReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val alarm = context.getSystemService(AlarmManager::class.java)
            try {
                if (Build.VERSION.SDK_INT >= 31 && !alarm.canScheduleExactAlarms()) alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
                else if (Build.VERSION.SDK_INT >= 23) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
                else alarm.setExact(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
            } catch (_: SecurityException) { alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending) }
        }
    }
    companion object {
        const val CHANNEL = "chillehyar_native"
        const val PREFS = "chillehyar_native"
        const val REQUEST_CODE = 71
    }
}

class BootReceiver : AndroidBroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED && intent?.action != "android.intent.action.MY_PACKAGE_REPLACED") return
        val preferences = context.getSharedPreferences(ReminderReceiver.PREFS, Context.MODE_PRIVATE)
        if (!preferences.contains("reminder_hour")) return
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, preferences.getInt("reminder_hour", 9))
            set(Calendar.MINUTE, preferences.getInt("reminder_minute", 0))
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val pending = PendingIntent.getBroadcast(context, ReminderReceiver.REQUEST_CODE, Intent(context, ReminderReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarm = context.getSystemService(AlarmManager::class.java)
        try {
            if (Build.VERSION.SDK_INT >= 31 && !alarm.canScheduleExactAlarms()) alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
            else if (Build.VERSION.SDK_INT >= 23) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
            else alarm.setExact(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
        } catch (_: SecurityException) { alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending) }
    }
}
