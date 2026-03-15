package com.example.recettes.data.repository

import com.example.recettes.data.api.*
import com.example.recettes.data.db.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MealRepository(private val dao: MealDao) {

    private val api: MealApi = Retrofit.Builder()
        .baseUrl("https://www.themealdb.com/api/json/v1/1/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(MealApi::class.java)

    private val gson = Gson()
    private val CACHE_DURATION = 10 * 60 * 1000L // 10 min

    suspend fun searchMeals(query: String): Result<List<MealEntity>> {
        return try {
            val resp = api.searchMeals(query)
            val meals = resp.meals?.map { it.toEntity() } ?: emptyList()
            dao.upsertMeals(meals)
            Result.success(meals)
        } catch (e: Exception) {
            val cached = if (query.isBlank()) dao.getAllMeals() else dao.searchMeals(query)
            if (cached.isNotEmpty()) Result.success(cached)
            else Result.failure(e)
        }
    }

    suspend fun filterByCategory(category: String): Result<List<MealEntity>> {
        return try {
            val resp = api.filterByCategory(category)
            val meals = resp.meals?.map { it.toEntity(category) } ?: emptyList()
            dao.upsertMeals(meals)
            Result.success(meals)
        } catch (e: Exception) {
            val cached = dao.getMealsByCategory(category)
            if (cached.isNotEmpty()) Result.success(cached)
            else Result.failure(e)
        }
    }

    suspend fun getMealDetail(id: String): Result<MealEntity> {
        return try {
            val resp = api.getMealById(id)
            val detail = resp.meals?.firstOrNull()
                ?: return Result.failure(Exception("Recette non trouvée"))
            val entity = detail.toEntity()
            dao.upsertMeal(entity)
            Result.success(entity)
        } catch (e: Exception) {
            val cached = dao.getMealById(id)
            if (cached != null) Result.success(cached)
            else Result.failure(e)
        }
    }

    suspend fun getCategories(): Result<List<CategoryEntity>> {
        return try {
            val resp = api.getCategories()
            val cats = resp.categories?.map {
                CategoryEntity(it.idCategory, it.strCategory, it.strCategoryThumb)
            } ?: emptyList()
            dao.upsertCategories(cats)
            Result.success(cats)
        } catch (e: Exception) {
            val cached = dao.getAllCategories()
            if (cached.isNotEmpty()) Result.success(cached)
            else Result.failure(e)
        }
    }

    suspend fun refreshIfNeeded() {
        val oldest = dao.getOldestUpdate() ?: return
        if (System.currentTimeMillis() - oldest > CACHE_DURATION) {
            try { searchMeals("") } catch (_: Exception) { }
        }
    }

    // conversion dto -> entity

    private fun MealDto.toEntity(cat: String? = null) = MealEntity(
        idMeal = idMeal, strMeal = strMeal,
        strCategory = strCategory ?: cat, strArea = null,
        strInstructions = null, strMealThumb = strMealThumb,
        ingredients = null
    )

    private fun MealDetailDto.toEntity() = MealEntity(
        idMeal = idMeal, strMeal = strMeal,
        strCategory = strCategory, strArea = strArea,
        strInstructions = strInstructions, strMealThumb = strMealThumb,
        ingredients = gson.toJson(getIngredients().map { listOf(it.first, it.second) })
    )
}

// pour parser les ingredients depuis le json stocké en bdd
fun MealEntity.parseIngredients(): List<Pair<String, String>> {
    if (ingredients.isNullOrBlank()) return emptyList()
    val type = object : TypeToken<List<List<String>>>() {}.type
    val list: List<List<String>> = Gson().fromJson(ingredients, type)
    return list.map { (it.getOrElse(0) { "" }) to (it.getOrElse(1) { "" }) }
}
