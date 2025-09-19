package com.bignerdranch.android.criminalintent

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class CrimeRepository private constructor(
    context: Context,
    private val coroutineScope: CoroutineScope = GlobalScope
) {

    private val database: CrimeDatabase = CrimeDatabase.getDatabase(context)
    private val crimeDao = database.crimeDao()

    // Fetch all crimes
    fun getCrimes(): Flow<List<Crime>> = crimeDao.getCrimes()

    // Fetch a specific crime by its ID
    fun getCrime(id: UUID): Flow<Crime> = crimeDao.getCrime(id)

    // Add a new crime (suspending function)
    suspend fun addCrime(crime: Crime) = crimeDao.addCrime(crime)

    // Update an existing crime
    suspend fun updateCrime(crime: Crime) {
        crimeDao.updateCrime(crime) // Make this suspending to ensure the database is updated immediately
    }

    // Update a suspect for a specific crime
    suspend fun updateSuspect(crimeId: UUID, suspect: String) {
        val crime = crimeDao.getCrime(crimeId).first() // Use first() to fetch a single value from Flow
        crimeDao.updateCrime(crime.copy(suspect = suspect))
    }

    // Delete a crime
    suspend fun deleteCrime(crime: Crime) {
        crimeDao.deleteCrime(crime) // Ensure you have the @Delete annotation in the CrimeDao interface
    }

    companion object {
        @Volatile
        private var INSTANCE: CrimeRepository? = null

        // Initialize the repository
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = CrimeRepository(context)
                    }
                }
            }
        }

        // Get the repository instance
        fun get(): CrimeRepository {
            return INSTANCE
                ?: throw IllegalStateException("CrimeRepository must be initialized")
        }
    }
}
