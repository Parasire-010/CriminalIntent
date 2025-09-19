package com.bignerdranch.android.criminalintent.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import com.bignerdranch.android.criminalintent.Crime
import java.util.*

@Dao
interface CrimeDao {
    @Query("SELECT * FROM Crime")
    fun getCrimes(): Flow<List<Crime>>

    @Query("SELECT * FROM Crime WHERE id=(:id)")
    fun getCrime(id: UUID): Flow<Crime>

    @Insert
    suspend fun addCrime(crime: Crime) // Changed from insertCrime to addCrime

    @Update
    suspend fun updateCrime(crime: Crime)

    @Delete
    suspend fun deleteCrime(crime: Crime) // Add this method to delete a crime from the database
}
