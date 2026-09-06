package com.aida.s34597506.medtrack.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenFdaApi {
    @GET("drug/label.json")
    suspend fun searchDrug(
        @Query("search") searchQuery: String,
        @Query("limit") limit: Int = 1
    ): OpenFdaResponse
}

data class OpenFdaResponse(
    val results: List<DrugLabel>? = emptyList()
)

data class DrugLabel(
    val openfda: OpenFdaInfo? = null,
    val purpose: List<String>? = emptyList(),
    val warnings: List<String>? = emptyList(),
    val dosage_and_administration: List<String>? = emptyList(),
    val indications_and_usage: List<String>? = emptyList(),
    val active_ingredient: List<String>? = emptyList()
)

data class OpenFdaInfo(
    val brand_name: List<String>? = emptyList(),
    val generic_name: List<String>? = emptyList()
)