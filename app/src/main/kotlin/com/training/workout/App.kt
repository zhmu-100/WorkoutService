package com.training.workout

import com.training.workout.actions.WorkoutAction
import com.training.workout.router.registerWorkoutRoutes
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.engine.*
import com.training.workout.service.WorkoutService
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*

fun main() {
  val dotenv = dotenv { ignoreIfMissing = true }
  val port = dotenv["PORT"]?.toIntOrNull() ?: 8002

  embeddedServer(Netty, port = port) {
    install(ContentNegotiation) { json() }

    val workoutAction = WorkoutAction()
    val workoutService = WorkoutService(workoutAction)

    registerWorkoutRoutes(workoutService)
  }.start(wait = true)
}