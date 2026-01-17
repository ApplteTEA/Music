package com.test.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.test.presentation.screen.detail.DetailScreen
import com.test.presentation.screen.detail.DetailViewModel
import com.test.presentation.screen.list.ListScreen
import com.test.presentation.util.rememberNavGuard

@Composable
fun AppNavHost(navController: NavHostController) {
    val navigate = rememberNavGuard(navController)

    NavHost(
        navController = navController,
        startDestination = Routes.LIST
    ) {
        composable(Routes.LIST) {
            ListScreen(
                onOpenDetail = { trackId ->
                    navigate(Routes.detail(trackId))
                }
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("trackId") { type = NavType.LongType })
        ) {
            val viewModel: DetailViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            DetailScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onEvent = viewModel::onEvent
            )
        }
    }
}
