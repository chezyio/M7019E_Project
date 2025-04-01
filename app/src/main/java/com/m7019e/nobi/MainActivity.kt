package com.m7019e.nobi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.m7019e.nobi.ui.theme.NobiTheme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NobiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BottomTabbedLayout()
                }
            }
        }
    }
}

@Composable
fun BottomTabbedLayout() {
    var selectedTabIndex by remember { mutableStateOf(0) }

    val tabItems = listOf(
        Triple("Tab 1", Icons.Default.Home, "Hello Tab 1!"),
        Triple("Tab 2", Icons.Default.Favorite, "Hello Tab 2!"),
        Triple("Tab 3", Icons.Default.Settings, "Hello Tab 3!")
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.height(64.dp)
            ) {
                tabItems.forEachIndexed { index, (title, _) ->
                    NavigationBarItem(
                        icon = { Icon(tabItems[index].second, contentDescription = tabItems[index].first) },
                        label = { Text(title) },
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            // Replace Box with LazyVerticalGrid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2), // 2 columns
                modifier = Modifier.padding(8.dp)
            ) {
                items(20) { index -> // You can change the number of items as needed
                    SimpleCard(
                        title = "Title $index",
                        subtitle = "Subtitle $index",
                        imageResId = android.R.drawable.ic_menu_camera
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleCard(title: String, subtitle: String, imageResId: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BottomTabbedLayoutPreview() {
    NobiTheme {
        BottomTabbedLayout()
    }
}