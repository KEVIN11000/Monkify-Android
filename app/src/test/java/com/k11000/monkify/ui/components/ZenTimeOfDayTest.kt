package com.k11000.monkify.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ZenTimeOfDayTest {

    @Test
    fun `las horas de madrugada y noche profunda mapean a NIGHT`() {
        val nightHours = listOf(0, 1, 2, 3, 4, 5, 21, 22, 23)
        for (h in nightHours) {
            assertEquals("Hora $h debería ser NIGHT", ZenTimeOfDay.NIGHT, resolveCurrentZenTimeOfDay(h))
        }
    }

    @Test
    fun `las horas de la mañana temprano mapean a DAWN`() {
        val dawnHours = listOf(6, 7, 8)
        for (h in dawnHours) {
            assertEquals("Hora $h debería ser DAWN", ZenTimeOfDay.DAWN, resolveCurrentZenTimeOfDay(h))
        }
    }

    @Test
    fun `las horas diurnas plenas mapean a DAY`() {
        val dayHours = (9..17).toList()
        for (h in dayHours) {
            assertEquals("Hora $h debería ser DAY", ZenTimeOfDay.DAY, resolveCurrentZenTimeOfDay(h))
        }
    }

    @Test
    fun `las horas del atardecer mapean a SUNSET`() {
        val sunsetHours = listOf(18, 19, 20)
        for (h in sunsetHours) {
            assertEquals("Hora $h debería ser SUNSET", ZenTimeOfDay.SUNSET, resolveCurrentZenTimeOfDay(h))
        }
    }

    @Test
    fun `todas las 24 horas del dia tienen una franja valida asignada`() {
        for (h in 0..23) {
            val resolved = resolveCurrentZenTimeOfDay(h)
            org.junit.Assert.assertNotNull("Hora $h debe resolver a un ZenTimeOfDay válido", resolved)
        }
    }
}
