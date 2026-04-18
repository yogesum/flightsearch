package com.example.flightsearch.ui


import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flightsearch.data.Airport
import com.example.flightsearch.data.Favorite
import com.example.flightsearch.data.FlightRepository
import com.example.flightsearch.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FlightSearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: FlightRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    private val _departureCode = savedStateHandle.getMutableStateFlow("departureCode", "")
    val departureCode: StateFlow<String> = _departureCode.asStateFlow()
    private var searchJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    var searchQuery: StateFlow<String> = userPreferencesRepository.searchQuery
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val favorites: StateFlow<List<Favorite>> = repository.getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<UiState> = combine(searchQuery, departureCode) { query, code ->
        listOf(query, code)
    }.flatMapLatest { (query, code) ->
        when {
            code.isNotBlank() -> combine(
                repository.getAllFavoritesByDepartureCode(code),
                repository.getAllAirports()
            ) { favorites, airports ->
                val departure = airports.firstOrNull { it.code == code } ?: Airport()
                airports.filter { it.code != code }.map { destination ->
                    val id = favorites.firstOrNull {
                        it.departureCode == code && it.destinationCode == destination.code
                    }?.id ?: 0
                    FlightRoute(id, departure, destination)
                }.let { UiState.Routes(departure, it) }
            }

            query.isBlank() -> combine(
                favorites,
                repository.getAllAirports()
            ) { favorites, airports ->
                favorites.map { favorite ->
                    FlightRoute(
                        id = favorite.id,
                        departure = airports.firstOrNull { it.code == favorite.departureCode }
                            ?: Airport(),
                        destination = airports.firstOrNull { it.code == favorite.destinationCode }
                            ?: Airport(),
                    )
                }.let { UiState.Favorites(it) }
            }

            else -> repository.searchFlights(query).map { UiState.Airports(it) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun updateSearchQuery(query: String, clearDeparture: Boolean = true) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300L)
            userPreferencesRepository.saveSearchQuery(query)
        }
    }

    fun setDeparture(code: String) {
        _departureCode.apply { value = code }
    }

    fun resetDeparture() = setDeparture("")

    fun toggleFavorite(route: FlightRoute) {
        viewModelScope.launch {
            repository.toggleFavorite(route.toFavorite())
        }
    }
}

sealed interface UiState {

    object Loading : UiState
    data class Favorites(val routes: List<FlightRoute>) : UiState
    data class Airports(val airports: List<Airport>) : UiState
    data class Routes(val departure: Airport, val routes: List<FlightRoute>) : UiState
}

data class FlightRoute(
    val id: Int = 0,
    val departure: Airport,
    val destination: Airport,
)

fun FlightRoute.toFavorite() = Favorite(id, departure.code, destination.code)
