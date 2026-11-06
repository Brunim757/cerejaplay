package com.cerejaplay.app.data.api

import com.cerejaplay.app.data.model.ConfigResponse
import com.cerejaplay.app.data.model.DetailResponse
import com.cerejaplay.app.data.model.HomeResponse
import com.cerejaplay.app.data.model.ListResponse
import com.cerejaplay.app.data.model.SearchResponse
import com.cerejaplay.app.data.model.WatchResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface CerejaService {

    @GET("api/app/config")
    suspend fun config(): ConfigResponse

    @GET
    suspend fun home(@Url path: String): HomeResponse

    @GET
    suspend fun catalog(@Url path: String, @Query("page") page: Int): ListResponse

    @GET
    suspend fun search(
        @Url path: String,
        @Query("q") query: String,
        @Query("page") page: Int = 1
    ): SearchResponse

    @GET
    suspend fun detail(
        @Url path: String,
        @Query("type") type: String,
        @Query("id") id: String
    ): DetailResponse

    @GET
    suspend fun watch(
        @Url path: String,
        @Query("type") type: String,
        @Query("id") id: String,
        @Query("season") season: Int? = null,
        @Query("episode") episode: Int? = null,
        @Query("lang") language: String? = null,
        @Query("audio") audio: String? = null,
        @Query("quality") quality: String? = null
    ): WatchResponse
}
