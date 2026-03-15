package com.example.recettes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.recettes.data.db.MealEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    meal: MealEntity?,
    ingredients: List<Pair<String, String>>,
    loading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(meal?.strMeal ?: "Détail") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                }
            }
        )

        when {
            loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error != null && meal == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(error)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onRetry) { Text("Réessayer") }
                    }
                }
            }
            meal != null -> {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    AsyncImage(
                        model = meal.strMealThumb,
                        contentDescription = meal.strMeal,
                        modifier = Modifier.fillMaxWidth().height(250.dp),
                        contentScale = ContentScale.Crop
                    )
                    Column(Modifier.padding(16.dp)) {
                        Text(meal.strMeal, style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)

                        // date de derniere maj
                        val fmt = SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH)
                        Text("Updated ${fmt.format(Date(meal.lastUpdated))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(Modifier.height(16.dp))

                        ingredients.forEach { (ing, mesure) ->
                            Text("$mesure $ing".trim(),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 1.dp))
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}
