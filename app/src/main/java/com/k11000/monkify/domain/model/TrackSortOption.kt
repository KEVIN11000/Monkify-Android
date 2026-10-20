package com.k11000.monkify.domain.model

enum class TrackSortOption(val displayName: String) {
    DEFAULT("Orden original"),
    TITLE_ASC("Título A-Z"),
    TITLE_DESC("Título Z-A"),
    ARTIST_ASC("Artista"),
    DURATION_DESC("Duración")
}
