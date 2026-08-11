package com.example.mareaswatchv3.data

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

object TideCalculator {
    fun calculate(longitude: Double, date: Date = Date()): TideInfo {
        val referenceMoon = Instant.parse("2024-01-11T11:57:00Z").toEpochMilli()
        val synodicMonthMs = 29.53058867 * 86_400_000.0
        var lunarPhase = ((date.time - referenceMoon) % synodicMonthMs) / synodicMonthMs
        if (lunarPhase < 0) lunarPhase += 1.0

        val coefficient = (35 + abs(cos(lunarPhase * 2 * PI)) * 75).roundToInt()
        val meanSeaLevel = 1.8
        val amplitude = 1.4 * (coefficient / 70.0)
        val periodMs = (12 * 3600 + 25 * 60 + 12) * 1000L
        val zone = ZoneId.systemDefault()
        val startOfDay = date.toInstant().atZone(zone).toLocalDate()
            .atStartOfDay(zone).toInstant().toEpochMilli()
        val phaseShift = 3 * 3_600_000L + ((longitude / 360.0) * 86_400_000).toLong()

        fun heightAt(timeMs: Long): Double {
            val phase = ((timeMs - startOfDay + phaseShift).toDouble() / periodMs) * 2 * PI
            val height = meanSeaLevel + amplitude * (sin(phase) + 0.25 * sin(phase * 2 + 0.5))
            return max(0.1, (height * 100).roundToInt() / 100.0)
        }

        val now = date.time
        val current = heightAt(now)
        val future = heightAt(now + 10 * 60 * 1000L)
        val trend = when {
            future > current + 0.01 -> TideTrend.RISING
            future < current - 0.01 -> TideTrend.FALLING
            else -> TideTrend.STATIONARY
        }

        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        var previous = heightAt(now - 15 * 60 * 1000L)
        var cursor = now
        var nextEvent: TideEvent? = null
        while (cursor <= now + 18 * 3_600_000L && nextEvent == null) {
            val value = heightAt(cursor)
            val nextValue = heightAt(cursor + 10 * 60 * 1000L)
            val type = when {
                value > previous && value >= nextValue && value > meanSeaLevel + 0.2 -> TideType.HIGH
                value < previous && value <= nextValue && value < meanSeaLevel - 0.2 -> TideType.LOW
                else -> null
            }
            if (type != null) {
                nextEvent = TideEvent(
                    time = Instant.ofEpochMilli(cursor).atZone(zone).format(formatter),
                    type = type,
                    height = value
                )
            }
            previous = value
            cursor += 10 * 60 * 1000L
        }

        val curve = (0..24).map { hour ->
            TidePoint(hour, heightAt(startOfDay + hour * 3_600_000L))
        }
        val currentHour = date.toInstant().atZone(zone).let { it.hour + it.minute / 60.0 }

        return TideInfo(current, trend, nextEvent, curve, currentHour, coefficient)
    }
}
