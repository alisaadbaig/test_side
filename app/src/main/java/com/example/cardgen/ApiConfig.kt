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

    /**
     * Image model to use.
     *
     * "gpt-image-2" (released Apr 2026) is best for layout/text-heavy cards and
     * always uses high input fidelity automatically (so the input_fidelity param
     * must NOT be sent for it).
     *
     * Change to "gpt-image-1" if your OpenAI org isn't verified for gpt-image-2;
     * the request code adds input_fidelity=high automatically for gpt-image-1.
     */
    const val IMAGE_MODEL: String = "gpt-image-2"

    fun isKeyConfigured(): Boolean =
        OPENAI_API_KEY.isNotBlank() && OPENAI_API_KEY != "sk-xxxxxxxxxxxxxxxxxxxxxxxx"
}
