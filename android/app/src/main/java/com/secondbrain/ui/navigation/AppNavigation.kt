package com.secondbrain.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.secondbrain.di.AppModule
import com.secondbrain.ui.calendar.CalendarScreen
import com.secondbrain.ui.calendar.CalendarViewModel
import com.secondbrain.ui.dashboard.DashboardScreen
import com.secondbrain.ui.notes.NoteDetailScreen
import com.secondbrain.ui.notes.NoteEditScreen
import com.secondbrain.ui.people.PersonDetailScreen
import com.secondbrain.ui.people.PersonEditScreen
import com.secondbrain.ui.search.SearchScreen
import com.secondbrain.ui.search.SearchViewModel
import com.secondbrain.ui.settings.SettingsScreen
import com.secondbrain.ui.settings.SettingsViewModel
import com.secondbrain.ui.habits.HabitEditScreen
import com.secondbrain.ui.habits.HabitListScreen
import com.secondbrain.ui.tasks.RepeatingScreen
import com.secondbrain.ui.tasks.TaskDetailScreen
import com.secondbrain.ui.tasks.TaskEditScreen
import com.secondbrain.ui.workspace.WorkspaceScreen

/**
 * Root navigation composable.
 *
 * Uses a Box wrapper to layer the FAB creation menu over the main scaffold.
 * Bottom bar has 3 tabs (Dashboard, Workspace, Settings) with a floating FAB
 * in the center for quick entity creation.
 */
@Composable
fun AppNavigation(
    settingsViewModel: SettingsViewModel? = null
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // FAB menu visibility
    var fabMenuVisible by remember { mutableStateOf(false) }

    // Show bottom bar only on main destinations (not detail/edit screens)
    val bottomBarRoutes = listOf(Screen.Dashboard, Screen.Workspace, Screen.Settings)
    val showBottomBar = currentDestination?.let { dest ->
        bottomBarRoutes.any { dest.hasRoute(it::class) }
    } ?: true

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (showBottomBar) {
                    SecondBrainBottomBar(
                        currentDestination = currentDestination,
                        onTabSelected = { screen ->
                            navController.navigate(screen) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onFabClick = { fabMenuVisible = true }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard,
                modifier = Modifier.padding(innerPadding),
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                },
                popEnterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                }
            ) {
                // -- Bottom tab destinations --

                composable<Screen.Dashboard> {
                    DashboardScreen(
                        onNavigateToNotes = {
                            navController.navigate(Screen.Workspace) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToTasks = {
                            navController.navigate(Screen.Workspace) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToPeople = {
                            navController.navigate(Screen.Workspace) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToNoteDetail = { noteId ->
                            navController.navigate(Screen.NoteDetail(noteId))
                        },
                        onNavigateToTaskDetail = { taskId ->
                            navController.navigate(Screen.TaskDetail(taskId))
                        },
                        onNavigateToCalendar = {
                            navController.navigate(Screen.Calendar)
                        },
                        onNavigateToHabits = {
                            navController.navigate(Screen.HabitList)
                        }
                    )
                }

                composable<Screen.Workspace> {
                    WorkspaceScreen(
                        onNavigateToNoteDetail = { noteId ->
                            navController.navigate(Screen.NoteDetail(noteId))
                        },
                        onNavigateToTaskDetail = { taskId ->
                            navController.navigate(Screen.TaskDetail(taskId))
                        },
                        onNavigateToPersonDetail = { personId ->
                            navController.navigate(Screen.PersonDetail(personId))
                        },
                        onNavigateToNoteEdit = {
                            navController.navigate(Screen.NoteEdit())
                        },
                        onNavigateToTaskEdit = {
                            navController.navigate(Screen.TaskEdit())
                        },
                        onNavigateToPersonEdit = {
                            navController.navigate(Screen.PersonEdit())
                        },
                        onNavigateToHabitDetail = { habitId ->
                            navController.navigate(Screen.HabitEdit(habitId))
                        },
                        onNavigateToHabitEdit = {
                            navController.navigate(Screen.HabitEdit())
                        }
                    )
                }

                composable<Screen.Settings> {
                    val vm = settingsViewModel ?: viewModel()
                    SettingsScreen(viewModel = vm)
                }

                // -- Detail and Edit screens (pushed on top) --

                composable<Screen.NoteDetail> { backStackEntry ->
                    val screen: Screen.NoteDetail = backStackEntry.toRoute()
                    NoteDetailScreen(
                        noteId = screen.noteId,
                        onEditClick = {
                            navController.navigate(Screen.NoteEdit(noteId = screen.noteId))
                        },
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToNote = { noteId ->
                            navController.navigate(Screen.NoteDetail(noteId))
                        },
                        onNavigateToTask = { taskId ->
                            navController.navigate(Screen.TaskDetail(taskId))
                        },
                        onNavigateToPerson = { personId ->
                            navController.navigate(Screen.PersonDetail(personId))
                        }
                    )
                }

                composable<Screen.NoteEdit> { backStackEntry ->
                    val screen: Screen.NoteEdit = backStackEntry.toRoute()
                    NoteEditScreen(
                        noteId = screen.noteId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable<Screen.TaskDetail> { backStackEntry ->
                    val screen: Screen.TaskDetail = backStackEntry.toRoute()
                    TaskDetailScreen(
                        taskId = screen.taskId,
                        onEditClick = {
                            navController.navigate(Screen.TaskEdit(taskId = screen.taskId))
                        },
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToNote = { noteId ->
                            navController.navigate(Screen.NoteDetail(noteId))
                        },
                        onNavigateToTask = { taskId ->
                            navController.navigate(Screen.TaskDetail(taskId))
                        },
                        onNavigateToPerson = { personId ->
                            navController.navigate(Screen.PersonDetail(personId))
                        }
                    )
                }

                composable<Screen.TaskEdit> { backStackEntry ->
                    val screen: Screen.TaskEdit = backStackEntry.toRoute()
                    TaskEditScreen(
                        taskId = screen.taskId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable<Screen.PersonDetail> { backStackEntry ->
                    val screen: Screen.PersonDetail = backStackEntry.toRoute()
                    PersonDetailScreen(
                        personId = screen.personId,
                        onEditClick = {
                            navController.navigate(Screen.PersonEdit(personId = screen.personId))
                        },
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToNote = { noteId ->
                            navController.navigate(Screen.NoteDetail(noteId))
                        },
                        onNavigateToTask = { taskId ->
                            navController.navigate(Screen.TaskDetail(taskId))
                        },
                        onNavigateToPerson = { personId ->
                            navController.navigate(Screen.PersonDetail(personId))
                        }
                    )
                }

                composable<Screen.PersonEdit> { backStackEntry ->
                    val screen: Screen.PersonEdit = backStackEntry.toRoute()
                    PersonEditScreen(
                        personId = screen.personId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // -- Calendar screen --

                composable<Screen.Calendar> {
                    val calendarViewModel = CalendarViewModel(
                        taskRepository = AppModule.taskRepository
                    )
                    CalendarScreen(
                        viewModel = calendarViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onTaskClick = { taskId ->
                            navController.navigate(Screen.TaskDetail(taskId))
                        }
                    )
                }

                // -- Search screen --

                composable<Screen.Search> {
                    val searchViewModel = SearchViewModel(
                        searchRepository = AppModule.searchRepository
                    )
                    SearchScreen(
                        viewModel = searchViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNoteClick = { noteId ->
                            navController.navigate(Screen.NoteDetail(noteId))
                        },
                        onTaskClick = { taskId ->
                            navController.navigate(Screen.TaskDetail(taskId))
                        },
                        onPersonClick = { personId ->
                            navController.navigate(Screen.PersonDetail(personId))
                        }
                    )
                }

                // -- Repeating screen --

                composable<Screen.Repeating> {
                    RepeatingScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onTemplateClick = { taskId ->
                            navController.navigate(Screen.TaskDetail(taskId))
                        }
                    )
                }

                // -- Habit screens --

                composable<Screen.HabitList> {
                    HabitListScreen(
                        onHabitClick = { habitId ->
                            navController.navigate(Screen.HabitEdit(habitId))
                        },
                        onAddHabit = {
                            navController.navigate(Screen.HabitEdit())
                        }
                    )
                }

                composable<Screen.HabitEdit> { backStackEntry ->
                    val screen: Screen.HabitEdit = backStackEntry.toRoute()
                    HabitEditScreen(
                        habitId = screen.habitId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }

        // FAB creation menu overlay (drawn above scaffold)
        FabMenuOverlay(
            isVisible = fabMenuVisible,
            onDismiss = { fabMenuVisible = false },
            onNewNote = {
                fabMenuVisible = false
                navController.navigate(Screen.NoteEdit())
            },
            onNewTask = {
                fabMenuVisible = false
                navController.navigate(Screen.TaskEdit())
            },
            onNewPerson = {
                fabMenuVisible = false
                navController.navigate(Screen.PersonEdit())
            },
            onNewHabit = {
                fabMenuVisible = false
                navController.navigate(Screen.HabitEdit())
            }
        )
    }
}