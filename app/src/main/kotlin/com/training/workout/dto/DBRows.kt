package com.training.workout.dto

import kotlinx.serialization.Serializable

/**
 * Строка тренировки из БД
 *
 * @property id Идентификатор тренировки
 * @property userid Идентификатор пользователя
 * @property name Название тренировки
 * @property date Дата тренировки
 */
@Serializable
data class DbWorkoutRow(val id: String, val userid: String, val name: String, val date: String)

/**
 * Строка упражнения из БД
 *
 * @property id Идентификатор упражнения
 * @property workoutid Идентификатор тренировки
 * @property name Название упражнения
 * @property duration Длительность упражнения
 * @property exercise_type Тип упражнения
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
