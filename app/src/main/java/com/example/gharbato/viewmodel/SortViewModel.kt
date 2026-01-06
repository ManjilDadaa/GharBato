package com.example.gharbato.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gharbato.data.model.PropertyModel
import com.example.gharbato.data.model.SortOption
import com.example.gharbato.data.model.SortState
import com.example.gharbato.data.repository.SortRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "SortViewModel"

/**
 * ViewModel for managing sorting functionality
 * Follows MVVM architecture pattern
 */
class SortViewModel(
    private val sortRepository: SortRepository
) : ViewModel() {

    private val _sortState = MutableStateFlow(SortState())
    val sortState: StateFlow<SortState> = _sortState.asStateFlow()

    init {
        loadSortPreference()
        loadAvailableSorts()
    }

    /**
     * Load saved sort preference from repository
     */
    private fun loadSortPreference() {
        viewModelScope.launch {
            try {
                sortRepository.getSortPreference().collect { preferences ->
                    _sortState.value = _sortState.value.copy(
                        selectedSort = preferences.currentSort
                    )
                    Log.d(TAG, "Sort preference loaded: ${preferences.currentSort}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading sort preference", e)
            }
        }
    }

    /**
     * Load available sort options
     */
    private fun loadAvailableSorts() {
        val availableSorts = sortRepository.getAvailableSortOptions()
        _sortState.value = _sortState.value.copy(availableSorts = availableSorts)
        Log.d(TAG, "Loaded ${availableSorts.size} sort options")
    }

    /**
     * Apply a sort option
     * @param sortOption The sort option to apply
     * @param properties The properties to sort
     * @return Sorted list of properties
     */
    suspend fun applySortToProperties(
        sortOption: SortOption,
        properties: List<PropertyModel>
    ): List<PropertyModel> {
        Log.d(TAG, "Applying sort: $sortOption to ${properties.size} properties")

        _sortState.value = _sortState.value.copy(isLoading = true)

        return try {
            val sortedProperties = sortRepository.sortProperties(properties, sortOption)

            // Update selected sort
            _sortState.value = _sortState.value.copy(
                selectedSort = sortOption,
                isLoading = false
            )

            // Save preference
            saveSortPreference(sortOption)

            Log.d(TAG, "Sort applied successfully")
            sortedProperties
        } catch (e: Exception) {
            Log.e(TAG, "Error applying sort", e)
            _sortState.value = _sortState.value.copy(isLoading = false)
            properties // Return original list on error
        }
    }

    /**
     * Update the selected sort option
     * @param sortOption The new sort option
     */
    fun updateSelectedSort(sortOption: SortOption) {
        _sortState.value = _sortState.value.copy(selectedSort = sortOption)
        saveSortPreference(sortOption)
        Log.d(TAG, "Sort option updated: $sortOption")
    }

    /**
     * Save sort preference
     */
    private fun saveSortPreference(sortOption: SortOption) {
        viewModelScope.launch {
            try {
                sortRepository.saveSortPreference(sortOption)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving sort preference", e)
            }
        }
    }

    /**
     * Reset sort to default
     */
    fun resetSort() {
        viewModelScope.launch {
            try {
                sortRepository.resetSortPreference()
                _sortState.value = _sortState.value.copy(
                    selectedSort = SortOption.DATE_NEWEST
                )
                Log.d(TAG, "Sort reset to default")
            } catch (e: Exception) {
                Log.e(TAG, "Error resetting sort", e)
            }
        }
    }

    /**
     * Get current sort option
     */
    fun getCurrentSort(): SortOption {
        return _sortState.value.selectedSort
    }

    /**
     * Get all available sort options
     */
    fun getAvailableSorts(): List<SortOption> {
        return _sortState.value.availableSorts
    }
}

/**
 * Factory for creating SortViewModel
 */
class SortViewModelFactory(
    private val sortRepository: SortRepository
) : androidx.lifecycle.ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SortViewModel::class.java)) {
            return SortViewModel(sortRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}