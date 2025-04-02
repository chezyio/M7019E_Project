package com.m7019e.nobi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import kotlinx.coroutines.launch

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
        composable("overlay") { OverlayScreen(navController) }
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
                modifier = Modifier.height(80.dp)
            ) {
                tabItems.forEachIndexed { index, (title, icon, _) ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* Handle navigation (e.g., open drawer) */ }) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Menu"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("overlay") }) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Open Overlay"
                        )
                    }
                }
            )
        }

        // Carousel section
        item {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Summary",
                    style = MaterialTheme.typography.headlineSmall
                )
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val carouselItems = List(5) { index ->
                        Triple("Featured $index", "Highlight $index", android.R.drawable.ic_menu_gallery)
                    }
                    items(carouselItems) { (title, subtitle, imageResId) ->
                        SimpleCarouselCard(title, subtitle, imageResId) {
                            navController.navigate("detail/$title/$subtitle/$imageResId")
                        }
                    }
                }
            }
        }

        // Grid of 4 cards
        item {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = "Exercises",
                    style = MaterialTheme.typography.headlineSmall,
                )

                val items = List(4) { index ->
                    Triple("Item $index", "Description $index", android.R.drawable.ic_menu_camera)
                }

                items.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp), // Add vertical spacing between rows
                        horizontalArrangement = Arrangement.spacedBy(16.dp) // Horizontal spacing between cards
                    ) {
                        rowItems.forEach { (title, subtitle, imageResId) ->
                            Box(modifier = Modifier.weight(1f)) {
                                SimpleCard(
                                    title = title,
                                    subtitle = subtitle,
                                    imageResId = imageResId,
                                    onClick = { navController.navigate("detail/$title/$subtitle/$imageResId") }
                                )
                            }
                        }
                    }
                }
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
fun SimpleCarouselCard(title: String, subtitle: String, imageResId: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(160.dp)
            .clickable { onClick() },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = imageResId),
                contentDescription = "Carousel Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SimpleCard(title: String, subtitle: String, imageResId: Int, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(title: String, subtitle: String, imageResId: Int, navController: NavController) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlayScreen(navController: NavController) {
    var isVisible by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInHorizontally(initialOffsetX = { it }), // Slide from right
            exit = slideOutHorizontally(targetOffsetX = { it })   // Slide to right
        ) {
            DrawerContent(
                onClose = {
                    scope.launch {
                        isVisible = false
                        // Delay to allow animation to complete before navigating back
                        kotlinx.coroutines.delay(300)
                        navController.popBackStack()
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerContent(onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "Overlay Menu",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Close"
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(20) { index ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* Handle item click if needed */ },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Item icon",
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        Column {
                            Text(
                                text = "Item $index",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Description $index",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
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