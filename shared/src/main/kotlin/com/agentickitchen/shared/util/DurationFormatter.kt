package com.agentickitchen.shared.util

import java.time.Duration

object DurationFormatter {

    fun format(duration: Duration, turkish: Boolean = false): String {
        val totalSeconds = duration.seconds
        if (totalSeconds <= 0) return if (turkish) "0 sn" else "0s"

        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 && minutes > 0 -> {
                if (turkish) "${hours}sa ${minutes}dk" else "${hours}h ${minutes}m"
            }
            hours > 0 -> {
                if (turkish) "${hours}sa" else "${hours}h"
            }
            minutes > 0 && seconds > 0 -> {
                if (turkish) "${minutes}dk ${seconds}sn" else "${minutes}m ${seconds}s"
            }
            minutes > 0 -> {
                if (turkish) "${minutes}dk" else "${minutes}m"
            }
            else -> {
                if (turkish) "${seconds}sn" else "${seconds}s"
            }
        }
    }

    fun formatCompact(duration: Duration): String {
        val totalMinutes = duration.toMinutes()
        return when {
            totalMinutes >= 60 -> "${totalMinutes / 60}h ${totalMinutes % 60}m"
            totalMinutes > 0 -> "${totalMinutes}m"
            else -> "${duration.seconds}s"
        }
    }
}
