package com.training.workout.model

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Workout(
    val id: String = "",
    val userId: String,
    val name: String,
    val date: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC),
    val exercises: List<Exercise> = emptyList()
)
