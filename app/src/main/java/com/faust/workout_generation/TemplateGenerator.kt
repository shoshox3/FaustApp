package com.faust.workout_generation

import com.faust.models.*
import com.faust.persistence.dao.ExerciseDao
import com.faust.persistence.dao.PerformedWorkoutDao
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates personalized workout templates based on:
 * - User's workout history (frequency, volume, progression)
 * - Muscle group distribution
 * - Available equipment
 * - Recovery time since last session
 */
@Singleton
class TemplateGenerator @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val performedWorkoutDao: PerformedWorkoutDao
) {
    data class GeneratorInput(
        val targetMuscles: List<String>,
        val equipment: List<String> = emptyList(),
        val durationMinutes: Int = 60,
        val experienceLevel: ExperienceLevel = ExperienceLevel.INTERMEDIATE,
        val userId: String = ""
    )

    enum class ExperienceLevel { BEGINNER, INTERMEDIATE, ADVANCED }

    suspend fun generateWorkout(input: GeneratorInput): WorkoutTemplate {
        val exercises = exerciseDao.getAllExercises().first()
        val history = getRecentHistory(input.userId)

        // Filter exercises by target muscles and equipment
        val candidates = exercises.filter { exercise ->
            input.targetMuscles.any { muscle ->
                exercise.primaryMuscle.contains(muscle, ignoreCase = true)
            } && (input.equipment.isEmpty() || input.equipment.any { equip ->
                exercise.equipment.contains(equip, ignoreCase = true)
            })
        }

        // Apply intelligent selection avoiding recently overworked muscles
        val recentlyWorked = getRecentlyWorkedMuscles(history)
        val selected = selectExercises(candidates, recentlyWorked, input)

        // Build workout template
        return WorkoutTemplate(
            name = generateWorkoutName(input.targetMuscles),
            description = "AI-generated workout targeting ${input.targetMuscles.joinToString(", ")}",
            exercises = selected.mapIndexed { index, exercise ->
                WorkoutExercise(
                    exercise = exercise,
                    sets = buildSets(exercise, input.experienceLevel),
                    restTimerSeconds = when (input.experienceLevel) {
                        ExperienceLevel.BEGINNER -> 120
                        ExperienceLevel.INTERMEDIATE -> 90
                        ExperienceLevel.ADVANCED -> 60
                    },
                    order = index
                )
            },
            estimatedDurationMinutes = input.durationMinutes,
            targetMuscles = input.targetMuscles
        )
    }

    private fun selectExercises(
        candidates: List<Exercise>,
        recentlyWorked: Map<String, Int>,
        input: GeneratorInput
    ): List<Exercise> {
        val exercisesPerHour = 5
        val targetCount = (input.durationMinutes / 60f * exercisesPerHour).toInt().coerceIn(3, 8)

        // Score each exercise (higher = better to include)
        val scored = candidates.map { exercise ->
            val recentWorkCount = recentlyWorked[exercise.primaryMuscle] ?: 0
            val recoveryScore = (7 - recentWorkCount).coerceAtLeast(0) // Prefer rested muscles
            exercise to recoveryScore
        }.sortedByDescending { it.second }

        // Ensure muscle variety
        val selected = mutableListOf<Exercise>()
        val musclesCovered = mutableSetOf<String>()

        for ((exercise, _) in scored) {
            if (selected.size >= targetCount) break
            if (musclesCovered.size < input.targetMuscles.size || !musclesCovered.contains(exercise.primaryMuscle)) {
                selected.add(exercise)
                musclesCovered.add(exercise.primaryMuscle)
            }
        }

        // Fill remaining slots
        for ((exercise, _) in scored) {
            if (selected.size >= targetCount) break
            if (!selected.contains(exercise)) selected.add(exercise)
        }

        return selected.take(targetCount)
    }

    private fun buildSets(exercise: Exercise, level: ExperienceLevel): List<WorkoutSet> {
        val (setCount, repRange) = when (level) {
            ExperienceLevel.BEGINNER -> 3 to (10..12)
            ExperienceLevel.INTERMEDIATE -> 4 to (8..10)
            ExperienceLevel.ADVANCED -> 5 to (6..8)
        }

        return buildList {
            // Warmup set
            add(WorkoutSet(setType = SetType.WARMUP, reps = 12))
            // Working sets
            repeat(setCount) {
                add(WorkoutSet(setType = SetType.NORMAL, reps = repRange.first))
            }
        }
    }

    private suspend fun getRecentHistory(userId: String): List<PerformedWorkout> {
        val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        return performedWorkoutDao.getWorkoutsInRange(weekAgo, System.currentTimeMillis()).first()
    }

    private fun getRecentlyWorkedMuscles(history: List<PerformedWorkout>): Map<String, Int> {
        val muscleCount = mutableMapOf<String, Int>()
        history.forEach { workout ->
            workout.exercises.forEach { exercise ->
                val muscle = exercise.exercise.primaryMuscle
                muscleCount[muscle] = (muscleCount[muscle] ?: 0) + 1
            }
        }
        return muscleCount
    }

    private fun generateWorkoutName(targetMuscles: List<String>): String {
        val dayNames = listOf("Push", "Pull", "Legs", "Upper Body", "Lower Body", "Full Body")
        val muscle = targetMuscles.firstOrNull() ?: ""
        return when {
            muscle.contains("chest", true) || muscle.contains("tricep", true) ||
                muscle.contains("shoulder", true) -> "Push Day"
            muscle.contains("back", true) || muscle.contains("bicep", true) -> "Pull Day"
            muscle.contains("quad", true) || muscle.contains("hamstring", true) ||
                muscle.contains("glute", true) || muscle.contains("calf", true) -> "Leg Day"
            else -> "${targetMuscles.joinToString(" & ")} Workout"
        }
    }
}
