package com.codetiger.mymusicapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.codetiger.mymusicapp.AppContainer
import com.codetiger.mymusicapp.data.ListRef
import com.codetiger.mymusicapp.ui.screens.AddSongScreen
import com.codetiger.mymusicapp.ui.screens.AddToListScreen
import com.codetiger.mymusicapp.ui.screens.DefaultListScreen
import com.codetiger.mymusicapp.ui.screens.HomeScreen
import com.codetiger.mymusicapp.ui.screens.NowPlayingScreen
import com.codetiger.mymusicapp.ui.screens.PhotoPreviewScreen
import com.codetiger.mymusicapp.ui.screens.PlaylistScreen
import com.codetiger.mymusicapp.ui.screens.RecentlyRemovedScreen
import com.codetiger.mymusicapp.ui.screens.SettingsScreen
import com.codetiger.mymusicapp.ui.screens.SetupScreen
import com.codetiger.mymusicapp.ui.screens.SongScreen
import com.codetiger.mymusicapp.ui.screens.StorageScreen
import com.codetiger.mymusicapp.ui.screens.TextSizeScreen
import com.codetiger.mymusicapp.ui.screens.UpNextScreen
import com.codetiger.mymusicapp.ui.screens.WelcomeNameScreen
import com.codetiger.mymusicapp.ui.screens.WelcomePhotoScreen
import com.codetiger.mymusicapp.ui.screens.YourNameAndPhotoScreen
import com.codetiger.mymusicapp.ui.theme.MusicColors

/** All screens. First launch starts at Welcome; afterwards at Home. */
@Composable
fun MyMusicApp(app: AppContainer, startAtWelcome: Boolean) {
    val nav = rememberNavController()
    val ui = app.ui

    DisposableEffect(nav) {
        val listener = androidx.navigation.NavController.OnDestinationChangedListener { _, _, _ -> ui.messages.onScreenChanged() }
        nav.addOnDestinationChangedListener(listener)
        onDispose { nav.removeOnDestinationChangedListener(listener) }
    }

    // Something shared from another app opens Add Song (ADD-2).
    val incoming by ui.incoming.collectAsStateWithLifecycle()
    LaunchedEffect(incoming != null) {
        if (incoming != null && nav.currentDestination?.route?.startsWith("welcome") != true) {
            nav.navigate(Routes.ADD) { launchSingleTop = true }
        }
    }
    val openPlayer by ui.openPlayer.collectAsStateWithLifecycle()
    LaunchedEffect(openPlayer) {
        if (openPlayer) {
            ui.openPlayer.value = false
            if (app.player.state.value.hasSong) nav.navigate(Routes.PLAYER) { launchSingleTop = true }
        }
    }

    CompositionLocalProvider(LocalApp provides app, LocalNav provides nav, LocalUi provides ui) {
        Surface(color = MusicColors.Surface, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.safeDrawingPadding()) {
                NavHost(nav, startDestination = if (startAtWelcome) Routes.WELCOME else Routes.HOME) {
                    composable(Routes.WELCOME) { WelcomeNameScreen() }
                    composable(Routes.WELCOME_PHOTO) { WelcomePhotoScreen() }
                    composable(Routes.PHOTO_PREVIEW) { PhotoPreviewScreen(it.arguments?.getString("from") ?: Routes.FROM_SETTINGS) }
                    composable(Routes.SETUP) { SetupScreen(it.arguments?.getString("from") ?: Routes.FROM_SETTINGS) }
                    composable(Routes.HOME) { HomeScreen() }
                    composable(Routes.ADD) { AddSongScreen() }
                    composable(Routes.LIST) { entry ->
                        ListRef.decode(entry.arguments?.getString("ref"))?.let { PlaylistScreen(it) }
                    }
                    composable(
                        Routes.SONG,
                        arguments = listOf(
                            navArgument("id") { type = NavType.LongType },
                            navArgument("from") { type = NavType.StringType; nullable = true; defaultValue = null },
                        ),
                    ) { entry ->
                        SongScreen(entry.arguments!!.getLong("id"), ListRef.decode(entry.arguments?.getString("from")))
                    }
                    composable(Routes.ADD_TO_LIST, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                        AddToListScreen(entry.arguments!!.getLong("id"))
                    }
                    composable(Routes.PLAYER) { NowPlayingScreen() }
                    composable(Routes.UP_NEXT) { UpNextScreen() }
                    composable(Routes.SETTINGS) { SettingsScreen() }
                    composable(Routes.YOU) { YourNameAndPhotoScreen() }
                    composable(Routes.TEXT_SIZE) { TextSizeScreen() }
                    composable(Routes.DEFAULT_LIST) { DefaultListScreen() }
                    composable(Routes.STORAGE) { StorageScreen() }
                    composable(Routes.REMOVED) { RecentlyRemovedScreen() }
                }
            }
        }
    }
}
