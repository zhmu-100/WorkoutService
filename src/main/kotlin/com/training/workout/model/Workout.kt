package com.training.workout.model

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data class representing a workout
 *
 * @property id Ид тренировки
 * @property userId Ид пользователя
 * @property name Название тренировки
 * @property date Дата тренировки
 * @property exercises Список упражнений
 */
@Serializable
data class Workout(
    val id: String = "",
    val userId: String,
    val name: String,
    val date: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC),
    @SerialName("excercises") val exercises: List<Exercise> = emptyList()
)
