package com.k11000.monkify.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZenPhraseCatalogTest {

    @Test
    fun `resolveTitle returns appropriate greeting for all solar day parts`() {
        assertEquals("Buen amanecer", ZenPhraseCatalog.resolveTitle(ZenDayPart.DAWN))
        assertEquals("Buenos días", ZenPhraseCatalog.resolveTitle(ZenDayPart.DAY))
        assertEquals("Buenas tardes", ZenPhraseCatalog.resolveTitle(ZenDayPart.SUNSET))
        assertEquals("Buenas noches", ZenPhraseCatalog.resolveTitle(ZenDayPart.NIGHT))
    }

    @Test
    fun `every weather and dayPart combination contains at least 3 distinct non-empty phrases`() {
        for (condition in WeatherCondition.values()) {
            for (dayPart in ZenDayPart.values()) {
                val phrases = ZenPhraseCatalog.getPhrases(condition, dayPart)
                assertNotNull("Phrases for $condition and $dayPart must not be null", phrases)
                assertTrue("Expected at least 3 phrases for $condition and $dayPart but found ${phrases.size}", phrases.size >= 3)
                
                // Ensure no empty/blank phrases
                for (phrase in phrases) {
                    assertFalse("Phrase must not be blank for $condition and $dayPart", phrase.isBlank())
                }

                // Ensure all phrases are unique within the combination
                val uniqueSet = phrases.toSet()
                assertEquals("Phrases must be distinct for $condition and $dayPart", phrases.size, uniqueSet.size)
            }
        }
    }

    @Test
    fun `resolvePhrase rotates deterministically and handles negative or large offsets safely`() {
        val condition = WeatherCondition.RAINY
        val dayPart = ZenDayPart.DAY
        val phrases = ZenPhraseCatalog.getPhrases(condition, dayPart)
        val size = phrases.size

        val p0 = ZenPhraseCatalog.resolvePhrase(condition, dayPart, 0)
        val p1 = ZenPhraseCatalog.resolvePhrase(condition, dayPart, 1)
        val pCycle = ZenPhraseCatalog.resolvePhrase(condition, dayPart, size)
        val pLarge = ZenPhraseCatalog.resolvePhrase(condition, dayPart, size * 10 + 1)
        val pNegative = ZenPhraseCatalog.resolvePhrase(condition, dayPart, -1)

        assertEquals("Offset 0 should match first item", phrases[0], p0)
        assertEquals("Offset 1 should match second item", phrases[1], p1)
        assertEquals("Offset equal to size should cycle to first item", phrases[0], pCycle)
        assertEquals("Large offset should cycle accurately", phrases[1], pLarge)
        assertNotNull("Negative offset should resolve safely without throwing", pNegative)
        assertTrue("Negative offset result must belong to the phrase set", phrases.contains(pNegative))
    }
}
