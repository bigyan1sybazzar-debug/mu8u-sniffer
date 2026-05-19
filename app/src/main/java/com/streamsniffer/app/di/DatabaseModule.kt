package com.streamsniffer.app.di

import android.content.Context
import androidx.room.Room
import com.streamsniffer.app.data.local.dao.StreamDao
import com.streamsniffer.app.data.local.database.StreamSnifferDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): StreamSnifferDatabase =
        Room.databaseBuilder(
            context,
            StreamSnifferDatabase::class.java,
            StreamSnifferDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()

    @Provides
    @Singleton
    fun provideStreamDao(db: StreamSnifferDatabase): StreamDao = db.streamDao()
}
