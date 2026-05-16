package com.faust.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

// ─── Exercise ────────────────────────────────────────────────────
@Parcelize
@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val primaryMuscle: String = "",
    val secondaryMuscles: List<String> = emptyList(),
    val equipment: String = "",
    val exerciseType: ExerciseType = ExerciseType.STRENGTH,
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

enum class ExerciseType { STRENGTH, CARDIO, FLEXIBILITY, SPORT }

// ─── Workout Set ─────────────────────────────────────────────────
@Parcelize
data class WorkoutSet(
    val id: String = java.util.UUID.randomUUID().toString(),
    val reps: Int = 0,
    val weight: Float = 0f,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val rpe: Float? = null,   // Rate of Perceived Exertion
    val rir: Int? = null,     // Reps in Reserve
    val setType: SetType = SetType.NORMAL,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null
) : Parcelable

enum class WeightUnit { KG, LBS }
enum class SetType { NORMAL, WARMUP, DROP_SET, SUPERSET, FAILURE }

// ─── Workout Exercise ─────────────────────────────────────────────
@Parcelize
data class WorkoutExercise(
    val id: String = java.util.UUID.randomUUID().toString(),
    val exercise: Exercise,
    val sets: List<WorkoutSet> = emptyList(),
    val notes: String = "",
    val restTimerSeconds: Int = 90,
    val order: Int = 0
) : Parcelable

// ─── Workout Template ─────────────────────────────────────────────
@Parcelize
@Entity(tableName = "workout_templates")
data class WorkoutTemplate(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val exercises: List<WorkoutExercise> = emptyList(),
    val estimatedDurationMinutes: Int = 60,
    val targetMuscles: List<String> = emptyList(),
    val createdBy: String = "",
    val isPublic: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable

// ─── Performed Workout ────────────────────────────────────────────
@Parcelize
@Entity(tableName = "performed_workouts")
data class PerformedWorkout(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val templateId: String? = null,
    val name: String,
    val exercises: List<WorkoutExercise> = emptyList(),
    val durationSeconds: Long = 0,
    val totalVolume: Float = 0f,   // total kg lifted
    val totalSets: Int = 0,
    val totalReps: Int = 0,
    val caloriesBurned: Int? = null,
    val notes: String = "",
    val imageUrls: List<String> = emptyList(),
    val isSynced: Boolean = false,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null
) : Parcelable

// ─── Workout Collection ───────────────────────────────────────────
@Parcelize
@Entity(tableName = "workout_collections")
data class WorkoutCollection(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val templateIds: List<String> = emptyList(),
    val coverImageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

// ─── User ─────────────────────────────────────────────────────────
@Parcelize
data class User(
    val id: String = "",
    val email: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatarUrl: String? = null,
    val bio: String = "",
    val weightUnit: WeightUnit = WeightUnit.KG,
    val isPremium: Boolean = false,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val totalWorkouts: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

// ─── Workout Goal ─────────────────────────────────────────────────
@Parcelize
@Entity(tableName = "workout_goals")
data class WorkoutGoal(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val type: GoalType,
    val targetValue: Float,
    val currentValue: Float = 0f,
    val unit: String = "",
    val deadlineAt: Long? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

enum class GoalType {
    WEEKLY_WORKOUTS,
    MONTHLY_VOLUME,
    EXERCISE_MAX,
    BODY_WEIGHT,
    STREAK
}

// ─── Measurement ─────────────────────────────────────────────────
@Parcelize
@Entity(tableName = "measurements")
data class BodyMeasurement(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val type: MeasurementType,
    val value: Float,
    val unit: String,
    val measuredAt: Long = System.currentTimeMillis()
) : Parcelable

enum class MeasurementType {
    WEIGHT, BODY_FAT, CHEST, WAIST, HIPS,
    BICEP_LEFT, BICEP_RIGHT, THIGH_LEFT, THIGH_RIGHT,
    SHOULDERS, NECK, CALF_LEFT, CALF_RIGHT
}

// ─── Exercise Record ──────────────────────────────────────────────
@Parcelize
@Entity(tableName = "exercise_records")
data class ExerciseRecord(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val exerciseId: String,
    val recordType: RecordType,
    val value: Float,
    val unit: String = "kg",
    val reps: Int? = null,
    val achievedAt: Long = System.currentTimeMillis()
) : Parcelable

enum class RecordType { ONE_REP_MAX, BEST_VOLUME, BEST_SET }
