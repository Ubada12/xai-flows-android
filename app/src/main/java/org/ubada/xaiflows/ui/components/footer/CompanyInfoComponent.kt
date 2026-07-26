/**
 * CompanyInfoComponent.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Top section of the footer: logo, company name, description, and a working
 * newsletter subscribe form.
 *
 * Subscribe button behaviour:
 *  1. Validates the email address with a standard regex.
 *  2. Shows an inline error message if invalid.
 *  3. On success: shows a Toast, clears the field, hides the error.
 */
package org.ubada.xaiflows.ui.components.footer

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ubada.xaiflows.R
import org.ubada.xaiflows.ui.models.COMPANY_INFO

/** Simple RFC-5322 subset regex — good enough for client-side UX validation. */
private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")

@Preview
@Composable
fun CompanyInfoComponent() {
    val context = LocalContext.current
    var email       by remember { mutableStateOf("") }
    var emailError  by remember { mutableStateOf<String?>(null) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        // ── Logo + Company Name ──────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter            = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier           = Modifier.size(58.dp).scale(1.1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text       = COMPANY_INFO.name,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF60A5FA)
                )
                Text(
                    text     = "Flood Prediction System",
                    fontSize = 12.sp,
                    color    = Color(0xFF9CA3AF)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Description ─────────────────────────────────────────────────────
        Text(
            text      = COMPANY_INFO.description,
            color     = Color(0xFF9CA3AF),
            fontSize  = 12.sp,
            modifier  = Modifier.padding(horizontal = 16.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Stay Updated ─────────────────────────────────────────────────────
        Text(
            text       = "Stay Updated",
            color      = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize   = 16.sp,
            modifier   = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Email Input
            OutlinedTextField(
                value         = email,
                onValueChange = {
                    email = it
                    if (emailError != null) emailError = null   // clear error on edit
                },
                placeholder = {
                    Text(
                        text       = "Enter your email",
                        fontSize   = 14.sp,
                        color      = Color(0xFF9CA3AF),
                        fontWeight = FontWeight.Normal
                    )
                },
                isError  = emailError != null,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .background(Color(0xFF1F2937), shape = RoundedCornerShape(8.dp)),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor     = Color.Transparent,
                    unfocusedContainerColor   = Color.Transparent,
                    errorContainerColor       = Color.Transparent,
                    focusedIndicatorColor     = Color(0xFF3B82F6),
                    unfocusedIndicatorColor   = Color(0xFF374151),
                    errorIndicatorColor       = Color(0xFFEF4444),
                    cursorColor               = Color.White,
                    focusedTextColor          = Color.White,
                    unfocusedTextColor        = Color.White,
                    errorTextColor            = Color.White,
                    focusedPlaceholderColor   = Color(0xFF9CA3AF),
                    unfocusedPlaceholderColor = Color(0xFF9CA3AF)
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Subscribe Button — full-height to match the TextField
            Button(
                onClick = {
                    when {
                        email.isBlank() ->
                            emailError = "Email cannot be empty"
                        !EMAIL_REGEX.matches(email) ->
                            emailError = "Enter a valid email address"
                        else -> {
                            // Success
                            Toast.makeText(context, "Subscribed! Thanks for joining.", Toast.LENGTH_SHORT).show()
                            email      = ""
                            emailError = null
                        }
                    }
                },
                modifier = Modifier
                    .height(56.dp)          // matches TextField height — was 26dp (too small)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF3B82F6), Color(0xFF6366F1))
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ),
                shape  = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Text("Subscribe", color = Color.White, fontSize = 14.sp)
            }
        }

        // Inline validation error
        if (emailError != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text     = emailError!!,
                color    = Color(0xFFEF4444),
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text      = "Get the latest updates on flood predictions and system improvements.",
            color     = Color(0xFF9CA3AF),
            fontSize  = 10.sp,
            textAlign = TextAlign.Start,
            modifier  = Modifier.fillMaxWidth()
        )
    }
}
