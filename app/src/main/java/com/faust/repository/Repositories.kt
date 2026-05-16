package com.faust.repository

import com.faust.models.*
import com.faust.persistence.dao.*
import com.faust.util.network.ApiService
import com.faust.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// ─── Workout Repository ───────────────────────────────────────────
@Singleton
class WorkoutRepository @Inject constructor(
    private val performedWorkoutDao: PerformedWorkoutDao,
    private val workoutTemplateDao: WorkoutTemplateDao,
    private val exerciseRecordDao: ExerciseRecordDao,
    private val apiService: ApiService
) {
    fun getAllWorkouts(): Flow<List<PerformedWorkout>> =
        performedWorkoutDao.getAllPerformedWorkouts()

    fun getWorkoutsInRange(from: Long, to: Long): Flow<List<PerformedWorkout>> =
        performedWorkoutDao.getWorkoutsInRange(from, to)

    suspend fun saveWorkout(workout: PerformedWorkout) {
        performedWorkoutDao.insert(workout)
        updateExerciseRecords(workout)
    }

    suspend fun deleteWorkout(workout: PerformedWorkout) {
        performedWorkoutDao.delete(workout)
    }

    private suspend fun updateExerciseRecords(workout: PerformedWorkout) {
        workout.exercises.forEach { workoutExercise ->
            val completedSets = workoutExercise.sets.filter { it.isCompleted }
            if (completedSets.isEmpty()) return@forEach

            // Check 1RM
            val best1RM = completedSets.maxOfOrNull { set ->
                set.weight * (1 + set.reps / 30f) // Epley formula
            } ?: return@forEach

            val currentBest = exerciseRecordDao.getBestRecord(
                workoutExercise.exercise.id,
                RecordType.ONE_REP_MAX.name
            )

            if (currentBest == null || best1RM > currentBest.value) {
                exerciseRecordDao.insert(
                    ExerciseRecord(
                        exerciseId = workoutExercise.exercise.id,
                        recordType = RecordType.ONE_REP_MAX,
                        value = best1RM,
                        unit = "kg"
                    )
                )
            }
        }
    }

    suspend fun syncUnsyncedWorkouts(): Resource<Unit> = try {
        val unsynced = performedWorkoutDao.getUnsyncedWorkouts()
        unsynced.forEach { workout ->
            apiService.uploadWorkout(workout)
            performedWorkoutDao.markAsSynced(workout.id)
        }
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Sync failed")
    }

    fun getAllTemplates(): Flow<List<WorkoutTemplate>> =
        workoutTemplateDao.getAllTemplates()

    suspend fun saveTemplate(template: WorkoutTemplate) =
        workoutTemplateDao.insert(template)

    suspend fun deleteTemplate(template: WorkoutTemplate) =
        workoutTemplateDao.delete(template)
}

// ─── Exercise Repository ──────────────────────────────────────────
@Singleton
class ExerciseRepository @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val exerciseRecordDao: ExerciseRecordDao,
    private val apiService: ApiService
) {
    fun getAllExercises(): Flow<List<Exercise>> =
        exerciseDao.getAllExercises()

    fun searchExercises(query: String): Flow<List<Exercise>> =
        exerciseDao.searchExercises(query)

    fun getExercisesByMuscle(muscle: String): Flow<List<Exercise>> =
        exerciseDao.getExercisesByMuscle(muscle)

    fun getRecordsForExercise(exerciseId: String): Flow<List<ExerciseRecord>> =
        exerciseRecordDao.getRecordsForExercise(exerciseId)

    suspend fun syncExercises(): Resource<Unit> = try {
        val exercises = apiService.getExercises()
        exerciseDao.deleteAllNonCustom()
        exerciseDao.insertAll(exercises)
        Resource.Success(Unit)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Failed to fetch exercises")
    }

    suspend fun createCustomExercise(exercise: Exercise) {
        exerciseDao.insert(exercise.copy(isCustom = true))
    }
}

// ─── User Repository ──────────────────────────────────────────────
@Singleton
class UserRepository @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: com.faust.util.SessionManager
) {
    suspend fun login(email: String, password: String): Resource<com.faust.models.User> =
        try {
            val response = apiService.login(mapOf("email" to email, "password" to password))
            sessionManager.saveToken(response.token)
            sessionManager.saveUser(response.user)
            Resource.Success(response.user)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Login failed")
        }

    suspend fun register(
        email: String,
        password: String,
        username: String
    ): Resource<User> = try {
        val response = apiService.register(
            mapOf("email" to email, "password" to password, "username" to username)
        )
        sessionManager.saveToken(response.token)
        sessionManager.saveUser(response.user)
        Resource.Success(response.user)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Registration failed")
    }

    fun getCurrentUser(): User? = sessionManager.getUser()

    fun isLoggedIn(): Boolean = sessionManager.getToken() != null
}
