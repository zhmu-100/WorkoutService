package com.training.workout.actions

import com.training.workout.dto.*
import com.training.workout.model.*
import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.dto.*

class WorkoutAction : IWorkoutAction {

  private val dotenv = dotenv { ignoreIfMissing = true }
  private val dbMode = dotenv["DB_MODE"] ?: "LOCAL"
  private val dbHost = dotenv["DB_HOST"] ?: "localhost"
  private val dbPort = dotenv["DB_PORT"] ?: "8080"
  private val baseUrl =
      if (dbMode.equals("gateway", true)) {
        "http://$dbHost:$dbPort/api/db"
      } else {
        "http://$dbHost:$dbPort"
      }

  private val httpClient = HttpClient { install(ContentNegotiation) { json() } }

  private suspend inline fun <reified T : Any> HttpClient.sendJson(
      url: String,
      payload: T,
      method: HttpMethod
  ): HttpResponse =
      request(url) {
        this.method = method
        contentType(ContentType.Application.Json)
        setBody(Json.encodeToString(payload))
      }

  private fun MutableMap<String, String>.putIfNotNull(key: String, value: Any?) {
    value?.let { this[key] = it.toString() }
  }

  override suspend fun createWorkout(workout: Workout): Workout =
      withContext(Dispatchers.IO) {
        val workoutReq =
            DbCreateRequest(
                table = "workouts",
                data =
                    mapOf(
                        "id" to workout.id,
                        "userid" to workout.userId,
                        "name" to workout.name,
                        "date" to workout.date.toString()))
        val workoutResp = httpClient.sendJson("$baseUrl/create", workoutReq, HttpMethod.Post)
        val dbResp: DbResponse = workoutResp.body()

        if (dbResp.success != true) throw Exception("Failed to create workout: ${dbResp.error}")

        workout.exercises.forEach { ex ->
          val data: MutableMap<String, String> =
              mutableMapOf(
                  "id" to ex.id,
                  "workoutid" to workout.id,
                  "name" to ex.name.name,
                  "duration" to ex.duration.toString(),
                  "exercise_type" to ex.exerciseType.name,
              )
          data.putIfNotNull("sets", ex.sets)
          data.putIfNotNull("reps", ex.reps)
          data.putIfNotNull("distance", ex.distance)
          data.putIfNotNull("steps", ex.steps)
          data.putIfNotNull("bmp", ex.bmp)
          data.putIfNotNull("speed", ex.speed)
          data.putIfNotNull("weight", ex.weight)
          data.putIfNotNull("calories", ex.calories)
          data.putIfNotNull("reaction", ex.reaction?.name)
          data.putIfNotNull("note", ex.note)

          val exReq = DbCreateRequest(table = "exercises", data = data)
          val exResp =
              httpClient.sendJson("$baseUrl/create", exReq, HttpMethod.Post).body<DbResponse>()

          if (exResp.success != true)
              throw Exception("Failed to create exercise '${ex.name}': ${exResp.error}")
        }

        workout
      }

  override suspend fun getWorkout(id: String): Workout? =
      withContext(Dispatchers.IO) {
        val workoutRows =
            httpClient
                .sendJson(
                    "$baseUrl/read",
                    DbReadRequest("workouts", filters = mapOf("id" to id)),
                    HttpMethod.Post)
                .body<List<DbWorkoutRow>>()

        val row = workoutRows.firstOrNull() ?: return@withContext null

        val exerciseRows =
            httpClient
                .sendJson(
                    "$baseUrl/read",
                    DbReadRequest("exercises", filters = mapOf("workoutid" to id)),
                    HttpMethod.Post)
                .body<List<DbExerciseRow>>()

        return@withContext row.toModel(exerciseRows)
      }

  override suspend fun listWorkouts(userId: String, page: Int, pageSize: Int): List<Workout> =
      withContext(Dispatchers.IO) {
        val filters = if (userId.isBlank()) null else mapOf("userid" to userId)
        val rows =
            httpClient
                .sendJson(
                    "$baseUrl/read", DbReadRequest("workouts", filters = filters), HttpMethod.Post)
                .body<List<DbWorkoutRow>>()

        val sliced =
            rows
                .sortedByDescending { LocalDateTime.parse(it.date) }
                .drop((page - 1) * pageSize)
                .take(pageSize)

        sliced.map { it.toModel(emptyList()) }
      }

  override suspend fun updateWorkout(workout: Workout): Workout? =
      withContext(Dispatchers.IO) {
        val updateUrl = "$baseUrl/update"

        val updateReq =
            DbUpdateRequest(
                table = "workouts",
                data =
                    mapOf(
                        "userid" to workout.userId,
                        "name" to workout.name,
                        "date" to workout.date.toString()),
                condition = "id = ?",
                conditionParams = listOf(workout.id))
        val resp =
            httpClient.sendJson("$baseUrl/update", updateReq, HttpMethod.Put).body<DbResponse>()

        if (resp.success != true) return@withContext null

        deleteAllExercises(workout.id)
        workout.exercises.forEach { ex ->
          val exerciseMap =
              mutableMapOf(
                  "id" to ex.id,
                  "workoutid" to workout.id,
                  "name" to ex.name.name,
                  "duration" to ex.duration.toString(),
                  "exercise_type" to ex.exerciseType.name,
              )
          exerciseMap.putIfNotNull("sets", ex.sets)
          exerciseMap.putIfNotNull("reps", ex.reps)
          exerciseMap.putIfNotNull("distance", ex.distance)
          exerciseMap.putIfNotNull("steps", ex.steps)
          exerciseMap.putIfNotNull("bmp", ex.bmp)
          exerciseMap.putIfNotNull("speed", ex.speed)
          exerciseMap.putIfNotNull("weight", ex.weight)
          exerciseMap.putIfNotNull("calories", ex.calories)
          exerciseMap.putIfNotNull("reaction", ex.reaction?.name)
          exerciseMap.putIfNotNull("note", ex.note)
          httpClient.sendJson(
              "$baseUrl/create", DbCreateRequest("exercises", exerciseMap), HttpMethod.Post)
        }

        workout
      }

  override suspend fun deleteWorkout(id: String, userId: String): Boolean =
      withContext(Dispatchers.IO) {
        val existing = getWorkout(id) ?: return@withContext false
        if (existing.userId != userId) return@withContext false

        deleteAllExercises(id)

        val resp =
            httpClient
                .sendJson(
                    "$baseUrl/delete",
                    DbDeleteRequest("workouts", "id = ?", listOf(id)),
                    HttpMethod.Delete)
                .body<DbResponse>()

        resp.success == true
      }

  override suspend fun getWorkoutExercises(id: String): List<Exercise> =
      withContext(Dispatchers.IO) {
        httpClient
            .sendJson(
                "$baseUrl/read",
                DbReadRequest("exercises", filters = mapOf("workoutid" to id)),
                HttpMethod.Post)
            .body<List<DbExerciseRow>>()
            .map { it.toModel() }
      }

  override suspend fun createCustomWorkout(workout: Workout): Workout {
    return createWorkout(workout)
  }

  private suspend fun deleteAllExercises(workoutId: String) {
    httpClient.sendJson(
        "$baseUrl/delete",
        DbDeleteRequest("exercises", "workoutid = ?", listOf(workoutId)),
        HttpMethod.Post)
  }

  private fun DbExerciseRow.toModel(): Exercise =
      Exercise(
          id = id,
          name = ExerciseName.valueOf(name),
          duration = LocalDateTime.parse(duration),
          exerciseType = ExerciseType.valueOf(exercise_type),
          sets = sets?.toIntOrNull(),
          reps = reps?.toIntOrNull(),
          distance = distance?.toIntOrNull(),
          steps = steps?.toIntOrNull(),
          bmp = bmp?.toIntOrNull(),
          speed = speed?.toIntOrNull(),
          weight = weight?.toIntOrNull(),
          calories = calories?.toDoubleOrNull(),
          reaction = reaction?.let { ExerciseReaction.valueOf(it) },
          note = note)

  private fun DbWorkoutRow.toModel(exRows: List<DbExerciseRow>): Workout =
      Workout(
          id = id,
          userId = userid,
          name = name,
          date = LocalDateTime.parse(date),
          exercises = exRows.map { it.toModel() })
}
