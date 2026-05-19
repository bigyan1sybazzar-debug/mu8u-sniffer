package com.streamsniffer.app.ui.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.datasource.DefaultHttpDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val streamUrl: String = "",
    val streamTitle: String = "",
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isFullscreen: Boolean = false,
    val error: String? = null,
    val currentQuality: String = "Auto",
    val availableQualities: List<String> = emptyList()
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    var exoPlayer: ExoPlayer? = null
        private set

    fun initPlayer(streamUrl: String, title: String) {
        _uiState.update { it.copy(streamUrl = streamUrl, streamTitle = title) }

        val player = ExoPlayer.Builder(context).build().also {
            exoPlayer = it
        }

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                _uiState.update {
                    it.copy(
                        isBuffering = state == Player.STATE_BUFFERING,
                        isPlaying = player.isPlaying
                    )
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _uiState.update { it.copy(error = "Playback error: ${error.message}") }
            }
        })

        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)

        val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(streamUrl))

        player.setMediaSource(mediaSource)
        player.prepare()
        player.playWhenReady = true
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) player.pause() else player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    fun toggleFullscreen() {
        _uiState.update { it.copy(isFullscreen = !it.isFullscreen) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer?.release()
        exoPlayer = null
    }
}
