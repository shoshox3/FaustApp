package com.faust.persistence.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.faust.models.*
import com.faust.persistence.dao.*
import com.faust.persistence.type_converters.Converters

@Database(
    entities = [
        Exercise::class,
        WorkoutTemplate::class,
        PerformedWorkout::class,
        WorkoutCollection::class,
        WorkoutGoal::class,
        BodyMeasurement::class,
        ExerciseRecord::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao
    abstract fun performedWorkoutDao(): PerformedWorkoutDao
    abstract fun workoutGoalDao(): WorkoutGoalDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun exerciseRecordDao(): ExerciseRecordDao
}
