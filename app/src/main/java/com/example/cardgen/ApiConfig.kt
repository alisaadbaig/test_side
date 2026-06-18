package com.example.cardgen

/**
 * Configuration for the OpenAI API.
 *
 * WARNING: Hard-coding the API key directly in the app is fine for PERSONAL use
 * only. Anyone who obtains your APK can extract this key. For anything beyond
 * personal experimentation, proxy the request through your own backend so the
 * key never ships inside the app.
 */
object ApiConfig {
    // Replace with your own key, e.g. "sk-xxxxxxxxxxxxxxxxxxxxxxxx"
    const val OPENAI_API_KEY: String = "sk-xxxxxxxxxxxxxxxxxxxxxxxx"

    fun isKeyConfigured(): Boolean =
        OPENAI_API_KEY.isNotBlank() && OPENAI_API_KEY != "sk-xxxxxxxxxxxxxxxxxxxxxxxx"
}
