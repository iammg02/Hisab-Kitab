package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- 1. USER ENTITY ---
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String, // Treat email as unique user ID
    val name: String,
    val passwordHash: String,
    val activeCurrencySymbol: String = "$", // e.g., "$", "€", "₹", "£"
    val monthlyBudgetLimit: Double = 1000.0
)

// --- 2. EXPENSE ENTITY ---
@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["email"],
            childColumns = ["userEmail"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userEmail"]), Index(value = ["dateTimeMillis"])]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userEmail: String,
    val title: String,
    val amount: Double,
    val category: String, // Food, Travel, Shopping, Bills, Entertainment, Education, Health, Other
    val dateTimeMillis: Long,
    val notes: String = ""
)

// --- 3. DAOS ---
@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE userEmail = :email ORDER BY dateTimeMillis DESC")
    fun getExpensesFlow(email: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE userEmail = :email ORDER BY dateTimeMillis DESC")
    suspend fun getExpensesList(email: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: Long): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Query("DELETE FROM expenses WHERE userEmail = :email")
    suspend fun clearAllExpensesForUser(email: String)
}

// --- 4. ROOM DATABASE ---
@Database(entities = [UserEntity::class, ExpenseEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract val userDao: UserDao
    abstract val expenseDao: ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_tracker_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
