package ru.ytkab0bp.beamklipper.ui

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.ytkab0bp.beamklipper.PermissionsChecker
import ru.ytkab0bp.beamklipper.ui.screens.PermissionScreen
import ru.ytkab0bp.beamklipper.ui.state.MainViewModel

@Composable
fun CreamApp(
    isCurrentLauncher: Boolean,
    mainViewModel: MainViewModel = viewModel()
) {
    var permissionBlocked by rememberSaveable { mutableStateOf(PermissionsChecker.needBlockStart()) }

    Crossfade(targetState = permissionBlocked, label = "gate") { blocked ->
        if (blocked) {
            PermissionScreen(
                onNext = {
                    permissionBlocked = false
                }
            )
        } else {
            NavHost(
                isCurrentLauncher = isCurrentLauncher,
                mainViewModel = mainViewModel
            )
        }
    }
}
