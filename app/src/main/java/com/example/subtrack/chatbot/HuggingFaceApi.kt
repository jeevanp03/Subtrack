package com.example.subtrack.chatbot

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

data class HuggingFaceRequest(
    val inputs: String,
    val parameters: Map<String, Any> = mapOf(
        "max_new_tokens" to 150,
        "temperature" to 0.7,
        "do_sample" to true,
        "return_full_text" to false
    )
)

data class HuggingFaceResponse(
    val generated_text: String? = null
)

interface HuggingFaceApi {
    @POST("models/{model}")
    suspend fun generateText(
        @Path("model") model: String,
        @Body request: HuggingFaceRequest
    ): Response<List<HuggingFaceResponse>>
}