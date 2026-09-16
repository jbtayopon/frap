package com.jrm.frap.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    fun create(baseUrl: String): ApiService {

        val normalizedUrl = if (baseUrl.endsWith("/")) {
            baseUrl
        } else {
            "$baseUrl/"
        }

        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}