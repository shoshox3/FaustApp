package com.faust.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.faust.models.User
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// ─── Resource sealed class ────────────────────────────────────────
sealed class Resource<T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error<T>(val message: String, val data: T? = null) : Resource<T>()
    class Loading<T> : Resource<T>()
}

// ─── Session Manager ──────────────────────────────────────────────
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "faust_prefs"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER = "current_user"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveToken(token: String) = prefs.edit().putString(KEY_TOKEN, token).apply()
    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun clearToken() = prefs.edit().remove(KEY_TOKEN).apply()

    fun saveUser(user: User) = prefs.edit().putString(KEY_USER, gson.toJson(user)).apply()
    fun getUser(): User? = prefs.getString(KEY_USER, null)?.let {
        gson.fromJson(it, User::class.java)
    }
    fun clearUser() = prefs.edit().remove(KEY_USER).apply()

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean = getToken() != null
}

// ─── Constants ────────────────────────────────────────────────────
object Constants {
    const val BASE_URL = "https://api.faust.com/v1/"
    const val DB_NAME = "faust.db"

    object NotificationIds {
        const val LIVE_WORKOUT = 1001
        const val REST_TIMER = 1002
        const val WORKOUT_REMINDER = 1003
        const val SYNC = 1004
    }

    object SharedPrefsKeys {
        const val DEFAULT_WEIGHT_UNIT = "default_weight_unit"
        const val REST_TIMER_DURATION = "rest_timer_duration"
        const val TIMER_SOUND_ENABLED = "timer_sound_enabled"
        const val THEME = "app_theme"
    }

    val MUSCLE_GROUPS = listOf(
        "Chest", "Back", "Shoulders", "Biceps", "Triceps",
        "Forearms", "Core", "Glutes", "Quads", "Hamstrings",
        "Calves", "Traps", "Lats", "Full Body", "Cardio"
    )

    val EQUIPMENT_TYPES = listOf(
        "Barbell", "Dumbbell", "Cable", "Machine", "Bodyweight",
        "Kettlebell", "Resistance Band", "Smith Machine", "Other"
    )
}

// ─── Extension Functions ──────────────────────────────────────────
package com.faust.util

import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

fun View.show() { visibility = View.VISIBLE }
fun View.hide() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }

fun Fragment.showToast(message: String) {
    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
}

fun Long.toFormattedDate(pattern: String = "MMM dd, yyyy"): String {
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toDurationString(): String {
    val hours = TimeUnit.SECONDS.toHours(this)
    val minutes = TimeUnit.SECONDS.toMinutes(this) % 60
    val seconds = this % 60
    return if (hours > 0) {
        String.format("%dh %02dm", hours, minutes)
    } else {
        String.format("%dm %02ds", minutes, seconds)
    }
}

fun Float.toVolumeString(unit: String = "kg"): String =
    if (this >= 1000f) String.format("%.1fk %s", this / 1000f, unit)
    else String.format("%.1f %s", this, unit)

fun Float.to1RM(reps: Int): Float = this * (1 + reps / 30f) // Epley formula
