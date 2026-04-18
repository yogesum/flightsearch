package com.example.flightsearch.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlightRepository @Inject constructor(
    private val flightDao: FlightDao
) {
    fun searchFlights(query: String): Flow<List<Airport>> =
        flightDao.searchAirports(
            code = if (query.length < 3) "$query%" else query.substring(0..2),
            name = "%$query%"
        )

    fun getAirportsByCodes(codes: List<String>): Flow<List<Airport>> =
        flightDao.getAirportsByCodes(codes)

    fun getAllAirports(): Flow<List<Airport>> =
        flightDao.getAllAirports()

    fun getAllFavorites(): Flow<List<Favorite>> =
        flightDao.getAllFavorites()

    fun getAllFavoritesByDepartureCode(departureCode: String): Flow<List<Favorite>> =
        flightDao.getAllFavoritesByDepartureCode(departureCode)

    suspend fun toggleFavorite(favorite: Favorite) {
        if (favorite.id != 0) {
            flightDao.deleteFavorite(favorite)
        } else {
            flightDao.insertFavorite(favorite)
        }
    }
}
