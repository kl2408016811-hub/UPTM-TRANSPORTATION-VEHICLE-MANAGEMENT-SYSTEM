package com.example.uptmtransportation.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import com.applandeo.materialcalendarview.CalendarDay
import com.example.uptmtransportation.R
import java.text.SimpleDateFormat
import java.util.*

class EventDayView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.red_booked)
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.black)
        textAlign = Paint.Align.CENTER
    }

    private var dayNumber: String? = null
    private var isEventDay = false

    fun setDayData(calendarDay: CalendarDay, hasEvent: Boolean) {
        dayNumber = calendarDay.calendar.get(Calendar.DAY_OF_MONTH).toString()
        isEventDay = hasEvent
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        // Draw day number
        textPaint.textSize = height * 0.4f
        dayNumber?.let {
            val x = width / 2
            val y = height / 2 - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText(it, x, y, textPaint)
        }

        // Draw red circle above the number if has event
        if (isEventDay) {
            val circleRadius = width * 0.12f
            val circleX = width - circleRadius - 4
            val circleY = circleRadius + 4
            canvas.drawCircle(circleX, circleY, circleRadius, paint)
        }
    }
}