package com.faust.util.network

import com.faust.models.*
import retrofit2.http.*

// ─── API Models ───────────────────────────────────────────────────
data class AuthResponse(val token: String, val user: User)

// ─── API Service ──────────────────────────────────────────────────
interface ApiService {

    // Auth
    @POST("auth/login")
    suspend fun login(@Body body: Map<String, String>): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body body: Map<String, String>): AuthResponse

    // Exercises
    @GET("exercises")
    suspend fun getExercises(): List<Exercise>

    @GET("exercises/{id}")
    suspend fun getExercise(@Path("id") id: String): Exercise

    // Workouts
    @POST("workouts")
    suspend fun uploadWorkout(@Body workout: PerformedWorkout): PerformedWorkout

    @GET("workouts")
    suspend fun getUserWorkouts(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): List<PerformedWorkout>

    // Templates
    @GET("templates")
    suspend fun getPublicTemplates(): List<WorkoutTemplate>

    @POST("templates")
    suspend fun uploadTemplate(@Body template: WorkoutTemplate): WorkoutTemplate

    // Profile
    @GET("users/me")
    suspend fun getProfile(): User

    @PUT("users/me")
    suspend fun updateProfile(@Body user: User): User
}

// ─────────────────────────────────────────────────────────────────
package com.faust.di

import android.content.Context
import androidx.room.Room
import com.faust.BuildConfig
import com.faust.persistence.database.WorkoutDatabase
import com.faust.persistence.dao.*
import com.faust.util.Constants
import com.faust.util.SessionManager
import com.faust.util.network.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ─── Database ─────────────────────────────────────────────────
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WorkoutDatabase =
        Room.databaseBuilder(context, WorkoutDatabase::class.java, Constants.DB_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideExerciseDao(db: WorkoutDatabase): ExerciseDao = db.exerciseDao()
    @Provides fun provideTemplateDao(db: WorkoutDatabase): WorkoutTemplateDao = db.workoutTemplateDao()
    @Provides fun providePerformedWorkoutDao(db: WorkoutDatabase): PerformedWorkoutDao = db.performedWorkoutDao()
    @Provides fun provideGoalDao(db: WorkoutDatabase): WorkoutGoalDao = db.workoutGoalDao()
    @Provides fun provideMeasurementDao(db: WorkoutDatabase): MeasurementDao = db.measurementDao()
    @Provides fun provideRecordDao(db: WorkoutDatabase): ExerciseRecordDao = db.exerciseRecordDao()

    // ─── Networking ───────────────────────────────────────────────
    @Provides
    @Singleton
    fun provideOkHttpClient(sessionManager: SessionManager): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }

        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = sessionManager.getToken()
                val request = if (token != null) {
                    chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                } else chain.request()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)
}
