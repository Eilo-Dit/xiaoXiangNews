package com.xxcb.news.data.repository

import com.xxcb.news.data.api.RetrofitClient
import com.xxcb.news.data.model.NewspaperPage

class NewspaperRepository {

    private val api = RetrofitClient.newspaperApi

    suspend fun getNewspaper(date: String): Result<List<NewspaperPage>> {
        return try {
            val response = api.getNewspaper(date)
            if (response.stat == 1 && response.data != null) {
                android.util.Log.d("ApiDebug", "API returned ${response.data.list.size} pages")
                response.data.list.forEachIndexed { index, page ->
                    android.util.Log.d("ApiDebug", "Page $index - pdfUrl: ${page.pdfUrl}")
                }
                Result.success(response.data.list)
            } else {
                Result.failure(Exception(response.message ?: "获取报纸数据失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLastDate(date: String): Result<String?> {
        return try {
            val response = api.getNewspaper(date)
            if (response.stat == 1 && response.data != null) {
                Result.success(response.data.lastDate)
            } else {
                Result.failure(Exception(response.message ?: "获取日期失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllValidDates(): Result<List<String>> {
        return try {
            val response = api.getAllValidDates()
            if (response.stat == 1 && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "获取有效日期失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
