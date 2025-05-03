package com.training.workout.actions

import com.training.workout.model.Exercise
import com.training.workout.model.Workout

/**
 * Интерфейс для работы с тренировками
 *
 * Возможные операции:
 * - Создание тренировки
 * - Получение тренировки по ID
 * - Получение списка тренировок
 * - Обновление тренировки
 * - Удаление тренировки
 * - Получение списка упражнений в тренировке
 * - Создание кастомной тренировки
 */
interface IWorkoutAction {

  /**
   * Создает новую тренировку
   *
   * @param workout Тренировка
   * @return Созданная тренировка
   */
  suspend fun createWorkout(workout: Workout): Workout

  /**
   * Получает тренировку по ее ID
   *
   * @param id ID тренировки
   * @return Тренировка или null, если она не найдена
   */
  suspend fun getWorkout(id: String): Workout?

  /**
   * Получает список всех тренировок для пользователя с учетом пейджинга
   *
   * @param userId ID пользователя
   * @param page Номер страницы (начиная с 1)
   * @param pageSize Количество записей на странице
   * @return List тренировок
   */
  suspend fun listWorkouts(userId: String, page: Int, pageSize: Int): List<Workout>

  /**
   * Обновляет существующую тренировку
   *
   * @param workout Обновленная тренировка
   * @return Тренировка или null, если тренировка не была найдена/обновлена
   */
  suspend fun updateWorkout(workout: Workout): Workout?

  /**
   * Удаляет тренировку
   *
   * @param id ID тренировки
   * @param userId ID пользователя
   * @return true, если удалена, иначе false
   */
  suspend fun deleteWorkout(id: String, userId: String): Boolean

  /**
   * Получает список упражнений в тренировке
   *
   * @param id ID тренировки
   * @return Список упражнений
   * @throws Exception Если тренировка не найдена
   */
  suspend fun getWorkoutExercises(id: String): List<Exercise>

  /**
   * Создает кастомную тренировку
   *
   * @param workout Тренировка
   * @return Созданная кастомная тренировка
   */
  suspend fun createCustomWorkout(workout: Workout): Workout
}
