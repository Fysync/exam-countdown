package com.example.examcountdown

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.examcountdown.databinding.ActivityConfigBinding
import java.time.LocalDate

/**
 * 小组件配置页：选择考试日期（默认 2027-12-25）。
 */
class CountdownWidgetConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfigBinding
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

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

        val examDate = CountdownEngine.getExamDate(this, appWidgetId)
        val initial = CountdownEngine.parseDate(examDate) ?: LocalDate.of(2027, 12, 25)
        binding.datePicker.init(initial.year, initial.monthValue - 1, initial.dayOfMonth, null)

        binding.btnConfirm.setOnClickListener {
            val date = LocalDate.of(
                binding.datePicker.year,
                binding.datePicker.month + 1,
                binding.datePicker.dayOfMonth
            )
            if (date.isBefore(LocalDate.now())) {
                Toast.makeText(this, "考试日期不能早于今天", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            CountdownEngine.saveExamDate(this, appWidgetId, date.toString())

            val manager = AppWidgetManager.getInstance(this)
            CountdownWidgetProvider().onUpdate(this, manager, intArrayOf(appWidgetId))

            val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(Activity.RESULT_OK, result)
            finish()
        }

        binding.btnCancel.setOnClickListener { finish() }
    }
}