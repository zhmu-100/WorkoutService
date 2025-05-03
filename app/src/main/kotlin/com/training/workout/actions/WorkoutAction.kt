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

/**
 * Реализация интерфейс [IWorkoutAction]. Может работать с локальной БД или через Гейтвей
 *
 * @see IWorkoutAction
 */
class WorkoutAction : IWorkoutAction {

  /**
   * Конфигурация подключения к БД
   *
   * @see dotenv
   */
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

  /**
   * Отправляет запрос на сервер с заданным методом и телом запроса в формате JSON
   *
   * @param url URL-адрес для отправки запроса
   * @param payload Тело запроса в формате JSON
   * @param method HTTP-метод (например, GET, POST, PUT, DELETE)
   * @return Ответ от сервера
   */
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

  /** Проверяет, не является ли значение null, и если нет, добавляет его в карту */
  private fun MutableMap<String, String>.putIfNotNull(key: String, value: Any?) {
    value?.let { this[key] = it.toString() }
  }

  /**
   * Создает новую тренировку. Сначала создается запись в таблице workouts, затем в таблице
   * exercises. Если создание записи в таблице workouts не удалось, то выбрасывается исключение.
   *
   * @param workout Тренировка для создания
   * @return Созданная тренировка
   * @see Workout Тренировка
   */
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

  /**
   * Получает тренировку по ее ID. Если тренировка не найдена, возвращает null.
   *
   * @param id ID тренировки
   * @return Тренировка или null, если она не найдена
   * @see Workout Тренировка
   */
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

  /**
   * Получает список всех тренировок для пользователя с учетом пейджинга. Если userId пустой, то
   * возвращает все тренировки.
   *
   * @param userId ID пользователя
   * @param page Номер страницы (начиная с 1)
   * @param pageSize Количество записей на странице
   * @return List тренировок
   * @see Workout Тренировка
   */
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

  /**
   * Обновляет существующую тренировку. Сначала обновляется запись в таблице workouts, затем
   * удаляются все упражнения и создаются новые.
   *
   * @param workout Обновленная тренировка
   * @return Тренировка или null, если тренировка не была найдена/обновлена
   * @see Workout Тренировка
   */
  override suspend fun updateWorkout(workout: Workout): Workout? =
      withContext(Dispatchers.IO) {
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

  /**
   * Удаляет существующую тренировку на основе ее id и id пользователя. Если id пользователя не
   * совпадает с id тренировки, то возвращает false.
   *
   * @param id ID тренировки
   * @param userId ID пользователя
   * @return true, если удалена, иначе false
   * @see Workout Тренировка
   */
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

  /**
   * Получает список упражнений в тренировке по ее ID. Если тренировка не найдена, возвращает пустой
   * список.
   *
   * @param id ID тренировки
   * @return Список упражнений
   * @see Exercise Упражнение
   */
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

  /**
   * Создает кастомную тренировку. Сначала создается запись в таблице workouts, затем в таблице
   * exercises. Если создание записи в таблице workouts не удалось, то выбрасывается исключение.
   *
   * @param workout Тренировка для создания
   * @return Созданная кастомная тренировка
   * @see Workout Тренировка
   */
  override suspend fun createCustomWorkout(workout: Workout): Workout {
    return createWorkout(workout)
  }

  /**
   * Удаляет все упражнения в тренировке по ее ID. Если тренировка не найдена, возвращает пустой
   * список.
   *
   * @param workoutId ID тренировки
   * @return Список упражнений
   * @see Exercise Упражнение
   * @see Workout Тренировка
   */
  private suspend fun deleteAllExercises(workoutId: String) {
    httpClient.sendJson(
        "$baseUrl/delete",
        DbDeleteRequest("exercises", "workoutid = ?", listOf(workoutId)),
        HttpMethod.Post)
  }

  /** Преобразует строку из БД в объект Exercise */
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

  /** Преобразует строку из БД в объект Workout */
  private fun DbWorkoutRow.toModel(exRows: List<DbExerciseRow>): Workout =
      Workout(
          id = id,
          userId = userid,
          name = name,
          date = LocalDateTime.parse(date),
          exercises = exRows.map { it.toModel() })
}
