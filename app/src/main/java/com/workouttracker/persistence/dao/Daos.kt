package com.faust.persistence.dao

import androidx.room.*
import com.faust.models.*
import kotlinx.coroutines.flow.Flow

// ─── Exercise DAO ─────────────────────────────────────────────────
@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: String): Exercise?

    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' OR primaryMuscle LIKE '%' || :query || '%'")
    fun searchExercises(query: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE primaryMuscle = :muscle")
    fun getExercisesByMuscle(muscle: String): Flow<List<Exercise>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<Exercise>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: Exercise)

    @Delete
    suspend fun delete(exercise: Exercise)

    @Query("DELETE FROM exercises WHERE isCustom = 0")
    suspend fun deleteAllNonCustom()
}

// ─── Workout Template DAO ─────────────────────────────────────────
@Dao
interface WorkoutTemplateDao {
    @Query("SELECT * FROM workout_templates ORDER BY updatedAt DESC")
    fun getAllTemplates(): Flow<List<WorkoutTemplate>>

    @Query("SELECT * FROM workout_templates WHERE id = :id")
    suspend fun getTemplateById(id: String): WorkoutTemplate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: WorkoutTemplate)

    @Update
    suspend fun update(template: WorkoutTemplate)

    @Delete
    suspend fun delete(template: WorkoutTemplate)

    @Query("DELETE FROM workout_templates")
    suspend fun deleteAll()
}

// ─── Performed Workout DAO ────────────────────────────────────────
@Dao
interface PerformedWorkoutDao {
    @Query("SELECT * FROM performed_workouts ORDER BY startedAt DESC")
    fun getAllPerformedWorkouts(): Flow<List<PerformedWorkout>>

    @Query("SELECT * FROM performed_workouts WHERE id = :id")
    suspend fun getById(id: String): PerformedWorkout?

    @Query("SELECT * FROM performed_workouts WHERE startedAt >= :from AND startedAt <= :to ORDER BY startedAt DESC")
    fun getWorkoutsInRange(from: Long, to: Long): Flow<List<PerformedWorkout>>

    @Query("SELECT * FROM performed_workouts WHERE isSynced = 0")
    suspend fun getUnsyncedWorkouts(): List<PerformedWorkout>

    @Query("SELECT COUNT(*) FROM performed_workouts")
    fun getTotalWorkouts(): Flow<Int>

    @Query("SELECT SUM(totalVolume) FROM performed_workouts WHERE startedAt >= :from")
    fun getTotalVolumeFrom(from: Long): Flow<Float?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workout: PerformedWorkout)

    @Update
    suspend fun update(workout: PerformedWorkout)

    @Delete
    suspend fun delete(workout: PerformedWorkout)

    @Query("UPDATE performed_workouts SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}

// ─── Workout Goals DAO ────────────────────────────────────────────
@Dao
interface WorkoutGoalDao {
    @Query("SELECT * FROM workout_goals ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<WorkoutGoal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: WorkoutGoal)

    @Update
    suspend fun update(goal: WorkoutGoal)

    @Delete
    suspend fun delete(goal: WorkoutGoal)

    @Query("UPDATE workout_goals SET currentValue = :value WHERE id = :id")
    suspend fun updateProgress(id: String, value: Float)
}

// ─── Measurement DAO ──────────────────────────────────────────────
@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurements WHERE type = :type ORDER BY measuredAt DESC")
    fun getMeasurementsByType(type: String): Flow<List<BodyMeasurement>>

    @Query("SELECT * FROM measurements ORDER BY measuredAt DESC")
    fun getAllMeasurements(): Flow<List<BodyMeasurement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(measurement: BodyMeasurement)

    @Delete
    suspend fun delete(measurement: BodyMeasurement)
}

// ─── Exercise Record DAO ──────────────────────────────────────────
@Dao
interface ExerciseRecordDao {
    @Query("SELECT * FROM exercise_records WHERE exerciseId = :exerciseId ORDER BY achievedAt DESC")
    fun getRecordsForExercise(exerciseId: String): Flow<List<ExerciseRecord>>

    @Query("SELECT * FROM exercise_records WHERE exerciseId = :exerciseId AND recordType = :type ORDER BY value DESC LIMIT 1")
    suspend fun getBestRecord(exerciseId: String, type: String): ExerciseRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ExerciseRecord)

    @Delete
    suspend fun delete(record: ExerciseRecord)
}
