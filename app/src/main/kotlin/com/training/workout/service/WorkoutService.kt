package com.training.workout.service

import com.training.workout.actions.IWorkoutAction
import com.training.workout.model.Exercise
import com.training.workout.model.Workout
import java.util.UUID
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Реаализует интерфейс [IWorkoutService] для бизнес логики работы с тренировками */
class WorkoutService(private val action: IWorkoutAction) : IWorkoutService {

  /**
   * Создает новую тренировку. Генерирует UUID и дату создания.
   *
   * @param workout Тренировка для создания
   * @return Созданная тренировка с UUID и датой создания
   */
  override suspend fun createWorkout(workout: Workout): Workout {
    val workoutId = UUID.randomUUID().toString()

    val preparedExercises: List<Exercise> =
        workout.exercises.map { ex ->
          if (ex.id.isBlank()) ex.copy(id = UUID.randomUUID().toString()) else ex
        }
    val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

    val newWorkout =
        workout.copy(
            id = workoutId,
            date = now,
            exercises = preparedExercises,
        )

    return action.createWorkout(newWorkout)
  }

  /**
   * Получает тренировку по ее ID
   *
   * @param id ID тренировки
   * @return Тренировка или null, если она не найдена
   */
  override suspend fun getWorkout(id: String): Workout? = action.getWorkout(id)

  /**
   * Получает список всех тренировок для пользователя с учетом пейджинга
   *
   * @param userId ID пользователя
   * @param page Номер страницы (начиная с 1)
   * @param pageSize Количество записей на странице
   * @return List тренировок
   */
  override suspend fun listWorkouts(userId: String, page: Int, pageSize: Int): List<Workout> =
      action.listWorkouts(userId, page, pageSize)

  /**
   * Обновляет существующую тренировку. Обновляет дату обновления.
   *
   * @param workout Обновленная тренировка
   * @return Тренировка или null, если тренировка не была найдена/обновлена
   */
  override suspend fun updateWorkout(workout: Workout): Workout? {
    val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    val updated = workout.copy(date = now)
    return action.updateWorkout(updated)
  }

  /**
   * Удаляет тренировку
   *
   * @param id ID тренировки
   * @param userId ID пользователя
   * @return true, если удалена, иначе false
   */
  override suspend fun deleteWorkout(id: String, userId: String): Boolean =
      action.deleteWorkout(id, userId)

  /**
   * Получает список всех упражнений в тренировке
   *
   * @param id ID тренировки
   * @return Список упражнений
   */
  override suspend fun getWorkoutExercises(id: String): List<Exercise> =
      action.getWorkoutExercises(id)

  /**
   * Создает кастомную тренировку
   *
   * @param workout Тренировка
   * @return Созданная кастомная тренировка
   */
  override suspend fun createCustomWorkout(workout: Workout): Workout =
      action.createWorkout(workout)
}
