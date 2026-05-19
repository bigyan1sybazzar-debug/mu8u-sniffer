package com.streamsniffer.app.domain.sniffer

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

data class DetectedStream(
    val url: String,
    val pageUrl: String,
    val title: String = extractTitle(url),
    val quality: String = detectQuality(url)
)

private fun extractTitle(url: String): String {
    return try {
        val segment = url.substringAfterLast("/").substringBefore("?")
        if (segment.isNotBlank() && segment.length > 3)
            segment.removeSuffix(".m3u8").replace("_", " ").replace("-", " ")
                .replaceFirstChar { it.uppercase() }
        else "HLS Stream"
    } catch (e: Exception) {
        "HLS Stream"
    }
}

private fun detectQuality(url: String): String {
    return when {
        url.contains("1080", ignoreCase = true) -> "1080p"
        url.contains("720", ignoreCase = true) -> "720p"
        url.contains("480", ignoreCase = true) -> "480p"
        url.contains("360", ignoreCase = true) -> "360p"
        url.contains("240", ignoreCase = true) -> "240p"
        url.contains("4k", ignoreCase = true) || url.contains("2160", ignoreCase = true) -> "4K"
        else -> "Auto"
    }
}

@Singleton
class HlsSniffer @Inject constructor() {

    private val _detectedStreams = Channel<DetectedStream>(Channel.UNLIMITED)
    val detectedStreams: Flow<DetectedStream> = _detectedStreams.receiveAsFlow()

    // Patterns that indicate an HLS stream URL
    private val HLS_PATTERNS = listOf(
        ".m3u8",
        "application/x-mpegurl",
        "application/vnd.apple.mpegurl",
        "x-mpegurl"
    )

    // Patterns to explicitly exclude (avoid false positives)
    private val EXCLUDE_PATTERNS = listOf(
        ".png", ".jpg", ".jpeg", ".gif", ".css", ".js", ".woff", ".ico", ".svg"
    )

    /**
     * Called from the WebViewClient to intercept every network request.
     * Returns null to allow the request to continue normally.
     */
    fun interceptRequest(request: WebResourceRequest, pageUrl: String): WebResourceResponse? {
        val url = request.url.toString().lowercase()

        if (EXCLUDE_PATTERNS.any { url.endsWith(it) }) return null

        val isHls = HLS_PATTERNS.any { url.contains(it, ignoreCase = true) }
        val contentType = request.requestHeaders["Accept"] ?: ""
        val isHlsContentType = contentType.contains("mpegurl", ignoreCase = true)

        if (isHls || isHlsContentType) {
            val detected = DetectedStream(
                url = request.url.toString(),
                pageUrl = pageUrl
            )
            _detectedStreams.trySend(detected)
        }

        return null // Always return null to let the request proceed
    }

    /**
     * Scan a raw HTML/JS string for embedded m3u8 URLs (e.g., in page source)
     */
    fun scanPageContent(content: String, pageUrl: String) {
        val regex = Regex("""https?://[^\s"'<>]+\.m3u8[^\s"'<>]*""", RegexOption.IGNORE_CASE)
        val matches = regex.findAll(content)
        matches.forEach { match ->
            val detected = DetectedStream(url = match.value, pageUrl = pageUrl)
            _detectedStreams.trySend(detected)
        }
    }
}
