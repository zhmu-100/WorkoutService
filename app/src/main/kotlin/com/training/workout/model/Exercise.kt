package com.training.workout.model

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Exercise(
    val id: String = "",
    val name: ExerciseName = ExerciseName.EXERCISE_NAME_UNSPECIFIED,
    val duration: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC),
    @SerialName("excercise_type")
    val exerciseType: ExerciseType = ExerciseType.EXERCISE_TYPE_UNSPECIFIED,
    val sets: Int? = null,
    val reps: Int? = null,
    val distance: Int? = null,
    val steps: Int? = null,
    val bmp: Int? = null,
    val speed: Int? = null,
    val weight: Int? = null,
    val calories: Double? = null,
    val reaction: ExerciseReaction? = null,
    val note: String? = null
)
