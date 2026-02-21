package com.xxcb.news

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xxcb.news.ui.screen.NewspaperScreen
import com.xxcb.news.ui.screen.PdfViewerScreen
import com.xxcb.news.ui.theme.XiaoXiangNewsTheme
import com.xxcb.news.ui.viewmodel.NewspaperViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            XiaoXiangNewsTheme {
                NewspaperApp()
            }
        }
    }
}

@Composable
fun NewspaperApp() {
    val navController = rememberNavController()
    val viewModel: NewspaperViewModel = viewModel()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            NewspaperScreen(
                viewModel = viewModel,
                onPageClick = { page ->
                    val uiState = viewModel.uiState.value
                    val index = uiState.pages.indexOf(page).coerceAtLeast(0)
                    navController.navigate("pdf/$index")
                }
            )
        }
        composable(
            route = "pdf/{pageIndex}",
            arguments = listOf(
                navArgument("pageIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val pageIndex = backStackEntry.arguments?.getInt("pageIndex") ?: 0
            val uiState by viewModel.uiState.collectAsState()
            PdfViewerScreen(
                pages = uiState.pages,
                initialPageIndex = pageIndex,
                onBack = { navController.popBackStack() }
            )
        }
    }
}