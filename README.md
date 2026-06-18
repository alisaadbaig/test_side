# Card Gen — AI Trading Card Generator (Android)

A small Android app (Kotlin + Jetpack Compose) that generates premium-looking
collectible trading cards using OpenAI's `gpt-image-1` image model.

The user fills in card details (name, title, team/theme, optional card number),
optionally picks a subject photo and reference card images, and taps **Generate
Card**. The generated card is displayed and can be saved to the gallery.

## Project structure

```
.
├── settings.gradle.kts
├── build.gradle.kts                # top-level, plugin versions
├── gradle.properties
├── gradle/wrapper/                 # wrapper config (see note below)
└── app/
    ├── build.gradle.kts            # module config + dependencies
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/values/             # strings, theme
        └── java/com/example/cardgen/
            ├── ApiConfig.kt        # API key + guard
            ├── CardDetails.kt      # data model + prompt builder
            ├── CardGenerator.kt    # OkHttp call to gpt-image-1
            ├── ImageUtils.kt       # thumbnail load + save to gallery
            └── MainActivity.kt     # Compose UI
```

## Setup

1. **Add your API key.** Open `app/src/main/java/com/example/cardgen/ApiConfig.kt`
   and replace the placeholder:

   ```kotlin
   const val OPENAI_API_KEY = "sk-..."  // your real key
   ```

   > ⚠️ Hard-coding the key is fine for **personal use only**. Anyone with your
   > APK can extract it. For wider distribution, proxy requests through your own
   > backend so the key never ships in the app.

2. **Open in Android Studio** (Giraffe/Koala or newer) and let it sync, or build
   from the command line (see Gradle wrapper note).

3. Run on a device/emulator with API level 29+.

## How the API call works

The networking lives in `CardGenerator.kt` and posts to
`https://api.openai.com/v1/images/generations` with `model: "gpt-image-1"`,
the prompt, and `size: "1024x1536"` (≈ the 2.5×3.5 trading-card ratio).
`gpt-image-1` returns the image as base64 in `data[0].b64_json`, which is decoded
into a `Bitmap`.

### ⚠️ Important limitation: images are not uploaded in this mode

The `/v1/images/generations` endpoint is **text-prompt only**. The subject photo
and reference cards picked in the UI are previewed for convenience but are **not
sent to the model** — the card is generated purely from the text prompt, so it
will *not* preserve the uploaded person's face.

To actually feed the images to the model (and preserve the subject's identity /
match a reference card design), switch to the **`/v1/images/edits`** endpoint and
send a multipart request with the `image[]` parts plus the prompt. The data model
(`CardDetails`) already carries the image `Uri`s, so only `CardGenerator.kt`
needs to change.

## Gradle wrapper note

This scaffold includes `gradle/wrapper/gradle-wrapper.properties` but **not** the
binary `gradle-wrapper.jar` (binaries aren't generated here). To get a working
`./gradlew`, either:

- Open the project in Android Studio (it provides Gradle and can regenerate the
  wrapper), or
- Run `gradle wrapper` once with a locally installed Gradle.

## Dependencies

- Jetpack Compose (BOM) + Material 3
- OkHttp 4.12.0 for networking
- `org.json` — uses the platform's built-in version on Android (the Maven
  `org.json:json` artifact is intentionally **not** declared, as it would cause a
  duplicate-class build error)
