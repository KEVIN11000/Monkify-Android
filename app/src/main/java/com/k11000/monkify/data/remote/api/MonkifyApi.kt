package com.k11000.monkify.data.remote.api

import com.k11000.monkify.data.remote.dto.BackendTrackDto
import com.k11000.monkify.data.remote.dto.IngestRequest
import com.k11000.monkify.data.remote.dto.IngestResponse
import com.k11000.monkify.data.remote.dto.LoginRequest
import com.k11000.monkify.data.remote.dto.LoginResponse
import com.k11000.monkify.data.remote.dto.LyricsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MonkifyApi {
    @GET("tracks")
    suspend fun getTracks(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("q") query: String? = null
    ): List<BackendTrackDto>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("ingest")
    suspend fun ingest(@Body request: IngestRequest): Response<IngestResponse>

    @GET("tracks/{track_id}/lyrics")
    suspend fun getLyrics(@Path("track_id") trackId: String): LyricsResponse
}
