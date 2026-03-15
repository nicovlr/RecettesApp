package com.example.recettes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.recettes.data.db.CategoryEntity
import com.example.recettes.data.db.MealDatabase
import com.example.recettes.data.db.MealEntity
import com.example.recettes.data.repository.MealRepository
import com.example.recettes.data.repository.parseIngredients
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class Screen {
    data object Splash : Screen()
    data object List : Screen()
    data class Detail(val mealId: String) : Screen()
}

class MealViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = MealRepository(MealDatabase.get(app).mealDao())

    private val _screen = MutableStateFlow<Screen>(Screen.Splash)
    val screen: StateFlow<Screen> = _screen

    private val _meals = MutableStateFlow<List<MealEntity>>(emptyList())
    val meals: StateFlow<List<MealEntity>> = _meals

    private val _displayCount = MutableStateFlow(30)
    val displayCount: StateFlow<Int> = _displayCount

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories

    private val _selectedCat = MutableStateFlow<String?>(null)
    val selectedCat: StateFlow<String?> = _selectedCat

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _detail = MutableStateFlow<MealEntity?>(null)
    val detail: StateFlow<MealEntity?> = _detail

    private val _detailIngredients = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val detailIngredients: StateFlow<List<Pair<String, String>>> = _detailIngredients

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _detailLoading = MutableStateFlow(false)
    val detailLoading: StateFlow<Boolean> = _detailLoading

    private val _detailError = MutableStateFlow<String?>(null)
    val detailError: StateFlow<String?> = _detailError

    fun loadInitial() {
        viewModelScope.launch {
            _loading.value = true
            val catJob = launch { repo.getCategories().onSuccess { _categories.value = it } }
            val result = repo.searchMeals("")
            catJob.join()
            result.onSuccess {
                _meals.value = it
                _error.value = null
            }.onFailure {
                _error.value = "Impossible de charger les recettes"
            }
            _loading.value = false
            _screen.value = Screen.List
        }
    }

    fun search(q: String) {
        _query.value = q
        _selectedCat.value = null
        _displayCount.value = 30
        fetchMeals()
    }

    fun selectCategory(cat: String?) {
        _selectedCat.value = if (_selectedCat.value == cat) null else cat
        _query.value = ""
        _displayCount.value = 30
        fetchMeals()
    }

    fun fetchMeals() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            val result = when {
                _selectedCat.value != null -> repo.filterByCategory(_selectedCat.value!!)
                _query.value.isNotBlank() -> repo.searchMeals(_query.value)
                else -> repo.searchMeals("")
            }
            result.onSuccess { _meals.value = it }
                .onFailure { _error.value = "Erreur réseau, vérifiez votre connexion." }
            _loading.value = false
        }
    }

    fun loadMore() {
        _displayCount.value = (_displayCount.value + 30).coerceAtMost(_meals.value.size)
    }

    fun openDetail(id: String) {
        _screen.value = Screen.Detail(id)
        _detail.value = null
        _detailIngredients.value = emptyList()
        _detailError.value = null
        viewModelScope.launch {
            _detailLoading.value = true
            repo.getMealDetail(id).onSuccess {
                _detail.value = it
                _detailIngredients.value = it.parseIngredients()
            }.onFailure {
                _detailError.value = "Impossible de charger les détails"
            }
            _detailLoading.value = false
        }
    }

    fun goBack() { _screen.value = Screen.List }

    fun retry() {
        when (_screen.value) {
            is Screen.Detail -> openDetail((_screen.value as Screen.Detail).mealId)
            is Screen.List -> fetchMeals()
            else -> loadInitial()
        }
    }

    fun checkCache() {
        viewModelScope.launch { repo.refreshIfNeeded() }
    }
}
