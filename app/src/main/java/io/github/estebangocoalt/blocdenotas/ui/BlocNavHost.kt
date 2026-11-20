package io.github.estebangocoalt.blocdenotas.ui

import io.github.estebangocoalt.blocdenotas.ui.trash.TrashScreen
import io.github.estebangocoalt.blocdenotas.ui.shopping.ShoppingModeScreen
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
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
import io.github.estebangocoalt.blocdenotas.ui.dashboard.DashboardScreen
import io.github.estebangocoalt.blocdenotas.ui.editor.EditorScreen
import io.github.estebangocoalt.blocdenotas.ui.home.HomeScreen
import io.github.estebangocoalt.blocdenotas.ui.inbox.InboxScreen
import io.github.estebangocoalt.blocdenotas.ui.settings.SettingsScreen
import io.github.estebangocoalt.blocdenotas.ui.tasks.TasksScreen

private object Routes {
    const val DASHBOARD = "dashboard"
    const val NOTES = "notes"
    const val TASKS = "tasks"
    const val BOARD = "board"
    const val CALENDAR = "calendar"
    const val INBOX = "inbox"
    const val SETTINGS = "settings"
    const val TRASH = "trash"
    const val EDITOR_PATTERN = "editor?noteId={noteId}&kind={kind}&due={due}&template={template}&parent={parent}"
    const val SHOPPING_PATTERN = "shopping/{noteId}"

    fun shopping(noteId: String): String = "shopping/$noteId"

    fun editor(
        noteId: String? = null,
        kind: NoteKind = NoteKind.NOTE,
        dueAt: Long? = null,
        template: String? = null,
        parentId: String? = null,
    ): String = buildString {
        append("editor?kind=").append(kind.name)
        if (noteId != null) append("&noteId=").append(noteId)
        if (dueAt != null) append("&due=").append(dueAt)
        if (template != null) append("&template=").append(template)
        if (parentId != null) append("&parent=").append(parentId)
    }
}

private class TopLevelDestination(
    val route: String,
    @StringRes val label: Int,
    val icon: ImageVector,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.DASHBOARD, R.string.nav_dashboard, Icons.Default.Home),
    TopLevelDestination(Routes.NOTES, R.string.nav_notes, Icons.Default.Create),
    TopLevelDestination(Routes.TASKS, R.string.nav_tasks, Icons.Default.CheckCircle),
    TopLevelDestination(Routes.BOARD, R.string.nav_board, Icons.AutoMirrored.Filled.List),
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
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onOpenNotes = { navController.goToTab(Routes.NOTES) },
                    onOpenTasks = { navController.goToTab(Routes.TASKS) },
                    onOpenBoard = { navController.goToTab(Routes.BOARD) },
                    onOpenCalendar = { navController.goToTab(Routes.CALENDAR) },
                    onOpenNote = { id -> navController.navigate(Routes.editor(id)) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.NOTES) {
                HomeScreen(
                    onNewNote = { navController.navigate(Routes.editor()) },
                    onNewFromTemplate = { t -> navController.navigate(Routes.editor(kind = t.kind, template = t.name)) },
                    onNewFromCustom = { t ->
                        navController.navigate(
                            Routes.editor(
                                kind = t.noteKind,
                                template = io.github.estebangocoalt.blocdenotas.data.CustomTemplateRef.of(t.id),
                            ),
                        )
                    },
                    onOpenNote = { id -> navController.navigate(Routes.editor(id)) },
                    onOpenInbox = { navController.navigate(Routes.INBOX) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.TASKS) {
                TasksScreen(
                    onOpenTask = { id -> navController.navigate(Routes.editor(id)) },
                    onNewTask = { navController.navigate(Routes.editor(kind = NoteKind.TASK)) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.BOARD) {
                BoardScreen(
                    onOpenCard = { id -> navController.navigate(Routes.editor(id)) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.CALENDAR) {
                CalendarScreen(
                    onOpenTask = { id -> navController.navigate(Routes.editor(id)) },
                    onNewTask = { dueAt -> navController.navigate(Routes.editor(kind = NoteKind.TASK, dueAt = dueAt)) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.INBOX) { InboxScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenTrash = { navController.navigate(Routes.TRASH) },
                )
            }
            composable(Routes.TRASH) { TrashScreen(onBack = { navController.popBackStack() }) }
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
                    navArgument("template") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("parent") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                EditorScreen(
                    onBack = { navController.popBackStack() },
                    onShoppingMode = { id -> navController.navigate(Routes.shopping(id)) },
                    onOpenNote = { id -> navController.navigate(Routes.editor(id)) },
                    onNewSubpage = { parent -> navController.navigate(Routes.editor(parentId = parent)) },
                )
            }
            composable(
                route = Routes.SHOPPING_PATTERN,
                arguments = listOf(navArgument("noteId") { type = NavType.StringType }),
            ) {
                ShoppingModeScreen(onBack = { navController.popBackStack() })
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

/** Cambia de pestaña conservando el estado de cada una. */
private fun NavHostController.goToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun BottomBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar {
        topLevelDestinations.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { navController.goToTab(destination.route) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(stringResource(destination.label)) },
            )
        }
    }
}
