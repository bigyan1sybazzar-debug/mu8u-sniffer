package com.streamsniffer.app.data.repository

import com.streamsniffer.app.data.local.dao.StreamDao
import com.streamsniffer.app.data.local.entity.StreamEntity
import com.streamsniffer.app.domain.model.Stream
import com.streamsniffer.app.domain.model.toStream
import com.streamsniffer.app.domain.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamRepository @Inject constructor(
    private val dao: StreamDao
) {
    fun getAllStreams(): Flow<List<Stream>> =
        dao.getAllStreams().map { list -> list.map { it.toStream() } }

    fun getFavoriteStreams(): Flow<List<Stream>> =
        dao.getFavoriteStreams().map { list -> list.map { it.toStream() } }

    fun getIPTVStreams(): Flow<List<Stream>> =
        dao.getIPTVStreams().map { list -> list.map { it.toStream() } }

    fun searchStreams(query: String): Flow<List<Stream>> =
        dao.searchStreams(query).map { list -> list.map { it.toStream() } }

    fun getHistoryCount(): Flow<Int> = dao.getHistoryCount()

    suspend fun saveStream(stream: Stream): Long =
        dao.insertStream(stream.toEntity())

    suspend fun saveStreams(streams: List<Stream>) =
        dao.insertStreams(streams.map { it.toEntity() })

    suspend fun getStreamByUrl(url: String): Stream? =
        dao.getStreamByUrl(url)?.toStream()

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) =
        dao.toggleFavorite(id, isFavorite)

    suspend fun deleteStream(id: Long) =
        dao.deleteStreamById(id)

    suspend fun clearHistory() = dao.clearHistory()

    suspend fun clearIPTVPlaylist() = dao.clearIPTVPlaylist()
}
