package com.aivideogen.data.local

import androidx.room.TypeConverter
import com.aivideogen.data.model.AIProvider
import com.aivideogen.data.model.GenerationStatus
import com.aivideogen.data.model.VideoResolution
import com.aivideogen.data.model.VideoStyle
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromVideoStyle(style: VideoStyle): String = style.name

    @TypeConverter
    fun toVideoStyle(name: String): VideoStyle =
        VideoStyle.values().find { it.name == name } ?: VideoStyle.NONE

    @TypeConverter
    fun fromVideoResolution(res: VideoResolution): String = res.name

    @TypeConverter
    fun toVideoResolution(name: String): VideoResolution =
        VideoResolution.values().find { it.name == name } ?: VideoResolution.HD_720

    @TypeConverter
    fun fromGenerationStatus(status: GenerationStatus): String = status.name

    @TypeConverter
    fun toGenerationStatus(name: String): GenerationStatus =
        GenerationStatus.values().find { it.name == name } ?: GenerationStatus.PENDING

    @TypeConverter
    fun fromAIProvider(provider: AIProvider): String = provider.name

    @TypeConverter
    fun toAIProvider(name: String): AIProvider =
        AIProvider.values().find { it.name == name } ?: AIProvider.STABILITY_AI
}
