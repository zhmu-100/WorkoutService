package com.training.workout.actions

import com.mad.client.LoggerClient
import com.mad.model.LogLevel
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

  // Инициализация клиента логирования
  private val loggerClient =
      LoggerClient(
          host = dotenv["REDIS_HOST"] ?: "localhost",
          port = dotenv["REDIS_PORT"]?.toIntOrNull() ?: 6379,
          password = dotenv["REDIS_PASSWORD"] ?: "")

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
        loggerClient.logActivity(
            event = "Создание тренировки",
            userId = workout.userId,
            additionalData = mapOf("workoutId" to workout.id, "workoutName" to workout.name))

        try {
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

          if (dbResp.success != true) {
            loggerClient.logError(
                event = "Ошибка создания тренировки",
                errorMessage = "Failed to create workout: ${dbResp.error}",
                userId = workout.userId)
            throw Exception("Failed to create workout: ${dbResp.error}")
          }

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

            if (exResp.success != true) {
              loggerClient.logError(
                  event = "Ошибка создания упражнения",
                  errorMessage = "Failed to create exercise '${ex.name}': ${exResp.error}",
                  userId = workout.userId)
              throw Exception("Failed to create exercise '${ex.name}': ${exResp.error}")
            }
          }

          loggerClient.logActivity(
              event = "Тренировка успешно создана",
              userId = workout.userId,
              additionalData =
                  mapOf(
                      "workoutId" to workout.id,
                      "exercisesCount" to workout.exercises.size.toString()))

          workout
        } catch (e: Exception) {
          loggerClient.logError(
              event = "Исключение при создании тренировки",
              errorMessage = e.message ?: "Unknown error",
              userId = workout.userId,
              stackTrace = e.stackTraceToString())
          throw e
        }
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
        loggerClient.logActivity(
            event = "Запрос тренировки", additionalData = mapOf("workoutId" to id))

        try {
          val workoutRows =
              httpClient
                  .sendJson(
                      "$baseUrl/read",
                      DbReadRequest("workouts", filters = mapOf("id" to id)),
                      HttpMethod.Post)
                  .body<List<DbWorkoutRow>>()

          val row = workoutRows.firstOrNull()
          if (row == null) {
            loggerClient.logActivity(
                event = "Тренировка не найдена",
                level = LogLevel.WARN,
                additionalData = mapOf("workoutId" to id))
            return@withContext null
          }

          val exerciseRows =
              httpClient
                  .sendJson(
                      "$baseUrl/read",
                      DbReadRequest("exercises", filters = mapOf("workoutid" to id)),
                      HttpMethod.Post)
                  .body<List<DbExerciseRow>>()

          val workout = row.toModel(exerciseRows)
          loggerClient.logActivity(
              event = "Тренировка успешно получена",
              userId = workout.userId,
              additionalData =
                  mapOf("workoutId" to id, "exercisesCount" to exerciseRows.size.toString()))

          return@withContext workout
        } catch (e: Exception) {
          // Логируем информацию об ошибке
          loggerClient.logActivity(
              event = "Ошибка при получении тренировки",
              level = LogLevel.ERROR,
              additionalData = mapOf("workoutId" to id, "error" to (e.message ?: "Unknown error")))

          loggerClient.logError(
              event = "Ошибка при получении тренировки",
              errorMessage = e.message ?: "Unknown error",
              stackTrace = e.stackTraceToString())
          throw e
        }
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
        loggerClient.logActivity(
            event = "Запрос списка тренировок",
            userId = if (userId.isNotBlank()) userId else null,
            additionalData = mapOf("page" to page.toString(), "pageSize" to pageSize.toString()))

        try {
          val filters = if (userId.isBlank()) null else mapOf("userid" to userId)
          val rows =
              httpClient
                  .sendJson(
                      "$baseUrl/read",
                      DbReadRequest("workouts", filters = filters),
                      HttpMethod.Post)
                  .body<List<DbWorkoutRow>>()

          val sliced =
              rows
                  .sortedByDescending { LocalDateTime.parse(it.date) }
                  .drop((page - 1) * pageSize)
                  .take(pageSize)

          val workouts = sliced.map { it.toModel(emptyList()) }

          loggerClient.logActivity(
              event = "Список тренировок успешно получен",
              userId = if (userId.isNotBlank()) userId else null,
              additionalData =
                  mapOf(
                      "totalCount" to rows.size.toString(),
                      "returnedCount" to workouts.size.toString()))

          workouts
        } catch (e: Exception) {
          // Логируем информацию об ошибке
          loggerClient.logActivity(
              event = "Ошибка при получении списка тренировок",
              userId = if (userId.isNotBlank()) userId else null,
              level = LogLevel.ERROR,
              additionalData = mapOf("error" to (e.message ?: "Unknown error")))

          loggerClient.logError(
              event = "Ошибка при получении списка тренировок",
              errorMessage = e.message ?: "Unknown error",
              userId = if (userId.isNotBlank()) userId else null,
              stackTrace = e.stackTraceToString())
          throw e
        }
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
        loggerClient.logActivity(
            event = "Обновление тренировки",
            userId = workout.userId,
            additionalData = mapOf("workoutId" to workout.id, "workoutName" to workout.name))

        try {
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

          if (resp.success != true) {
            loggerClient.logActivity(
                event = "Тренировка не найдена при обновлении",
                userId = workout.userId,
                level = LogLevel.WARN,
                additionalData = mapOf("workoutId" to workout.id))
            return@withContext null
          }

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

          loggerClient.logActivity(
              event = "Тренировка успешно обновлена",
              userId = workout.userId,
              additionalData =
                  mapOf(
                      "workoutId" to workout.id,
                      "exercisesCount" to workout.exercises.size.toString()))

          workout
        } catch (e: Exception) {
          // Логируем информацию об ошибке
          loggerClient.logActivity(
              event = "Ошибка при обновлении тренировки",
              userId = workout.userId,
              level = LogLevel.ERROR,
              additionalData =
                  mapOf("workoutId" to workout.id, "error" to (e.message ?: "Unknown error")))

          loggerClient.logError(
              event = "Ошибка при обновлении тренировки",
              errorMessage = e.message ?: "Unknown error",
              userId = workout.userId,
              stackTrace = e.stackTraceToString())
          throw e
        }
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
        loggerClient.logActivity(
            event = "Запрос на удаление тренировки",
            userId = userId,
            additionalData = mapOf("workoutId" to id))

        try {
          val existing = getWorkout(id) ?: return@withContext false
          if (existing.userId != userId) {
            loggerClient.logActivity(
                event = "Отказ в удалении тренировки - несоответствие ID пользователя",
                userId = userId,
                level = LogLevel.WARN,
                additionalData = mapOf("workoutId" to id, "workoutUserId" to existing.userId))
            return@withContext false
          }

          deleteAllExercises(id)

          val resp =
              httpClient
                  .sendJson(
                      "$baseUrl/delete",
                      DbDeleteRequest("workouts", "id = ?", listOf(id)),
                      HttpMethod.Delete)
                  .body<DbResponse>()

          val success = resp.success == true

          if (success) {
            loggerClient.logActivity(
                event = "Тренировка успешно удалена",
                userId = userId,
                additionalData = mapOf("workoutId" to id))
          } else {
            loggerClient.logActivity(
                event = "Ошибка при удалении тренировки",
                userId = userId,
                level = LogLevel.ERROR,
                additionalData =
                    mapOf("workoutId" to id, "error" to (resp.error ?: "Unknown error")))
          }

          success
        } catch (e: Exception) {
          // Логируем информацию об ошибке
          loggerClient.logActivity(
              event = "Исключение при удалении тренировки",
              userId = userId,
              level = LogLevel.ERROR,
              additionalData = mapOf("workoutId" to id, "error" to (e.message ?: "Unknown error")))

          loggerClient.logError(
              event = "Исключение при удалении тренировки",
              errorMessage = e.message ?: "Unknown error",
              userId = userId,
              stackTrace = e.stackTraceToString())
          throw e
        }
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
        loggerClient.logActivity(
            event = "Запрос упражнений тренировки", additionalData = mapOf("workoutId" to id))

        try {
          val exercises =
              httpClient
                  .sendJson(
                      "$baseUrl/read",
                      DbReadRequest("exercises", filters = mapOf("workoutid" to id)),
                      HttpMethod.Post)
                  .body<List<DbExerciseRow>>()
                  .map { it.toModel() }

          loggerClient.logActivity(
              event = "Упражнения тренировки успешно получены",
              additionalData =
                  mapOf("workoutId" to id, "exercisesCount" to exercises.size.toString()))

          exercises
        } catch (e: Exception) {
          // Логируем информацию об ошибке
          loggerClient.logActivity(
              event = "Ошибка при получении упражнений тренировки",
              level = LogLevel.ERROR,
              additionalData = mapOf("workoutId" to id, "error" to (e.message ?: "Unknown error")))

          loggerClient.logError(
              event = "Ошибка при получении упражнений тренировки",
              errorMessage = e.message ?: "Unknown error",
              stackTrace = e.stackTraceToString())
          throw e
        }
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
    loggerClient.logActivity(
        event = "Создание кастомной тренировки",
        userId = workout.userId,
        additionalData = mapOf("workoutId" to workout.id, "workoutName" to workout.name))

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
    loggerClient.logActivity(
        event = "Удаление всех упражнений тренировки",
        additionalData = mapOf("workoutId" to workoutId))

    try {
      val response =
          httpClient.sendJson(
              "$baseUrl/delete",
              DbDeleteRequest("exercises", "workoutid = ?", listOf(workoutId)),
              HttpMethod.Post)

      val dbResp: DbResponse = response.body()

      if (dbResp.success == true) {
        loggerClient.logActivity(
            event = "Упражнения тренировки успешно удалены",
            additionalData = mapOf("workoutId" to workoutId))
      } else {
        loggerClient.logActivity(
            event = "��шибка при удалении упражнений тренировки",
            level = LogLevel.ERROR,
            additionalData =
                mapOf("workoutId" to workoutId, "error" to (dbResp.error ?: "Unknown error")))
      }
    } catch (e: Exception) {
      // Логируем информацию об ошибке
      loggerClient.logActivity(
          event = "Исключение при удалении упражнений тренировки",
          level = LogLevel.ERROR,
          additionalData =
              mapOf("workoutId" to workoutId, "error" to (e.message ?: "Unknown error")))

      loggerClient.logError(
          event = "Исключение при удалении упражнений тренировки",
          errorMessage = e.message ?: "Unknown error",
          stackTrace = e.stackTraceToString())
      throw e
    }
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
