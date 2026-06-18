package com.example.cardgen

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CardGenScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardGenScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var teamOrTheme by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }

    var subjectPhoto by remember { mutableStateOf<Uri?>(null) }
    var referenceFront by remember { mutableStateOf<Uri?>(null) }
    var referenceBack by remember { mutableStateOf<Uri?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var resultBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Trading Card Generator") }) }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Card details",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = teamOrTheme,
                onValueChange = { teamOrTheme = it },
                label = { Text("Team / Theme") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = cardNumber,
                onValueChange = { cardNumber = it },
                label = { Text("Card Number (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Divider()

            Text(
                "Images",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                "For best results upload a clear subject photo and a reference " +
                    "front card. These images are sent to the model (via the edits " +
                    "endpoint) so it can match the card design and the subject's face.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ImagePickerRow(
                label = "Subject photo",
                uri = subjectPhoto,
                onPicked = { subjectPhoto = it }
            )
            ImagePickerRow(
                label = "Reference front",
                uri = referenceFront,
                onPicked = { referenceFront = it }
            )
            ImagePickerRow(
                label = "Reference back (optional)",
                uri = referenceBack,
                onPicked = { referenceBack = it }
            )

            Divider()

            Button(
                onClick = {
                    val details = CardDetails(
                        name = name.trim(),
                        title = title.trim(),
                        teamOrTheme = teamOrTheme.trim(),
                        cardNumber = cardNumber.trim(),
                        subjectPhoto = subjectPhoto,
                        referenceFront = referenceFront,
                        referenceBack = referenceBack,
                    )
                    if (details.name.isBlank()) {
                        statusMessage = "Please enter a name."
                        return@Button
                    }
                    isLoading = true
                    statusMessage = null
                    resultBitmap = null
                    val prompt = buildCardPrompt(details)
                    scope.launch(Dispatchers.IO) {
                        // Read the picked images off the main thread. Order
                        // matches the prompt: subject, reference front, back.
                        val images = buildList {
                            details.subjectPhoto?.let {
                                ImageUtils.toInputImage(context, it, "subject")?.let(::add)
                            }
                            details.referenceFront?.let {
                                ImageUtils.toInputImage(context, it, "reference_front")?.let(::add)
                            }
                            details.referenceBack?.let {
                                ImageUtils.toInputImage(context, it, "reference_back")?.let(::add)
                            }
                        }
                        CardGenerator.generate(prompt, images) { result ->
                            scope.launch(Dispatchers.Main) {
                                isLoading = false
                                when (result) {
                                    is CardGenerator.Result.Success -> {
                                        resultBitmap = result.bitmap
                                        statusMessage = "Card generated."
                                    }
                                    is CardGenerator.Result.Error -> {
                                        statusMessage = result.message
                                    }
                                }
                            }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLoading) "Generating…" else "Generate Card")
            }

            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Generating your card… this can take up to a minute.",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            statusMessage?.let {
                Text(
                    it,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            resultBitmap?.let { bmp ->
                Spacer(Modifier.height(8.dp))
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Generated card",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2.5f / 3.5f)
                )
                Button(
                    onClick = {
                        val fileName = "card_${System.currentTimeMillis()}"
                        val uri = ImageUtils.saveToGallery(context, bmp, fileName)
                        val msg = if (uri != null) "Saved to Pictures/CardGen"
                        else "Failed to save image."
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save to Gallery")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ImagePickerRow(
    label: String,
    uri: Uri?,
    onPicked: (Uri?) -> Unit,
) {
    val context = LocalContext.current

    // Gallery picker.
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { picked -> if (picked != null) onPicked(picked) }

    // Camera capture. We pre-create a destination Uri and remember it so the
    // result callback knows which Uri was just written to.
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) onPicked(pendingCameraUri) }

    val thumbnail = remember(uri) {
        uri?.let { ImageUtils.loadThumbnail(context, it) }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail.asImageBitmap(),
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp)
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                if (uri == null) label else "$label ✓",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Gallery")
                }
                OutlinedButton(
                    onClick = {
                        val captureUri = ImageUtils.createCaptureUri(context)
                        pendingCameraUri = captureUri
                        cameraLauncher.launch(captureUri)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Camera")
                }
            }
        }
    }
}
