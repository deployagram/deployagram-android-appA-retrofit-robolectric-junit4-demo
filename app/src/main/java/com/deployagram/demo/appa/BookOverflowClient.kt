package com.deployagram.demo.appa

import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import okhttp3.ResponseBody
import java.io.IOException

/** BookOverflow answered with a non-success status code. */
class BookOverflowRefused(val statusCode: Int) : IOException("BookOverflow answered $statusCode")

/** BookOverflow's API, described for Retrofit. The body is read as JSON by [BookSuggestion.fromJson]. */
interface BookOverflowService {
    @GET("BookOverflow/suggestion")
    fun suggestion(@Query("email") email: String): Call<ResponseBody>
}

/** Calls BookOverflow's GET /suggestion?email=... through Retrofit, and reports the outcome. */
class BookOverflowClient(
    private val http: OkHttpClient = OkHttpClient(),
) {
    fun suggestionFor(email: String, onResult: (Result<BookSuggestion>) -> Unit) {
        // The base URL is read on each call, so a test can point the app at its own double.
        val service = Retrofit.Builder()
            .baseUrl("${BookOverflowConfig.baseUrl}/")
            .client(http)
            .build()
            .create(BookOverflowService::class.java)

        service.suggestion(email).enqueue(object : Callback<ResponseBody> {
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                onResult(Result.failure(t))
            }

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                val body = response.body()?.string().orEmpty()
                onResult(
                    if (response.isSuccessful) Result.success(BookSuggestion.fromJson(body))
                    else Result.failure(BookOverflowRefused(response.code()))
                )
            }
        })
    }
}
