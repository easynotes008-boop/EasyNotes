package uk.ac.tees.mad.easynotes.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import uk.ac.tees.mad.easynotes.presentation.screens.auth.AuthScreen
import uk.ac.tees.mad.easynotes.presentation.screens.auth.AuthViewModel

import uk.ac.tees.mad.easynotes.presentation.screens.splash.SplashScreen
import uk.ac.tees.mad.easynotes.presentation.screens.splash.SplashViewModel

@Composable
fun EasyNotesNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) {
            val viewModel: SplashViewModel = viewModel()
            SplashScreen(
                viewModel = viewModel,
                onNavigateToAuth = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Auth.route) {
            val viewModel: AuthViewModel = viewModel()
            AuthScreen(
                viewModel = viewModel,
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }
//
//        composable(Screen.Home.route) {
//            val viewModel: HomeViewModel = viewModel()
//            HomeScreen(
//                viewModel = viewModel,
//                onNavigateToNotes = { subjectId ->
//                    navController.navigate(Screen.Notes.createRoute(subjectId))
//                },
//                onNavigateToSettings = {
//                    navController.navigate(Screen.Settings.route)
//                }
//            )
//        }
//
//        composable(
//            route = Screen.Notes.route,
//            arguments = listOf(
//                navArgument(Screen.Notes.SUBJECT_ID_ARG) {
//                    type = NavType.StringType
//                }
//            )
//        ) { backStackEntry ->
//            val subjectId = backStackEntry.arguments?.getString(Screen.Notes.SUBJECT_ID_ARG)
//            requireNotNull(subjectId)
//
//            val viewModel: NotesViewModel = viewModel()
//            NotesScreen(
//                viewModel = viewModel,
//                subjectId = subjectId,
//                onNavigateBack = {
//                    navController.popBackStack()
//                }
//            )
//        }
//
//        composable(Screen.Settings.route) {
//            val viewModel: SettingsViewModel = viewModel()
//            SettingsScreen(
//                viewModel = viewModel,
//                onNavigateBack = {
//                    navController.popBackStack()
//                },
//                onLogout = {
//                    navController.navigate(Screen.Auth.route) {
//                        popUpTo(0) { inclusive = true }
//                    }
//                }
//            )
//        }
    }
}