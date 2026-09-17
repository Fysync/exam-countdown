package com.example.examcountdown

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.widget.RemoteViews
import androidx.core.content.res.ResourcesCompat
import kotlin.math.ceil

/**
 * 桌面小组件核心：用 DSEG7 数码管字体把数字渲染成图片再交给小组件，
 * 绕开部分桌面（如一加）不渲染 RemoteViews 自定义字体的问题。
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
        val small = minWidth < 220

        val layout = if (small) R.layout.widget_countdown_small else R.layout.widget_countdown
        val views = RemoteViews(context.packageName, layout)

        val target = CountdownEngine.parseDate(CountdownEngine.getExamDate(context, appWidgetId))
            ?: return views

        val days = CountdownEngine.calendarDaysLeft(target)
        val displayDays = if (days > 0) days else 0L

        views.setImageViewBitmap(
            R.id.days_image,
            renderDigits(context, String.format("%03d", displayDays), 360, COLOR_AMBER, true)
        )
        views.setImageViewBitmap(
            R.id.footer_date_image,
            renderDigits(context, CountdownEngine.formatDot(target), 34, COLOR_DATE, false)
        )

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
        return views
    }

    /** 用 DSEG7 字体把数字渲染成琥珀色带光晕的图片。 */
    private fun renderDigits(
        context: Context,
        text: String,
        textSizePx: Int,
        color: Int,
        glow: Boolean
    ): Bitmap {
        val typeface = ResourcesCompat.getFont(context, R.font.dseg7_classic_bold)
            ?: Typeface.MONOSPACE
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            this.textSize = textSizePx.toFloat()
            this.color = color
            if (glow) setShadowLayer(textSizePx * 0.05f, 0f, 0f, 0xFFF5BB56.toInt())
        }

        val pad = if (glow) textSizePx / 12 + 6 else 6
        val width = ceil(paint.measureText(text)).toInt() + pad * 2
        val height = textSizePx + pad * 2

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val fm = paint.fontMetrics
        val baseline = (height - fm.ascent - fm.descent) / 2f
        canvas.drawText(text, (width - paint.measureText(text)) / 2f, baseline, paint)
        return bitmap
    }

    companion object {
        const val ACTION_REFRESH = "com.example.examcountdown.ACTION_REFRESH"

        private const val COLOR_AMBER = 0xFFF1AD3D.toInt()
        private const val COLOR_DATE = 0xFFADBC98.toInt()

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