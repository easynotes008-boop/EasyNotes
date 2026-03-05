package uk.ac.tees.mad.easynotes.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Auth : Screen("auth")
    object Home : Screen("home")
    object Settings : Screen("settings")

    object Notes : Screen("notes/{subjectId}") {
        const val SUBJECT_ID_ARG = "subjectId"
        fun createRoute(subjectId: String) = "notes/$subjectId"
    }
}