package com.example.cardgen

import android.net.Uri

/**
 * User-supplied details for the card to generate.
 *
 * [subjectPhoto], [referenceFront] and [referenceBack] are uploaded to the
 * /v1/images/edits endpoint as image[] parts (subject first, then the reference
 * card front/back) so gpt-image-1 can match the card design while preserving the
 * subject's real face.
 */
data class CardDetails(
    val name: String,
    val title: String,
    val teamOrTheme: String,
    val cardNumber: String = "",
    val subjectPhoto: Uri? = null,
    val referenceFront: Uri? = null,
    val referenceBack: Uri? = null,
)

/**
 * Builds the gpt-image-1 prompt by filling the template placeholders with the
 * user's card details.
 *
 * The prompt is written for the /v1/images/edits endpoint, where the images are
 * sent positionally: image 1 = subject, image 2 = reference front, image 3 =
 * optional reference back. It strongly emphasises preserving the subject's exact
 * age and identity so an adult is never rendered as a child.
 */
fun buildCardPrompt(details: CardDetails): String {
    val cardNumberLine = if (details.cardNumber.isBlank()) {
        "- Card Number: (none)"
    } else {
        "- Card Number: ${details.cardNumber}"
    }

    return """
        Create a premium collectible trading card by editing the provided images.

        Images provided (in order):
        1. SUBJECT PHOTO — a real person. This is the face to put on the card.
        2. REFERENCE CARD FRONT — the design template to copy.
        3. (Optional) REFERENCE CARD BACK.

        ===== CRITICAL — SUBJECT IDENTITY (highest priority) =====
        - The person on the final card MUST be the exact same person as in the
          subject photo (image 1).
        - Preserve their EXACT age. Do NOT make them younger or older. If the
          subject is an adult, the result MUST be that same adult — NEVER a child
          or a teenager.
        - Keep every distinguishing feature: face shape, skin tone, eyes,
          eyebrows, nose, mouth, hairstyle and hairline, facial hair/beard/stubble,
          wrinkles, and eyeglasses if the subject is wearing them.
        - Keep the same gender and the same expression.
        - This is a likeness of a specific real adult, not a generic or stylised
          character. Do not beautify, de-age, or cartoonify the face.

        ===== CARD DESIGN =====
        - Use the reference card (image 2) as the exact design template.
        - Copy the reference card's layout, borders, colors, background, effects,
          typography placement, logos and badge placement, and overall premium look.
        - Keep a similar pose and framing to the reference card.
        - Replace ONLY the original player/subject with the subject from image 1.

        Card Details (text to render on the card):
        - Name: ${details.name}
        - Title: ${details.title}
        - Team/Theme: ${details.teamOrTheme}
        $cardNumberLine

        Output Requirements:
        - Front card only.
        - Flat printable design (not inside a plastic case).
        - Trading card size ratio (2.5 x 3.5 inches), portrait.
        - High resolution, sharp text and graphics.
        - Realistic face with correct adult head-to-body ratio.
        - No blur, no distorted face, no extra fingers or limbs, no watermarks.

        The final result should look like an officially printed premium trading
        card that matches the reference card style while showing the exact same
        adult person from the subject photo.
    """.trimIndent()
}

