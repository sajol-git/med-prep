package com.example.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.MedPrepApplication
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TodayStudyWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        try {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    updateWidgetFields(context, appWidgetManager, appWidgetIds)
                } catch (t: Throwable) {
                    t.printStackTrace()
                } finally {
                    try {
                        pendingResult?.finish()
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    updateWidgetFields(context, appWidgetManager, appWidgetIds)
                } catch (inner: Throwable) {
                    inner.printStackTrace()
                }
            }
        }
    }

    private fun toBanglaDigits(num: Int): String {
        return num.toString()
    }

    private suspend fun updateWidgetFields(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        try {
            val app = context.applicationContext as? MedPrepApplication ?: return

            // Calculate today's start and end times in local system timezone
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = calendar.timeInMillis
            val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1

            val sessions = try {
                app.repository.allSessionsImmediate()
            } catch (t: Throwable) {
                emptyList()
            }

            val todaySessions = sessions.filter { it.dateMillis in startOfDay..endOfDay }
            val totalMinutes = todaySessions.sumOf { it.durationMinutes }

            // Group by subject, sum hours, sort descending high to low
            val subjectDurations = todaySessions.groupBy { it.subject }
                .map { entry -> Pair(entry.key, entry.value.sumOf { it.durationMinutes }) }
                .sortedByDescending { it.second }

            val totalHours = totalMinutes / 60
            val totalMins = totalMinutes % 60

            val currentCalendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("dd/MM, h:mm a", Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("Asia/Dhaka") }
            val formattedDate = dateFormat.format(currentCalendar.time).lowercase(Locale.US)

            // Canvas drawing for Segmented Progress bar
            val progressBarBitmap = drawProgressBar(subjectDurations, totalMinutes)

            val colors = listOf("#06B6D4", "#3B82F6", "#8B5CF6", "#EC4899", "#F59E0B", "#10B981")

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.today_study_widget)

                // Set dynamic total study duration in Bangla digits
                if (totalHours > 0) {
                    views.setViewVisibility(R.id.widget_hours_val, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_hours_unit, View.VISIBLE)
                    views.setTextViewText(R.id.widget_hours_val, toBanglaDigits(totalHours))
                    
                    if (totalMins > 0) {
                        views.setViewVisibility(R.id.widget_mins_val, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_mins_unit, View.VISIBLE)
                        views.setTextViewText(R.id.widget_mins_val, toBanglaDigits(totalMins))
                    } else {
                        views.setViewVisibility(R.id.widget_mins_val, View.GONE)
                        views.setViewVisibility(R.id.widget_mins_unit, View.GONE)
                    }
                } else {
                    views.setViewVisibility(R.id.widget_hours_val, View.GONE)
                    views.setViewVisibility(R.id.widget_hours_unit, View.GONE)
                    
                    views.setViewVisibility(R.id.widget_mins_val, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_mins_unit, View.VISIBLE)
                    views.setTextViewText(R.id.widget_mins_val, toBanglaDigits(totalMins))
                }

                // Set progress bar image bitmap
                views.setImageViewBitmap(R.id.widget_progress_bar, progressBarBitmap)

                // Fill the rows
                val rowContainers = arrayOf(R.id.widget_row1, R.id.widget_row2, R.id.widget_row3)
                val rowBullets = arrayOf(R.id.widget_row1_bullet, R.id.widget_row2_bullet, R.id.widget_row3_bullet)
                val rowNames = arrayOf(R.id.widget_row1_name, R.id.widget_row2_name, R.id.widget_row3_name)
                val rowTimes = arrayOf(R.id.widget_row1_time, R.id.widget_row2_time, R.id.widget_row3_time)

                for (i in 0 until 3) {
                    if (i < subjectDurations.size) {
                        val (subjName, mins) = subjectDurations[i]
                        views.setViewVisibility(rowContainers[i], View.VISIBLE)
                        
                        val bngSubjName = when(subjName.trim().lowercase()) {
                            "botany", "বোটানি" -> "উদ্ভিদবিজ্ঞান"
                            "zoology", "প্রাণিবিজ্ঞান", "জুলজি" -> "প্রাণিবিজ্ঞান"
                            "chemistry 1st paper", "রসায়ন ১ম পত্র" -> "রসায়ন ১ম"
                            "chemistry 2nd paper", "রসায়ন ২য় পত্র" -> "রসায়ন ২য়"
                            "chemistry", "রসায়ন" -> "রসায়ন"
                            "physics 1st paper", "পদার্থবিজ্ঞান ১ম পত্র" -> "পদার্থ ১ম"
                            "physics 2nd paper", "পদার্থবিজ্ঞান ২য় পত্র" -> "পদার্থ ২য়"
                            "physics", "পদার্থবিজ্ঞান" -> "পদার্থ"
                            "english", "ইংরেজি" -> "ইংরেজি"
                            "general knowledge & ethics", "general knowledge", "gk", "জিকে", "সাধারণ জ্ঞান" -> "সাধারণ জ্ঞান"
                            else -> subjName
                        }
                        views.setTextViewText(rowNames[i], bngSubjName)
                        
                        val hr = mins / 60
                        val mn = mins % 60
                        val formattedTime = if (hr > 0) {
                            if (mn > 0) "${hr}h ${mn}m" else "${hr}h"
                        } else {
                            "${mn}m"
                        }
                        views.setTextViewText(rowTimes[i], formattedTime)
                        
                        val colorHex = colors[i % colors.size]
                        
                        // Draw bullet instead of using setInt(setColorFilter)
                        val bulletBitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
                        val bulletCanvas = Canvas(bulletBitmap)
                        val bulletPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.parseColor(colorHex)
                        }
                        bulletCanvas.drawCircle(8f, 8f, 8f, bulletPaint)
                        views.setImageViewBitmap(rowBullets[i], bulletBitmap)
                    } else {
                        views.setViewVisibility(rowContainers[i], View.GONE)
                    }
                }

                // Set footer timestamp
                views.setTextViewText(R.id.widget_timestamp, "$formattedDate ↻")

                // Dynamic PendingIntent to open MainActivity when the widget background is clicked
                val openAppIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun drawProgressBar(subjects: List<Pair<String, Int>>, totalMin: Int): Bitmap {
        val width = 450
        val height = 24
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        // Draw unoccupied slate background segment representing dark theme track
        paint.color = Color.parseColor("#1E293B")
        val rectF = RectF(0f, 0f, width.toFloat(), height.toFloat())
        val rx = height / 2f
        canvas.drawRoundRect(rectF, rx, rx, paint)
        
        if (totalMin <= 0 || subjects.isEmpty()) {
            return bitmap
        }
        
        val colors = listOf("#06B6D4", "#3B82F6", "#8B5CF6", "#EC4899", "#F59E0B", "#10B981")
        var currentX = 0f
        
        val clipPath = Path().apply {
            addRoundRect(rectF, rx, rx, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clipPath)
        
        for (i in subjects.indices) {
            val minutes = subjects[i].second
            val fraction = minutes.toFloat() / totalMin
            val segmentWidth = fraction * width
            if (segmentWidth > 0) {
                paint.color = Color.parseColor(colors[i % colors.size])
                val segmentRect = RectF(currentX, 0f, currentX + segmentWidth, height.toFloat())
                canvas.drawRect(segmentRect, paint)
                currentX += segmentWidth
            }
        }
        canvas.restore()
        return bitmap
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            try {
                val intent = Intent(context, TodayStudyWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                        ComponentName(context, TodayStudyWidget::class.java)
                    )
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}
