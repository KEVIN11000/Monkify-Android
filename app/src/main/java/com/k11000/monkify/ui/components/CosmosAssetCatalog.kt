package com.k11000.monkify.ui.components

import android.content.Context
import androidx.annotation.DrawableRes
import com.k11000.monkify.data.model.CosmosTheme

/**
 * Catálogo y resolutor seguro de activos visuales para el motor COSMOS.
 * Garantiza resiliencia ante recursos no empaquetados o fallas en tiempo de ejecución.
 */
object CosmosAssetCatalog {

    /**
     * Retorna la lista inmutable de todas las variantes cósmicas disponibles.
     */
    fun getAvailableThemes(): List<CosmosTheme> = CosmosTheme.values().toList()

    /**
     * Resuelve de forma segura el tema cósmico solicitado.
     * Si el identificador es nulo o desconocido, recurre al tema procedural seguro.
     */
    fun resolveTheme(id: String?): CosmosTheme = CosmosTheme.fromId(id)

    /**
     * Retorna el tema de respaldo por defecto (Espacio Profundo procedural).
     */
    fun getFallbackTheme(): CosmosTheme = CosmosTheme.DEEP_SPACE

    /**
     * Resuelve el ID de recurso drawable empaquetado para el tema dado.
     * Retorna `null` si el tema es procedural puro o si el recurso aún no ha sido empaquetado.
     */
    @DrawableRes
    fun getDrawableRes(context: Context, theme: CosmosTheme): Int? {
        val assetName = theme.assetName ?: return null
        return try {
            val resId = context.resources.getIdentifier(assetName, "drawable", context.packageName)
            if (resId != 0) resId else null
        } catch (_: Exception) {
            null
        }
    }
}
