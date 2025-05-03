package com.training.workout.model

import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Serializable
data class Exercise(
  val id: String = "",
  val name: ExerciseName = ExerciseName.EXERCISE_NAME_UNSPECIFIED,
  val duration: Duration = 0.seconds,
  val exerciseType: ExerciseType = ExerciseType.EXERCISE_TYPE_UNSPECIFIED,
  val sets: Int? = null,
  val reps: Int? = null,
  val distance: Int? = null,
  val steps: Int? = null,
  val bmp: Int? = null,
  val speed: Int? = null,
  val weight: Int? = null,
  val calories: Double? = null,
  val reaction: ExerciseReaction? = null,
  val note: String? = null
  )