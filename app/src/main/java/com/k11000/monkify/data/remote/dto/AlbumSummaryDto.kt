package com.k11000.monkify.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AlbumSummaryDto(
    @SerializedName("album") val album: String,
    @SerializedName("artist") val artist: String,
    @SerializedName("track_count") val trackCount: Int,
    @SerializedName("cover_url") val coverUrl: String? = null
)
