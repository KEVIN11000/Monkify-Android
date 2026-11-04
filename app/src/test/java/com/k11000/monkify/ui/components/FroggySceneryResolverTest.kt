package com.k11000.monkify.ui.components

import com.k11000.monkify.domain.model.FroggyScenery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FroggySceneryResolverTest {

    @Test
    fun `seleccion explicita respeta el escenario elegido`() {
        assertEquals(FroggyScenery.FIELDS, FroggyAssetCatalog.resolveScenery("FIELDS", dayOfYear = 5))
        assertEquals(FroggyScenery.HILL, FroggyAssetCatalog.resolveScenery("HILL", dayOfYear = 5))
        assertEquals(FroggyScenery.MUSHROOM, FroggyAssetCatalog.resolveScenery("MUSHROOM", dayOfYear = 5))
    }

    @Test
    fun `AUTO rota entre los 3 escenarios oficiales`() {
        val seen = (1..3).map { FroggyAssetCatalog.resolveScenery("AUTO", dayOfYear = it) }.toSet()
        assertEquals(FroggyScenery.values().toSet(), seen)
    }

    @Test
    fun `valor desconocido cae en AUTO sin fallar`() {
        assertTrue(FroggyAssetCatalog.resolveScenery("???", dayOfYear = 10) in FroggyScenery.values())
    }
}
