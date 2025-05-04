package com.training.workout.router

import com.mad.client.LoggerClient
import com.mad.model.LogLevel
import com.training.workout.model.Workout
import com.training.workout.service.IWorkoutService
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * REST роутер для работы с тренировками
 *
 * Эндпоинты:
 * - POST /training/workouts - Создать новую тренировку
 * - GET /training/workouts/{id} - Получить тренировку по ID
 * - GET /training/workouts - Получить список тренировок с постраничной навигацией
 * - PUT /training/workouts/{id} - Обновить тренировку
 * - DELETE /training/workouts/{id} - Удалить тренировку
 * - GET /training/workouts/{id}/exercises - Получить список упражнений в тренировке
 * - POST /training/workouts/custom - Создать кастомную тренировку
 */
fun Application.registerWorkoutRoutes(workoutService: IWorkoutService) {
  val dotenv = dotenv { ignoreIfMissing = true }
  val loggerClient = LoggerClient(
    host = dotenv["REDIS_HOST"] ?: "localhost",
    port = dotenv["REDIS_PORT"]?.toIntOrNull() ?: 6379,
    password = dotenv["REDIS_PASSWORD"] ?: ""
  )
  
  routing {
    route("/training/workouts") {
      post {
        try {
          val workout = call.receive<Workout>()
          loggerClient.logActivity(
            event = "API: Запрос на создание тренировки",
            userId = workout.userId,
            additionalData = mapOf("workoutName" to workout.name)
          )
          
          val created = workoutService.createWorkout(workout)
          
          loggerClient.logActivity(
            event = "API: Тренировка успешно создана",
            userId = created.userId,
            additionalData = mapOf(
              "workoutId" to created.id,
              "workoutName" to created.name
            )
          )
          
          call.respond(HttpStatusCode.Created, created)
        } catch (e: Exception) {
          loggerClient.logError(
            event = "API: Ошибка при создании тренировки",
            errorMessage = e.message ?: "Unknown error",
            stackTrace = e.stackTraceToString()
          )
          call.respond(HttpStatusCode.InternalServerError, "Error creating workout: ${e.message}")
        }
      }

      get("{id}") {
        try {
          val id = call.parameters["id"]
              ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing id")
          
          loggerClient.logActivity(
            event = "API: Запрос тренировки по ID",
            additionalData = mapOf("workoutId" to id)
          )
          
          val workout = workoutService.getWorkout(id)
          if (workout == null) {
            loggerClient.logActivity(
              event = "API: Тренировка не найдена",
              level = LogLevel.WARN,
              additionalData = mapOf("workoutId" to id)
            )
            call.respond(HttpStatusCode.NotFound, "Workout not found")
          } else {
            loggerClient.logActivity(
              event = "API: Тренировка успешно получена",
              userId = workout.userId,
              additionalData = mapOf(
                "workoutId" to id,
                "workoutName" to workout.name
              )
            )
            call.respond(workout)
          }
        } catch (e: Exception) {
          loggerClient.logError(
            event = "API: Ошибка при получении тренировки",
            errorMessage = e.message ?: "Unknown error",
            stackTrace = e.stackTraceToString()
          )
          call.respond(HttpStatusCode.InternalServerError, "Error fetching workout: ${e.message}")
        }
      }

      get {
        try {
          val userId = call.request.queryParameters["user_id"] ?: ""
          val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
          val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 10

          loggerClient.logActivity(
            event = "API: Запрос списка тренировок",
            userId = if (userId.isNotBlank()) userId else null,
            additionalData = mapOf(
              "page" to page.toString(),
              "pageSize" to pageSize.toString()
            )
          )
          
          val list = workoutService.listWorkouts(userId, page, pageSize)
          
          loggerClient.logActivity(
            event = "API: Список тренировок успешно получен",
            userId = if (userId.isNotBlank()) userId else null,
            additionalData = mapOf("count" to list.size.toString())
          )
          
          call.respond(list)
        } catch (e: Exception) {
          loggerClient.logError(
            event = "API: Ошибка при получении списка тренировок",
            errorMessage = e.message ?: "Unknown error",
            stackTrace = e.stackTraceToString()
          )
          call.respond(HttpStatusCode.InternalServerError, "Error listing workouts: ${e.message}")
        }
      }

      put("{id}") {
        try {
          val id = call.parameters["id"]
              ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing id")
          val workout = call.receive<Workout>()
          
          loggerClient.logActivity(
            event = "API: Запрос на обновление тренировки",
            userId = workout.userId,
            additionalData = mapOf(
              "workoutId" to id,
              "workoutName" to workout.name
            )
          )
          
          val updated = workoutService.updateWorkout(workout.copy(id = id))
          if (updated == null) {
            loggerClient.logActivity(
              event = "API: Тренировка не найдена при обновлении",
              userId = workout.userId,
              level = LogLevel.WARN,
              additionalData = mapOf("workoutId" to id)
            )
            call.respond(HttpStatusCode.NotFound, "Workout not found")
          } else {
            loggerClient.logActivity(
              event = "API: Тренировка успешно обновлена",
              userId = updated.userId,
              additionalData = mapOf(
                "workoutId" to id,
                "workoutName" to updated.name
              )
            )
            call.respond(updated)
          }
        } catch (e: Exception) {
          loggerClient.logError(
            event = "API: Ошибка при обновлении тренировки",
            errorMessage = e.message ?: "Unknown error",
            stackTrace = e.stackTraceToString()
          )
          call.respond(HttpStatusCode.InternalServerError, "Error updating workout: ${e.message}")
        }
      }

      delete("{id}") {
        try {
          val id = call.parameters["id"]
              ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing id")
          val userId = call.request.queryParameters["user_id"]
              ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing user_id")

          loggerClient.logActivity(
            event = "API: Запрос на удаление тренировки",
            userId = userId,
            additionalData = mapOf("workoutId" to id)
          )
          
          val success = workoutService.deleteWorkout(id, userId)
          if (success) {
            loggerClient.logActivity(
              event = "API: Тренировка успешно удалена",
              userId = userId,
              additionalData = mapOf("workoutId" to id)
            )
            call.respond(HttpStatusCode.OK, "Workout deleted")
          } else {
            loggerClient.logActivity(
              event = "API: Тренировка не найдена при удалении",
              userId = userId,
              level = LogLevel.WARN,
              additionalData = mapOf("workoutId" to id)
            )
            call.respond(HttpStatusCode.NotFound, "Workout not found or not deleted")
          }
        } catch (e: Exception) {
          loggerClient.logError(
            event = "API: Ошибка при удалении тренировки",
            errorMessage = e.message ?: "Unknown error",
            stackTrace = e.stackTraceToString()
          )
          call.respond(HttpStatusCode.InternalServerError, "Error deleting workout: ${e.message}")
        }
      }

      get("{id}/exercises") {
        try {
          val id = call.parameters["id"]
              ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing id")
          
          loggerClient.logActivity(
            event = "API: Запрос упражнений тренировки",
            additionalData = mapOf("workoutId" to id)
          )
          
          val exercises = workoutService.getWorkoutExercises(id)
          
          loggerClient.logActivity(
            event = "API: Упражнения тренировки успешно получены",
            additionalData = mapOf(
              "workoutId" to id,
              "exercisesCount" to exercises.size.toString()
            )
          )
          
          call.respond(exercises)
        } catch (e: Exception) {
          loggerClient.logError(
            event = "API: Ошибка при получении упражнений тренировки",
            errorMessage = e.message ?: "Unknown error",
            stackTrace = e.stackTraceToString()
          )
          call.respond(HttpStatusCode.InternalServerError, "Error fetching exercises: ${e.message}")
        }
      }

      post("/custom") {
        try {
          val workout = call.receive<Workout>()
          
          loggerClient.logActivity(
            event = "API: Запрос на создание кастомной тренировки",
            userId = workout.userId,
            additionalData = mapOf("workoutName" to workout.name)
          )
          
          val created = workoutService.createCustomWorkout(workout)
          
          loggerClient.logActivity(
            event = "API: Кастомная тренировка успешно создана",
            userId = created.userId,
            additionalData = mapOf(
              "workoutId" to created.id,
              "workoutName" to created.name
            )
          )
          
          call.respond(HttpStatusCode.Created, mapOf("id" to created.id))
        } catch (e: Exception) {
          loggerClient.logError(
            event = "API: Ошибка при создании кастомной тренировки",
            errorMessage = e.message ?: "Unknown error",
            stackTrace = e.stackTraceToString()
          )
          call.respond(HttpStatusCode.InternalServerError, "Error creating custom workout: ${e.message}")
        }
      }
    }
  }
}
