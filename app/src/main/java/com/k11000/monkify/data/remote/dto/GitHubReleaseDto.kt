package com.k11000.monkify.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GitHubReleaseDto(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("name") val name: String? = null,
    @SerializedName("body") val body: String? = null,
    @SerializedName("published_at") val publishedAt: String? = null,
    @SerializedName("assets") val assets: List<GitHubAssetDto> = emptyList()
)

data class GitHubAssetDto(
    @SerializedName("name") val name: String,
    @SerializedName("size") val size: Long = 0,
    @SerializedName("browser_download_url") val downloadUrl: String,
    @SerializedName("content_type") val contentType: String? = null
)
