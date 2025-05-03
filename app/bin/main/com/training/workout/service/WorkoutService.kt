package com.training.workout.service

import com.mad.client.LoggerClient
import com.mad.model.LogLevel
import com.training.workout.actions.IWorkoutAction
import com.training.workout.model.Exercise
import com.training.workout.model.Workout
import java.util.UUID
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Реаализует интерфейс [IWorkoutService] для бизнес логики работы с тренировками
 */
class WorkoutService(private val action: IWorkoutAction) : IWorkoutService {
    
    // Инициализация логгера
    private val loggerClient = LoggerClient(
        host = System.getenv("REDIS_HOST") ?: "localhost",
        port = System.getenv("REDIS_PORT")?.toIntOrNull() ?: 6379,
        password = System.getenv("REDIS_PASSWORD") ?: ""
    )

    /**
     * Создает новую тренировку. Генерирует UUID и дату создания.
     * @param workout Тренировка для создания
     * @return Созданная тренировка с UUID и датой создания
     */
    override suspend fun createWorkout(workout: Workout): Workout {
        try {
            val workoutId = UUID.randomUUID().toString()
            val preparedExercises: List<Exercise> = workout.exercises.map { ex ->
                if (ex.id.isBlank()) ex.copy(id = UUID.randomUUID().toString()) else ex
            }
            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

            val newWorkout = workout.copy(
                id = workoutId,
                date = now,
                exercises = preparedExercises,
            )

            val result = action.createWorkout(newWorkout)
            
            // Логирование успешного создания тренировки
            loggerClient.logActivity(
                event = "WORKOUT_CREATED",
                userId = result.userId
            )
            
            return result
        } catch (e: Exception) {
            // Логирование ошибки при создании тренировки
            loggerClient.logError(
                event = "WORKOUT_CREATION_ERROR",
                errorMessage = e.message ?: "Неизвестная ошибка при создании тренировки",
                userId = workout.userId
            )
            throw e
        }
    }

    /**
     * Получает тренировку по ее ID
     * @param id ID тренировки
     * @return Тренировка или null, если она не найдена
     */
    override suspend fun getWorkout(id: String): Workout? {
        try {
            val result = action.getWorkout(id)
            
            // Логирование получения тренировки
            if (result != null) {
                loggerClient.logActivity(
                    event = "WORKOUT_RETRIEVED",
                    userId = result.userId
                )
            } else {
                loggerClient.logActivity(
                    event = "WORKOUT_NOT_FOUND"
                )
            }
            
            return result
        } catch (e: Exception) {
            // Логирование ошибки при получении тренировки
            loggerClient.logError(
                event = "WORKOUT_RETRIEVAL_ERROR",
                errorMessage = e.message ?: "Неизвестная ошибка при получении тренировки"
            )
            throw e
        }
    }

    /**
     * Получает список всех тренировок для пользователя с учетом пейджинга
     * @param userId ID пользователя
     * @param page Номер страницы (начиная с 1)
     * @param pageSize Количество записей на странице
     * @return List тренировок
     */
    override suspend fun listWorkouts(userId: String, page: Int, pageSize: Int): List<Workout> {
        try {
            val result = action.listWorkouts(userId, page, pageSize)
            
            // Логирование получения списка тренировок
            loggerClient.logActivity(
                event = "WORKOUTS_LISTED",
                userId = if (userId.isNotBlank()) userId else null
            )
            
            return result
        } catch (e: Exception) {
            // Логирование ошибки при получении списка тренировок
            loggerClient.logError(
                event = "WORKOUTS_LISTING_ERROR",
                errorMessage = e.message ?: "Неизвестная ошибка при получении списка тренировок",
                userId = if (userId.isNotBlank()) userId else null
            )
            throw e
        }
    }

    /**
     * Обновляет существующую тренировку. Обновляет дату обновления.
     * @param workout Обновленная тренировка
     * @return Тренировка или null, если тренировка не была найдена/обновлена
     */
    override suspend fun updateWorkout(workout: Workout): Workout? {
        try {
            val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            val updated = workout.copy(date = now)
            val result = action.updateWorkout(updated)
            
            // Логирование обновления тренировки
            if (result != null) {
                loggerClient.logActivity(
                    event = "WORKOUT_UPDATED",
                    userId = workout.userId
                )
            } else {
                loggerClient.logActivity(
                    event = "WORKOUT_UPDATE_FAILED",
                    userId = workout.userId,
                    level = LogLevel.WARN
                )
            }
            
            return result
        } catch (e: Exception) {
            // Логирование ошибки при обновлении тренировки
            loggerClient.logError(
                event = "WORKOUT_UPDATE_ERROR",
                errorMessage = e.message ?: "Неизвестная ошибка при обновлении тренировки",
                userId = workout.userId
            )
            throw e
        }
    }

    /**
     * Удаляет тренировку
     * @param id ID тренировки
     * @param userId ID пользователя
     * @return true, если удалена, иначе false
     */
    override suspend fun deleteWorkout(id: String, userId: String): Boolean {
        try {
            val result = action.deleteWorkout(id, userId)
            
            // Логирование удаления тренировки
            if (result) {
                loggerClient.logActivity(
                    event = "WORKOUT_DELETED",
                    userId = userId
                )
            } else {
                loggerClient.logActivity(
                    event = "WORKOUT_DELETION_FAILED",
                    userId = userId,
                    level = LogLevel.WARN
                )
            }
            
            return result
        } catch (e: Exception) {
            // Логирование ошибки при удалении тренировки
            loggerClient.logError(
                event = "WORKOUT_DELETION_ERROR",
                errorMessage = e.message ?: "Неизвестная ошибка при удалении тренировки",
                userId = userId
            )
            throw e
        }
    }

    /**
     * Получает список всех упражнений в тренировке
     * @param id ID тренировки
     * @return Список упражнений
     */
    override suspend fun getWorkoutExercises(id: String): List<Exercise> {
        try {
            val result = action.getWorkoutExercises(id)
            
            // Логирование получения упражнений
            loggerClient.logActivity(
                event = "EXERCISES_RETRIEVED"
            )
            
            return result
        } catch (e: Exception) {
            // Логирование ошибки при получении упражнений
            loggerClient.logError(
                event = "EXERCISES_RETRIEVAL_ERROR",
                errorMessage = e.message ?: "Неизвестная ошибка при получении упражнений"
            )
            throw e
        }
    }

    /**
     * Создает кастомную тренировку
     * @param workout Тренировка
     * @return Созданная кастомная тренировка
     */
    override suspend fun createCustomWorkout(workout: Workout): Workout {
        try {
            val result = action.createWorkout(workout)
            
            // Логирование создания кастомной тренировки
            loggerClient.logActivity(
                event = "CUSTOM_WORKOUT_CREATED",
                userId = result.userId
            )
            
            return result
        } catch (e: Exception) {
            // Логирование ошибки при создании кастомной тренировки
            loggerClient.logError(
                event = "CUSTOM_WORKOUT_CREATION_ERROR",
                errorMessage = e.message ?: "Неизвестная ошибка при создании кастомной тренировки",
                userId = workout.userId
            )
            throw e
        }
    }
}
