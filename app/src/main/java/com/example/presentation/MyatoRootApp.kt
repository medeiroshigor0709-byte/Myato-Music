package com.example.presentation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.core.designsystem.AddToPlaylistsDialog
import com.example.core.designsystem.AppFontCatalog
import com.example.core.designsystem.BuiltInThemes
import com.example.core.designsystem.CreatePlaylistDialog
import com.example.core.designsystem.DeleteTracksConfirmDialog
import com.example.core.designsystem.LocalMotionSettings
import com.example.core.designsystem.LocalMyatoColors
import com.example.core.designsystem.MiniPlayerBar
import com.example.core.designsystem.MyatoMusicTheme
import com.example.core.designsystem.SlantedStripesTransitionOverlay
import com.example.core.designsystem.TrackContextMenuSheet
import com.example.core.designsystem.TrackDetailsDialog
import com.example.core.designsystem.rememberMotionEnvironmentState
import com.example.core.storage.TrackShareAndDeleteHelper
import com.example.domain.models.MusicTrack
import com.example.domain.models.QueueOriginType
import com.example.feature.equalizer.EqualizerScreen
import com.example.feature.equalizer.SleepTimerScreen
import com.example.feature.filemanager.FileManagerScreen
import com.example.feature.home.HomeScreen
import com.example.feature.home.OnboardingScreen
import com.example.feature.home.PermissionsScreen
import com.example.feature.home.SplashScreen
import com.example.feature.library.AlbumsScreen
import com.example.feature.library.AllSongsScreen
import com.example.feature.library.ArtistsScreen
import com.example.feature.library.FavoritesScreen
import com.example.feature.library.FoldersScreen
import com.example.feature.library.GenresScreen
import com.example.feature.library.LibraryHubScreen
import com.example.feature.lyrics.LyricsScreen
import com.example.feature.lyrics.TagEditorScreen
import com.example.feature.player.FullPlayerScreen
import com.example.feature.queues.PlaylistsScreen
import com.example.feature.queues.QueuesScreen
import com.example.feature.statistics.AboutScreen
import com.example.feature.statistics.BackupScreen
import com.example.feature.statistics.SettingsScreen
import com.example.feature.statistics.StatisticsScreen
import com.example.feature.statistics.WidgetsShortcutsScreen
import com.example.feature.themes.FontPreviewScreen
import com.example.feature.themes.FontSettingsScreen
import com.example.feature.themes.ThemeCenterScreen
import com.example.feature.themes.ThemeEditorScreen
import com.example.presentation.navigation.MyatoRoute
import kotlinx.coroutines.flow.flowOf

private data class TopNavItem(
    val route: MyatoRoute,
    val label: String,
    val icon: ImageVector,
    val testTag: String,
)

@Composable
fun MyatoRootApp(
    viewModel: MyatoMainViewModel,
    openPlayerOnStart: Boolean = false,
    openPlaylistPickerOnStart: Boolean = false,
) {
    val activeTheme by viewModel.activeTheme.collectAsState()
    val allThemes by viewModel.allThemes.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val tracks by viewModel.tracks.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val genres by viewModel.genres.collectAsState()
    val favorites by viewModel.favoriteTracks.collectAsState()
    val recentTracks by viewModel.recentlyAddedTracks.collectAsState()
    val mostPlayed by viewModel.mostPlayedTracks.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val queues by viewModel.queues.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val eqState by viewModel.equalizerState.collectAsState()
    val eqPresets by viewModel.equalizerPresets.collectAsState()
    val spectrumBars by viewModel.spectrumBars.collectAsState()
    val hasRealFftSignal by viewModel.hasRealFftSignal.collectAsState()
    val sleepTimerState by viewModel.sleepTimerState.collectAsState()
    val statsSummary by viewModel.statsSummary.collectAsState()
    val backupHistory by viewModel.backupHistory.collectAsState()
    val previewedBackup by viewModel.previewedBackup.collectAsState()
    val duplicates by viewModel.duplicates.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val currentTrackId = playbackState.currentTrack?.id ?: -1L
    val currentBookmarks by remember(currentTrackId) {
        if (currentTrackId != -1L) viewModel.observeBookmarks(currentTrackId) else flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    val currentNote by remember(currentTrackId) {
        if (currentTrackId != -1L) viewModel.observeNote(currentTrackId) else flowOf(null)
    }.collectAsState(initial = null)

    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var contextMenuTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var playlistDialogTrackIds by remember { mutableStateOf<List<Long>?>(null) }
    var viewDetailsTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var deleteConfirmTracks by remember { mutableStateOf<List<MusicTrack>?>(null) }
    var createPlaylistWithTracksTarget by remember { mutableStateOf<List<Long>?>(null) }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    LaunchedEffect(openPlayerOnStart, openPlaylistPickerOnStart) {
        if (openPlaylistPickerOnStart && playbackState.currentTrack != null) {
            playlistDialogTrackIds = listOf(playbackState.currentTrack!!.id)
        }
    }

    val activeGeneralOption = remember(preferences.activeGeneralFontId, preferences.customFonts) {
        AppFontCatalog.findFontOption(preferences.activeGeneralFontId, preferences.customFonts)
    }
    val activeDecorativeOption = remember(preferences.activeDecorativeFontId, preferences.customFonts) {
        AppFontCatalog.findFontOption(preferences.activeDecorativeFontId, preferences.customFonts)
    }
    val activeFontSummary = remember(activeGeneralOption, activeDecorativeOption) {
        if (activeGeneralOption.id == activeDecorativeOption.id) {
            activeGeneralOption.name
        } else {
            "${activeDecorativeOption.name} + ${activeGeneralOption.name}"
        }
    }

    val motionSettings = rememberMotionEnvironmentState(
        preferences = preferences,
        themeAnimationMode = activeTheme.animationMode,
        onAppForegroundChanged = { inFg -> viewModel.setAppForeground(inFg) },
    )

    CompositionLocalProvider(LocalMotionSettings provides motionSettings) {
        MyatoMusicTheme(
            activeTheme = activeTheme,
            activeFontId = preferences.activeGeneralFontId,
            activeGeneralFontId = preferences.activeGeneralFontId,
            activeDecorativeFontId = preferences.activeDecorativeFontId,
            customFonts = preferences.customFonts,
        ) {
            val colors = LocalMyatoColors.current
            val navBackStack by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStack?.destination
            val currentRouteKey = currentDestination?.route ?: ""

        val isChromeless = currentDestination?.hasRoute<MyatoRoute.Splash>() == true ||
            currentDestination?.hasRoute<MyatoRoute.Onboarding>() == true ||
            currentDestination?.hasRoute<MyatoRoute.Permissions>() == true ||
            currentDestination?.hasRoute<MyatoRoute.FullPlayer>() == true

        val topItems = remember {
            listOf(
                TopNavItem(MyatoRoute.Home, "Início", Icons.Rounded.Home, "nav_home"),
                TopNavItem(MyatoRoute.LibraryHub, "Biblioteca", Icons.Rounded.LibraryMusic, "nav_library"),
                TopNavItem(MyatoRoute.Playlists, "Playlists", Icons.AutoMirrored.Rounded.QueueMusic, "nav_playlists"),
                TopNavItem(MyatoRoute.ThemeCenter, "Temas", Icons.Rounded.Palette, "nav_themes"),
                TopNavItem(MyatoRoute.Settings, "Ajustes", Icons.Rounded.Settings, "nav_settings"),
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background),
        ) {
            val useRail = !isChromeless && maxWidth >= 600.dp

            Row(modifier = Modifier.fillMaxSize()) {
                if (useRail) {
                    NavigationRail(
                        containerColor = colors.altBackground,
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        topItems.forEach { item ->
                            val selected = currentDestination?.hasRoute(item.route::class) == true
                            val iconScale by animateFloatAsState(
                                targetValue = if (selected && motionSettings.isAnimationsActive) 1.14f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "rail_icon_scale_${item.testTag}",
                            )
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo<MyatoRoute.Home> { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        item.icon,
                                        contentDescription = item.label,
                                        modifier = Modifier.scale(iconScale),
                                    )
                                },
                                label = { Text(item.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = colors.primary,
                                    indicatorColor = colors.primary,
                                    unselectedIconColor = colors.textSecondary,
                                    unselectedTextColor = colors.textSecondary,
                                ),
                                modifier = Modifier.testTag(item.testTag),
                            )
                        }
                    }
                }

                Scaffold(
                    containerColor = colors.background,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (!isChromeless) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.altBackground)
                                    .navigationBarsPadding(),
                            ) {
                                MiniPlayerBar(
                                    track = playbackState.currentTrack,
                                    isPlaying = playbackState.isPlaying,
                                    positionMs = playbackState.positionMs,
                                    durationMs = playbackState.durationMs,
                                    onOpenPlayer = { navController.navigate(MyatoRoute.FullPlayer) },
                                    onPlayPause = { viewModel.playbackController.togglePlayPause() },
                                    onNext = { viewModel.playbackController.skipNext() },
                                )
                                if (!useRail) {
                                    NavigationBar(
                                        containerColor = colors.altBackground,
                                        contentColor = colors.textPrimary,
                                    ) {
                                        topItems.forEach { item ->
                                            val selected = currentDestination?.hasRoute(item.route::class) == true
                                            val iconScale by animateFloatAsState(
                                                targetValue = if (selected && motionSettings.isAnimationsActive) 1.14f else 1f,
                                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                                label = "bar_icon_scale_${item.testTag}",
                                            )
                                            NavigationBarItem(
                                                selected = selected,
                                                onClick = {
                                                    navController.navigate(item.route) {
                                                        popUpTo<MyatoRoute.Home> { saveState = true }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                },
                                                icon = {
                                                    Icon(
                                                        item.icon,
                                                        contentDescription = item.label,
                                                        modifier = Modifier.scale(iconScale),
                                                    )
                                                },
                                                label = { Text(item.label, maxLines = 1) },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = Color.Black,
                                                    selectedTextColor = colors.primary,
                                                    indicatorColor = colors.primary,
                                                    unselectedIconColor = colors.textSecondary,
                                                    unselectedTextColor = colors.textSecondary,
                                                ),
                                                modifier = Modifier.testTag(item.testTag),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    ) {
                        val navEnter = remember(motionSettings.isAnimationsActive, motionSettings.isReducedMotionActive) {
                            when {
                                !motionSettings.isAnimationsActive -> EnterTransition.None
                                motionSettings.isReducedMotionActive -> fadeIn(tween(180))
                                else -> slideInHorizontally(tween(380, easing = FastOutSlowInEasing)) { (it * 0.28f).toInt() } +
                                    fadeIn(tween(320))
                            }
                        }
                        val navExit = remember(motionSettings.isAnimationsActive, motionSettings.isReducedMotionActive) {
                            when {
                                !motionSettings.isAnimationsActive -> ExitTransition.None
                                motionSettings.isReducedMotionActive -> fadeOut(tween(180))
                                else -> slideOutHorizontally(tween(380, easing = FastOutSlowInEasing)) { -(it * 0.25f).toInt() } +
                                    fadeOut(tween(260))
                            }
                        }
                        val navPopEnter = remember(motionSettings.isAnimationsActive, motionSettings.isReducedMotionActive) {
                            when {
                                !motionSettings.isAnimationsActive -> EnterTransition.None
                                motionSettings.isReducedMotionActive -> fadeIn(tween(180))
                                else -> slideInHorizontally(tween(380, easing = FastOutSlowInEasing)) { -(it * 0.25f).toInt() } +
                                    fadeIn(tween(320))
                            }
                        }
                        val navPopExit = remember(motionSettings.isAnimationsActive, motionSettings.isReducedMotionActive) {
                            when {
                                !motionSettings.isAnimationsActive -> ExitTransition.None
                                motionSettings.isReducedMotionActive -> fadeOut(tween(180))
                                else -> slideOutHorizontally(tween(380, easing = FastOutSlowInEasing)) { (it * 0.28f).toInt() } +
                                    fadeOut(tween(260))
                            }
                        }

                        NavHost(
                            navController = navController,
                            startDestination = MyatoRoute.Splash,
                            enterTransition = { navEnter },
                            exitTransition = { navExit },
                            popEnterTransition = { navPopEnter },
                            popExitTransition = { navPopExit },
                        ) {
                            composable<MyatoRoute.Splash> {
                                SplashScreen(
                                    onFinished = {
                                        val dest: MyatoRoute = when {
                                            openPlayerOnStart -> MyatoRoute.FullPlayer
                                            !preferences.hasCompletedOnboarding -> MyatoRoute.Onboarding
                                            else -> MyatoRoute.Home
                                        }
                                        if (dest == MyatoRoute.Home) {
                                            viewModel.refreshLibrary()
                                        }
                                        navController.navigate(dest) {
                                            popUpTo<MyatoRoute.Splash> { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable<MyatoRoute.Onboarding> {
                                OnboardingScreen(
                                    onContinue = {
                                        viewModel.completeOnboarding()
                                        navController.navigate(MyatoRoute.Permissions) {
                                            popUpTo<MyatoRoute.Onboarding> { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable<MyatoRoute.Permissions> {
                                PermissionsScreen(
                                    onPermissionsGranted = {
                                        viewModel.refreshLibrary()
                                        navController.navigate(MyatoRoute.Home) {
                                            popUpTo<MyatoRoute.Permissions> { inclusive = true }
                                        }
                                    },
                                    onImportSafFolder = { uri -> viewModel.importSafFolder(uri) },
                                )
                            }

                            composable<MyatoRoute.Home> {
                                HomeScreen(
                                    tracks = tracks,
                                    recentTracks = recentTracks,
                                    mostPlayed = mostPlayed,
                                    queues = queues,
                                    playbackState = playbackState,
                                    statsSummary = statsSummary,
                                    isScanning = isScanning,
                                    onRefreshLibrary = { viewModel.refreshLibrary() },
                                    onOpenPermissions = { navController.navigate(MyatoRoute.Permissions) },
                                    onPlayTrackList = { list, idx, name, type ->
                                        viewModel.playTracks(list, idx, name, type)
                                    },
                                    onSwitchQueue = { qid -> viewModel.playbackController.switchToQueue(qid) },
                                    onOpenPlayer = { navController.navigate(MyatoRoute.FullPlayer) },
                                    onOpenAllSongs = { navController.navigate(MyatoRoute.AllSongs) },
                                    onOpenQueues = { navController.navigate(MyatoRoute.Queues) },
                                    onOpenEqualizer = { navController.navigate(MyatoRoute.Equalizer) },
                                    onOpenSleepTimer = { navController.navigate(MyatoRoute.SleepTimer) },
                                    onOpenFileManager = { navController.navigate(MyatoRoute.FileManager) },
                                    onOpenStats = { navController.navigate(MyatoRoute.Statistics) },
                                    onOpenBackup = { navController.navigate(MyatoRoute.Backup) },
                                    onOpenWidgets = { navController.navigate(MyatoRoute.WidgetsShortcuts) },
                                    onTrackMoreClick = { contextMenuTrack = it },
                                )
                            }

                            composable<MyatoRoute.LibraryHub> {
                                LibraryHubScreen(
                                    tracksCount = tracks.size,
                                    albumsCount = albums.size,
                                    artistsCount = artists.size,
                                    foldersCount = folders.size,
                                    genresCount = genres.size,
                                    favoritesCount = favorites.size,
                                    onOpenAllSongs = { navController.navigate(MyatoRoute.AllSongs) },
                                    onOpenAlbums = { navController.navigate(MyatoRoute.Albums) },
                                    onOpenArtists = { navController.navigate(MyatoRoute.Artists) },
                                    onOpenFolders = { navController.navigate(MyatoRoute.Folders) },
                                    onOpenGenres = { navController.navigate(MyatoRoute.Genres) },
                                    onOpenFavorites = { navController.navigate(MyatoRoute.Favorites) },
                                    onOpenFileManager = { navController.navigate(MyatoRoute.FileManager) },
                                    onOpenQueues = { navController.navigate(MyatoRoute.Queues) },
                                    onRefreshLibrary = { viewModel.refreshLibrary() },
                                )
                            }

                            composable<MyatoRoute.AllSongs> {
                                AllSongsScreen(
                                    tracks = tracks,
                                    currentTrackId = playbackState.currentTrack?.id,
                                    duplicates = duplicates,
                                    initialSortOrder = preferences.savedSortOrder,
                                    initialFormatFilter = preferences.savedFormatFilter,
                                    onBack = { navController.popBackStack() },
                                    onRefresh = { viewModel.refreshLibrary() },
                                    onScanDuplicates = { viewModel.scanDuplicates() },
                                    onPlayTracks = { list, idx, name, type ->
                                        viewModel.playTracks(list, idx, name, type)
                                    },
                                    onBatchAddToPlaylists = { ids -> playlistDialogTrackIds = ids },
                                    onCreatePlaylistWithTracks = { name, ids ->
                                        viewModel.createPlaylistWithTracks(name, ids)
                                    },
                                    onBatchAddToQueue = { tracksToAdd ->
                                        viewModel.addTracksToActiveQueue(tracksToAdd)
                                    },
                                    onBatchShare = { tracksToShare ->
                                        TrackShareAndDeleteHelper.shareTracks(context, tracksToShare)
                                    },
                                    onBatchDeleteFiles = { tracksToDelete ->
                                        deleteConfirmTracks = tracksToDelete
                                    },
                                    onBatchEditTags = { ids ->
                                        navController.navigate(MyatoRoute.TagEditor(ids.joinToString(",")))
                                    },
                                    onTrackMoreClick = { contextMenuTrack = it },
                                    onSaveSortOrder = { viewModel.setLibrarySortOrder(it) },
                                    onSaveFormatFilter = { viewModel.setLibraryFormatFilter(it) },
                                    onSmartSelect = { criteria, ref ->
                                        viewModel.selectTracksByCriteria(tracks, criteria, ref)
                                    },
                                    sortTracks = { list, order, q -> viewModel.sortTracks(list, order, q) },
                                )
                            }

                            composable<MyatoRoute.Artists> {
                                ArtistsScreen(
                                    artists = artists,
                                    tracks = tracks,
                                    onBack = { navController.popBackStack() },
                                    onPlayArtistQueue = { name, list ->
                                        viewModel.playTracks(list, 0, name, QueueOriginType.ARTIST, name)
                                    },
                                    onPinShortcut = { type, key, label -> viewModel.pinShortcut(type, key, label) },
                                )
                            }

                            composable<MyatoRoute.Albums> {
                                AlbumsScreen(
                                    albums = albums,
                                    tracks = tracks,
                                    onBack = { navController.popBackStack() },
                                    onPlayAlbumQueue = { album, list, idx ->
                                        viewModel.playTracks(list, idx, album.title, QueueOriginType.ALBUM, album.id.toString())
                                    },
                                )
                            }

                            composable<MyatoRoute.Folders> {
                                FoldersScreen(
                                    folders = folders,
                                    tracks = tracks,
                                    monitoredFolders = preferences.monitoredFolders,
                                    onUpdateMonitoredFolders = { viewModel.setMonitoredFolders(it) },
                                    onBack = { navController.popBackStack() },
                                    onPlayFolder = { folder, list ->
                                        viewModel.playTracks(list, 0, folder.name, QueueOriginType.FOLDER, folder.path)
                                    },
                                )
                            }

                            composable<MyatoRoute.Genres> {
                                GenresScreen(
                                    genres = genres,
                                    tracks = tracks,
                                    onBack = { navController.popBackStack() },
                                    onPlayGenre = { genreName, list ->
                                        viewModel.playTracks(list, 0, genreName, QueueOriginType.CUSTOM, genreName)
                                    },
                                )
                            }

                            composable<MyatoRoute.Favorites> {
                                FavoritesScreen(
                                    favorites = favorites,
                                    currentTrackId = playbackState.currentTrack?.id,
                                    onBack = { navController.popBackStack() },
                                    onPlayFavorites = { list, idx ->
                                        viewModel.playTracks(list, idx, "Favoritos", QueueOriginType.FAVORITES, "favorites")
                                    },
                                    onTrackMoreClick = { contextMenuTrack = it },
                                )
                            }

                            composable<MyatoRoute.Playlists> {
                                PlaylistsScreen(
                                    playlists = playlists,
                                    allTracks = tracks,
                                    observePlaylistTrackIds = { pid -> viewModel.observePlaylistTrackIds(pid) },
                                    onCreatePlaylist = { name -> viewModel.createPlaylist(name) },
                                    onRenamePlaylist = { id, name -> viewModel.renamePlaylist(id, name) },
                                    onDeletePlaylist = { id -> viewModel.deletePlaylist(id) },
                                    onPlayPlaylist = { pl, list, shuffle ->
                                        val final = if (shuffle) list.shuffled() else list
                                        viewModel.playTracks(final, 0, pl.name, QueueOriginType.PLAYLIST, pl.id.toString())
                                    },
                                    onRemoveTrackFromPlaylist = { pid, tid -> viewModel.removeTrackFromPlaylist(pid, tid) },
                                    onReorderPlaylist = { pid, ids -> viewModel.reorderPlaylist(pid, ids) },
                                    onAddTracksToPlaylist = { pid, ids -> viewModel.addTracksToPlaylists(listOf(pid), ids) },
                                    onExportM3u = { pid, uri -> viewModel.exportPlaylistM3u(pid, uri) },
                                    onImportM3u = { uri, name -> viewModel.importPlaylistM3u(uri, name) },
                                    onDuplicatePlaylist = { id -> viewModel.duplicatePlaylist(id) },
                                    onClearPlaylist = { id -> viewModel.clearPlaylist(id) },
                                    onToggleFavorite = { id -> viewModel.togglePlaylistFavorite(id) },
                                    onUpdateCover = { id, uri -> viewModel.updatePlaylistCover(id, uri) },
                                    onRemoveTracksFromPlaylist = { pid, ids -> viewModel.removeTracksFromPlaylist(pid, ids) },
                                    onAddTracksToPlaylistsWithResult = { pids, ids, allowDup ->
                                        viewModel.addTracksToPlaylistsWithResult(pids, ids, allowDup)
                                    },
                                    onTrackMoreClick = { contextMenuTrack = it },
                                )
                            }

                            composable<MyatoRoute.Queues> {
                                QueuesScreen(
                                    queues = queues,
                                    allTracks = tracks,
                                    playbackState = playbackState,
                                    onBack = { navController.popBackStack() },
                                    onSwitchQueue = { qid -> viewModel.playbackController.switchToQueue(qid) },
                                    onCreateQueue = { name, ids -> viewModel.createQueue(name, ids) },
                                    onRenameQueue = { id, name -> viewModel.renameQueue(id, name) },
                                    onDuplicateQueue = { id -> viewModel.duplicateQueue(id) },
                                    onDeleteQueue = { id -> viewModel.deleteQueue(id) },
                                    onMergeQueues = { src, dst -> viewModel.mergeQueues(src, dst) },
                                    onReplaceQueue = { qid, ids -> viewModel.replaceQueueWithTracks(qid, ids) },
                                    onMoveActiveTrack = { from, to -> viewModel.playbackController.moveTrackInActiveQueue(from, to) },
                                    onRemoveActiveTrack = { idx -> viewModel.playbackController.removeTrackFromActiveQueue(idx) },
                                    onPlayTrackAtIndex = { idx -> viewModel.playTrackAtIndexInQueue(idx) },
                                    onClearActiveQueue = { viewModel.clearActiveQueue() },
                                    onSaveQueueAsPlaylist = { name, ids -> viewModel.saveQueueAsPlaylist(name, ids) },
                                )
                            }

                            composable<MyatoRoute.FullPlayer> {
                                FullPlayerScreen(
                                    playbackState = playbackState,
                                    spectrumBars = spectrumBars,
                                    visualizerEnabled = preferences.visualizerEnabled,
                                    bookmarks = currentBookmarks,
                                    note = currentNote,
                                    allTracks = tracks,
                                    statsSummary = statsSummary,
                                    visualizerStyle = preferences.visualizerStyle,
                                    animationIntensity = preferences.visualizerAnimationIntensity,
                                    particlesEnabled = preferences.playerParticlesEnabled,
                                    ambientBgEnabled = preferences.playerAmbientBackgroundEnabled,
                                    showStatsEnabled = preferences.playerShowStats,
                                    queueVisibleCount = preferences.playerQueueVisibleCount,
                                    animatedTransitions = preferences.playerAnimatedTransitions,
                                    hasRealFftSignal = hasRealFftSignal,
                                    playerVisualStyle = preferences.playerVisualStyle,
                                    animationsDisabled = preferences.animationsDisabled,
                                    reduceMotion = preferences.reduceMotion,
                                    reduceEffectsOnLowBattery = preferences.reduceEffectsOnLowBattery,
                                    onClose = { navController.popBackStack() },
                                    onPlayPause = { viewModel.playbackController.togglePlayPause() },
                                    onNext = { viewModel.playbackController.skipNext() },
                                    onPrevious = { viewModel.playbackController.skipPrevious() },
                                    onSeekTo = { viewModel.playbackController.seekTo(it) },
                                    onSeekForward10s = { viewModel.playbackController.seekForward10s() },
                                    onSeekBack10s = { viewModel.playbackController.seekBackward10s() },
                                    onToggleShuffle = { viewModel.playbackController.toggleShuffle() },
                                    onCycleRepeat = { viewModel.playbackController.cycleRepeatMode() },
                                    onToggleFavorite = { viewModel.playbackController.toggleCurrentTrackFavorite() },
                                    onVolumeChange = { viewModel.playbackController.setVolumeFraction(it) },
                                    onOpenQueue = { navController.navigate(MyatoRoute.Queues) },
                                    onOpenLyrics = { tid -> navController.navigate(MyatoRoute.Lyrics(tid)) },
                                    onOpenEqualizer = { navController.navigate(MyatoRoute.Equalizer) },
                                    onOpenSleepTimer = { navController.navigate(MyatoRoute.SleepTimer) },
                                    onOpenTagEditor = { tid -> navController.navigate(MyatoRoute.TagEditor(tid.toString())) },
                                    onOpenStats = { navController.navigate(MyatoRoute.Statistics) },
                                    onPlayQueueIndex = { idx -> viewModel.playTrackAtIndexInQueue(idx) },
                                    onRemoveFromQueue = { idx -> viewModel.playbackController.removeTrackFromActiveQueue(idx) },
                                    onMoveQueueTrack = { from, to -> viewModel.playbackController.moveTrackInActiveQueue(from, to) },
                                    onClearQueue = { viewModel.clearActiveQueue() },
                                    onAddBookmark = { tid, label, pos -> viewModel.addBookmark(tid, label, pos) },
                                    onDeleteBookmark = { id -> viewModel.deleteBookmark(id) },
                                    onSaveNote = { tid, content -> viewModel.saveNote(tid, content) },
                                    onDeleteNote = { tid -> viewModel.deleteNote(tid) },
                                    onSetVisualizerEnabled = { viewModel.setVisualizerEnabled(it) },
                                    onSetVisualizerStyle = { viewModel.setVisualizerStyle(it) },
                                    onSetAnimationIntensity = { viewModel.setVisualizerAnimationIntensity(it) },
                                    onSetParticlesEnabled = { viewModel.setPlayerParticlesEnabled(it) },
                                    onSetAmbientBgEnabled = { viewModel.setPlayerAmbientBackgroundEnabled(it) },
                                    onSetShowStatsEnabled = { viewModel.setPlayerShowStats(it) },
                                    onSetQueueCount = { viewModel.setPlayerQueueVisibleCount(it) },
                                    onSetAnimatedTransitions = { viewModel.setPlayerAnimatedTransitions(it) },
                                    onSetPlayerVisualStyle = { viewModel.setPlayerVisualStyle(it) },
                                    onSetAnimationsDisabled = { viewModel.setAnimationsDisabled(it) },
                                    onSetReduceMotion = { viewModel.setReduceMotion(it) },
                                    onSetReduceEffectsOnLowBattery = { viewModel.setReduceEffectsOnLowBattery(it) },
                                )
                            }

                            composable<MyatoRoute.Lyrics> { entry ->
                                val route = entry.toRoute<MyatoRoute.Lyrics>()
                                val targetTrack = tracks.firstOrNull { it.id == route.trackId } ?: playbackState.currentTrack
                                LyricsScreen(
                                    track = targetTrack,
                                    currentPositionMs = playbackState.positionMs,
                                    onBack = { navController.popBackStack() },
                                    onSeekTo = { viewModel.playbackController.seekTo(it) },
                                    loadLyrics = { t -> viewModel.loadTrackLyrics(t) },
                                    onSaveLyrics = { t, content -> viewModel.saveTrackLyrics(t, content) },
                                )
                            }

                            composable<MyatoRoute.TagEditor> { entry ->
                                val route = entry.toRoute<MyatoRoute.TagEditor>()
                                val ids = route.trackIdsCsv.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
                                val targets = tracks.filter { it.id in ids }
                                TagEditorScreen(
                                    targetTracks = targets,
                                    onBack = { navController.popBackStack() },
                                    inspectTags = { t -> viewModel.inspectTags(t) },
                                    onSaveTags = { list, payload, isBatch -> viewModel.saveTagsBatch(list, payload, isBatch) },
                                )
                            }

                            composable<MyatoRoute.FileManager> {
                                FileManagerScreen(
                                    initialPath = viewModel.defaultRootDir(),
                                    onBack = { navController.popBackStack() },
                                    listDirectory = { path -> viewModel.listDirectory(path) },
                                    listLinearAudio = { viewModel.listAllAudioLinear() },
                                    onCreateFolder = { parent, name, done -> viewModel.createFolder(parent, name, done) },
                                    onRenameNode = { path, name, done -> viewModel.renameFileOrFolder(path, name, done) },
                                    onCopyFiles = { paths, dst, done -> viewModel.copyFiles(paths, dst, done) },
                                    onMoveFiles = { paths, dst, done -> viewModel.moveFiles(paths, dst, done) },
                                    onDeleteFiles = { paths, done -> viewModel.deleteFiles(paths, done) },
                                    onRefreshLibrary = { viewModel.refreshLibrary() },
                                )
                            }

                            composable<MyatoRoute.Equalizer> {
                                val eqEngine = viewModel.playbackController.equalizerEngine
                                EqualizerScreen(
                                    eqState = eqState,
                                    savedPresets = eqPresets,
                                    builtInPresetNames = eqEngine.builtInPresets.keys.toList(),
                                    visualizerEnabled = preferences.visualizerEnabled,
                                    onBack = { navController.popBackStack() },
                                    onToggleEnabled = { eqEngine.setEnabled(it) },
                                    onSelectOutputDevice = { eqEngine.selectOutputDeviceProfile(it) },
                                    onBandChange = { idx, level -> eqEngine.setBandLevel(idx, level) },
                                    onBassBoostChange = { eqEngine.setBassBoost(it) },
                                    onVirtualizerChange = { eqEngine.setVirtualizer(it) },
                                    onLoudnessChange = { eqEngine.setLoudnessGain(it) },
                                    onTrebleChange = { eqEngine.setTrebleBoost(it) },
                                    onApplyBuiltInPreset = { eqEngine.applyBuiltInPreset(it) },
                                    onApplySavedPreset = { eqEngine.applySavedPreset(it) },
                                    onSaveCustomPreset = { eqEngine.saveCustomPreset(it) },
                                    onResetDefaults = { eqEngine.resetDefaults() },
                                    onToggleVisualizer = { viewModel.setVisualizerEnabled(it) },
                                )
                            }

                            composable<MyatoRoute.SleepTimer> {
                                val timer = viewModel.playbackController.sleepTimerManager
                                SleepTimerScreen(
                                    timerState = sleepTimerState,
                                    onBack = { navController.popBackStack() },
                                    onStartMinutes = { timer.startDurationTimer(it) },
                                    onStartClockTime = { h, m -> timer.startUntilClockTime(h, m) },
                                    onStartTrackCount = { timer.startTrackCountTimer(it) },
                                    onPauseResume = { timer.pauseOrResume() },
                                    onCancel = { timer.cancel() },
                                )
                            }

                            composable<MyatoRoute.Statistics> {
                                StatisticsScreen(
                                    summary = statsSummary,
                                    onBack = { navController.popBackStack() },
                                    onExportCsv = { uri -> viewModel.exportTopTracksCsv(uri) },
                                )
                            }

                            composable<MyatoRoute.ThemeCenter> {
                                ThemeCenterScreen(
                                    themes = allThemes,
                                    activeThemeId = activeTheme.id,
                                    onSelectTheme = { viewModel.selectTheme(it) },
                                    onOpenEditor = { tid -> navController.navigate(MyatoRoute.ThemeEditor(tid)) },
                                    onDuplicateTheme = { t, name -> viewModel.duplicateTheme(t, name) },
                                    onDeleteCustomTheme = { viewModel.deleteCustomTheme(it) },
                                    onImportThemeJson = { raw -> viewModel.importThemeJson(raw) },
                                    activeFontName = activeFontSummary,
                                    onOpenFontSettings = { navController.navigate(MyatoRoute.FontSettings) },
                                )
                            }

                            composable<MyatoRoute.FontSettings> {
                                FontSettingsScreen(
                                    activeGeneralFontId = preferences.activeGeneralFontId,
                                    activeDecorativeFontId = preferences.activeDecorativeFontId,
                                    favoriteFontIds = preferences.favoriteFontIds,
                                    recentFontIds = preferences.recentFontIds,
                                    customFonts = preferences.customFonts,
                                    onBack = { navController.popBackStack() },
                                    onOpenPreview = { fid -> navController.navigate(MyatoRoute.FontPreview(fid)) },
                                    onApplyFont = { fid, target -> viewModel.applyAppFont(fid, target) },
                                    onToggleFavorite = { fid -> viewModel.toggleFavoriteFont(fid) },
                                    onRestoreDefault = { viewModel.resetDefaultAppFont() },
                                    onImportCustomFontUri = { uri ->
                                        viewModel.importCustomFont(uri) { imported ->
                                            navController.navigate(MyatoRoute.FontPreview(imported.id))
                                        }
                                    },
                                    onDeleteCustomFont = { font -> viewModel.deleteCustomFont(font) },
                                )
                            }

                            composable<MyatoRoute.FontPreview> { entry ->
                                val route = entry.toRoute<MyatoRoute.FontPreview>()
                                val fontOption = remember(route.fontId, preferences.customFonts) {
                                    AppFontCatalog.findFontOption(route.fontId, preferences.customFonts)
                                }
                                FontPreviewScreen(
                                    fontOption = fontOption,
                                    customFonts = preferences.customFonts,
                                    activeGeneralFontId = preferences.activeGeneralFontId,
                                    activeDecorativeFontId = preferences.activeDecorativeFontId,
                                    isFavorite = fontOption.id in preferences.favoriteFontIds,
                                    onToggleFavorite = { viewModel.toggleFavoriteFont(fontOption.id) },
                                    onApplyFont = { target -> viewModel.applyAppFont(fontOption.id, target) },
                                    onDeleteCustomFont = if (fontOption.isCustom) {
                                        { viewModel.deleteCustomFont(fontOption) }
                                    } else null,
                                    onBack = { navController.popBackStack() },
                                )
                            }

                            composable<MyatoRoute.ThemeEditor> { entry ->
                                val route = entry.toRoute<MyatoRoute.ThemeEditor>()
                                val source = allThemes.firstOrNull { it.id == route.sourceThemeId } ?: BuiltInThemes.MidnightOrange
                                ThemeEditorScreen(
                                    sourceTheme = source,
                                    onBack = { navController.popBackStack() },
                                    onSaveTheme = { viewModel.saveCustomTheme(it) },
                                    onGenerateFromCover = { cb -> viewModel.generateThemeFromCurrentCover(cb) },
                                    onExportThemeJson = { t -> viewModel.exportThemeJson(t) },
                                    onNotify = { msg -> viewModel.showStatus(msg) },
                                )
                            }

                            composable<MyatoRoute.WidgetsShortcuts> {
                                WidgetsShortcutsScreen(
                                    albums = albums,
                                    artists = artists,
                                    folders = folders,
                                    playlists = playlists,
                                    onBack = { navController.popBackStack() },
                                    onPinShortcut = { type, key, label -> viewModel.pinShortcut(type, key, label) },
                                )
                            }

                            composable<MyatoRoute.Backup> {
                                BackupScreen(
                                    preferences = preferences,
                                    backupHistory = backupHistory,
                                    previewedBackup = previewedBackup?.first,
                                    onBack = { navController.popBackStack() },
                                    onExportManual = { uri -> viewModel.exportManualBackup(uri) },
                                    onSelectBackupToPreview = { uri -> viewModel.loadBackupPreview(uri) },
                                    onConfirmRestore = { preserve -> viewModel.confirmRestoreBackup(preserve) },
                                    onDismissPreview = { viewModel.dismissBackupPreview() },
                                    onConfigureAutoBackup = { days, uri -> viewModel.configureAutoBackup(days, uri) },
                                )
                            }

                            composable<MyatoRoute.Settings> {
                                SettingsScreen(
                                    preferences = preferences,
                                    capabilities = viewModel.audioCapabilities,
                                    onSetGapless = { viewModel.setGapless(it) },
                                    onSetBatterySaverAnim = { viewModel.setBatterySaverAnimations(it) },
                                    onSetNotificationPrefs = { compact, plBtn -> viewModel.setNotificationPreferences(compact, plBtn) },
                                    onSetHeadsetMappings = { c1, c2, c3, c4 -> viewModel.setHeadsetMappings(c1, c2, c3, c4) },
                                    onOpenPermissions = { navController.navigate(MyatoRoute.Permissions) },
                                    onOpenBackup = { navController.navigate(MyatoRoute.Backup) },
                                    onOpenWidgets = { navController.navigate(MyatoRoute.WidgetsShortcuts) },
                                    onOpenAbout = { navController.navigate(MyatoRoute.About) },
                                    activeFontName = activeFontSummary,
                                    onOpenFontSettings = { navController.navigate(MyatoRoute.FontSettings) },
                                    onSetPlayerVisualStyle = { viewModel.setPlayerVisualStyle(it) },
                                    onSetEffectIntensity = { viewModel.setVisualizerAnimationIntensity(it) },
                                    onSetAnimationsDisabled = { viewModel.setAnimationsDisabled(it) },
                                    onSetReduceMotion = { viewModel.setReduceMotion(it) },
                                    onSetReduceEffectsOnLowBattery = { viewModel.setReduceEffectsOnLowBattery(it) },
                                )
                            }

                            composable<MyatoRoute.About> {
                                AboutScreen(onBack = { navController.popBackStack() })
                            }
                        }

                        // Synchronized Slanted Stripes transition overlay on tab/section switches
                        val isSlantedTransitionEligible = motionSettings.shouldAnimateTabTransitions &&
                            currentDestination?.hasRoute<MyatoRoute.Splash>() != true &&
                            currentDestination?.hasRoute<MyatoRoute.Onboarding>() != true
                        SlantedStripesTransitionOverlay(
                            routeKey = currentRouteKey,
                            enabled = isSlantedTransitionEligible,
                            intensity = motionSettings.effectiveIntensity,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }

        contextMenuTrack?.let { track ->
            TrackContextMenuSheet(
                track = track,
                isInPlaylist = false,
                onDismiss = { contextMenuTrack = null },
                onPlayNow = {
                    viewModel.playTracks(listOf(track), 0, track.title, QueueOriginType.CUSTOM)
                },
                onPlayNext = {
                    viewModel.addTrackNextInActiveQueue(track)
                },
                onAddToQueue = {
                    viewModel.addTracksToActiveQueue(listOf(track))
                },
                onAddToPlaylists = {
                    playlistDialogTrackIds = listOf(track.id)
                },
                onCreatePlaylistWithTrack = {
                    createPlaylistWithTracksTarget = listOf(track.id)
                },
                onPlayFromHere = {
                    val idx = tracks.indexOfFirst { it.id == track.id }
                    if (idx != -1) {
                        viewModel.playTracks(tracks.drop(idx) + tracks.take(idx), 0, "Biblioteca", QueueOriginType.ALL_SONGS)
                    } else {
                        viewModel.playTracks(listOf(track), 0, track.title, QueueOriginType.CUSTOM)
                    }
                },
                onToggleFavorite = {
                    viewModel.toggleFavorite(track.id)
                },
                onShareTrack = {
                    TrackShareAndDeleteHelper.shareTracks(context, listOf(track))
                },
                onEditTags = {
                    navController.navigate(MyatoRoute.TagEditor(track.id.toString()))
                },
                onViewDetails = {
                    viewDetailsTrack = track
                },
                onOpenLocation = {
                    navController.navigate(MyatoRoute.FileManager)
                },
                onDeleteTrack = {
                    deleteConfirmTracks = listOf(track)
                },
            )
        }

        playlistDialogTrackIds?.let { targetIds ->
            AddToPlaylistsDialog(
                playlists = playlists,
                targetTrackIds = targetIds,
                tracks = tracks,
                onDismiss = { playlistDialogTrackIds = null },
                onCreatePlaylist = { name, ids ->
                    viewModel.createPlaylistWithTracks(name, ids)
                },
                onConfirm = { chosenPlaylists, allowDuplicates ->
                    viewModel.addTracksToPlaylistsWithResult(chosenPlaylists, targetIds, allowDuplicates)
                },
            )
        }

        viewDetailsTrack?.let { track ->
            TrackDetailsDialog(
                track = track,
                onDismiss = { viewDetailsTrack = null },
            )
        }

        deleteConfirmTracks?.let { targetTracks ->
            DeleteTracksConfirmDialog(
                tracks = targetTracks,
                onDismiss = { deleteConfirmTracks = null },
                onConfirm = {
                    viewModel.deleteTracksPermanently(targetTracks)
                },
            )
        }

            createPlaylistWithTracksTarget?.let { targetIds ->
                CreatePlaylistDialog(
                    initialName = "",
                    trackCount = targetIds.size,
                    onDismiss = { createPlaylistWithTracksTarget = null },
                    onConfirm = { name ->
                        viewModel.createPlaylistWithTracks(name, targetIds)
                    },
                )
            }
        }
    }
}
