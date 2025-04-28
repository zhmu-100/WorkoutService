package com.training.workout.controller

import com.training.workout.dto.WorkoutDto
import com.training.workout.dto.ExerciseDto
import com.training.workout.service.WorkoutService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/workouts")
class WorkoutController(private val workoutService: WorkoutService) {

    @PostMapping
    fun createWorkout(@RequestBody workoutDto: WorkoutDto): ResponseEntity<WorkoutDto> {
        val workout = workoutDto.toModel()
        val createdWorkout = workoutService.createWorkout(workout)
        return ResponseEntity.status(HttpStatus.CREATED).body(WorkoutDto.fromModel(createdWorkout))
    }

    @GetMapping("/{id}")
    fun getWorkout(@PathVariable id: String): ResponseEntity<WorkoutDto> {
        val workout = workoutService.getWorkout(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(WorkoutDto.fromModel(workout))
    }

    @GetMapping
    fun listWorkouts(): ResponseEntity<List<WorkoutDto>> {
        val workouts = workoutService.listWorkouts()
        return ResponseEntity.ok(workouts.map { WorkoutDto.fromModel(it) })
    }

    @PutMapping("/{id}")
    fun updateWorkout(@PathVariable id: String, @RequestBody workoutDto: WorkoutDto): ResponseEntity<WorkoutDto> {
        val workout = workoutDto.toModel().copy(id = id)
        val updatedWorkout = workoutService.updateWorkout(workout) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(WorkoutDto.fromModel(updatedWorkout))
    }

    @DeleteMapping("/{id}")
    fun deleteWorkout(@PathVariable id: String): ResponseEntity<Void> {
        val deleted = workoutService.deleteWorkout(id)
        return if (deleted) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()
    }

    @GetMapping("/{id}/exercises")
    fun getWorkoutExercises(@PathVariable id: String): ResponseEntity<List<ExerciseDto>> {
        val exercises = workoutService.getWorkoutExercises(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(exercises.map { ExerciseDto.fromModel(it) })
    }

    @PostMapping("/custom")
    fun createCustomWorkout(@RequestBody workoutDto: WorkoutDto): ResponseEntity<Map<String, String>> {
        val workout = workoutDto.toModel()
        val customId = workoutService.createCustomWorkout(workout)
        return ResponseEntity.status(HttpStatus.CREATED).body(mapOf("id" to customId))
    }
    
    @PostMapping("/{id}/track-data")
    fun uploadTrackData(
        @PathVariable id: String,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<Map<String, Boolean>> {
        val result = workoutService.uploadTrackData(id, file.bytes)
        return ResponseEntity.ok(mapOf("success" to result))
    }
}
