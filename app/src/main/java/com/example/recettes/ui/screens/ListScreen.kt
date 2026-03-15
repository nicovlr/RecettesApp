package com.example.recettes.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.recettes.data.db.CategoryEntity
import com.example.recettes.data.db.MealEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    meals: List<MealEntity>,
    displayCount: Int,
    categories: List<CategoryEntity>,
    selectedCat: String?,
    query: String,
    loading: Boolean,
    error: String?,
    onSearch: (String) -> Unit,
    onCategoryClick: (String?) -> Unit,
    onMealClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit
) {
    val visible = meals.take(displayCount)
    val listState = rememberLazyListState()

    // quand on arrive en bas on charge la suite
    val atBottom by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= visible.size - 2 && displayCount < meals.size
        }
    }
    LaunchedEffect(atBottom) { if (atBottom) onLoadMore() }

    Column(Modifier.fillMaxSize()) {
        // barre de recherche
        OutlinedTextField(
            value = query,
            onValueChange = onSearch,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // categories
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCat == cat.strCategory,
                    onClick = { onCategoryClick(cat.strCategory) },
                    label = { Text(cat.strCategory) }
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        if (error != null && meals.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRetry) { Text("Réessayer") }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(visible, key = { it.idMeal }) { meal ->
                    MealCard(meal) { onMealClick(meal.idMeal) }
                }
                if (loading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealCard(meal: MealEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            AsyncImage(
                model = meal.strMealThumb,
                contentDescription = meal.strMeal,
                modifier = Modifier.fillMaxWidth().height(180.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                meal.strMeal,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
