package com.bignerdranch.android.criminalintent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class CrimeDetailViewModel(crimeId: UUID) : ViewModel() {

    private val crimeRepository = CrimeRepository.get()

    private val _crime: MutableStateFlow<Crime?> = MutableStateFlow(null)
    val crime: StateFlow<Crime?> = _crime.asStateFlow()

    init {
        viewModelScope.launch {
            _crime.value = crimeRepository.getCrime(crimeId).first()
        }
    }

    // Update crime information and save it to the database
    fun updateCrime(onUpdate: (Crime) -> Crime) {
        _crime.update { oldCrime ->
            oldCrime?.let { updatedCrime ->
                val newCrime = onUpdate(updatedCrime)
                // Save updated crime to the database
                viewModelScope.launch {
                    crimeRepository.updateCrime(newCrime)
                }
                newCrime
            }
        }
    }

    // Delete crime from the database
    fun deleteCrime(crime: Crime) {
        viewModelScope.launch {
            crimeRepository.deleteCrime(crime) // Delete the crime from the database
        }
    }

    override fun onCleared() {
        super.onCleared()
        _crime.value?.let { crime ->
            viewModelScope.launch {
                crimeRepository.updateCrime(crime)
            }
        }
    }
}

class CrimeDetailViewModelFactory(
    private val crimeId: UUID
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CrimeDetailViewModel(crimeId) as T
    }
}
