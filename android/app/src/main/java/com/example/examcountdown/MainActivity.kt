package com.example.examcountdown

import android.animation.ValueAnimator
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.examcountdown.databinding.ActivityMainBinding
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * 主程序：两页切换 —— 管理台（设置考试日期 / 桌面尺寸 / 立即刷新）+ 桌面时钟（横屏常亮）。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var currentPage = PAGE_MANAGE
    private val handler = Handler(Looper.getMainLooper())
    private var clockRunning = false
    private var glowAnimator: ValueAnimator? = null

    private val clockTick = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        renderCurrentDate()
        renderSizeMode()
        bindEvents()
        showPage(PAGE_MANAGE)
    }

    override fun onResume() {
        super.onResume()
        if (currentPage == PAGE_CLOCK) enterClockMode()
    }

    override fun onPause() {
        if (currentPage == PAGE_CLOCK) exitClockMode()
        super.onPause()
    }

    /* ---------- 页面切换 ---------- */

    private fun showPage(page: Int) {
        currentPage = page
        val clock = page == PAGE_CLOCK
        binding.pageManage.visibility = if (clock) View.GONE else View.VISIBLE
        binding.pageClock.visibility = if (clock) View.VISIBLE else View.GONE
        stylePill(binding.tabManage, !clock)
        stylePill(binding.tabClock, clock)
        if (clock) enterClockMode() else exitClockMode()
    }

    /* ---------- 桌面时钟模式 ---------- */

    private fun enterClockMode() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars(true)
        startClock()
        startGlow()
    }

    private fun exitClockMode() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars(false)
        stopClock()
        stopGlow()
    }

    private fun hideSystemBars(hide: Boolean) {
        WindowCompat.setDecorFitsSystemWindows(window, !hide)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        if (hide) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun updateClock() {
        val now = LocalTime.now()
        binding.clockHour.text = String.format("%02d", now.hour)
        binding.clockMinute.text = String.format("%02d", now.minute)
        binding.clockSeconds.text = String.format("%02d", now.second)
        binding.clockColon.alpha = if (now.second % 2 == 0) 1f else 0.18f

        val today = LocalDate.now()
        binding.clockDate.text =
            "${CountdownEngine.formatDot(today)} ${CountdownEngine.weekdayLabel(today)}"

        val target = CountdownEngine.parseDate(CountdownEngine.getDefaultExamDate(this))
            ?: LocalDate.of(2027, 12, 25)
        val days = CountdownEngine.calendarDaysLeft(target)
        binding.clockCountdown.text = when {
            days > 0 -> "距研究生考试还有 $days 天"
            days == 0L -> "就是今天，全力以赴"
            else -> "考试已结束"
        }
    }

    private fun startClock() {
        if (clockRunning) return
        clockRunning = true
        updateClock()
        handler.postDelayed(clockTick, 500)
    }

    private fun stopClock() {
        clockRunning = false
        handler.removeCallbacks(clockTick)
    }

    private fun startGlow() {
        if (glowAnimator != null) return
        glowAnimator = ValueAnimator.ofFloat(0.15f, 0.45f).apply {
            duration = 2600
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { binding.clockGlow.alpha = it.animatedValue as Float }
            start()
        }
    }

    private fun stopGlow() {
        glowAnimator?.cancel()
        glowAnimator = null
        binding.clockGlow.alpha = 0.3f
    }

    /* ---------- 设置渲染 ---------- */

    private fun renderCurrentDate() {
        val date = CountdownEngine.parseDate(CountdownEngine.getDefaultExamDate(this))
            ?: LocalDate.of(2027, 12, 25)
        binding.currentDateText.text = CountdownEngine.formatDot(date)
    }

    private fun renderSizeMode() {
        val mode = CountdownEngine.getSizeMode(this)
        sizeButtons().forEach { (button, m) -> stylePill(button, m == mode) }
    }

    private fun sizeButtons(): List<Pair<Button, String>> = listOf(
        binding.sizeAuto to CountdownEngine.SIZE_AUTO,
        binding.sizeSmall to CountdownEngine.SIZE_SMALL,
        binding.sizeMedium to CountdownEngine.SIZE_MEDIUM,
        binding.sizeLarge to CountdownEngine.SIZE_LARGE
    )

    private fun stylePill(button: Button, active: Boolean) {
        button.background = getDrawable(if (active) R.drawable.pill_active else R.drawable.pill_inactive)
        button.setTextColor(getColor(if (active) android.R.color.white else R.color.ink))
    }

    /* ---------- 事件 ---------- */

    private fun bindEvents() {
        binding.tabManage.setOnClickListener { showPage(PAGE_MANAGE) }
        binding.tabClock.setOnClickListener { showPage(PAGE_CLOCK) }

        binding.btnPickDate.setOnClickListener { openDatePicker() }
        binding.quickButton2027.setOnClickListener { applyExamDate("2027-12-25") }
        binding.quickButtonReserve.setOnClickListener { applyExamDate("2027-12-18") }

        sizeButtons().forEach { (button, mode) ->
            button.setOnClickListener {
                CountdownEngine.setSizeMode(this, mode)
                renderSizeMode()
                refreshCountdownWidgets()
            }
        }

        binding.btnRefresh.setOnClickListener { refreshCountdownWidgets() }
    }

    private fun openDatePicker() {
        val initial = CountdownEngine.parseDate(CountdownEngine.getDefaultExamDate(this))
            ?: LocalDate.of(2027, 12, 25)
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("选择考试日期")
            .setSelection(initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            val date = Instant.ofEpochMilli(selection).atZone(ZoneOffset.UTC).toLocalDate()
            applyExamDate(date.toString())
        }
        picker.show(supportFragmentManager, "main_date_picker")
    }

    private fun applyExamDate(date: String) {
        CountdownEngine.setDefaultExamDate(this, date)
        val manager = AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(ComponentName(this, CountdownWidgetProvider::class.java))
        ids.forEach { CountdownEngine.saveExamDate(this, it, date) }
        if (ids.isNotEmpty()) CountdownWidgetProvider().onUpdate(this, manager, ids)
        renderCurrentDate()
    }

    private fun refreshCountdownWidgets() {
        val manager = AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(ComponentName(this, CountdownWidgetProvider::class.java))
        CountdownWidgetProvider().onUpdate(this, manager, ids)
    }

    companion object {
        private const val PAGE_MANAGE = 0
        private const val PAGE_CLOCK = 1
    }
}