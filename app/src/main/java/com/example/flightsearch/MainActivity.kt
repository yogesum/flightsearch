package com.example.flightsearch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flightsearch.data.Airport
import com.example.flightsearch.ui.FlightRoute
import com.example.flightsearch.ui.FlightSearchViewModel
import com.example.flightsearch.ui.UiState
import com.example.flightsearch.ui.theme.FlightSearchTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlightSearchTheme {
                FlightSearchScreen()
            }
        }
    }
}

@OptIn(FlowPreview::class, ExperimentalMaterial3Api::class)
@Composable
fun FlightSearchScreen(
    viewModel: FlightSearchViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState(initial = "")
    var query by remember(searchQuery) { mutableStateOf(searchQuery) }
    val focusManager = LocalFocusManager.current
    val code by viewModel.departureCode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Flights",
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        BackHandler(enabled = code.isNotBlank()) {
            query = searchQuery
            viewModel.resetDeparture()
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    viewModel.resetDeparture()
                    query = it
                    coroutineScope.launch { viewModel.updateSearchQuery(it) }
                },
                label = { Text("Search Flights") },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                is UiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                is UiState.Favorites -> FlightRouteList(
                    headline = "Favourite Routes",
                    routes = state.routes,
                    onToggleFavorite = { viewModel.toggleFavorite(it) }
                )
                is UiState.Airports -> SearchList(
                    airports = state.airports,
                    onSelectItem = {
                        viewModel.setDeparture(it)
                        query = it
                        focusManager.clearFocus()
                    }
                )
                is UiState.Routes -> FlightRouteList(
                    headline = "Flights from $code",
                    routes = state.routes,
                    onToggleFavorite = { viewModel.toggleFavorite(it) }
                )
            }
        }
    }
}

@Composable
fun SearchList(
    airports: List<Airport>,
    onSelectItem: (code: String) -> Unit = {}
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(airports, key = { it.id }) {
            it.apply { SearchItem(code, name, onSelectItem) }
            HorizontalDivider(thickness = 0.dp)
        }
    }
}

@Composable
fun FlightRouteList(
    routes: List<FlightRoute>,
    headline: String = "",
    onToggleFavorite: (FlightRoute) -> Unit = {}
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "headline") {
            Text(
                text = headline,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(routes, key = { "${it.departure.code}-${it.destination.code}" }) { route ->
            FlightRouteItem(
                route = route,
                onFavoriteClick = { onToggleFavorite(route) }
            )
        }
    }
}

@Composable
fun FlightRouteItem(
    route: FlightRoute,
    onFavoriteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("DEPART", style = MaterialTheme.typography.bodySmall)
                }
                route.departure.apply { SearchItem(code, name) }

                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("ARRIVE", style = MaterialTheme.typography.bodySmall)
                }
                route.destination.apply { SearchItem(code, name) }
            }

            IconButton(onClick = onFavoriteClick) {
                Icon(
                    painter = painterResource(R.drawable.star_24),
                    contentDescription = if (route.id > 0) {
                        "Remove from favorites"
                    } else {
                        "Add to favorites"
                    },
                    tint = if (route.id > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    }
                )
            }
        }
    }
}

@Composable
fun SearchItem(
    code: String,
    name: String,
    onSelectItem: (code: String) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onSelectItem(code) }),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SearchListPreview() {
    FlightSearchTheme {
        val bom = Airport(1, "BOM", "Mumbai, India")
        val del = Airport(2, "DEL", "New Delhi, India")
        val lax = Airport(3, "LAX", "Los Angeles, USA")
        val dub = Airport(4, "DUB", "Dublin, Ireland")
        SearchList(listOf(bom, del, lax, dub))
    }
}

@Preview(showBackground = true)
@Composable
fun FlightRouteListPreview() {
    FlightSearchTheme {
        val bom = Airport(1, "BOM", "Mumbai, India")
        val del = Airport(2, "DEL", "New Delhi, India")
        val lax = Airport(3, "LAX", "Los Angeles, USA")
        val dub = Airport(4, "DUB", "Dublin, Ireland")

        listOf(
            FlightRoute(1, bom, del),
            FlightRoute(0, bom, lax),
            FlightRoute(3, bom, dub)
        ).let { FlightRouteList(it, "Flight Routes for BOM")  }
    }
}
