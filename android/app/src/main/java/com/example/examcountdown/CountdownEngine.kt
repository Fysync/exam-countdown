package com.example.examcountdown

import android.content.Context
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * 日期计算与每个小组件独立的考试日期存储。
 */
object CountdownEngine {

    const val DEFAULT_EXAM_DATE = "2027-12-25"

    private const val PREFS = "exam_countdown_prefs"
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun getExamDate(context: Context, appWidgetId: Int): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString("exam_$appWidgetId", DEFAULT_EXAM_DATE) ?: DEFAULT_EXAM_DATE
    }

    fun saveExamDate(context: Context, appWidgetId: Int, date: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("exam_$appWidgetId", date)
            .apply()
    }

    fun parseDate(text: String): LocalDate? =
        runCatching { LocalDate.parse(text, dateFormatter) }.getOrNull()

    /** 日历天差：今天是 0，明天是 1，考试当天返回 0，已过返回负数。 */
    fun calendarDaysLeft(target: LocalDate): Long =
        ChronoUnit.DAYS.between(LocalDate.now(), target)

    fun formatDot(date: LocalDate): String =
        String.format("%d.%02d.%02d", date.year, date.monthValue, date.dayOfMonth)

    fun formatSlash(date: LocalDate): String =
        String.format("%d / %02d / %02d", date.year, date.monthValue, date.dayOfMonth)
}