package com.streamsniffer.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamsniffer.app.data.repository.StreamRepository
import com.streamsniffer.app.domain.model.Stream
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HistoryTab { ALL, FAVORITES, IPTV }

data class HistoryUiState(
    val streams: List<Stream> = emptyList(),
    val isLoading: Boolean = true,
    val selectedTab: HistoryTab = HistoryTab.ALL,
    val searchQuery: String = "",
    val showClearDialog: Boolean = false
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: StreamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadStreams()
    }

    private fun loadStreams() {
        viewModelScope.launch {
            combine(
                repository.getAllStreams(),
                _uiState.map { it.selectedTab }.distinctUntilChanged(),
                _uiState.map { it.searchQuery }.distinctUntilChanged()
            ) { allStreams, tab, query ->
                Triple(allStreams, tab, query)
            }.collect { (allStreams, tab, query) ->
                val filtered = when (tab) {
                    HistoryTab.ALL -> allStreams.filter { !it.isIPTV }
                    HistoryTab.FAVORITES -> allStreams.filter { it.isFavorite }
                    HistoryTab.IPTV -> allStreams.filter { it.isIPTV }
                }.let { streams ->
                    if (query.isBlank()) streams
                    else streams.filter {
                        it.title.contains(query, ignoreCase = true) ||
                        it.url.contains(query, ignoreCase = true) ||
                        (it.groupTitle?.contains(query, ignoreCase = true) == true)
                    }
                }
                _uiState.update { it.copy(streams = filtered, isLoading = false) }
            }
        }
    }

    fun setTab(tab: HistoryTab) = _uiState.update { it.copy(selectedTab = tab) }
    fun setSearch(query: String) = _uiState.update { it.copy(searchQuery = query) }
    fun showClearDialog() = _uiState.update { it.copy(showClearDialog = true) }
    fun dismissClearDialog() = _uiState.update { it.copy(showClearDialog = false) }

    fun toggleFavorite(stream: Stream) {
        viewModelScope.launch {
            repository.toggleFavorite(stream.id, !stream.isFavorite)
        }
    }

    fun deleteStream(stream: Stream) {
        viewModelScope.launch { repository.deleteStream(stream.id) }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _uiState.update { it.copy(showClearDialog = false) }
        }
    }
}
