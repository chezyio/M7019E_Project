package com.m7019e.nobi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.m7019e.nobi.ui.theme.NobiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NobiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    MainNavigation(navController)
                }
            }
        }
    }
}

@Composable
fun MainNavigation(navController: NavHostController) {
    NavHost(navController, startDestination = "home") {
        composable("home") { BottomTabbedLayout(navController) }
        composable("detail/{title}/{subtitle}/{imageResId}") { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title") ?: "No Title"
            val subtitle = backStackEntry.arguments?.getString("subtitle") ?: "No Subtitle"
            val imageResId = backStackEntry.arguments?.getString("imageResId")?.toIntOrNull()
                ?: android.R.drawable.ic_menu_camera
            DetailScreen(title, subtitle, imageResId, navController)
        }
    }
}

@Composable
fun BottomTabbedLayout(navController: NavController) {
    var selectedTabIndex by remember { mutableStateOf(0) }

    val tabItems = listOf(
        Triple("Home", Icons.Default.Home, "Welcome to Home"),
        Triple("Favorites", Icons.Default.Favorite, "Your Favorite Items"),
        Triple("Settings", Icons.Default.Settings, "Manage Settings")
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
            when (selectedTabIndex) {
                0 -> HomeScreen(navController)
                1 -> FavoritesScreen()
                2 -> SettingsScreen()
            }
        }
    }
}

@Composable
fun HomeScreen(navController: NavController) {
    val items = List(6) { index ->
        Triple("Item $index", "Description $index", android.R.drawable.ic_menu_camera)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.padding(16.dp)
    ) {
        items(items) { (title, subtitle, imageResId) ->
            SimpleCard(title, subtitle, imageResId) {
                navController.navigate("detail/$title/$subtitle/$imageResId")
            }
        }
    }
}

@Composable
fun FavoritesScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Favorites Screen", style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
fun SettingsScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Settings Screen", style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
fun SimpleCard(title: String, subtitle: String, imageResId: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }, // Click to navigate
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = imageResId),
                contentDescription = "Card Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(bottom = 8.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun DetailScreen(title: String, subtitle: String, imageResId: Int, navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = imageResId),
            contentDescription = "Detail Image",
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Text(text = subtitle, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { navController.popBackStack() }) {
            Text("Back to Home")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BottomTabbedLayoutPreview() {
    NobiTheme {
        val navController = rememberNavController()
        MainNavigation(navController)
    }
}