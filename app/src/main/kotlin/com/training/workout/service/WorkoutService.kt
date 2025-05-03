package com.training.workout.service

import com.training.workout.actions.IWorkoutAction
import com.training.workout.model.Exercise
import com.training.workout.model.Workout
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.UUID

class WorkoutService(private val action: IWorkoutAction) : IWorkoutService {

  override suspend fun createWorkout(workout: Workout): Workout {
    val workoutId = UUID.randomUUID().toString()

    val preparedExercises: List<Exercise> =
      workout.exercises.map { ex ->
        if (ex.id.isBlank())
          ex.copy(id = UUID.randomUUID().toString())
        else
          ex
      }
    val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

    val newWorkout =
      workout.copy(
        id = workoutId,
        date = now,
        exercises = preparedExercises,
      )

    return action.createWorkout(newWorkout)
  }

  override suspend fun getWorkout(id: String): Workout? = action.getWorkout(id)

  override suspend fun listWorkouts(userId: String, page: Int, pageSize: Int): List<Workout> =
    action.listWorkouts(userId, page, pageSize)

  override suspend fun updateWorkout(workout: Workout): Workout? {
    val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    val updated = workout.copy(date = now)
    return action.updateWorkout(workout)
  }

  override suspend fun deleteWorkout(id: String, userId: String): Boolean = action.deleteWorkout(id, userId)

  override suspend fun getWorkoutExercises(id: String): List<Exercise> = action.getWorkoutExercises(id)

  override suspend fun createCustomWorkout(workout: Workout): Workout = action.createWorkout(workout)
}