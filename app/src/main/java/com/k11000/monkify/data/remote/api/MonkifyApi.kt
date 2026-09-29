package com.k11000.monkify.data.remote.api

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

interface MonkifyApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("ingest")
    suspend fun ingest(@Body request: IngestRequest): Response<IngestResponse>

    @GET("tracks/{track_id}/lyrics")
    suspend fun getLyrics(@Path("track_id") trackId: String): LyricsResponse
}
