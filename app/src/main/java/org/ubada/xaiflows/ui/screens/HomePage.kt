package org.ubada.xaiflows.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.ubada.xaiflows.ui.components.common.FooterMobile
import org.ubada.xaiflows.ui.components.home.*

@Composable
fun HomePage(
    modifier: Modifier = Modifier,
    onNavigateToPredictions: () -> Unit = {},
    /** Forwarded to FooterMobile for in-app section link navigation. */
    onNavigate: (String) -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFE0F2FE), Color.White, Color(0xFFEDE9FE))
                )
            )
            .verticalScroll(scrollState) // Enables scroll
            .padding(0.dp) // Can tweak padding here if needed
    ) {
        // Hero Section
        Column(
            Modifier.padding(
                PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp
                )
            )
        ) {
            HeroSection(onExploreClick = onNavigateToPredictions)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Landing video Section
        Column(
            Modifier.padding(horizontal = 16.dp)
        ) {
            LoopingVideoPlayer(
                videoResId = org.ubada.xaiflows.R.raw.landing,  // Place your video in res/raw folder
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Vision Section
        Column(
            Modifier.padding(horizontal = 16.dp)
        ) {
            VisionSection(
                title = "Our Vision",
                description = stringResource(org.ubada.xaiflows.R.string.vision)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Goals Section
        Column(
            Modifier.padding(horizontal = 16.dp)
        ) {
            GoalsSection()
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Features Section
        Column(
            Modifier.padding(horizontal = 16.dp)
        ) {
            FeaturesSection()
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Footer Section
        FooterMobile(onNavigate = onNavigate)
    }
}

@Preview(showBackground = true)
@Composable
fun HomePagePreview() {
    HomePage()
}
