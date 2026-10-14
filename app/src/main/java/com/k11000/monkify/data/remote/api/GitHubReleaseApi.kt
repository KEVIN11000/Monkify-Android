package com.k11000.monkify.data.remote.api

import com.k11000.monkify.data.remote.dto.GitHubReleaseDto
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

interface GitHubReleaseApi {

    @Headers(
        "Accept: application/vnd.github.v3+json",
        "User-Agent: Monkify-Android"
    )
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String = "KEVIN11000",
        @Path("repo") repo: String = "Monkify-Android"
    ): GitHubReleaseDto
}
