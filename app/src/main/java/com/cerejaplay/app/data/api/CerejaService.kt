package com.cerejaplay.app.data.api

import com.cerejaplay.app.data.model.CalendarResponse
import com.cerejaplay.app.data.model.BecauseYouWatchedResponse
import com.cerejaplay.app.data.model.CategoriesResponse
import com.cerejaplay.app.data.model.ConfigResponse
import com.cerejaplay.app.data.model.DetailResponse
import com.cerejaplay.app.data.model.GenresResponse
import com.cerejaplay.app.data.model.HomeResponse
import com.cerejaplay.app.data.model.ListResponse
import com.cerejaplay.app.data.model.RecommendationsResponse
import com.cerejaplay.app.data.model.RelatedResponse
import com.cerejaplay.app.data.model.SearchResponse
import com.cerejaplay.app.data.model.WatchResponse
import com.cerejaplay.app.data.model.FeedbackRequest
import com.cerejaplay.app.data.model.FeedbackResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface CerejaService {

    @GET("api/app/config")
    suspend fun config(): ConfigResponse

    @GET
    suspend fun home(@Url path: String): HomeResponse

    @GET
    suspend fun becauseYouWatched(
        @Url path: String,
        @Query("seeds") seeds: String
    ): BecauseYouWatchedResponse

    @GET
    suspend fun catalog(
        @Url path: String,
        @Query("page") page: Int,
        @Query("category") category: String? = null
    ): ListResponse

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
    suspend fun related(
        @Url path: String,
        @Query("type") type: String,
        @Query("id") id: String
    ): RelatedResponse

    @GET
    suspend fun recommendations(
        @Url path: String,
        @Query("type") type: String,
        @Query("id") id: String
    ): RecommendationsResponse

    @GET
    suspend fun categories(@Url path: String): CategoriesResponse

    @GET
    suspend fun genres(@Url path: String): GenresResponse

    @GET
    suspend fun discover(
        @Url path: String,
        @Query("kind") kind: String,
        @Query("genre") genre: String? = null,
        @Query("year") year: Int? = null,
        @Query("minRating") minRating: Double? = null,
        @Query("sort") sort: String? = null,
        @Query("page") page: Int = 1
    ): ListResponse

    @GET
    suspend fun calendar(@Url path: String, @Query("days") days: Int = 7): CalendarResponse

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

    @POST("api/watch/feedback")
    suspend fun feedback(@Body body: FeedbackRequest): FeedbackResponse
}

