package com.example.xai_flows.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xai_flows.R
import kotlinx.coroutines.launch

@Preview
@Composable
fun NavbarMobilePreview() {
    NavbarMobile(
        onHomeClick = {},
        onPredictionsClick = {},
        onAnalyticsClick = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavbarMobile(
    onHomeClick: () -> Unit,
    onPredictionsClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    // ── Auth affordance (added for the login/signup feature) ────────────────
    // Defaulted so every existing call site (and the Preview above) keeps
    // compiling unchanged; MainActivity is the only caller that passes
    // real values, sourced from AuthSession.state.
    isLoggedIn: Boolean = false,
    userEmail: String? = null,
    onLoginClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var isMenuOpen by remember { mutableStateOf(false) }

    // Gradient background
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF1F2937), Color(0xFF1E3A8A), Color(0xFF374151))
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo + Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.logo), // Use your file name here
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(58.dp)
                        .scale(1.1f) // mimic hover scale
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "XAI-FLOWS",
                        color = Color.White,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "Explainable AI - Flood Warning System",
                        color = Color(0xFF9CA3AF),
                        fontSize = 10.sp
                    )
                }
            }

            // Mobile Menu Button
            IconButton(onClick = {
                isMenuOpen = true
                scope.launch { sheetState.show() }
            }) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Menu",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }

    // Modal Bottom Sheet (Drawer)
    if (isMenuOpen) {
        ModalBottomSheet(
            onDismissRequest = {
                isMenuOpen = false
                scope.launch { sheetState.hide() }
            },
            sheetState = sheetState,
            containerColor = Color(0xFF111827).copy(alpha = 0.95f), // dark backdrop
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Logo in Drawer
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.logo), // Use your file name here
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(58.dp)
                            .scale(1.1f) // mimic hover scale
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "XAI-FLOWS",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))

                // Menu Items
                DrawerItem("Home") {
                    isMenuOpen = false
                    onHomeClick()
                }
                DrawerItem("Predictions") {
                    isMenuOpen = false
                    onPredictionsClick()
                }
                DrawerItem("Analytics") {
                    isMenuOpen = false
                    onAnalyticsClick()
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(8.dp))

                // Auth section — logged-in state shows who's signed in and a
                // logout action; logged-out shows a way into AuthNavHost.
                if (isLoggedIn) {
                    if (!userEmail.isNullOrBlank()) {
                        Text(
                            text = "Signed in as $userEmail",
                            color = Color(0xFF9CA3AF),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    DrawerItem("Log out") {
                        isMenuOpen = false
                        onLogoutClick()
                    }
                } else {
                    DrawerItem("Log in") {
                        isMenuOpen = false
                        onLoginClick()
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerItem(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = Color.White,
        fontSize = 16.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable { onClick() }
    )
}
