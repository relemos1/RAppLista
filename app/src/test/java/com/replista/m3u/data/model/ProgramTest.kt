package com.replista.m3u.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgramTest {

    @Test
    fun testProgressCalculation() {
        val now = System.currentTimeMillis()
        val start = now - 30 * 60 * 1000L // começou há 30 min
        val end = now + 30 * 60 * 1000L   // termina em 30 min (duração total: 60 min)

        val program = Program(
            title = "Noite de Cinema",
            startTimeMs = start,
            endTimeMs = end
        )

        // O progresso deve estar em torno de 50% (0.5f)
        assertTrue(program.progress in 0.48f..0.52f)
    }

    @Test
    fun testFormattedTimeRange() {
        val start = 1728689400000L // tempo fixo
        val end = 1728696600000L

        val program = Program(
            title = "Noite de Cinema",
            startTimeMs = start,
            endTimeMs = end
        )

        val formatted = program.formattedTime
        // Deve conter hífen ou travessão separando horários
        assertTrue(formatted.contains("–") || formatted.contains("-"))
    }
}
