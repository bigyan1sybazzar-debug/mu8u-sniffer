package com.streamsniffer.app.domain.model

import com.streamsniffer.app.data.local.entity.StreamEntity

data class Stream(
    val id: Long = 0,
    val url: String,
    val title: String,
    val sourceUrl: String = "",
    val quality: String = "Auto",
    val isIPTV: Boolean = false,
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val thumbnailUrl: String? = null,
    val durationMs: Long? = null,
    val groupTitle: String? = null
)

fun StreamEntity.toStream() = Stream(
    id = id,
    url = url,
    title = title,
    sourceUrl = sourceUrl,
    quality = quality,
    isIPTV = isIPTV,
    isFavorite = isFavorite,
    timestamp = timestamp,
    thumbnailUrl = thumbnailUrl,
    durationMs = durationMs,
    groupTitle = groupTitle
)

fun Stream.toEntity() = StreamEntity(
    id = id,
    url = url,
    title = title,
    sourceUrl = sourceUrl,
    quality = quality,
    isIPTV = isIPTV,
    isFavorite = isFavorite,
    timestamp = timestamp,
    thumbnailUrl = thumbnailUrl,
    durationMs = durationMs,
    groupTitle = groupTitle
)
