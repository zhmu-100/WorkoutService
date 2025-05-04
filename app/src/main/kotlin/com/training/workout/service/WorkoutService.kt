package com.training.workout.service

import com.mad.client.LoggerClient
import com.mad.model.LogLevel
import com.training.workout.actions.IWorkoutAction
import com.training.workout.model.Exercise
import com.training.workout.model.Workout
import io.github.cdimascio.dotenv.dotenv
import java.util.UUID
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Реаализует интерфейс [IWorkoutService] для бизнес логики работы с тренировками */
class WorkoutService(private val action: IWorkoutAction) : IWorkoutService {
  private val dotenv = dotenv { ignoreIfMissing = true }
  private val loggerClient = LoggerClient(
    host = dotenv["REDIS_HOST"] ?: "localhost",
    port = dotenv["REDIS_PORT"]?.toIntOrNull() ?: 6379,
    password = dotenv["REDIS_PASSWORD"] ?: ""
  )

  /**
   * Создает новую тренировку. Генерирует UUID и дату создания.
   *
   * @param workout Тренировка для создания
   * @return Созданная тренировка с UUID и датой создания
   */
  override suspend fun createWorkout(workout: Workout): Workout {
    loggerClient.logActivity(
      event = "Сервис: Подготовка к созданию тренировки",
      userId = workout.userId,
      additionalData = mapOf("workoutName" to workout.name)
    )
    
    try {
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

      loggerClient.logActivity(
        event = "Сервис: Тренировка подготовлена к созданию",
        userId = workout.userId,
        additionalData = mapOf(
          "workoutId" to workoutId,
          "exercisesCount" to preparedExercises.size.toString()
        )
      )
      
      val result = action.createWorkout(newWorkout)
      
      loggerClient.logActivity(
        event = "Сервис: Тренировка успешно создана",
        userId = workout.userId,
        additionalData = mapOf(
          "workoutId" to workoutId,
          "workoutName" to workout.name
        )
      )
      
      return result
    } catch (e: Exception) {
      // Логируем информацию об ошибке
      loggerClient.logActivity(
        event = "Сервис: Ошибка при создании тренировки",
        userId = workout.userId,
        level = LogLevel.ERROR,
        additionalData = mapOf("error" to (e.message ?: "Unknown error"))
      )
      
      loggerClient.logError(
        event = "Сервис: Ошибка при создании тренировки",
        errorMessage = e.message ?: "Unknown error",
        userId = workout.userId,
        stackTrace = e.stackTraceToString()
      )
      throw e
    }
  }

  /**
   * Получает тренировку по ее ID
   *
   * @param id ID тренировки
   * @return Тренировка или null, если она не найдена
   */
  override suspend fun getWorkout(id: String): Workout? {
    loggerClient.logActivity(
      event = "Сервис: Запрос тренировки по ID",
      additionalData = mapOf("workoutId" to id)
    )
    
    try {
      val workout = action.getWorkout(id)
      
      if (workout == null) {
        loggerClient.logActivity(
          event = "Сервис: Тренировка не найдена",
          level = LogLevel.WARN,
          additionalData = mapOf("workoutId" to id)
        )
      } else {
        loggerClient.logActivity(
          event = "Сервис: Тренировка успешно получена",
          userId = workout.userId,
          additionalData = mapOf(
            "workoutId" to id,
            "workoutName" to workout.name
          )
        )
      }
      
      return workout
    } catch (e: Exception) {
      // Логируем информацию об ошибке
      loggerClient.logActivity(
        event = "Сервис: Ошибка при получении тренировки",
        level = LogLevel.ERROR,
        additionalData = mapOf("workoutId" to id, "error" to (e.message ?: "Unknown error"))
      )
      
      loggerClient.logError(
        event = "Сервис: Ошибка при получении тренировки",
        errorMessage = e.message ?: "Unknown error",
        stackTrace = e.stackTraceToString()
      )
      throw e
    }
  }

  /**
   * Получает список всех тренировок для пользователя с учетом пейджинга
   *
   * @param userId ID пользователя
   * @param page Номер страницы (начиная с 1)
   * @param pageSize Количество записей на странице
   * @return List тренировок
   */
  override suspend fun listWorkouts(userId: String, page: Int, pageSize: Int): List<Workout> {
    loggerClient.logActivity(
      event = "Сервис: Запрос списка тренировок",
      userId = if (userId.isNotBlank()) userId else null,
      additionalData = mapOf(
        "page" to page.toString(),
        "pageSize" to pageSize.toString()
      )
    )
    
    try {
      val workouts = action.listWorkouts(userId, page, pageSize)
      
      loggerClient.logActivity(
        event = "Сервис: Список тренировок успешно получен",
        userId = if (userId.isNotBlank()) userId else null,
        additionalData = mapOf("count" to workouts.size.toString())
      )
      
      return workouts
    } catch (e: Exception) {
      // Логируем информацию об ошибке
      loggerClient.logActivity(
        event = "Сервис: Ошибка при получении списка тренировок",
        userId = if (userId.isNotBlank()) userId else null,
        level = LogLevel.ERROR,
        additionalData = mapOf("error" to (e.message ?: "Unknown error"))
      )
      
      loggerClient.logError(
        event = "Сервис: Ошибка при получении списка тренировок",
        errorMessage = e.message ?: "Unknown error",
        userId = if (userId.isNotBlank()) userId else null,
        stackTrace = e.stackTraceToString()
      )
      throw e
    }
  }

  /**
   * Обновляет существующую тренировку. Обновляет дату обновления.
   *
   * @param workout Обновленная тренировка
   * @return Тренировка или null, если тренировка не была найдена/обновлена
   */
  override suspend fun updateWorkout(workout: Workout): Workout? {
    loggerClient.logActivity(
      event = "Сервис: Подготовка к обновлению тренировки",
      userId = workout.userId,
      additionalData = mapOf(
        "workoutId" to workout.id,
        "workoutName" to workout.name
      )
    )
    
    try {
      val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
      val updated = workout.copy(date = now)
      
      val result = action.updateWorkout(updated)
      
      if (result == null) {
        loggerClient.logActivity(
          event = "Сервис: Тренировка не найдена при обновлении",
          userId = workout.userId,
          level = LogLevel.WARN,
          additionalData = mapOf("workoutId" to workout.id)
        )
      } else {
        loggerClient.logActivity(
          event = "Сервис: Тренировка успешно обновлена",
          userId = workout.userId,
          additionalData = mapOf(
            "workoutId" to workout.id,
            "workoutName" to workout.name
          )
        )
      }
      
      return result
    } catch (e: Exception) {
      // Логируем информацию об ошибке
      loggerClient.logActivity(
        event = "Сервис: Ошибка при обновлении тренировки",
        userId = workout.userId,
        level = LogLevel.ERROR,
        additionalData = mapOf(
          "workoutId" to workout.id,
          "error" to (e.message ?: "Unknown error")
        )
      )
      
      loggerClient.logError(
        event = "Сервис: Ошибка при обновлении тренировки",
        errorMessage = e.message ?: "Unknown error",
        userId = workout.userId,
        stackTrace = e.stackTraceToString()
      )
      throw e
    }
  }

  /**
   * Удаляет тренировку
   *
   * @param id ID тренировки
   * @param userId ID пользователя
   * @return true, если удалена, иначе false
   */
  override suspend fun deleteWorkout(id: String, userId: String): Boolean {
    loggerClient.logActivity(
      event = "Сервис: Запрос на удаление тренировки",
      userId = userId,
      additionalData = mapOf("workoutId" to id)
    )
    
    try {
      val success = action.deleteWorkout(id, userId)
      
      if (success) {
        loggerClient.logActivity(
          event = "Сервис: Тренировка успешно удалена",
          userId = userId,
          additionalData = mapOf("workoutId" to id)
        )
      } else {
        loggerClient.logActivity(
          event = "Сервис: Тренировка не найдена или не удалена",
          userId = userId,
          level = LogLevel.WARN,
          additionalData = mapOf("workoutId" to id)
        )
      }
      
      return success
    } catch (e: Exception) {
      // Логируем информацию об ошибке
      loggerClient.logActivity(
        event = "Сервис: Ошибка при удалении тренировки",
        userId = userId,
        level = LogLevel.ERROR,
        additionalData = mapOf(
          "workoutId" to id,
          "error" to (e.message ?: "Unknown error")
        )
      )
      
      loggerClient.logError(
        event = "Сервис: Ошибка при удалении тренировки",
        errorMessage = e.message ?: "Unknown error",
        userId = userId,
        stackTrace = e.stackTraceToString()
      )
      throw e
    }
  }

  /**
   * Получает список всех упражнений в тренировке
   *
   * @param id ID тренировки
   * @return Список упражнений
   */
  override suspend fun getWorkoutExercises(id: String): List<Exercise> {
    loggerClient.logActivity(
      event = "Сервис: Запрос упражнений тренировки",
      additionalData = mapOf("workoutId" to id)
    )
    
    try {
      val exercises = action.getWorkoutExercises(id)
      
      loggerClient.logActivity(
        event = "Сервис: Упражнения тренировки успешно получены",
        additionalData = mapOf(
          "workoutId" to id,
          "exercisesCount" to exercises.size.toString()
        )
      )
      
      return exercises
    } catch (e: Exception) {
      // Логируем информацию об ошибке
      loggerClient.logActivity(
        event = "Сервис: Ошибка при получении упражнений тренировки",
        level = LogLevel.ERROR,
        additionalData = mapOf(
          "workoutId" to id,
          "error" to (e.message ?: "Unknown error")
        )
      )
      
      loggerClient.logError(
        event = "Сервис: Ошибка при получении упражнений тренировки",
        errorMessage = e.message ?: "Unknown error",
        stackTrace = e.stackTraceToString()
      )
      throw e
    }
  }

  /**
   * Создает кастомную тренировку
   *
   * @param workout Тренировка
   * @return Созданная кастомная тренировка
   */
  override suspend fun createCustomWorkout(workout: Workout): Workout {
    loggerClient.logActivity(
      event = "Сервис: Запрос на создание кастомной тренировки",
      userId = workout.userId,
      additionalData = mapOf("workoutName" to workout.name)
    )
    
    try {
      val result = action.createWorkout(workout)
      
      loggerClient.logActivity(
        event = "Сервис: Кастомная тренировка успешно создана",
        userId = workout.userId,
        additionalData = mapOf(
          "workoutId" to result.id,
          "workoutName" to result.name
        )
      )
      
      return result
    } catch (e: Exception) {
      // Логируем информацию об ошибке
      loggerClient.logActivity(
        event = "Сервис: Ошибка при создании кастомной тренировки",
        userId = workout.userId,
        level = LogLevel.ERROR,
        additionalData = mapOf("error" to (e.message ?: "Unknown error"))
      )
      
      loggerClient.logError(
        event = "Сервис: Ошибка при создании кастомной тренировки",
        errorMessage = e.message ?: "Unknown error",
        userId = workout.userId,
        stackTrace = e.stackTraceToString()
      )
      throw e
    }
  }
}
