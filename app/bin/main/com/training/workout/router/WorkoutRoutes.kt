package com.training.workout.router

import com.mad.client.LoggerClient
import com.training.workout.model.Workout
import com.training.workout.service.IWorkoutService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * REST роутер для работы с тренировками
 * Эндпоинты:
 * POST /training/workouts - Создать новую тренировку
 * GET /training/workouts/{id} - Получить тренировку по ID
 * GET /training/workouts - Получить список тренировок с постраничной навигацией
 * PUT /training/workouts/{id} - Обновить тренировку
 * DELETE /training/workouts/{id} - Удалить тренировку
 * GET /training/workouts/{id}/exercises - Получить список упражнений в тренировке
 * POST /training/workouts/custom - Создать кастомную тренировку
 */
fun Application.registerWorkoutRoutes(workoutService: IWorkoutService) {
    // Инициализация логгера для API запросов
    val loggerClient = LoggerClient(
        host = System.getenv("REDIS_HOST") ?: "localhost",
        port = System.getenv("REDIS_PORT")?.toIntOrNull() ?: 6379,
        password = System.getenv("REDIS_PASSWORD") ?: ""
    )
    
    routing {
        route("/training/workouts") {
            post {
                try {
                    val workout = call.receive<Workout>()
                    
                    // Логирование запроса на создание тренировки
                    loggerClient.logActivity(
                        event = "API_CREATE_WORKOUT_REQUEST",
                        userId = workout.userId
                    )
                    
                    val created = workoutService.createWorkout(workout)
                    call.respond(HttpStatusCode.Created, created)
                    
                    // Логирование успешного создания тренировки
                    loggerClient.logActivity(
                        event = "API_CREATE_WORKOUT_SUCCESS",
                        userId = workout.userId
                    )
                } catch (e: Exception) {
                    // Логирование ошибки при создании тренировки
                    loggerClient.logError(
                        event = "API_CREATE_WORKOUT_ERROR",
                        errorMessage = e.message ?: "Неизвестная ошибка при создании тренировки"
                    )
                    call.respond(HttpStatusCode.InternalServerError, "Ошибка при создании тренировки: ${e.message}")
                }
            }

            get("{id}") {
                try {
                    val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing id")
                    
                    // Логирование запроса на получение тренировки
                    loggerClient.logActivity(
                        event = "API_GET_WORKOUT_REQUEST"
                    )
                    
                    val workout = workoutService.getWorkout(id)
                    if (workout == null) {
                        // Логирование отсутствия тренировки
                        loggerClient.logActivity(
                            event = "API_GET_WORKOUT_NOT_FOUND"
                        )
                        call.respond(HttpStatusCode.NotFound, "Workout not found")
                    } else {
                        call.respond(workout)
                    }
                } catch (e: Exception) {
                    // Логирование ошибки при получении тренировки
                    loggerClient.logError(
                        event = "API_GET_WORKOUT_ERROR",
                        errorMessage = e.message ?: "Неизвестная ошибка при получении тренировки"
                    )
                    call.respond(HttpStatusCode.InternalServerError, "Ошибка при получении тренировки: ${e.message}")
                }
            }

            get {
                try {
                    val userId = call.request.queryParameters["user_id"] ?: ""
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 10
                    
                    // Логирование запроса на получение списка тренировок
                    loggerClient.logActivity(
                        event = "API_LIST_WORKOUTS_REQUEST",
                        userId = if (userId.isNotBlank()) userId else null
                    )

                    val list = workoutService.listWorkouts(userId, page, pageSize)
                    call.respond(list)
                    
                    // Логирование успешного получения списка тренировок
                    loggerClient.logActivity(
                        event = "API_LIST_WORKOUTS_SUCCESS",
                        userId = if (userId.isNotBlank()) userId else null
                    )
                } catch (e: Exception) {
                    val userId = call.request.queryParameters["user_id"] ?: ""
                    // Логирование ошибки при получении списка тренировок
                    loggerClient.logError(
                        event = "API_LIST_WORKOUTS_ERROR",
                        errorMessage = e.message ?: "Неизвестная ошибка при получении списка тренировок",
                        userId = if (userId.isNotBlank()) userId else null
                    )
                    call.respond(HttpStatusCode.InternalServerError, "Ошибка при получении списка тренировок: ${e.message}")
                }
            }

            put("{id}") {
                try {
                    val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing id")
                    val workout = call.receive<Workout>()
                    
                    // Логирование запроса на обновление тренировки
                    loggerClient.logActivity(
                        event = "API_UPDATE_WORKOUT_REQUEST",
                        userId = workout.userId
                    )
                    
                    val updated = workoutService.updateWorkout(workout.copy(id = id))
                    if (updated == null) {
                        // Логирование отсутствия тренировки для обновления
                        loggerClient.logActivity(
                            event = "API_UPDATE_WORKOUT_NOT_FOUND",
                            userId = workout.userId
                        )
                        call.respond(HttpStatusCode.NotFound, "Workout not found")
                    } else {
                        call.respond(updated)
                        
                        // Логирование успешного обновления тренировки
                        loggerClient.logActivity(
                            event = "API_UPDATE_WORKOUT_SUCCESS",
                            userId = workout.userId
                        )
                    }
                } catch (e: Exception) {
                    // Логирование ошибки при обновлении тренировки
                    loggerClient.logError(
                        event = "API_UPDATE_WORKOUT_ERROR",
                        errorMessage = e.message ?: "Неизвестная ошибка при обновлении тренировки"
                    )
                    call.respond(HttpStatusCode.InternalServerError, "Ошибка при обновлении тренировки: ${e.message}")
                }
            }

            delete("{id}") {
                try {
                    val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing id")
                    val userId = call.request.queryParameters["user_id"] ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing user_id")
                    
                    // Логирование запроса на удаление тренировки
                    loggerClient.logActivity(
                        event = "API_DELETE_WORKOUT_REQUEST",
                        userId = userId
                    )

                    val success = workoutService.deleteWorkout(id, userId)
                    if (success) {
                        call.respond(HttpStatusCode.OK, "Workout deleted")
                        
                        // Логирование успешного удаления тренировки
                        loggerClient.logActivity(
                            event = "API_DELETE_WORKOUT_SUCCESS",
                            userId = userId
                        )
                    } else {
                        // Логирование неудачного удаления тренировки
                        loggerClient.logActivity(
                            event = "API_DELETE_WORKOUT_NOT_FOUND",
                            userId = userId
                        )
                        call.respond(HttpStatusCode.NotFound, "Workout not found or not deleted")
                    }
                } catch (e: Exception) {
                    val userId = call.request.queryParameters["user_id"] ?: "unknown"
                    // Логирование ошибки при удалении тренировки
                    loggerClient.logError(
                        event = "API_DELETE_WORKOUT_ERROR",
                        errorMessage = e.message ?: "Неизвестная ошибка при удалении тренировки",
                        userId = userId
                    )
                    call.respond(HttpStatusCode.InternalServerError, "Ошибка при удалении тренировки: ${e.message}")
                }
            }

            get("{id}/exercises") {
                try {
                    val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing id")
                    
                    // Логирование запроса на получение упражнений
                    loggerClient.logActivity(
                        event = "API_GET_EXERCISES_REQUEST"
                    )
                    
                    val exercises = workoutService.getWorkoutExercises(id)
                    call.respond(exercises)
                    
                    // Логирование успешного получения упражнений
                    loggerClient.logActivity(
                        event = "API_GET_EXERCISES_SUCCESS"
                    )
                } catch (e: Exception) {
                    // Логирование ошибки при получении упражнений
                    loggerClient.logError(
                        event = "API_GET_EXERCISES_ERROR",
                        errorMessage = e.message ?: "Неизвестная ошибка при получении упражнений"
                    )
                    call.respond(HttpStatusCode.InternalServerError, "Ошибка при получении упражнений: ${e.message}")
                }
            }

            post("/custom") {
                try {
                    val workout = call.receive<Workout>()
                    
                    // Логирование запроса на создание кастомной тренировки
                    loggerClient.logActivity(
                        event = "API_CREATE_CUSTOM_WORKOUT_REQUEST",
                        userId = workout.userId
                    )
                    
                    val created = workoutService.createCustomWorkout(workout)
                    call.respond(HttpStatusCode.Created, mapOf("id" to created.id))
                    
                    // Логирование успешного создания кастомной тренировки
                    loggerClient.logActivity(
                        event = "API_CREATE_CUSTOM_WORKOUT_SUCCESS",
                        userId = workout.userId
                    )
                } catch (e: Exception) {
                    // Логирование ошибки при создании кастомной тренировки
                    loggerClient.logError(
                        event = "API_CREATE_CUSTOM_WORKOUT_ERROR",
                        errorMessage = e.message ?: "Неизвестная ошибка при создании кастомной тренировки"
                    )
                    call.respond(HttpStatusCode.InternalServerError, "Ошибка при создании кастомной тренировки: ${e.message}")
                }
            }
        }
    }
}
