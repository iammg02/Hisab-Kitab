package com.example.data

import com.example.api.GeminiAdvisor
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class ExpenseRepository(private val db: AppDatabase) {

    // --- USER MANAGEMENT ---
    suspend fun getUserByEmail(email: String): UserEntity? {
        return db.userDao.getUserByEmail(email)
    }

    suspend fun registerUser(user: UserEntity) {
        db.userDao.insertUser(user)
    }

    suspend fun updateUser(user: UserEntity) {
        db.userDao.updateUser(user)
    }

    // --- EXPENSE MANAGEMENT ---
    fun getExpensesFlow(email: String): Flow<List<ExpenseEntity>> {
        return db.expenseDao.getExpensesFlow(email)
    }

    suspend fun getExpensesList(email: String): List<ExpenseEntity> {
        return db.expenseDao.getExpensesList(email)
    }

    suspend fun getExpenseById(id: Long): ExpenseEntity? {
        return db.expenseDao.getExpenseById(id)
    }

    suspend fun addExpense(expense: ExpenseEntity) {
        db.expenseDao.insertExpense(expense)
    }

    suspend fun updateExpense(expense: ExpenseEntity) {
        db.expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(id: Long) {
        db.expenseDao.deleteExpenseById(id)
    }

    suspend fun clearUserExpenses(email: String) {
        db.expenseDao.clearAllExpensesForUser(email)
    }

    // --- AI ADVISORY ---
    suspend fun getSuggestions(email: String): String {
        val user = getUserByEmail(email) ?: return "Please create a profile first."
        val expenses = getExpensesList(email)
        
        // Summarize expenses text-wise for model context
        val totalSpent = expenses.sumOf { it.amount }
        val categoryBreakdown = expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val breakdownStr = categoryBreakdown.entries.joinToString("\n") { (cat, amt) ->
            "- $cat: ${user.activeCurrencySymbol}${"%.2f".format(amt)}"
        }

        val summary = """
            Total Spent: ${user.activeCurrencySymbol}${"%.2f".format(totalSpent)}
            Category breakdown:
            $breakdownStr
            Total transactions count: ${expenses.size}
        """.trimIndent()

        return GeminiAdvisor.getSpendingSuggestions(
            monthlyBudget = user.monthlyBudgetLimit,
            currencySymbol = user.activeCurrencySymbol,
            expensesSummary = summary
        )
    }

    // --- SEED SAMPLE DATA FOR THE DEMONSTRATION & CHARTS ---
    suspend fun seedSampleData(userEmail: String) {
        // Clear past ones first to prevent clutter
        db.expenseDao.clearAllExpensesForUser(userEmail)

        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        // Set up distinct offsets
        // Today
        calendar.timeInMillis = now
        val todayMillis = calendar.timeInMillis
        
        calendar.add(Calendar.HOUR, -3)
        val todayMorningMillis = calendar.timeInMillis

        // Yesterday
        calendar.timeInMillis = now
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayMillis = calendar.timeInMillis

        calendar.add(Calendar.HOUR, -4)
        val yesterdayEveningMillis = calendar.timeInMillis

        // 3 days ago
        calendar.timeInMillis = now
        calendar.add(Calendar.DAY_OF_YEAR, -3)
        val threeDaysAgoMillis = calendar.timeInMillis

        // 5 days ago
        calendar.timeInMillis = now
        calendar.add(Calendar.DAY_OF_YEAR, -5)
        val fiveDaysAgoMillis = calendar.timeInMillis

        // 12 days ago
        calendar.timeInMillis = now
        calendar.add(Calendar.DAY_OF_YEAR, -12)
        val twelveDaysAgoMillis = calendar.timeInMillis

        // 18 days ago
        calendar.timeInMillis = now
        calendar.add(Calendar.DAY_OF_YEAR, -18)
        val eighteenDaysAgoMillis = calendar.timeInMillis

        // 25 days ago
        calendar.timeInMillis = now
        calendar.add(Calendar.DAY_OF_YEAR, -25)
        val twentyFiveDaysAgoMillis = calendar.timeInMillis

        val dummyData = listOf(
            ExpenseEntity(
                userEmail = userEmail,
                title = "Starbucks Coffee & Croissant",
                amount = 12.80,
                category = "Food",
                dateTimeMillis = todayMorningMillis,
                notes = "Breakfast on the go"
            ),
            ExpenseEntity(
                userEmail = userEmail,
                title = "Weekly Grocery Purchases",
                amount = 84.50,
                category = "Food",
                dateTimeMillis = yesterdayMillis,
                notes = "Oatmilk, avocados, eggs, salmon"
            ),
            ExpenseEntity(
                userEmail = userEmail,
                title = "Organic Dinner Box",
                amount = 45.00,
                category = "Food",
                dateTimeMillis = threeDaysAgoMillis,
                notes = "Vegan gourmet recipes"
            ),
            ExpenseEntity(
                userEmail = userEmail,
                title = "Metropolitan Transit Pass",
                amount = 35.00,
                category = "Travel",
                dateTimeMillis = fiveDaysAgoMillis,
                notes = "Monthly bus & light rail pass"
            ),
            ExpenseEntity(
                userEmail = userEmail,
                title = "Gas Station Refuel",
                amount = 58.00,
                category = "Travel",
                dateTimeMillis = twelveDaysAgoMillis,
                notes = "Premium fuel"
            ),
            ExpenseEntity(
                userEmail = userEmail,
                title = "Linen Summer Shirt",
                amount = 65.00,
                category = "Shopping",
                dateTimeMillis = todayMillis,
                notes = "Sand beige breathable shirt"
            ),
            ExpenseEntity(
                userEmail = userEmail,
                title = "Noise-Cancelling Headphones",
                amount = 199.00,
                category = "Shopping",
                dateTimeMillis = eighteenDaysAgoMillis,
                notes = "Over-ear active noise cancelling"
            ),
            ExpenseEntity(
                userEmail = userEmail,
                title = "Electricity & Heating Bill",
                amount = 120.00,
                category = "Bills",
                dateTimeMillis = twelveDaysAgoMillis,
                notes = "Spring utility billing cycle"
            ),
            ExpenseEntity(
                userEmail = userEmail,
                title = "Cloud Server VPS Hosting",
                amount = 15.00,
                category = "Bills",
                dateTimeMillis = twentyFiveDaysAgoMillis,
                notes = "Personal dev microinstance"
            ),
            ExpenseEntity(
                userEmail = userEmail,
                title = "Spotify Premium Duo",
                amount = 14.99,
                category = "Bills",
                dateTimeMillis = todayMorningMillis,
                notes = "Recurring subscription"
            ),
            ExpenseEntity(
                userEmail = userEmail,
                title = "Cinematograph Tickets",
                amount = 26.00,
                category = "Entertainment",
                dateTimeMillis = yesterdayEveningMillis,
                notes = "IMAX Sci-Fi release"
            ),
            ExpenseEntity(
                userEmail = userEmail,
                title = "Rock Concert General Admit",
                amount = 90.00,
                category = "Entertainment",
                dateTimeMillis = eighteenDaysAgoMillis,
                notes = "Live stadium show"
            ),
            ExpenseEntity(
                userEmail = userEmail,
                title = "Jetpack Compose Guide",
                amount = 45.00,
                category = "Education",
                dateTimeMillis = twentyFiveDaysAgoMillis,
                notes = "Advanced Android design patterns ebook"
            ),
            ExpenseEntity(
                userEmail = userEmail,
                title = "Fitness Club Monthly Pass",
                amount = 60.00,
                category = "Health",
                dateTimeMillis = twentyFiveDaysAgoMillis,
                notes = "Gym subscription"
            ),
            ExpenseEntity(
                userEmail = userEmail,
                title = "Teeth Polishing & Dental Check",
                amount = 110.00,
                category = "Health",
                dateTimeMillis = eightDaysAgoMillis(now),
                notes = "Preventative dental wash"
            ),
            ExpenseEntity(
                userEmail = userEmail,
                title = "Postal Courier Package Delivery",
                amount = 18.50,
                category = "Other",
                dateTimeMillis = yesterdayMillis,
                notes = "Shipped birthday box"
            )
        )

        for (expense in dummyData) {
            db.expenseDao.insertExpense(expense)
        }
    }

    private fun eightDaysAgoMillis(now: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = now
        c.add(Calendar.DAY_OF_YEAR, -8)
        return c.timeInMillis
    }
}
