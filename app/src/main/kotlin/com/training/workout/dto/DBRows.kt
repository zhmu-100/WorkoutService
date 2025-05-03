package com.training.workout.dto

import kotlinx.serialization.Serializable

@Serializable
data class DbWorkoutRow(val id: String, val userid: String, val name: String, val date: String)

@Serializable
data class DbExerciseRow(
    val id: String,
    val workoutid: String,
    val name: String,
    val duration: String,
    val exercise_type: String,
    val sets: String? = null,
    val reps: String? = null,
    val distance: String? = null,
    val steps: String? = null,
    val bmp: String? = null,
    val speed: String? = null,
    val weight: String? = null,
    val calories: String? = null,
    val reaction: String? = null,
    val note: String? = null
)
