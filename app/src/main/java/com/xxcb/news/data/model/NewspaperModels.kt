package com.xxcb.news.data.model

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("stat") val stat: Int,
    @SerializedName("data") val data: T?,
    @SerializedName("msg") val message: String?
)

data class NewspaperData(
    @SerializedName("list") val list: List<NewspaperPage>,
    @SerializedName("lastDate") val lastDate: String?
)

data class NewspaperPage(
    @SerializedName("imgUrl") val imgUrl: String,
    @SerializedName("pdfUrl") val pdfUrl: String,
    @SerializedName("edition") val edition: String
)

data class ValidDatesData(
    @SerializedName("data") val dates: List<String>
)
