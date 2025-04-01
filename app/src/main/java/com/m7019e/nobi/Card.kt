package com.m7019e.nobi

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.m7019e.nobi.ui.theme.NobiTheme

@Composable
fun SimpleCard(
    title: String,
    subtitle: String,
    imageResId: Int, // Resource ID for the image
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth(0.8f),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Image at the top
            Image(
                painter = painterResource(id = imageResId),
                contentDescription = "$title image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp), // Fixed height for the image
                contentScale = ContentScale.Crop // Crop the image to fit
            )

            // Text content below the image
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant // Slightly muted color for subtitle
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SimpleCardPreview() {
    NobiTheme {
        SimpleCard(
            title = "Card Title",
            subtitle = "This is the subtitle text",
            imageResId = android.R.drawable.ic_menu_gallery // Using a default Android drawable for preview
        )
    }
}