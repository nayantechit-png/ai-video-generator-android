package com.aivideogen.di

import android.content.Context
import androidx.room.Room
import com.aivideogen.data.local.AppDatabase
import com.aivideogen.data.local.VideoProjectDao
import com.aivideogen.data.remote.OpenAIService
import com.aivideogen.data.remote.StabilityAIService
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
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ── OkHttp ────────────────────────────────

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)   // video generation can take a while
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    // ── Retrofit instances ────────────────────

    @Provides
    @Singleton
    @Named("stability")
    fun provideStabilityRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.stability.ai/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    @Named("openai")
    fun provideOpenAIRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    // ── API Services ──────────────────────────

    @Provides
    @Singleton
    fun provideStabilityAIService(@Named("stability") retrofit: Retrofit): StabilityAIService =
        retrofit.create(StabilityAIService::class.java)

    @Provides
    @Singleton
    fun provideOpenAIService(@Named("openai") retrofit: Retrofit): OpenAIService =
        retrofit.create(OpenAIService::class.java)

    // ── Room Database ─────────────────────────

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideVideoProjectDao(db: AppDatabase): VideoProjectDao = db.videoProjectDao()
}
