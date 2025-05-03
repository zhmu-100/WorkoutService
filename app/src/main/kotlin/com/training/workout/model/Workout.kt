package com.training.workout.model

import java.time.LocalDateTime

data class Workout(
    val id: String? = null,
    val name: String,
    val date: LocalDateTime,
    val exercises: List<Exercise> = emptyList()
)
