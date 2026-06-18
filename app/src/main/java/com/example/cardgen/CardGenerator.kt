package com.example.cardgen

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Thin client around the OpenAI image API.
 *
 * Behaviour depends on whether input images are supplied:
 *
 *  - With images -> POST /v1/images/edits as multipart/form-data, sending the
 *    subject photo and reference card(s) as image[] parts. This is what lets the
 *    model preserve the uploaded subject's face and match the reference card.
 *
 *  - Without images -> POST /v1/images/generations (text-prompt only).
 *
 * Both endpoints return the image as base64 in data[0].b64_json.
 */
object CardGenerator {

    private const val TAG = "CardGen"
    private const val GENERATIONS_URL = "https://api.openai.com/v1/images/generations"
    private const val EDITS_URL = "https://api.openai.com/v1/images/edits"

    private val client = OkHttpClient.Builder()
        // Image generation can take a while; give it room.
        .callTimeout(180, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .build()

    /** An image to send to the edits endpoint. */
    data class InputImage(
        val bytes: ByteArray,
        val fileName: String,
        val mimeType: String,
    )

    sealed interface Result {
        data class Success(val bitmap: Bitmap) : Result
        data class Error(val message: String) : Result
    }

    /**
     * Generates a card from [prompt] and any [images], invoking [callback] on a
     * background thread.
     */
    fun generate(
        prompt: String,
        images: List<InputImage> = emptyList(),
        callback: (Result) -> Unit,
    ) {
        if (!ApiConfig.isKeyConfigured()) {
            callback(Result.Error("OpenAI API key is not set. Edit ApiConfig.OPENAI_API_KEY."))
            return
        }

        // Log exactly what we send so it can be inspected in Logcat (tag "CardGen").
        val endpoint = if (images.isNotEmpty()) "edits (images uploaded)" else "generations (text only)"
        Log.d(TAG, "Endpoint: $endpoint")
        Log.d(TAG, "Images: " + images.joinToString { "${it.fileName} (${it.bytes.size} bytes)" })
        Log.d(TAG, "Prompt:\n$prompt")

        val request = if (images.isNotEmpty()) {
            buildEditsRequest(prompt, images)
        } else {
            buildGenerationsRequest(prompt)
        }

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

    private fun buildGenerationsRequest(prompt: String): Request {
        val json = JSONObject().apply {
            put("model", ApiConfig.IMAGE_MODEL)
            put("prompt", prompt)
            put("size", "1024x1536")
            put("quality", "high")
            put("n", 1)
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        return Request.Builder()
            .url(GENERATIONS_URL)
            .addHeader("Authorization", "Bearer ${ApiConfig.OPENAI_API_KEY}")
            .post(body)
            .build()
    }

    private fun buildEditsRequest(prompt: String, images: List<InputImage>): Request {
        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("model", ApiConfig.IMAGE_MODEL)
            .addFormDataPart("prompt", prompt)
            .addFormDataPart("size", "1024x1536")
            .addFormDataPart("n", "1")
            // quality=high renders fine card detail (foil, badges, sharp text).
            .addFormDataPart("quality", "high")
            .addFormDataPart("output_format", "png")

        // input_fidelity preserves the subject's real face. gpt-image-2 always
        // does this automatically and REJECTS the param, so only send it for
        // gpt-image-1.
        if (ApiConfig.IMAGE_MODEL == "gpt-image-1") {
            builder.addFormDataPart("input_fidelity", "high")
        }

        // Multiple input images are sent as repeated "image[]" parts. Order
        // matches the prompt: subject first, then reference front/back.
        for (img in images) {
            builder.addFormDataPart(
                "image[]",
                img.fileName,
                img.bytes.toRequestBody(img.mimeType.toMediaType())
            )
        }

        return Request.Builder()
            .url(EDITS_URL)
            .addHeader("Authorization", "Bearer ${ApiConfig.OPENAI_API_KEY}")
            .post(builder.build())
            .build()
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
