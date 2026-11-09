package com.k11000.monkify.data.model

/**
 * Variantes visuales para el motor de ambientación astronómica COSMOS.
 */
enum class CosmosTheme(
    val id: String,
    val displayName: String,
    val assetName: String? = null
) {
    DEEP_SPACE("DEEP_SPACE", "Espacio Profundo (Procedural)", null),
    ORION_NEBULA("ORION_NEBULA", "Nebulosa de Orión", "zen_cosmos_orion"),
    ANDROMEDA("ANDROMEDA", "Galaxia de Andrómeda", "zen_cosmos_andromeda"),
    PILLARS_OF_CREATION("PILLARS_OF_CREATION", "Pilares de la Creación", "zen_cosmos_pillars"),
    DEEP_FIELD("DEEP_FIELD", "Campo Profundo (JWST)", "zen_cosmos_deep_field");

    companion object {
        fun fromId(id: String?): CosmosTheme =
            values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEEP_SPACE
    }
}
