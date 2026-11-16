package com.srmap.campusnavigator.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srmap.campusnavigator.data.repository.SearchRepository
import com.srmap.campusnavigator.domain.models.UnifiedSearchResult
import com.srmap.campusnavigator.presentation.components.SearchCategoryFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val selectedFilter: SearchCategoryFilter = SearchCategoryFilter.ALL,
    val searchResults: List<UnifiedSearchResult> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val isLoading: Boolean = false
)

class SearchViewModel(
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadRecentSearches()
        executeSearch("", SearchCategoryFilter.ALL)
    }

    fun onQueryChange(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery)
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            delay(200L) // Debounce
            executeSearch(newQuery, _uiState.value.selectedFilter)
        }
    }

    fun onFilterChange(filter: SearchCategoryFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
        executeSearch(_uiState.value.query, filter)
    }

    private fun executeSearch(query: String, filter: SearchCategoryFilter) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val results = searchRepository.searchCampus(query, filter)
            _uiState.value = _uiState.value.copy(
                searchResults = results,
                isLoading = false
            )
        }
    }

    fun addRecentSearch(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            searchRepository.addRecentSearch(query)
            loadRecentSearches()
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch(Dispatchers.IO) {
            searchRepository.clearRecentSearches()
            _uiState.value = _uiState.value.copy(recentSearches = emptyList())
        }
    }

    private fun loadRecentSearches() {
        viewModelScope.launch(Dispatchers.IO) {
            val recents = searchRepository.getRecentSearches()
            _uiState.value = _uiState.value.copy(recentSearches = recents)
        }
    }
}
