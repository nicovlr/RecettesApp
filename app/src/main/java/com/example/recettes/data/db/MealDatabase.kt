package com.example.recettes.data.db

import android.content.Context
import androidx.room.*

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey val idMeal: String,
    val strMeal: String,
    val strCategory: String?,
    val strArea: String?,
    val strInstructions: String?,
    val strMealThumb: String?,
    val ingredients: String?,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val idCategory: String,
    val strCategory: String,
    val strCategoryThumb: String?
)

@Dao
interface MealDao {

    @Query("SELECT * FROM meals")
    suspend fun getAllMeals(): List<MealEntity>

    @Query("SELECT * FROM meals WHERE strMeal LIKE '%' || :query || '%'")
    suspend fun searchMeals(query: String): List<MealEntity>

    @Query("SELECT * FROM meals WHERE strCategory = :category")
    suspend fun getMealsByCategory(category: String): List<MealEntity>

    @Query("SELECT * FROM meals WHERE idMeal = :id")
    suspend fun getMealById(id: String): MealEntity?

    @Upsert
    suspend fun upsertMeals(meals: List<MealEntity>)

    @Upsert
    suspend fun upsertMeal(meal: MealEntity)

    @Query("SELECT * FROM categories")
    suspend fun getAllCategories(): List<CategoryEntity>

    @Upsert
    suspend fun upsertCategories(categories: List<CategoryEntity>)

    @Query("SELECT MIN(lastUpdated) FROM meals")
    suspend fun getOldestUpdate(): Long?
}

@Database(entities = [MealEntity::class, CategoryEntity::class], version = 1, exportSchema = false)
abstract class MealDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao

    companion object {
        @Volatile private var instance: MealDatabase? = null

        fun get(context: Context): MealDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext, MealDatabase::class.java, "meals_db"
                ).build().also { instance = it }
            }
        }
    }
}
