package ru.ytkab0bp.beamklipper.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.ytkab0bp.beamklipper.R
import ru.ytkab0bp.beamklipper.ui.screens.HelpScreen
import ru.ytkab0bp.beamklipper.ui.screens.HomeScreen
import ru.ytkab0bp.beamklipper.ui.screens.SettingsScreen
import ru.ytkab0bp.beamklipper.ui.state.MainViewModel

const val PAGE_SETTINGS = 0
const val PAGE_MAIN = 1
const val PAGE_HELP = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagerHost(
    isCurrentLauncher: Boolean,
    mainViewModel: MainViewModel
) {
    val pagerState = rememberPagerState(initialPage = PAGE_MAIN) { 3 }
    val scope = rememberCoroutineScope()
    val navBarInsets = WindowInsets.navigationBars.asPaddingValues()
    val currentPage by remember {
        derivedStateOf { pagerState.currentPage }
    }

    BackHandler(enabled = currentPage != PAGE_MAIN) {
        scope.launch { pagerState.animateScrollToPage(PAGE_MAIN) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.AppName),
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                navigationIcon = {
                    if (currentPage != PAGE_SETTINGS) {
                        IconButton(onClick = { scope.launch { pagerState.animateScrollToPage(PAGE_SETTINGS) } }) {
                            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.Settings))
                        }
                    } else {
                        Spacer(Modifier.size(48.dp))
                    }
                },
                actions = {
                    if (currentPage != PAGE_HELP) {
                        IconButton(onClick = { scope.launch { pagerState.animateScrollToPage(PAGE_HELP) } }) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = stringResource(R.string.HelpTitle))
                        }
                    } else {
                        Spacer(Modifier.size(48.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            pageSize = PageSize.Fill,
            flingBehavior = PagerDefaults.flingBehavior(state = pagerState)
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = navBarInsets.calculateBottomPadding()),
                contentAlignment = Alignment.TopStart
            ) {
                when (page) {
                    PAGE_SETTINGS -> SettingsScreen()
                    PAGE_HELP -> HelpScreen()
                    else -> HomeScreen(
                        isCurrentLauncher = isCurrentLauncher,
                        mainViewModel = mainViewModel
                    )
                }
            }
        }
    }
}
