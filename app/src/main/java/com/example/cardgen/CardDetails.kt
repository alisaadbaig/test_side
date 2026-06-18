package com.example.cardgen

import android.net.Uri

/**
 * User-supplied details for the card to generate.
 *
 * [subjectPhoto], [referenceFront] and [referenceBack] are collected from the UI
 * so the app is ready to send them as input images. NOTE: the current networking
 * path uses the /v1/images/generations endpoint, which is text-prompt only and
 * does NOT upload these images. To actually feed the images to the model, switch
 * to the /v1/images/edits endpoint (see CardGenerator for details).
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
 */
fun buildCardPrompt(details: CardDetails): String {
    val cardNumberLine = if (details.cardNumber.isBlank()) {
        "- Card Number: (none)"
    } else {
        "- Card Number: ${details.cardNumber}"
    }

    return """
        Create a premium collectible trading card.

        Inputs:
        1. Subject photo (person, child, adult, pet, or character)
        2. Reference card front
        3. Optional reference card back

        Instructions:

        - Preserve the uploaded subject's face and identity accurately.
        - Keep facial features, hairstyle, skin tone, eyes, and expression as close as possible to the uploaded image.
        - Use the reference card as the design template.
        - Keep the original card's:
          - Pose and body position
          - Uniform or clothing style
          - Card layout
          - Background and effects
          - Colors and borders
          - Typography placement
          - Logos and badges placement
          - Overall premium look

        Replace only the original player/subject with the uploaded subject.

        Card Details:
        - Name: ${details.name}
        - Title: ${details.title}
        - Team/Theme: ${details.teamOrTheme}
        $cardNumberLine

        Output Requirements:
        - Front card only or front and back card as requested
        - Flat printable design (not inside plastic case)
        - Trading card size ratio (2.5 x 3.5 inches)
        - High resolution (300 DPI or higher)
        - Sharp text and graphics
        - Realistic face with correct head-to-body ratio
        - Professional collectible card appearance
        - No blur
        - No distorted face
        - No extra fingers or limbs
        - No watermarks
        - Ready for printing and room decoration

        The final result should look like an officially printed premium trading card
        that matches the reference card style while featuring the uploaded subject.
    """.trimIndent()
}
