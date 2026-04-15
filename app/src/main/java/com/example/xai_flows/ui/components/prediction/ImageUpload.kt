package com.example.xai_flows.ui.components.prediction

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

fun createMultipartFromUri(context: Context, uri: Uri): MultipartBody.Part? {
    val contentResolver = context.contentResolver
    val inputStream = contentResolver.openInputStream(uri) ?: return null

    // Create temp file
    val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
    tempFile.outputStream().use { fileOut ->
        inputStream.copyTo(fileOut)
    }

    // Convert to Multipart
    val requestBody = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
    return MultipartBody.Part.createFormData("image", tempFile.name, requestBody)
}

@Composable
fun ImageUpload(
    onImageSelected: (MultipartBody.Part?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // Image picker launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
        val part = uri?.let { createMultipartFromUri(context, it) }
        onImageSelected(part)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        // Title
        Text(
            text = "Upload Flood Image",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color(0xFF2563EB)
        )

        // Upload box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFE0F2FE), Color(0xFFEDE9FE))
                    )
                )
                .border(
                    width = 2.dp,
                    color = Color(0xFF93C5FD),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { launcher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            // Show placeholder when no image is selected
            androidx.compose.animation.AnimatedVisibility(
                visible = imageUri == null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudUpload,
                        contentDescription = "Upload",
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap to upload",
                        color = Color(0xFF3B82F6),
                        fontSize = 14.sp
                    )
                }
            }

            // Show uploaded image preview
            androidx.compose.animation.AnimatedVisibility(
                visible = imageUri != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                imageUri?.let {
                    Image(
                        painter = rememberAsyncImagePainter(it),
                        contentDescription = "Selected Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        // Remove image button
        if (imageUri != null) {
            OutlinedButton(
                onClick = {
                    imageUri = null
                    onImageSelected(null)
                },
                shape = CircleShape,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB))
            ) {
                Text("Remove Image")
            }
        }
    }
}
