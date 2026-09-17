package com.example.examcountdown

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * 桌面小组件核心：渲染倒计时牌、按尺寸偏好/桌面格子切换布局、响应定时刷新。
 */
class CountdownWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            val views = buildViews(context, appWidgetManager, appWidgetId)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        UpdateScheduler.scheduleMidnight(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        val views = buildViews(context, appWidgetManager, appWidgetId)
        appWidgetManager.updateAppWidget(appWidgetId, views)
        UpdateScheduler.scheduleMidnight(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED -> {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(
                    ComponentName(context, CountdownWidgetProvider::class.java)
                )
                onUpdate(context, manager, ids)
            }
        }
    }

    private fun buildViews(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int
    ): RemoteViews {
        val minWidth = manager.getAppWidgetOptions(appWidgetId)
            .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)

        val sizeMode = CountdownEngine.getSizeMode(context)
        val useLarge = sizeMode == CountdownEngine.SIZE_LARGE
        val useSmall = when (sizeMode) {
            CountdownEngine.SIZE_SMALL -> true
            CountdownEngine.SIZE_MEDIUM, CountdownEngine.SIZE_LARGE -> false
            else -> minWidth < 220 // 自动：跟随桌面格子宽度
        }

        val layout = when {
            useSmall -> R.layout.widget_countdown_small
            useLarge -> R.layout.widget_countdown_large
            else -> R.layout.widget_countdown
        }
        val views = RemoteViews(context.packageName, layout)

        val target = CountdownEngine.parseDate(CountdownEngine.getExamDate(context, appWidgetId))
            ?: return views

        val days = CountdownEngine.calendarDaysLeft(target)
        val displayDays = if (days > 0) days else 0L

        views.setTextViewText(R.id.days_display, String.format("%03d", displayDays))
        views.setTextViewText(R.id.date_chip, CountdownEngine.formatDot(target))
        views.setTextViewText(R.id.footer_date, CountdownEngine.formatSlash(target))

        val status = when {
            days == 0L -> "就是今天，全力以赴"
            days > 0L -> "KEEP GOING"
            else -> "考试已结束"
        }
        views.setTextViewText(R.id.status_text, status)

        val lightOn = days >= 0L
        views.setImageViewResource(
            R.id.power_light,
            if (lightOn) R.drawable.power_light else R.drawable.power_light_off
        )

        // 仅大号布局才有这两个附加信息
        if (useLarge) {
            views.setTextViewText(R.id.weeks_value, CountdownEngine.weeksLabel(days))
            views.setTextViewText(R.id.weekday_value, CountdownEngine.weekdayLabel(target))
        }
        return views
    }

    companion object {
        const val ACTION_REFRESH = "com.example.examcountdown.ACTION_REFRESH"

        fun refreshPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, CountdownWidgetProvider::class.java)
                .setAction(ACTION_REFRESH)
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}