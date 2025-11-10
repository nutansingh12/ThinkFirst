package com.thinkfirst.android.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.thinkfirst.android.presentation.auth.AuthViewModel
import com.thinkfirst.android.presentation.auth.ChildLoginScreen
import com.thinkfirst.android.presentation.auth.LoginModeScreen
import com.thinkfirst.android.presentation.auth.LoginScreen
import com.thinkfirst.android.presentation.auth.RegisterScreen
import com.thinkfirst.android.presentation.chat.ChatScreen
import com.thinkfirst.android.presentation.children.ChildManagementScreen
import com.thinkfirst.android.presentation.dashboard.DashboardScreen
import com.thinkfirst.android.presentation.quiz.QuizScreen

/**
 * Navigation routes for the app
 */
sealed class Screen(val route: String) {
    object LoginMode : Screen("login_mode")
    object ParentLogin : Screen("parent_login")
    object ChildLogin : Screen("child_login")
    object Register : Screen("register")
    object ChildManagement : Screen("child_management")
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
    startDestination: String = Screen.LoginMode.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Login Mode Selection
        composable(Screen.LoginMode.route) {
            LoginModeScreen(
                onParentLoginClick = {
                    navController.navigate(Screen.ParentLogin.route)
                },
                onChildLoginClick = {
                    navController.navigate(Screen.ChildLogin.route)
                }
            )
        }

        // Parent Login
        composable(Screen.ParentLogin.route) {
            LoginScreen(
                onLoginSuccess = { _ ->
                    navController.navigate(Screen.ChildManagement.route) {
                        popUpTo(Screen.LoginMode.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        // Child Login
        composable(Screen.ChildLogin.route) {
            ChildLoginScreen(
                onLoginSuccess = { childId ->
                    navController.navigate(Screen.Chat.createRoute(childId)) {
                        popUpTo(Screen.LoginMode.route) { inclusive = true }
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Register
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = { _ ->
                    navController.navigate(Screen.ChildManagement.route) {
                        popUpTo(Screen.LoginMode.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // Child Management (for parents)
        composable(Screen.ChildManagement.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            ChildManagementScreen(
                onChildSelected = { childId ->
                    navController.navigate(Screen.Dashboard.createRoute(childId))
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.LoginMode.route) {
                        popUpTo(0) { inclusive = true }
                    }
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
                },
                onLogout = {
                    navController.navigate(Screen.LoginMode.route) {
                        popUpTo(0) { inclusive = true }
                    }
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

