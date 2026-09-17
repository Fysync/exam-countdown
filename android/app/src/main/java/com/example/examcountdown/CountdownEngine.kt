package com.example.examcountdown

import android.content.Context
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * 日期计算、每个小组件独立的考试日期存储，以及全局设置。
 */
object CountdownEngine {

    const val DEFAULT_EXAM_DATE = "2027-12-25"

    // 尺寸模式
    const val SIZE_AUTO = "auto"
    const val SIZE_SMALL = "small"
    const val SIZE_MEDIUM = "medium"
    const val SIZE_LARGE = "large"

    private const val PREFS = "exam_countdown_prefs"
    private const val SETTINGS_PREFS = "exam_countdown_settings"

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val weekdays = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")

    /* ---------- 每个小组件的考试日期 ---------- */

    fun getExamDate(context: Context, appWidgetId: Int): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString("exam_$appWidgetId", null) ?: getDefaultExamDate(context)
    }

    fun saveExamDate(context: Context, appWidgetId: Int, date: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("exam_$appWidgetId", date)
            .apply()
    }

    /* ---------- 全局设置 ---------- */

    fun getDefaultExamDate(context: Context): String =
        context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
            .getString("exam_date", DEFAULT_EXAM_DATE) ?: DEFAULT_EXAM_DATE

    fun setDefaultExamDate(context: Context, date: String) {
        context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("exam_date", date)
            .apply()
    }

    fun getSizeMode(context: Context): String =
        context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
            .getString("size_mode", SIZE_AUTO) ?: SIZE_AUTO

    fun setSizeMode(context: Context, mode: String) {
        context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("size_mode", mode)
            .apply()
    }

    /* ---------- 日期工具 ---------- */

    fun parseDate(text: String): LocalDate? =
        runCatching { LocalDate.parse(text, dateFormatter) }.getOrNull()

    /** 日历天差：今天是 0，明天是 1，考试当天返回 0，已过返回负数。 */
    fun calendarDaysLeft(target: LocalDate): Long =
        ChronoUnit.DAYS.between(LocalDate.now(), target)

    fun formatDot(date: LocalDate): String =
        String.format("%d.%02d.%02d", date.year, date.monthValue, date.dayOfMonth)

    fun formatSlash(date: LocalDate): String =
        String.format("%d / %02d / %02d", date.year, date.monthValue, date.dayOfMonth)

    fun weekdayLabel(date: LocalDate): String =
        weekdays[date.dayOfWeek.value % 7]

    fun weeksLabel(days: Long): String =
        if (days <= 0) "—" else "${days / 7} 周 ${days % 7} 天"
}