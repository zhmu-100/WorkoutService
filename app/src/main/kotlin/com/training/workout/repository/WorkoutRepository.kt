package com.training.workout.repository

import com.training.workout.model.Workout
import org.springframework.stereotype.Repository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Repository
class WorkoutRepository {
    // Заглушка для хранения данных
    private val workouts = ConcurrentHashMap<String, Workout>()

    fun save(workout: Workout): Workout {
        val id = workout.id ?: UUID.randomUUID().toString()
        val savedWorkout = workout.copy(id = id)
        workouts[id] = savedWorkout
        return savedWorkout
    }

    fun findById(id: String): Workout? {
        return workouts[id]
    }

    fun findAll(): List<Workout> {
        return workouts.values.toList()
    }

    fun update(workout: Workout): Workout? {
        val id = workout.id ?: return null
        if (!workouts.containsKey(id)) {
            return null
        }
        workouts[id] = workout
        return workout
    }

    fun deleteById(id: String): Boolean {
        return workouts.remove(id) != null
    }
}
