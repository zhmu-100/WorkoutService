package com.training.workout

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class WorkoutServiceApplication

fun main(args: Array<String>) {
    runApplication<WorkoutServiceApplication>(*args)
}
