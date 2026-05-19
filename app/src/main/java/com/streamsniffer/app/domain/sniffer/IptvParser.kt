package com.streamsniffer.app.domain.sniffer

import com.streamsniffer.app.domain.model.Stream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses M3U/M3U8 IPTV playlists into a list of Stream objects.
 *
 * Supports:
 * - #EXTM3U header
 * - #EXTINF with tvg-id, tvg-name, tvg-logo, group-title attributes
 * - Stream URLs (http/https/rtmp)
 */
@Singleton
class IptvParser @Inject constructor() {

    fun parse(content: String, sourceUrl: String = ""): List<Stream> {
        val streams = mutableListOf<Stream>()
        val lines = content.lines().map { it.trim() }

        if (!lines.firstOrNull()?.startsWith("#EXTM3U")!!) {
            // Try anyway — some playlists omit the header
        }

        var currentInfo: ExtInfInfo? = null

        for (line in lines) {
            when {
                line.startsWith("#EXTINF:") -> {
                    currentInfo = parseExtInf(line)
                }
                line.startsWith("http://") || line.startsWith("https://") || line.startsWith("rtmp://") -> {
                    val info = currentInfo ?: ExtInfInfo()
                    streams.add(
                        Stream(
                            url = line,
                            title = info.name.ifBlank { extractNameFromUrl(line) },
                            sourceUrl = sourceUrl,
                            quality = detectQualityFromName(info.name),
                            isIPTV = true,
                            thumbnailUrl = info.logoUrl,
                            groupTitle = info.groupTitle
                        )
                    )
                    currentInfo = null
                }
            }
        }

        return streams
    }

    private data class ExtInfInfo(
        val name: String = "",
        val logoUrl: String? = null,
        val groupTitle: String? = null,
        val tvgId: String? = null
    )

    private fun parseExtInf(line: String): ExtInfInfo {
        // #EXTINF:-1 tvg-id="..." tvg-name="..." tvg-logo="..." group-title="...",Channel Name
        val tvgId = extractAttribute(line, "tvg-id")
        val tvgName = extractAttribute(line, "tvg-name")
        val tvgLogo = extractAttribute(line, "tvg-logo")
        val groupTitle = extractAttribute(line, "group-title")
        val name = tvgName ?: line.substringAfterLast(",").trim()

        return ExtInfInfo(
            name = name,
            logoUrl = tvgLogo,
            groupTitle = groupTitle,
            tvgId = tvgId
        )
    }

    private fun extractAttribute(line: String, attr: String): String? {
        val pattern = """$attr="([^"]*)"""".toRegex()
        return pattern.find(line)?.groupValues?.get(1)
    }

    private fun extractNameFromUrl(url: String): String {
        return url.substringAfterLast("/").substringBefore("?")
            .removeSuffix(".m3u8")
            .replace("_", " ")
            .replace("-", " ")
            .replaceFirstChar { it.uppercase() }
            .ifBlank { "IPTV Channel" }
    }

    private fun detectQualityFromName(name: String): String = when {
        name.contains("4K", ignoreCase = true) -> "4K"
        name.contains("1080", ignoreCase = true) || name.contains("FHD", ignoreCase = true) -> "1080p"
        name.contains("720", ignoreCase = true) || name.contains("HD", ignoreCase = true) -> "720p"
        name.contains("480", ignoreCase = true) || name.contains("SD", ignoreCase = true) -> "480p"
        else -> "Auto"
    }
}
