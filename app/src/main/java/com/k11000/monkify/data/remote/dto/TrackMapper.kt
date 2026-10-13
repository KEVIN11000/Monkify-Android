package com.k11000.monkify.data.remote.dto

import com.k11000.monkify.domain.model.Track

/**
 * Convierte la pista del Worker en el modelo de dominio.
 *
 * @param baseUrl URL base del API con barra final, ej. `https://host/api/v1/`.
 */
fun BackendTrackDto.toDomain(baseUrl: String): Track {
    val origin = baseUrl.substringBefore("/api/v1/").trimEnd('/')
    val resolvedStream = streamUrl
        ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
        ?: "${baseUrl.trimEnd('/')}/stream/$id"
    val resolvedCover = coverUrl?.takeIf { it.isNotBlank() }?.let {
        if (it.startsWith("http://") || it.startsWith("https://")) it
        else "$origin/${it.trimStart('/')}"
    }
    return Track(
        id = id,
        title = title,
        artist = artist,
        album = album.orEmpty(),
        durationMs = durationMs ?: 0L,
        coverUrl = resolvedCover,
        streamUrl = resolvedStream
    )
}
