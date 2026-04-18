package com.example.flightsearch.di

import android.content.Context
import androidx.room.Room
import com.example.flightsearch.data.FlightDao
import com.example.flightsearch.data.FlightDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): FlightDatabase {
        return Room.databaseBuilder(
            context,
            FlightDatabase::class.java,
            "flight_database"
        ).createFromAsset("database/flight_search.db").build()
    }

    @Provides
    fun provideFlightDao(database: FlightDatabase): FlightDao {
        return database.flightDao()
    }
}
