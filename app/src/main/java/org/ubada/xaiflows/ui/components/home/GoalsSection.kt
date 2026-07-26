package org.ubada.xaiflows.ui.components.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ubada.xaiflows.R
import androidx.compose.ui.draw.clip

data class GoalCard(val title: String, val description: String, val images: List<Int>)

@Composable
fun GoalsSection() {
    // Build list INSIDE Composable using stringResource
    val goalsList = listOf(
        GoalCard(
            title = "Prevent Drainage Blockages and Flooding",
            description = stringResource(R.string.goals_drainage),
            images = listOf(
                R.drawable.drainage1,
                R.drawable.drainage2,
                R.drawable.drainage3,
                R.drawable.drainage4,
                R.drawable.drainage5,
                R.drawable.drainage6,
                R.drawable.drainage7)
        ),
        GoalCard(
            title = "Focus on Low-Lying Areas",
            description = stringResource(R.string.goals_low_lying),
            images = listOf(R.drawable.low_lying)
        ),
        GoalCard(
            title = "Monitor Annual Rainfall Trends",
            description = stringResource(R.string.goals_rainfall),
            images = listOf(R.drawable.rainfall_graph)
        )
    )

    Column {
        SectionHeader(
            title = "Our Goals",
            subtitle = "Comprehensive flood prevention and monitoring solutions"
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            goalsList.forEach { goal ->
                GoalCardView(goal)
            }
        }
    }
}

@Composable
fun GoalCardView(goal: GoalCard) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = goal.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = goal.description, fontSize = 14.sp)

            Spacer(modifier = Modifier.height(12.dp))

            if (goal.images.size > 1) {
                // Show horizontal carousel for multiple images
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    items(goal.images) { imageRes ->
                        Image(
                            painter = painterResource(id = imageRes),
                            contentDescription = null,
                            modifier = Modifier
                                .width(240.dp)
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            } else {
                // Show single image normally
                Image(
                    painter = painterResource(id = goal.images.first()),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .padding(top = 8.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
