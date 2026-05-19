package com.streamsniffer.app.data.local.dao

import androidx.room.*
import com.streamsniffer.app.data.local.entity.StreamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StreamDao {

    @Query("SELECT * FROM streams ORDER BY timestamp DESC")
    fun getAllStreams(): Flow<List<StreamEntity>>

    @Query("SELECT * FROM streams WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteStreams(): Flow<List<StreamEntity>>

    @Query("SELECT * FROM streams WHERE isIPTV = 1 ORDER BY groupTitle ASC, title ASC")
    fun getIPTVStreams(): Flow<List<StreamEntity>>

    @Query("SELECT * FROM streams WHERE id = :id")
    suspend fun getStreamById(id: Long): StreamEntity?

    @Query("SELECT * FROM streams WHERE url = :url LIMIT 1")
    suspend fun getStreamByUrl(url: String): StreamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStream(stream: StreamEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreams(streams: List<StreamEntity>)

    @Update
    suspend fun updateStream(stream: StreamEntity)

    @Delete
    suspend fun deleteStream(stream: StreamEntity)

    @Query("DELETE FROM streams WHERE id = :id")
    suspend fun deleteStreamById(id: Long)

    @Query("DELETE FROM streams WHERE isIPTV = 0")
    suspend fun clearHistory()

    @Query("DELETE FROM streams WHERE isIPTV = 1")
    suspend fun clearIPTVPlaylist()

    @Query("UPDATE streams SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)

    @Query("SELECT * FROM streams WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchStreams(query: String): Flow<List<StreamEntity>>

    @Query("SELECT COUNT(*) FROM streams WHERE isIPTV = 0")
    fun getHistoryCount(): Flow<Int>
}
