package com.training.workout.model

import java.time.Duration

data class Exercise(
    val name: ExerciseName,
    val duration: Duration,
    val exerciseType: ExerciseType,
    val sets: Int? = null,
    val reps: Int? = null,
    val distance: Int? = null,
    val steps: Int? = null,
    val bpm: Int? = null,
    val speed: Int? = null,
    val weight: Int? = null,
    val calories: Double? = null,
    val reaction: ExerciseReaction? = null,
    val note: String? = null
)

enum class ExerciseName {
    UNSPECIFIED,
    PUSHUPS,
    PULLUPS,
    SQUATS,
    PLANK,
    RUNNING,
    CYCLING
}

enum class ExerciseType {
    UNSPECIFIED,
    STATIC,
    DYNAMIC
}

enum class ExerciseReaction {
    UNSPECIFIED,
    EXCELLENT,
    GOOD,
    OK,
    BAD,
    VERY_BAD
}
