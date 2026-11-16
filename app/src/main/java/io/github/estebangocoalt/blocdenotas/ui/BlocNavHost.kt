package io.github.estebangocoalt.blocdenotas.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.data.NoteKind
import io.github.estebangocoalt.blocdenotas.ui.board.BoardScreen
import io.github.estebangocoalt.blocdenotas.ui.calendar.CalendarScreen
import io.github.estebangocoalt.blocdenotas.ui.editor.EditorScreen
import io.github.estebangocoalt.blocdenotas.ui.home.HomeScreen
import io.github.estebangocoalt.blocdenotas.ui.inbox.InboxScreen
import io.github.estebangocoalt.blocdenotas.ui.settings.SettingsScreen
import io.github.estebangocoalt.blocdenotas.ui.tasks.TasksScreen

private object Routes {
    const val NOTES = "notes"
    const val TASKS = "tasks"
    const val BOARD = "board"
    const val CALENDAR = "calendar"
    const val INBOX = "inbox"
    const val SETTINGS = "settings"
    const val EDITOR_PATTERN = "editor?noteId={noteId}&kind={kind}&due={due}"

    fun editor(noteId: String? = null, kind: NoteKind = NoteKind.NOTE, dueAt: Long? = null): String = buildString {
        append("editor?kind=").append(kind.name)
        if (noteId != null) append("&noteId=").append(noteId)
        if (dueAt != null) append("&due=").append(dueAt)
    }
}

private class TopLevelDestination(
    val route: String,
    @StringRes val label: Int,
    val icon: ImageVector,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.NOTES, R.string.nav_notes, Icons.Default.Create),
    TopLevelDestination(Routes.TASKS, R.string.nav_tasks, Icons.Default.CheckCircle),
    TopLevelDestination(Routes.BOARD, R.string.nav_board, Icons.Default.List),
    TopLevelDestination(Routes.CALENDAR, R.string.nav_calendar, Icons.Default.DateRange),
)

@Composable
fun BlocNavHost(
    openNoteId: String? = null,
    onOpenNoteHandled: () -> Unit = {},
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (topLevelDestinations.any { it.route == currentRoute }) {
                BottomBar(navController, currentRoute)
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.NOTES,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.NOTES) {
                HomeScreen(
                    onNewNote = { navController.navigate(Routes.editor()) },
                    onOpenNote = { id -> navController.navigate(Routes.editor(id)) },
                    onOpenInbox = { navController.navigate(Routes.INBOX) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.TASKS) {
                TasksScreen(
                    onOpenTask = { id -> navController.navigate(Routes.editor(id)) },
                    onNewTask = { navController.navigate(Routes.editor(kind = NoteKind.TASK)) },
                )
            }
            composable(Routes.BOARD) {
                BoardScreen(onOpenCard = { id -> navController.navigate(Routes.editor(id)) })
            }
            composable(Routes.CALENDAR) {
                CalendarScreen(
                    onOpenTask = { id -> navController.navigate(Routes.editor(id)) },
                    onNewTask = { dueAt -> navController.navigate(Routes.editor(kind = NoteKind.TASK, dueAt = dueAt)) },
                )
            }
            composable(Routes.INBOX) { InboxScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.SETTINGS) { SettingsScreen(onBack = { navController.popBackStack() }) }
            composable(
                route = Routes.EDITOR_PATTERN,
                arguments = listOf(
                    navArgument("noteId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("kind") {
                        type = NavType.StringType
                        defaultValue = NoteKind.NOTE.name
                    },
                    navArgument("due") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                EditorScreen(onBack = { navController.popBackStack() })
            }
        }

        // Al tocar un aviso: abre la tarea. Va después del NavHost para que el grafo ya exista.
        LaunchedEffect(openNoteId) {
            if (openNoteId != null) {
                navController.navigate(Routes.editor(openNoteId))
                onOpenNoteHandled()
            }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar {
        topLevelDestinations.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(stringResource(destination.label)) },
            )
        }
    }
}
