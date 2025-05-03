package com.training.workout.model

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Дата класс упражнения
 *
 * @property id Идентификатор упражнения
 * @property name Название упражнения
 * @property duration Дата и время выполнения упражнения
 * @property exerciseType Тип упражнения
 * @property sets Количество подходов
 * @property reps Количество повторений
 * @property distance Дистанция
 * @property steps Количество шагов
 * @property bmp Частота сердечных сокращений
 * @property speed Скорость
 * @property weight Вес
 * @property calories Калории
 * @property reaction Реакция
 * @property note Примечание
 */
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
