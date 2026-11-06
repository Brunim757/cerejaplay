package com.cerejaplay.app.ui

import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cerejaplay.app.AppContainer
import com.cerejaplay.app.R
import com.cerejaplay.app.data.model.AppConfig
import com.cerejaplay.app.data.model.TabConfig
import com.cerejaplay.app.data.model.ThemeConfig
import com.cerejaplay.app.data.repo.CerejaRepository
import com.cerejaplay.app.data.repo.DefaultConfig
import com.cerejaplay.app.ui.components.CerejaWordmark
import com.cerejaplay.app.ui.screens.CatalogScreen
import com.cerejaplay.app.ui.screens.DetailScreen
import com.cerejaplay.app.ui.screens.HomeScreen
import com.cerejaplay.app.ui.screens.MyListScreen
import com.cerejaplay.app.ui.screens.PlayerScreen
import com.cerejaplay.app.ui.screens.SearchScreen
import com.cerejaplay.app.ui.theme.CerejaPlayTheme

@Composable
fun CerejaPlayRoot(container: AppContainer) {
    val viewModel: MainViewModel = viewModel(factory = MainViewModel.factory(container.repository))
    val state by viewModel.state.collectAsStateWithLifecycle()

    CerejaPlayTheme(config = state.config?.theme ?: ThemeConfig()) {
        val config = state.config
        if (config == null) {
            BootScreen()
        } else {
            CompositionLocalProvider(LocalAppConfig provides config) {
                AppNavigation(config = config, repository = container.repository)
            }
        }
    }
}

@Composable
private fun BootScreen() {
    val transition = rememberInfiniteTransition(label = "boot")
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "boot-scale"
    )
    val glow by transition.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "boot-glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = glow),
                        Color.Transparent
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(124.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_cereja_logo),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = Color.Unspecified
                )
            }
            Spacer(Modifier.height(24.dp))
            CerejaWordmark(fontSize = 30.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Filmes, séries e animes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(30.dp))
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Preparando seu catálogo...",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun AppNavigation(config: AppConfig, repository: CerejaRepository) {
    val navController = rememberNavController()
    val tabs = remember(config.tabs) {
        config.tabs.filter { it.id.isNotBlank() }.ifEmpty { DefaultConfig.value.tabs }
    }
    // A busca fica apenas no botão do topo: a aba correspondente continua
    // existindo para navegação, mas sai da barra inferior.
    val bottomTabs = remember(tabs) { tabs.filter { it.type != "search" } }
    val startTab = remember(tabs) { tabs.firstOrNull { it.type == "home" } ?: tabs.first() }
    val startRoute = remember(startTab) { mainRoute(startTab.id) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentTabId = remember(currentRoute, tabs) {
        tabs.firstOrNull { mainRoute(it.id) == currentRoute }?.id
    }
    val topLevel = currentRoute?.startsWith("main/") == true

    fun openTab(tabId: String) {
        val target = tabs.firstOrNull { it.id == tabId } ?: return
        navController.navigate(mainRoute(target.id)) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (topLevel) {
                CerejaBottomBar(
                    tabs = bottomTabs,
                    currentTabId = currentTabId,
                    onSelect = ::openTab
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(padding),
            enterTransition = {
                fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 12 }
            },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = {
                fadeOut(tween(180)) + slideOutHorizontally(tween(220)) { it / 12 }
            }
        ) {
            tabs.forEach { tab ->
                composable(route = mainRoute(tab.id)) {
                    when (tab.type) {
                        "home" -> HomeScreen(
                            repository = repository,
                            onOpenDetail = { type, id ->
                                navController.navigate(detailRoute(type, id))
                            },
                            onPlay = { type, id, season, episode, title, poster, backdrop ->
                                navController.navigate(
                                    playerRoute(type, id, season, episode, title, poster, backdrop)
                                )
                            },
                            onOpenTab = { tabId -> openTab(tabId) },
                            onOpenMyList = { navController.navigate("mylist") }
                        )

                        "search" -> SearchScreen(
                            repository = repository,
                            endpoint = tab.endpoint ?: config.endpoints.search,
                            onOpenDetail = { type, id ->
                                navController.navigate(detailRoute(type, id))
                            },
                            onOpenTab = { tabId -> openTab(tabId) }
                        )

                        else -> CatalogScreen(
                            repository = repository,
                            title = tab.title,
                            endpoint = catalogEndpoint(tab, config),
                            onOpenDetail = { type, id ->
                                navController.navigate(detailRoute(type, id))
                            },
                            onOpenSearch = {
                                config.tabs.firstOrNull { it.type == "search" }
                                    ?.let { searchTab -> openTab(searchTab.id) }
                            }
                        )
                    }
                }
            }

            composable(route = "mylist") {
                MyListScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() },
                    onOpenDetail = { type, id ->
                        navController.navigate(detailRoute(type, id))
                    }
                )
            }

            composable(
                route = "detail/{type}/{id}",
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType },
                    navArgument("id") { type = NavType.StringType }
                )
            ) { entry ->
                val type = entry.arguments?.getString("type") ?: "movie"
                val id = entry.arguments?.getString("id") ?: ""
                DetailScreen(
                    repository = repository,
                    type = type,
                    id = id,
                    onBack = { navController.popBackStack() },
                    onPlay = { season, episode, title, poster, backdrop ->
                        navController.navigate(
                            playerRoute(type, id, season, episode, title, poster, backdrop)
                        )
                    }
                )
            }

            composable(
                route = "player/{type}/{id}/{season}/{episode}?title={title}&poster={poster}&backdrop={backdrop}",
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType },
                    navArgument("id") { type = NavType.StringType },
                    navArgument("season") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("episode") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("title") { type = NavType.StringType; defaultValue = "" },
                    navArgument("poster") { type = NavType.StringType; defaultValue = "" },
                    navArgument("backdrop") { type = NavType.StringType; defaultValue = "" }
                )
            ) { entry ->
                val type = entry.arguments?.getString("type") ?: "movie"
                val id = entry.arguments?.getString("id") ?: ""
                val season = entry.arguments?.getInt("season") ?: 0
                val episode = entry.arguments?.getInt("episode") ?: 0
                val title = entry.arguments?.getString("title").orEmpty()
                val poster = entry.arguments?.getString("poster").orEmpty()
                val backdrop = entry.arguments?.getString("backdrop").orEmpty()
                PlayerScreen(
                    repository = repository,
                    type = type,
                    id = id,
                    season = season,
                    episode = episode,
                    title = title.ifBlank { "Reprodução" },
                    poster = poster.ifBlank { null },
                    backdrop = backdrop.ifBlank { null },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun CerejaBottomBar(
    tabs: List<TabConfig>,
    currentTabId: String?,
    onSelect: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
        tonalElevation = 0.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.9f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(68.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val selected = currentTabId == tab.id
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelect(tab.id) }
                            .padding(vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .width(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                    else Color.Transparent
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tabIcon(tab),
                                contentDescription = tab.title,
                                tint = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun catalogEndpoint(tab: TabConfig, config: AppConfig): String {
    tab.endpoint?.takeIf { it.isNotBlank() }?.let { return it }
    return when (tab.mediaType) {
        "movie" -> config.endpoints.catalog.movies
        "series" -> config.endpoints.catalog.series
        "anime" -> config.endpoints.catalog.animes
        else -> config.endpoints.catalog.movies
    }
}

private fun tabIcon(tab: TabConfig): ImageVector = when (tab.type) {
    "home" -> Icons.Rounded.Home
    "search" -> Icons.Rounded.Search
    else -> when (tab.mediaType) {
        "movie" -> Icons.Rounded.Movie
        "series" -> Icons.Rounded.Tv
        "anime" -> Icons.Rounded.AutoAwesome
        else -> Icons.Rounded.GridView
    }
}

private fun mainRoute(tabId: String): String = "main/${Uri.encode(tabId)}"

private fun detailRoute(type: String, id: String): String = "detail/${Uri.encode(type)}/${Uri.encode(id)}"

private fun playerRoute(
    type: String,
    id: String,
    season: Int,
    episode: Int,
    title: String,
    poster: String?,
    backdrop: String?
): String {
    val encodedTitle = Uri.encode(title)
    val encodedPoster = Uri.encode(poster.orEmpty())
    val encodedBackdrop = Uri.encode(backdrop.orEmpty())
    return "player/${Uri.encode(type)}/${Uri.encode(id)}/$season/$episode" +
        "?title=$encodedTitle&poster=$encodedPoster&backdrop=$encodedBackdrop"
}
