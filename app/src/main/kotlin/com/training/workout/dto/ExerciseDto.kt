package com.training.workout.dto

import com.training.workout.model.Exercise
import com.training.workout.model.ExerciseName
import com.training.workout.model.ExerciseReaction
import com.training.workout.model.ExerciseType
import java.time.Duration

data class ExerciseDto(
    val name: String,
    val durationSeconds: Long,
    val exerciseType: String,
    val sets: Int? = null,
    val reps: Int? = null,
    val distance: Int? = null,
    val steps: Int? = null,
    val bpm: Int? = null,
    val speed: Int? = null,
    val weight: Int? = null,
    val calories: Double? = null,
    val reaction: String? = null,
    val note: String? = null
) {
    fun toModel(): Exercise {
        return Exercise(
            name = ExerciseName.valueOf(name),
            duration = Duration.ofSeconds(durationSeconds),
            exerciseType = ExerciseType.valueOf(exerciseType),
            sets = sets,
            reps = reps,
            distance = distance,
            steps = steps,
            bpm = bpm,
            speed = speed,
            weight = weight,
            calories = calories,
            reaction = reaction?.let { ExerciseReaction.valueOf(it) },
            note = note
        )
    }

    companion object {
        fun fromModel(exercise: Exercise): ExerciseDto {
            return ExerciseDto(
                name = exercise.name.name,
                durationSeconds = exercise.duration.seconds,
                exerciseType = exercise.exerciseType.name,
                sets = exercise.sets,
                reps = exercise.reps,
                distance = exercise.distance,
                steps = exercise.steps,
                bpm = exercise.bpm,
                speed = exercise.speed,
                weight = exercise.weight,
                calories = exercise.calories,
                reaction = exercise.reaction?.name,
                note = exercise.note
            )
        }
    }
}
