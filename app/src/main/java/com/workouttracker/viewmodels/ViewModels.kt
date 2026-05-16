package com.faust.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faust.models.*
import com.faust.repository.WorkoutRepository
import com.faust.repository.ExerciseRepository
import com.faust.repository.UserRepository
import com.faust.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

// ─── Base ViewModel ───────────────────────────────────────────────
abstract class BaseViewModel : ViewModel() {
    protected val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    protected val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    protected fun handleError(message: String) {
        viewModelScope.launch { _error.emit(message) }
    }
}

// ─── Home ViewModel ───────────────────────────────────────────────
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val userRepository: UserRepository
) : BaseViewModel() {

    val currentUser: User? get() = userRepository.getCurrentUser()
    val isLoggedIn: Boolean get() = userRepository.isLoggedIn()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    fun syncData() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Syncing
            when (val result = workoutRepository.syncUnsyncedWorkouts()) {
                is Resource.Success -> _syncStatus.value = SyncStatus.Success
                is Resource.Error -> {
                    _syncStatus.value = SyncStatus.Failed(result.message)
                    handleError(result.message)
                }
                else -> {}
            }
        }
    }

    sealed class SyncStatus {
        object Idle : SyncStatus()
        object Syncing : SyncStatus()
        object Success : SyncStatus()
        data class Failed(val message: String) : SyncStatus()
    }
}

// ─── Live Workout ViewModel ───────────────────────────────────────
@HiltViewModel
class LiveWorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : BaseViewModel() {

    private val _currentWorkout = MutableStateFlow<PerformedWorkout?>(null)
    val currentWorkout: StateFlow<PerformedWorkout?> = _currentWorkout.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _restTimerSeconds = MutableStateFlow(0)
    val restTimerSeconds: StateFlow<Int> = _restTimerSeconds.asStateFlow()

    private val _workoutSaved = MutableSharedFlow<PerformedWorkout>()
    val workoutSaved: SharedFlow<PerformedWorkout> = _workoutSaved.asSharedFlow()

    fun startWorkout(template: WorkoutTemplate? = null) {
        val workout = PerformedWorkout(
            name = template?.name ?: "Quick Workout",
            exercises = template?.exercises ?: emptyList(),
            startedAt = System.currentTimeMillis()
        )
        _currentWorkout.value = workout
    }

    fun addExercise(exercise: Exercise) {
        val current = _currentWorkout.value ?: return
        val newExercise = WorkoutExercise(
            exercise = exercise,
            order = current.exercises.size
        )
        _currentWorkout.value = current.copy(
            exercises = current.exercises + newExercise
        )
    }

    fun completeSet(exerciseId: String, setId: String, reps: Int, weight: Float) {
        val current = _currentWorkout.value ?: return
        val updatedExercises = current.exercises.map { workoutExercise ->
            if (workoutExercise.id == exerciseId) {
                val updatedSets = workoutExercise.sets.map { set ->
                    if (set.id == setId) {
                        set.copy(
                            reps = reps,
                            weight = weight,
                            isCompleted = true,
                            completedAt = System.currentTimeMillis()
                        )
                    } else set
                }
                workoutExercise.copy(sets = updatedSets)
            } else workoutExercise
        }
        _currentWorkout.value = current.copy(exercises = updatedExercises)
    }

    fun finishWorkout() {
        viewModelScope.launch {
            val current = _currentWorkout.value ?: return@launch
            val finishedWorkout = current.copy(
                finishedAt = System.currentTimeMillis(),
                durationSeconds = (System.currentTimeMillis() - current.startedAt) / 1000,
                totalVolume = calculateTotalVolume(current.exercises),
                totalSets = countCompletedSets(current.exercises),
                totalReps = countTotalReps(current.exercises)
            )
            workoutRepository.saveWorkout(finishedWorkout)
            _workoutSaved.emit(finishedWorkout)
            _currentWorkout.value = null
        }
    }

    private fun calculateTotalVolume(exercises: List<WorkoutExercise>): Float =
        exercises.sumOf { exercise ->
            exercise.sets.filter { it.isCompleted }
                .sumOf { set -> (set.weight * set.reps).toDouble() }
        }.toFloat()

    private fun countCompletedSets(exercises: List<WorkoutExercise>): Int =
        exercises.sumOf { it.sets.count { set -> set.isCompleted } }

    private fun countTotalReps(exercises: List<WorkoutExercise>): Int =
        exercises.sumOf { exercise ->
            exercise.sets.filter { it.isCompleted }.sumOf { it.reps }
        }
}

// ─── History ViewModel ────────────────────────────────────────────
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : BaseViewModel() {

    val allWorkouts: StateFlow<List<PerformedWorkout>> =
        workoutRepository.getAllWorkouts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getWorkoutsThisMonth(): Flow<List<PerformedWorkout>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        val monthStart = calendar.timeInMillis
        val monthEnd = System.currentTimeMillis()
        return workoutRepository.getWorkoutsInRange(monthStart, monthEnd)
    }

    fun deleteWorkout(workout: PerformedWorkout) {
        viewModelScope.launch {
            workoutRepository.deleteWorkout(workout)
        }
    }
}

// ─── Exercise ViewModel ───────────────────────────────────────────
@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) : BaseViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val exercises: StateFlow<List<Exercise>> =
        _searchQuery
            .debounce(300)
            .flatMapLatest { query ->
                if (query.isBlank()) exerciseRepository.getAllExercises()
                else exerciseRepository.searchExercises(query)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun search(query: String) {
        _searchQuery.value = query
    }

    fun filterByMuscle(muscle: String): Flow<List<Exercise>> =
        exerciseRepository.getExercisesByMuscle(muscle)

    fun createCustomExercise(exercise: Exercise) {
        viewModelScope.launch {
            exerciseRepository.createCustomExercise(exercise)
        }
    }

    fun syncExercises() {
        viewModelScope.launch {
            _loading.value = true
            when (val result = exerciseRepository.syncExercises()) {
                is Resource.Error -> handleError(result.message)
                else -> {}
            }
            _loading.value = false
        }
    }
}

// ─── Auth ViewModel ───────────────────────────────────────────────
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository
) : BaseViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loading.value = true
            _authState.value = AuthState.Loading
            when (val result = userRepository.login(email, password)) {
                is Resource.Success -> _authState.value = AuthState.Success(result.data)
                is Resource.Error -> {
                    _authState.value = AuthState.Error(result.message)
                    handleError(result.message)
                }
                else -> {}
            }
            _loading.value = false
        }
    }

    fun register(email: String, password: String, username: String) {
        viewModelScope.launch {
            _loading.value = true
            _authState.value = AuthState.Loading
            when (val result = userRepository.register(email, password, username)) {
                is Resource.Success -> _authState.value = AuthState.Success(result.data)
                is Resource.Error -> {
                    _authState.value = AuthState.Error(result.message)
                    handleError(result.message)
                }
                else -> {}
            }
            _loading.value = false
        }
    }

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class Success(val user: User) : AuthState()
        data class Error(val message: String) : AuthState()
    }
}
