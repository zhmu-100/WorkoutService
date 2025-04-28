package com.training.workout.service

import com.training.workout.model.Exercise
import com.training.workout.model.Workout
import com.training.workout.repository.WorkoutRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class WorkoutService(private val workoutRepository: WorkoutRepository) {

    fun createWorkout(workout: Workout): Workout {
        return workoutRepository.save(workout)
    }

    fun getWorkout(id: String): Workout? {
        return workoutRepository.findById(id)
    }

    fun listWorkouts(): List<Workout> {
        return workoutRepository.findAll()
    }

    fun updateWorkout(workout: Workout): Workout? {
        return workoutRepository.update(workout)
    }

    fun deleteWorkout(id: String): Boolean {
        return workoutRepository.deleteById(id)
    }

    fun getWorkoutExercises(id: String): List<Exercise>? {
        return workoutRepository.findById(id)?.exercises
    }

    fun createCustomWorkout(workout: Workout): String {
        val customId = "custom-" + UUID.randomUUID().toString()
        val customWorkout = workout.copy(id = customId)
        workoutRepository.save(customWorkout)
        return customId
    }
    
    // Заглушка для загрузки данных GPS, пульса и калорий
    fun uploadTrackData(workoutId: String, trackData: ByteArray): Boolean {
        // В реальном приложении здесь был бы код для загрузки данных в Clickhouse
        println("Uploading track data for workout $workoutId, size: ${trackData.size} bytes")
        return true
    }
}
