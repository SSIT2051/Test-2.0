package com.example.domain.tunnel.playit

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface PlayitApiService {
    @POST("claim/setup")
    suspend fun setupClaim(
        @Body request: PlayitClaimSetupRequest
    ): Response<ResponseBody>

    @POST("claim/exchange")
    suspend fun exchangeClaim(
        @Body request: PlayitClaimExchangeRequest
    ): Response<ResponseBody>

    @POST("agents/rundata")
    suspend fun getAgentRunData(
        @Header("Authorization") authHeader: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    @POST("tunnels/list")
    suspend fun listTunnelsRaw(
        @Header("Authorization") authHeader: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    @POST("tunnels/create")
    suspend fun createTunnelRaw(
        @Header("Authorization") authHeader: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    @POST("tunnels/delete")
    suspend fun deleteTunnelRaw(
        @Header("Authorization") authHeader: String,
        @Body body: RequestBody
    ): Response<ResponseBody>
}
