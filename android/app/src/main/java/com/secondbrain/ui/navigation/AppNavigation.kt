package com.secondbrain.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NoteAlt
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.secondbrain.data.repository.NetworkResult
import com.secondbrain.ui.dashboard.DashboardScreen
import com.secondbrain.ui.notes.NoteDetailScreen
import com.secondbrain.ui.notes.NoteEditScreen
import com.secondbrain.ui.notes.NoteListScreen
import com.secondbrain.ui.people.PersonDetailScreen
import com.secondbrain.ui.people.PersonEditScreen
import com.secondbrain.ui.people.PersonListScreen
import com.secondbrain.ui.tasks.TaskDetailScreen
import com.secondbrain.ui.tasks.TaskEditScreen
import com.secondbrain.ui.tasks.TaskListScreen

private data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: Screen
)

private val bottomNavItems = listOf(
    BottomNavItem(
        label = "Dashboard",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        route = Screen.Dashboard
    ),
    BottomNavItem(
        label = "Notes",
        selectedIcon = Icons.Filled.NoteAlt,
        unselectedIcon = Icons.Outlined.NoteAlt,
        route = Screen.NoteList
    ),
    BottomNavItem(
        label = "Tasks",
        selectedIcon = Icons.Filled.CheckCircle,
        unselectedIcon = Icons.Outlined.CheckCircle,
        route = Screen.TaskList
    ),
    BottomNavItem(
        label = "People",
        selectedIcon = Icons.Filled.People,
        unselectedIcon = Icons.Outlined.People,
        route = Screen.PersonList
    )
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { item ->
        currentDestination?.hasRoute(item.route::class) == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hasRoute(item.route::class) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300))
            }
        ) {
            composable<Screen.Dashboard> {
                DashboardScreen(
                    onNavigateToNotes = { navController.navigate(Screen.NoteList) },
                    onNavigateToTasks = { navController.navigate(Screen.TaskList) },
                    onNavigateToPeople = { navController.navigate(Screen.PersonList) },
                    onNavigateToNoteDetail = { noteId ->
                        navController.navigate(Screen.NoteDetail(noteId))
                    },
                    onNavigateToTaskDetail = { taskId ->
                        navController.navigate(Screen.TaskDetail(taskId))
                    }
                )
            }

            composable<Screen.NoteList> {
                NoteListScreen(
                    onNoteClick = { noteId ->
                        navController.navigate(Screen.NoteDetail(noteId))
                    },
                    onAddNote = {
                        navController.navigate(Screen.NoteEdit())
                    }
                )
            }

            composable<Screen.NoteDetail> { backStackEntry ->
                val screen: Screen.NoteDetail = backStackEntry.toRoute()
                NoteDetailScreen(
                    noteId = screen.noteId,
                    onEditClick = {
                        navController.navigate(Screen.NoteEdit(noteId = screen.noteId))
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Screen.NoteEdit> { backStackEntry ->
                val screen: Screen.NoteEdit = backStackEntry.toRoute()
                NoteEditScreen(
                    noteId = screen.noteId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Screen.TaskList> {
                TaskListScreen(
                    onTaskClick = { taskId ->
                        navController.navigate(Screen.TaskDetail(taskId))
                    },
                    onAddTask = {
                        navController.navigate(Screen.TaskEdit())
                    }
                )
            }

            composable<Screen.TaskDetail> { backStackEntry ->
                val screen: Screen.TaskDetail = backStackEntry.toRoute()
                TaskDetailScreen(
                    taskId = screen.taskId,
                    onEditClick = {
                        navController.navigate(Screen.TaskEdit(taskId = screen.taskId))
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Screen.TaskEdit> { backStackEntry ->
                val screen: Screen.TaskEdit = backStackEntry.toRoute()
                TaskEditScreen(
                    taskId = screen.taskId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Screen.PersonList> {
                PersonListScreen(
                    onPersonClick = { personId ->
                        navController.navigate(Screen.PersonDetail(personId))
                    },
                    onAddPerson = {
                        navController.navigate(Screen.PersonEdit())
                    }
                )
            }

            composable<Screen.PersonDetail> { backStackEntry ->
                val screen: Screen.PersonDetail = backStackEntry.toRoute()
                PersonDetailScreen(
                    personId = screen.personId,
                    onEditClick = {
                        navController.navigate(Screen.PersonEdit(personId = screen.personId))
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Screen.PersonEdit> { backStackEntry ->
                val screen: Screen.PersonEdit = backStackEntry.toRoute()
                PersonEditScreen(
                    personId = screen.personId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
