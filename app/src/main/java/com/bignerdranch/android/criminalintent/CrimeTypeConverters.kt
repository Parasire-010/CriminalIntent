package com.bignerdranch.android.criminalintent.database

import android.util.Log
import androidx.room.TypeConverter
import java.nio.ByteBuffer
import java.util.Date
import java.util.UUID

class CrimeTypeConverters {
    @TypeConverter
    fun fromDate(date: Date): Long {
        return date.time
    }

    @TypeConverter
    fun toDate(millisSinceEpoch: Long): Date {
        return Date(millisSinceEpoch)
    }

    @TypeConverter
    fun fromUUID(uuid: UUID): ByteArray {
        val byteBuffer = ByteBuffer.wrap(ByteArray(16))
        byteBuffer.putLong(uuid.mostSignificantBits)
        byteBuffer.putLong(uuid.leastSignificantBits)
        return byteBuffer.array()
    }

    @TypeConverter
    fun toUUID(bytes: ByteArray): UUID {
        return try {
            val byteBuffer = ByteBuffer.wrap(bytes)
            val mostSigBits = byteBuffer.long
            val leastSigBits = byteBuffer.long
            UUID(mostSigBits, leastSigBits)
        } catch (e: Exception) {
            Log.e("CrimeTypeConverters", "Invalid UUID blob: ${bytes.contentToString()}. Generating a new UUID.")
            UUID.randomUUID() // Fallback to a new UUID if invalid
        }
    }
}
