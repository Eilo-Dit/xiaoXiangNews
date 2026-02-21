package com.xxcb.news

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
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
import java.net.URLDecoder
import java.net.URLEncoder

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
                    val encodedUrl = URLEncoder.encode(page.pdfUrl, "UTF-8")
                    val encodedTitle = URLEncoder.encode(page.edition, "UTF-8")
                    navController.navigate("pdf/$encodedUrl/$encodedTitle")
                }
            )
        }
        composable(
            route = "pdf/{pdfUrl}/{title}",
            arguments = listOf(
                navArgument("pdfUrl") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val pdfUrl = URLDecoder.decode(
                backStackEntry.arguments?.getString("pdfUrl") ?: "", "UTF-8"
            )
            val title = URLDecoder.decode(
                backStackEntry.arguments?.getString("title") ?: "", "UTF-8"
            )
            PdfViewerScreen(
                pdfUrl = pdfUrl,
                title = title,
                onBack = { navController.popBackStack() }
            )
        }
    }
}