package com.m7019e.nobi.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.m7019e.nobi.ui.theme.NobiTheme

@Composable
fun FavoritesScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Favorites Screen", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
    }
}

@Preview(showBackground = true)
@Composable
fun FavoritesScreenPreview() {
    NobiTheme {
        FavoritesScreen()
    }
}