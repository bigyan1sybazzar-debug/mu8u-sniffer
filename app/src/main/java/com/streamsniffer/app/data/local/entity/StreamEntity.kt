package com.streamsniffer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streams")
data class StreamEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val title: String,
    val sourceUrl: String,       // The webpage where the stream was found
    val quality: String = "Auto", // e.g., "1080p", "720p", "Auto"
    val isIPTV: Boolean = false,
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val thumbnailUrl: String? = null,
    val durationMs: Long? = null,
    val groupTitle: String? = null  // For IPTV playlist grouping
)
