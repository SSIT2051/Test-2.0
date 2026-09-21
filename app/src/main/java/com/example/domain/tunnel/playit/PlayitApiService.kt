package com.example.domain.tunnel.playit

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface PlayitApiService {
    @POST("claim/setup")
    suspend fun setupClaim(
        @Body request: PlayitClaimSetupRequest
    ): Response<PlayitClaimSetupResponse>

    @POST("tunnels/create")
    suspend fun createTunnel(
        @Header("Authorization") authHeader: String,
        @Body request: PlayitCreateTunnelRequest
    ): Response<PlayitCreateTunnelResponse>

    @POST("tunnels/delete")
    suspend fun deleteTunnel(
        @Header("Authorization") authHeader: String,
        @Body request: PlayitDeleteTunnelRequest
    ): Response<PlayitDeleteTunnelResponse>
}
