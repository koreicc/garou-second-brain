package com.secondbrain.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen {
    @Serializable
    data object Dashboard : Screen

    @Serializable
    data object Workspace : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data class NoteDetail(val noteId: String) : Screen

    @Serializable
    data class NoteEdit(val noteId: String? = null) : Screen

    @Serializable
    data class TaskDetail(val taskId: String) : Screen

    @Serializable
    data class TaskEdit(val taskId: String? = null) : Screen

    @Serializable
    data class PersonDetail(val personId: String) : Screen

    @Serializable
    data class PersonEdit(val personId: String? = null) : Screen

    @Serializable
    data object Calendar : Screen
}