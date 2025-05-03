package com.training.workout.dto

import com.training.workout.model.Exercise
import com.training.workout.model.Workout
import java.time.LocalDateTime

data class WorkoutDto(
    val id: String? = null,
    val name: String,
    val date: LocalDateTime,
    val exercises: List<ExerciseDto> = emptyList()
) {
    fun toModel(): Workout {
        return Workout(
            id = id,
            name = name,
            date = date,
            exercises = exercises.map { it.toModel() }
        )
    }

    companion object {
        fun fromModel(workout: Workout): WorkoutDto {
            return WorkoutDto(
                id = workout.id,
                name = workout.name,
                date = workout.date,
                exercises = workout.exercises.map { ExerciseDto.fromModel(it) }
            )
        }
    }
}
