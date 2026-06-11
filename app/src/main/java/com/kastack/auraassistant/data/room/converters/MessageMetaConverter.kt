package com.kastack.auraassistant.data.room.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.kastack.auraassistant.domain.models.MessageMeta

class MessageMetaConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromMessageMeta(meta: MessageMeta): String = gson.toJson(meta)

    @TypeConverter
    fun toMessageMeta(json: String): MessageMeta =
        gson.fromJson(json, MessageMeta::class.java)
}