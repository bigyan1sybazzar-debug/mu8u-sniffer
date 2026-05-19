package com.streamsniffer.app.ui.iptv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamsniffer.app.data.repository.StreamRepository
import com.streamsniffer.app.domain.model.Stream
import com.streamsniffer.app.domain.sniffer.IptvParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import javax.inject.Inject

data class IptvUiState(
    val channels: List<Stream> = emptyList(),
    val groupedChannels: Map<String, List<Stream>> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val playlistUrl: String = "",
    val searchQuery: String = "",
    val selectedGroup: String? = null
)

@HiltViewModel
class IptvViewModel @Inject constructor(
    private val repository: StreamRepository,
    private val iptvParser: IptvParser
) : ViewModel() {

    private val _uiState = MutableStateFlow(IptvUiState())
    val uiState: StateFlow<IptvUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getIPTVStreams().collect { streams ->
                val filtered = if (_uiState.value.searchQuery.isBlank()) streams
                else streams.filter {
                    it.title.contains(_uiState.value.searchQuery, ignoreCase = true) ||
                    (it.groupTitle?.contains(_uiState.value.searchQuery, ignoreCase = true) == true)
                }
                val grouped = filtered.groupBy { it.groupTitle ?: "Uncategorized" }
                _uiState.update { it.copy(channels = filtered, groupedChannels = grouped) }
            }
        }
    }

    fun loadPlaylist(url: String) {
        _uiState.update { it.copy(isLoading = true, error = null, playlistUrl = url) }
        viewModelScope.launch {
            try {
                val content = withContext(Dispatchers.IO) {
                    URL(url).readText()
                }
                val streams = iptvParser.parse(content, url)
                repository.clearIPTVPlaylist()
                repository.saveStreams(streams)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load playlist: ${e.message}") }
            }
        }
    }

    fun setSearch(query: String) = _uiState.update { it.copy(searchQuery = query) }
    fun selectGroup(group: String?) = _uiState.update { it.copy(selectedGroup = group) }
    fun clearError() = _uiState.update { it.copy(error = null) }
}
