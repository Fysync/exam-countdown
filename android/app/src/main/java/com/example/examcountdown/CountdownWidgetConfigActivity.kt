package com.example.examcountdown

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.examcountdown.databinding.ActivityConfigBinding
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * 小组件配置页：用 MaterialDatePicker 日历选择考试日期（默认 2027-12-25）。
 */
class CountdownWidgetConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfigBinding
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var handled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val initial = CountdownEngine.parseDate(CountdownEngine.getExamDate(this, appWidgetId))
            ?: LocalDate.of(2027, 12, 25)
        binding.currentDate.text = CountdownEngine.formatDot(initial)

        showDatePicker(initial)
    }

    private fun showDatePicker(initial: LocalDate) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("选择考试日期")
            .setSelection(initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            handled = true
            val date = Instant.ofEpochMilli(selection).atZone(ZoneOffset.UTC).toLocalDate()
            CountdownEngine.saveExamDate(this, appWidgetId, date.toString())

            val manager = AppWidgetManager.getInstance(this)
            CountdownWidgetProvider().onUpdate(this, manager, intArrayOf(appWidgetId))

            setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
            finish()
        }
        picker.addOnCancelListener { finish() }
        picker.addOnDismissListener { if (!handled) finish() }

        picker.show(supportFragmentManager, "exam_date_picker")
    }
}