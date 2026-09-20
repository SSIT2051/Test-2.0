package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ApiPluginItem(
    @Json(name = "id") val id: String?,
    @Json(name = "slug") val slug: String?,
    @Json(name = "name") val name: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "category") val category: String?,
    @Json(name = "author") val author: String?,
    @Json(name = "version") val version: String?,
    @Json(name = "mc_version") val mcVersion: String?,
    @Json(name = "downloads") val downloads: Int?,
    @Json(name = "rating") val rating: Float?,
    @Json(name = "icon_url") val iconUrl: String?,
    @Json(name = "is_mod") val isMod: Boolean?,
    @Json(name = "size") val size: String?
)

@JsonClass(generateAdapter = true)
data class ApiPluginsResponse(
    @Json(name = "items") val items: List<ApiPluginItem>?,
    @Json(name = "total") val total: Int?
)

interface PumpkinMarketApi {
    @GET("plugins")
    suspend fun getPlugins(
        @Query("query") query: String? = null,
        @Query("category") category: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ApiPluginsResponse>

    @GET("plugins/{id}")
    suspend fun getPluginDetails(
        @Path("id") id: String
    ): Response<ApiPluginItem>

    companion object {
        private const val BASE_URL = "https://market.pumpkinmc.org/api/"

        fun create(): PumpkinMarketApi {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(PumpkinMarketApi::class.java)
        }
    }
}
