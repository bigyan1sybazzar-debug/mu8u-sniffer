package com.streamsniffer.app.ui.browser

import android.webkit.WebResourceRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamsniffer.app.data.repository.StreamRepository
import com.streamsniffer.app.domain.model.Stream
import com.streamsniffer.app.domain.sniffer.DetectedStream
import com.streamsniffer.app.domain.sniffer.HlsSniffer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BrowserUiState(
    val currentUrl: String = "https://www.google.com",
    val isLoading: Boolean = false,
    val pageTitle: String = "",
    val detectedStreams: List<DetectedStream> = emptyList(),
    val showStreamPanel: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val hlsSniffer: HlsSniffer,
    private val repository: StreamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    private val _navigateTo = MutableSharedFlow<String>()
    val navigateTo: SharedFlow<String> = _navigateTo.asSharedFlow()

    init {
        // Collect newly detected streams
        viewModelScope.launch {
            hlsSniffer.detectedStreams.collect { detected ->
                val current = _uiState.value
                if (current.detectedStreams.none { it.url == detected.url }) {
                    _uiState.update {
                        it.copy(
                            detectedStreams = it.detectedStreams + detected,
                            showStreamPanel = true
                        )
                    }
                }
            }
        }
    }

    fun onUrlEntered(url: String) {
        val formattedUrl = when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            url.contains(".") -> "https://$url"
            else -> "https://www.google.com/search?q=${url.replace(" ", "+")}"
        }
        _uiState.update { it.copy(currentUrl = formattedUrl, detectedStreams = emptyList()) }
        viewModelScope.launch { _navigateTo.emit(formattedUrl) }
    }

    fun onPageStarted(url: String) {
        _uiState.update {
            it.copy(
                currentUrl = url,
                isLoading = true,
                detectedStreams = emptyList()
            )
        }
    }

    fun onPageFinished(url: String, title: String) {
        _uiState.update {
            it.copy(
                currentUrl = url,
                pageTitle = title,
                isLoading = false
            )
        }
    }

    fun onNavigationStateChanged(canGoBack: Boolean, canGoForward: Boolean) {
        _uiState.update { it.copy(canGoBack = canGoBack, canGoForward = canGoForward) }
    }

    fun interceptRequest(request: WebResourceRequest): android.webkit.WebResourceResponse? =
        hlsSniffer.interceptRequest(request, _uiState.value.currentUrl)

    fun scanPageSource(html: String) {
        hlsSniffer.scanPageContent(html, _uiState.value.currentUrl)
    }

    fun saveStream(detected: DetectedStream) {
        viewModelScope.launch {
            repository.saveStream(
                Stream(
                    url = detected.url,
                    title = detected.title,
                    sourceUrl = detected.pageUrl,
                    quality = detected.quality
                )
            )
        }
    }

    fun playStream(url: String) {
        viewModelScope.launch { _navigateTo.emit("play:$url") }
    }

    fun toggleStreamPanel() {
        _uiState.update { it.copy(showStreamPanel = !it.showStreamPanel) }
    }

    fun clearDetectedStreams() {
        _uiState.update { it.copy(detectedStreams = emptyList(), showStreamPanel = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
