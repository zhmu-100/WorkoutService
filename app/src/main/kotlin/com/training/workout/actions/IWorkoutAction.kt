package com.training.workout.actions

import com.training.workout.model.Exercise
import com.training.workout.model.Workout

interface IWorkoutAction {

  suspend fun createWorkout(workout: Workout): Workout
  suspend fun getWorkout(id: String): Workout?
  suspend fun listWorkouts(userId: String, page: Int, pageSize: Int): List<Workout>
  suspend fun updateWorkout(workout: Workout): Workout?
  suspend fun deleteWorkout(id: String, userId: String): Boolean

  suspend fun getWorkoutExercises(id: String): List<Exercise>
  suspend fun createCustomWorkout(workout: Workout): Workout
}