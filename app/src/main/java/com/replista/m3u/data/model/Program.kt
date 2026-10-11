package com.replista.m3u.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Program(
    val title: String,
    val description: String = "",
    val startTimeMs: Long,
    val endTimeMs: Long,
    val channelTvgId: String = ""
) {
    val formattedTime: String
        get() {
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            val start = formatter.format(Date(startTimeMs))
            val end = formatter.format(Date(endTimeMs))
            return "$start – $end"
        }

    val progress: Float
        get() {
            val now = System.currentTimeMillis()
            if (now <= startTimeMs) return 0f
            if (now >= endTimeMs) return 1f
            val total = (endTimeMs - startTimeMs).toFloat()
            if (total <= 0) return 0f
            return ((now - startTimeMs) / total).coerceIn(0f, 1f)
        }
}
