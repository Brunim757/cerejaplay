package com.cerejaplay.app.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.cerejaplay.app.ui.screens.CatalogScreen
import com.cerejaplay.app.ui.screens.DetailScreen
import com.cerejaplay.app.ui.screens.HomeScreen
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = R.drawable.ic_cereja_logo),
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color.Unspecified
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "CerejaPlay",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(22.dp))
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun AppNavigation(config: AppConfig, repository: CerejaRepository) {
    val navController = rememberNavController()
    val tabs = remember(config.tabs) {
        config.tabs.filter { it.id.isNotBlank() }.ifEmpty { DefaultConfig.value.tabs }
    }
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
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentTabId == tab.id,
                            onClick = { openTab(tab.id) },
                            icon = { Icon(tabIcon(tab), contentDescription = tab.title) },
                            label = { Text(tab.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(padding)
        ) {
            tabs.forEach { tab ->
                composable(route = mainRoute(tab.id)) {
                    when (tab.type) {
                        "home" -> HomeScreen(
                            repository = repository,
                            onOpenDetail = { type, id ->
                                navController.navigate(detailRoute(type, id))
                            },
                            onPlay = { type, id, season, episode, title, poster ->
                                navController.navigate(playerRoute(type, id, season, episode, title, poster))
                            },
                            onOpenTab = { tabId -> openTab(tabId) }
                        )

                        "search" -> SearchScreen(
                            repository = repository,
                            endpoint = tab.endpoint ?: config.endpoints.search,
                            onOpenDetail = { type, id ->
                                navController.navigate(detailRoute(type, id))
                            }
                        )

                        else -> CatalogScreen(
                            repository = repository,
                            title = tab.title,
                            endpoint = catalogEndpoint(tab, config),
                            onOpenDetail = { type, id ->
                                navController.navigate(detailRoute(type, id))
                            }
                        )
                    }
                }
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
                    onPlay = { season, episode, title, poster ->
                        navController.navigate(playerRoute(type, id, season, episode, title, poster))
                    }
                )
            }

            composable(
                route = "player/{type}/{id}/{season}/{episode}?title={title}&poster={poster}",
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType },
                    navArgument("id") { type = NavType.StringType },
                    navArgument("season") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("episode") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("title") { type = NavType.StringType; defaultValue = "" },
                    navArgument("poster") { type = NavType.StringType; defaultValue = "" }
                )
            ) { entry ->
                val type = entry.arguments?.getString("type") ?: "movie"
                val id = entry.arguments?.getString("id") ?: ""
                val season = entry.arguments?.getInt("season") ?: 0
                val episode = entry.arguments?.getInt("episode") ?: 0
                val title = entry.arguments?.getString("title").orEmpty()
                val poster = entry.arguments?.getString("poster").orEmpty()
                PlayerScreen(
                    repository = repository,
                    type = type,
                    id = id,
                    season = season,
                    episode = episode,
                    title = title.ifBlank { "Reprodução" },
                    poster = poster.ifBlank { null },
                    onBack = { navController.popBackStack() }
                )
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
    poster: String?
): String {
    val encodedTitle = Uri.encode(title)
    val encodedPoster = Uri.encode(poster.orEmpty())
    return "player/${Uri.encode(type)}/${Uri.encode(id)}/$season/$episode?title=$encodedTitle&poster=$encodedPoster"
}