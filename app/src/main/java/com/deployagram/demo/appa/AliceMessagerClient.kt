package com.deployagram.demo.appa

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import java.io.IOException

/** AliceMessager answered with a non-success status code, and its reply. */
class AliceMessagerRefused(val statusCode: Int, val reply: String) :
    IOException("AliceMessager answered $statusCode: $reply")

/** AliceMessager's API, described for Retrofit. */
interface AliceMessagerService {
    @POST("AliceMessager/config")
    fun config(@Body body: RequestBody): Call<ResponseBody>
}

/** Calls AliceMessager's POST /AliceMessager/config through Retrofit. The payload is a JSON object, or no body at all. */
class AliceMessagerClient(
    private val http: OkHttpClient = OkHttpClient(),
) {
    /** Sends [payload] as {"message": payload}, or an empty body when it is null. Reports the reply, such as "World". */
    fun sendConfig(payload: String?, onResult: (Result<String>) -> Unit) {
        val body: RequestBody = if (payload == null) {
            ByteArray(0).toRequestBody(null)
        } else {
            JSONObject().put("message", payload).toString().toRequestBody(JSON_TYPE)
        }
        // The base URL is read on each call, so a test can point the app at its own double.
        val service = Retrofit.Builder()
            .baseUrl("${AliceMessagerConfig.baseUrl}/")
            .client(http)
            .build()
            .create(AliceMessagerService::class.java)

        service.config(body).enqueue(object : Callback<ResponseBody> {
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                onResult(Result.failure(t))
            }

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                // Retrofit gives the body only for a success. A refusal's reply is in the error body.
                val raw = if (response.isSuccessful) response.body() else response.errorBody()
                val text = raw?.string().orEmpty()
                val reply = runCatching { JSONObject(text).getString("message") }.getOrDefault(text)
                onResult(
                    if (response.isSuccessful) Result.success(reply)
                    else Result.failure(AliceMessagerRefused(response.code(), reply))
                )
            }
        })
    }

    private companion object {
        val JSON_TYPE = "application/json".toMediaType()
    }
}
