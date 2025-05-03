package com.training.workout.router

import com.training.workout.model.Workout
import com.training.workout.service.IWorkoutService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.registerWorkoutRoutes(workoutService: IWorkoutService) {
  routing {
    route("/training/workouts") {
      post {
        val workout = call.receive<Workout>()
        val created = workoutService.createWorkout(workout)
        call.respond(HttpStatusCode.Created, created)
      }

      get("{id}") {
        val id =
            call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing id")
        val workout = workoutService.getWorkout(id)
        if (workout == null) call.respond(HttpStatusCode.NotFound, "Workout not found")
        else call.respond(workout)
      }

      get {
        val userId = call.request.queryParameters["userId"] ?: ""
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 10

        val list = workoutService.listWorkouts(userId, page, pageSize)
        call.respond(list)
      }

      put("{id}") {
        val id =
            call.parameters["id"]
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing id")
        val workout = call.receive<Workout>()
        val updated = workoutService.updateWorkout(workout.copy(id = id))
        if (updated == null) call.respond(HttpStatusCode.NotFound, "Workout not found")
        else call.respond(updated)
      }

      delete("{id}") {
        val id =
            call.parameters["id"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing id")
        val userId =
            call.request.queryParameters["user_id"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing user_id")

        val success = workoutService.deleteWorkout(id, userId)
        if (success) call.respond(HttpStatusCode.OK, "Workout deleted")
        else call.respond(HttpStatusCode.NotFound, "Workout not found or not deleted")
      }

      get("{id}/exercises") {
        val id =
            call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing id")
        val exercises = workoutService.getWorkoutExercises(id)
        call.respond(exercises)
      }

      post("/custom") {
        val workout = call.receive<Workout>()
        val created = workoutService.createCustomWorkout(workout)
        call.respond(HttpStatusCode.Created, created)
      }
    }
  }
}
