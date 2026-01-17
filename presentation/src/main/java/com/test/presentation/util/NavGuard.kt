package com.test.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import androidx.navigation.NavHostController

@Composable
fun rememberNavGuard(navController: NavHostController): (String) -> Unit {
    var navigating by remember { mutableStateOf(false) }

    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, _, _ ->
            navigating = false
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }

    return remember(navController) {
        { route: String ->
            if (navigating) return@remember
            navigating = true
            navController.navigate(route) {
                launchSingleTop = true
            }
        }
    }
}