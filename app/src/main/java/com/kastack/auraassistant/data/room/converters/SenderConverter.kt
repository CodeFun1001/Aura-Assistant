package com.kastack.auraassistant.data.room.converters

import androidx.room.TypeConverter
import com.kastack.auraassistant.domain.models.Sender

class SenderConverter {
    @TypeConverter
    fun fromSender(sender: Sender): String = sender.name

    @TypeConverter
    fun toSender(value: String): Sender = Sender.valueOf(value)
}