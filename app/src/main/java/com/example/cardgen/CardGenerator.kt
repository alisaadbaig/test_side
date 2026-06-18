package com.example.cardgen

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Thin client around the OpenAI image generation API.
 *
 * This uses the /v1/images/generations endpoint exactly as in the project notes.
 * That endpoint is TEXT-PROMPT ONLY: the subject/reference images picked in the
 * UI are not sent to the model here, so the result is generated purely from the
 * text prompt.
 *
 * To actually use the uploaded images (preserve the subject's face from the
 * reference card design), switch to the /v1/images/edits endpoint and send a
 * multipart request with the image[] parts plus the prompt. The data model
 * (CardDetails) already carries the image Uris for that future change.
 */
object CardGenerator {

    private const val GENERATIONS_URL = "https://api.openai.com/v1/images/generations"

    private val client = OkHttpClient.Builder()
        // Image generation can take a while; give it room.
        .callTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    sealed interface Result {
        data class Success(val bitmap: Bitmap) : Result
        data class Error(val message: String) : Result
    }

    /**
     * Generates a card from [prompt] and returns the result on [callback].
     * The callback is invoked on a background thread.
     */
    fun generate(prompt: String, callback: (Result) -> Unit) {
        if (!ApiConfig.isKeyConfigured()) {
            callback(Result.Error("OpenAI API key is not set. Edit ApiConfig.OPENAI_API_KEY."))
            return
        }

        val json = JSONObject().apply {
            put("model", "gpt-image-1")
            put("prompt", prompt)
            put("size", "1024x1536")
            put("n", 1)
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(GENERATIONS_URL)
            .addHeader("Authorization", "Bearer ${ApiConfig.OPENAI_API_KEY}")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(Result.Error("Network error: ${e.message}"))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val raw = it.body?.string()
                    if (!it.isSuccessful || raw == null) {
                        callback(Result.Error(parseError(raw, it.code)))
                        return
                    }
                    callback(parseImage(raw))
                }
            }
        })
    }

    /** gpt-image-1 returns the image as base64 in data[0].b64_json. */
    private fun parseImage(raw: String): Result {
        return try {
            val data = JSONObject(raw).optJSONArray("data")
            val b64 = data?.optJSONObject(0)?.optString("b64_json")
            if (b64.isNullOrBlank()) {
                Result.Error("Response did not contain image data.")
            } else {
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap == null) Result.Error("Could not decode returned image.")
                else Result.Success(bitmap)
            }
        } catch (e: Exception) {
            Result.Error("Failed to parse response: ${e.message}")
        }
    }

    private fun parseError(raw: String?, code: Int): String {
        if (raw == null) return "Request failed (HTTP $code)."
        return try {
            val message = JSONObject(raw).optJSONObject("error")?.optString("message")
            if (message.isNullOrBlank()) "Request failed (HTTP $code)." else message
        } catch (e: Exception) {
            "Request failed (HTTP $code)."
        }
    }
}
