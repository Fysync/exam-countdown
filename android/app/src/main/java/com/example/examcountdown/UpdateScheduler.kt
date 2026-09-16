package com.example.examcountdown

import android.app.AlarmManager
import android.content.Context
import java.time.LocalDate
import java.time.ZoneId

/**
 * 用不精确的 AlarmManager 调度午夜刷新，避开 Android 12+ 的 SCHEDULE_EXACT_ALARM 权限。
 * 配合 AppWidgetProviderInfo 里 30 分钟轮询作为保底。
 */
object UpdateScheduler {

    fun scheduleMidnight(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nextMidnight = LocalDate.now()
            .plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli() + 1000

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextMidnight,
            CountdownWidgetProvider.refreshPendingIntent(context)
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(CountdownWidgetProvider.refreshPendingIntent(context))
    }
}