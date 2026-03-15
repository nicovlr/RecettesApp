package com.example.recettes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recettes.ui.screens.DetailScreen
import com.example.recettes.ui.screens.ListScreen
import com.example.recettes.ui.screens.SplashScreen
import com.example.recettes.viewmodel.MealViewModel
import com.example.recettes.viewmodel.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface { App() }
            }
        }
    }
}

@Composable
fun App(vm: MealViewModel = viewModel()) {
    val screen by vm.screen.collectAsState()
    val meals by vm.meals.collectAsState()
    val displayCount by vm.displayCount.collectAsState()
    val categories by vm.categories.collectAsState()
    val selectedCat by vm.selectedCat.collectAsState()
    val query by vm.query.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val detail by vm.detail.collectAsState()
    val detailIngredients by vm.detailIngredients.collectAsState()
    val detailLoading by vm.detailLoading.collectAsState()
    val detailError by vm.detailError.collectAsState()

    LaunchedEffect(Unit) { vm.loadInitial() }

    // on verifie le cache toutes les 5min
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(5 * 60 * 1000L)
            vm.checkCache()
        }
    }

    when (screen) {
        is Screen.Splash -> SplashScreen()
        is Screen.List -> ListScreen(
            meals = meals, displayCount = displayCount,
            categories = categories, selectedCat = selectedCat,
            query = query, loading = loading, error = error,
            onSearch = vm::search, onCategoryClick = vm::selectCategory,
            onMealClick = vm::openDetail, onLoadMore = vm::loadMore,
            onRetry = vm::retry
        )
        is Screen.Detail -> {
            BackHandler { vm.goBack() }
            DetailScreen(
                meal = detail, ingredients = detailIngredients,
                loading = detailLoading, error = detailError,
                onBack = vm::goBack, onRetry = vm::retry
            )
        }
    }
}
