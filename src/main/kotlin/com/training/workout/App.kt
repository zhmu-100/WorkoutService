package com.training.workout

import com.mad.client.LoggerClient
import com.mad.model.LogLevel
import com.training.workout.actions.WorkoutAction
import com.training.workout.router.registerWorkoutRoutes
import com.training.workout.service.WorkoutService
import io.github.cdimascio.dotenv.dotenv
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json

/**
 * Точка входа в приложение
 *
 * Запускает сервер на порту, указанном в переменной окружения PORT или 8002 по умолчанию
 */
fun main() {
  val dotenv = dotenv { ignoreIfMissing = true }
  val port = dotenv["PORT"]?.toIntOrNull() ?: 8002
  
  // Инициализация клиента логирования
  val loggerClient = LoggerClient(
    host = dotenv["REDIS_HOST"] ?: "localhost",
    port = dotenv["REDIS_PORT"]?.toIntOrNull() ?: 6379,
    password = dotenv["REDIS_PASSWORD"] ?: ""
  )
  
  loggerClient.logActivity(
    event = "Запуск приложения",
    additionalData = mapOf(
      "port" to port.toString(),
      "redisHost" to (dotenv["REDIS_HOST"] ?: "localhost"),
      "redisPort" to (dotenv["REDIS_PORT"] ?: "6379")
    )
  )

  try {
    embeddedServer(Netty, port = port) {
          install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }

          val workoutAction = WorkoutAction()
          val workoutService = WorkoutService(workoutAction)

          registerWorkoutRoutes(workoutService)
          
          loggerClient.logActivity(
            event = "Сервер успешно запущен",
            additionalData = mapOf("port" to port.toString())
          )
        }
        .start(wait = true)
  } catch (e: Exception) {
    loggerClient.logError(
      event = "Ошибка при запуске сервера",
      errorMessage = e.message ?: "Unknown error",
      level = LogLevel.FATAL,
      stackTrace = e.stackTraceToString()
    )
    throw e
  }
}
