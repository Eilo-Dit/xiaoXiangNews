package com.xxcb.news.data.api

import com.xxcb.news.data.model.ApiResponse
import com.xxcb.news.data.model.NewspaperData
import retrofit2.http.GET
import retrofit2.http.Query

interface NewspaperApi {

    @GET("/api/newspaper/pdfs/get")
    suspend fun getNewspaper(@Query("date") date: String): ApiResponse<NewspaperData>

    @GET("/api/newspaper/all/valid/dates/get")
    suspend fun getAllValidDates(): ApiResponse<List<String>>

    @GET("/api/newspaper/valid/dates/get")
    suspend fun getValidDates(@Query("year") year: String): ApiResponse<List<String>>
}
