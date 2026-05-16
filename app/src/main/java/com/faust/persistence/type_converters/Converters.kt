package com.faust.persistence.type_converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.faust.models.WorkoutExercise

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String =
        gson.toJson(value ?: emptyList<String>())

    @TypeConverter
    fun toStringList(value: String): List<String> =
        gson.fromJson(value, object : TypeToken<List<String>>() {}.type) ?: emptyList()

    @TypeConverter
    fun fromWorkoutExerciseList(value: List<WorkoutExercise>?): String =
        gson.toJson(value ?: emptyList<WorkoutExercise>())

    @TypeConverter
    fun toWorkoutExerciseList(value: String): List<WorkoutExercise> =
        gson.fromJson(value, object : TypeToken<List<WorkoutExercise>>() {}.type) ?: emptyList()
}
