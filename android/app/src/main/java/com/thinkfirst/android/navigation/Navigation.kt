package com.thinkfirst.android.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.thinkfirst.android.presentation.auth.LoginScreen
import com.thinkfirst.android.presentation.auth.RegisterScreen
import com.thinkfirst.android.presentation.chat.ChatScreen
import com.thinkfirst.android.presentation.dashboard.DashboardScreen
import com.thinkfirst.android.presentation.quiz.QuizScreen

/**
 * Navigation routes for the app
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Chat : Screen("chat/{childId}") {
        fun createRoute(childId: Long) = "chat/$childId"
    }
    object Dashboard : Screen("dashboard/{childId}") {
        fun createRoute(childId: Long) = "dashboard/$childId"
    }
    object Quiz : Screen("quiz/{quizId}/{childId}") {
        fun createRoute(quizId: Long, childId: Long) = "quiz/$quizId/$childId"
    }
}

/**
 * Main navigation graph
 */
@Composable
fun ThinkFirstNavigation(
    navController: NavHostController,
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Authentication screens
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { childId ->
                    navController.navigate(Screen.Chat.createRoute(childId)) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }
        
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = { childId ->
                    navController.navigate(Screen.Chat.createRoute(childId)) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
        
        // Chat screen
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("childId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getLong("childId") ?: 1L
            ChatScreen(
                childId = childId,
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.createRoute(childId))
                }
            )
        }
        
        // Dashboard screen
        composable(
            route = Screen.Dashboard.route,
            arguments = listOf(
                navArgument("childId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getLong("childId") ?: 1L
            DashboardScreen(
                childId = childId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Quiz screen
        composable(
            route = Screen.Quiz.route,
            arguments = listOf(
                navArgument("quizId") { type = NavType.LongType },
                navArgument("childId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getLong("quizId") ?: 0L
            val childId = backStackEntry.arguments?.getLong("childId") ?: 1L
            QuizScreen(
                quizId = quizId,
                childId = childId,
                onQuizComplete = { score, passed ->
                    // Navigate back to chat with result
                    navController.popBackStack()
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

