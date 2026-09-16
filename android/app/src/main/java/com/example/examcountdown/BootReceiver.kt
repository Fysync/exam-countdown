package com.example.examcountdown

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * 开机/升级后恢复小组件刷新调度，防止跨天数字不更新。
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(
                    ComponentName(context, CountdownWidgetProvider::class.java)
                )
                if (ids.isNotEmpty()) {
                    CountdownWidgetProvider().onUpdate(context, manager, ids)
                    UpdateScheduler.scheduleMidnight(context)
                }
            }
        }
    }
}