package com.training.workout

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
 * Запускает сервер на порту, указанном в переменной окружения PORT или 8002 по умолчанию
 */
fun main() {
    val dotenv = dotenv { ignoreIfMissing = true }
    val port = dotenv["PORT"]?.toIntOrNull() ?: 8002
    
    embeddedServer(Netty, port = port) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }

        val workoutAction = WorkoutAction()
        val workoutService = WorkoutService(workoutAction)
        
        // Используем обычный сервис (логирование уже встроено в него)
        registerWorkoutRoutes(workoutService)
    }
    .start(wait = true)
}
