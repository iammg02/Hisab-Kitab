package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

sealed interface AiAdviceState {
    object Idle : AiAdviceState
    object Loading : AiAdviceState
    data class Success(val advice: String) : AiAdviceState
    data class Error(val error: String) : AiAdviceState
}

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repository = ExpenseRepository(db)

    // --- AUTHENTICATION STATES ---
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // --- EXPENSES DATA STATE ---
    private val _allExpenses = MutableStateFlow<List<ExpenseEntity>>(emptyList())
    
    // --- FILTER CONTROL STATES ---
    val searchQuery = MutableStateFlow("")
    val selectedCategoryFilter = MutableStateFlow("All") // "All" or specific categories
    val selectedDateFilter = MutableStateFlow("All") // "All", "Today", "This Week", "This Month"
    val minAmountFilter = MutableStateFlow("")
    val maxAmountFilter = MutableStateFlow("")

    // We combine search and filters to generate the final filtered list of expenses
    val filteredExpenses: StateFlow<List<ExpenseEntity>> = combine(
        _allExpenses,
        searchQuery,
        selectedCategoryFilter,
        selectedDateFilter,
        minAmountFilter,
        maxAmountFilter
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        val expenses = array[0] as List<ExpenseEntity>
        val search = array[1] as String
        val cat = array[2] as String
        val dateRange = array[3] as String
        val minString = array[4] as String
        val maxString = array[5] as String

        expenses.filter { expense ->
            val matchSearch = if (search.isBlank()) {
                true
            } else {
                expense.title.contains(search, ignoreCase = true) || expense.notes.contains(search, ignoreCase = true)
            }

            val matchCategory = if (cat == "All") {
                true
            } else {
                expense.category.equals(cat, ignoreCase = true)
            }

            val matchDate = when (dateRange) {
                "Today" -> isToday(expense.dateTimeMillis)
                "This Week" -> isThisWeek(expense.dateTimeMillis)
                "This Month" -> isThisMonth(expense.dateTimeMillis)
                else -> true // "All"
            }

            val minVal = minString.toDoubleOrNull()
            val matchMin = if (minVal != null) {
                expense.amount >= minVal
            } else {
                true
            }

            val maxVal = maxString.toDoubleOrNull()
            val matchMax = if (maxVal != null) {
                expense.amount <= maxVal
            } else {
                true
            }

            matchSearch && matchCategory && matchDate && matchMin && matchMax
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- AI INSIGHT STATE ---
    private val _aiAdvice = MutableStateFlow<AiAdviceState>(AiAdviceState.Idle)
    val aiAdvice: StateFlow<AiAdviceState> = _aiAdvice.asStateFlow()

    init {
        // Automatically check if a default demo user exists for seamless first experience,
        // or let user sign in. To make review immediate, we will auto-login a demo user if requested
        // but by default allow full auth control block.
    }

    // --- CHROME & SEED ---
    fun seedDummyData() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.seedSampleData(user.email)
            refreshExpenses()
        }
    }

    private fun refreshExpenses() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.getExpensesFlow(user.email).collect {
                _allExpenses.value = it
            }
        }
    }

    // --- AUTHENTICATION ACTIONS ---
    fun login(email: String, passwordCheck: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (email.isBlank() || passwordCheck.isBlank()) {
                onError("Please fill in all credentials.")
                return@launch
            }
            val user = repository.getUserByEmail(email)
            if (user != null && user.passwordHash == passwordCheck) {
                _currentUser.value = user
                onSuccess()
                // Start observing user-specific expenses
                viewModelScope.launch {
                    repository.getExpensesFlow(user.email).collect {
                        _allExpenses.value = it
                    }
                }
            } else {
                onError("Invalid email or password combination.")
            }
        }
    }

    fun signUp(
        name: String,
        email: String,
        passwordCheck: String,
        currency: String,
        budgetLimit: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (name.isBlank() || email.isBlank() || passwordCheck.isBlank()) {
                onError("Please fill in all credentials.")
                return@launch
            }
            val existing = repository.getUserByEmail(email)
            if (existing != null) {
                onError("An account with this email already exists.")
                return@launch
            }

            val newUser = UserEntity(
                email = email,
                name = name,
                passwordHash = passwordCheck,
                activeCurrencySymbol = currency,
                monthlyBudgetLimit = budgetLimit
            )
            repository.registerUser(newUser)
            _currentUser.value = newUser
            onSuccess()

            // Observe user expenses
            viewModelScope.launch {
                repository.getExpensesFlow(newUser.email).collect {
                    _allExpenses.value = it
                }
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _allExpenses.value = emptyList()
        _aiAdvice.value = AiAdviceState.Idle
    }

    fun updateSettings(currency: String, budget: Double, onComplete: () -> Unit) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updatedUser = user.copy(
                activeCurrencySymbol = currency,
                monthlyBudgetLimit = budget
            )
            repository.updateUser(updatedUser)
            _currentUser.value = updatedUser
            onComplete()
        }
    }

    // --- EXPENSE CRUD OPERATIONS ---
    fun addExpense(
        title: String,
        amount: Double,
        category: String,
        dateTimeMillis: Long,
        notes: String,
        onComplete: () -> Unit = {}
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val newExpense = ExpenseEntity(
                userEmail = user.email,
                title = title,
                amount = amount,
                category = category,
                dateTimeMillis = dateTimeMillis,
                notes = notes
            )
            repository.addExpense(newExpense)
            onComplete()
        }
    }

    fun updateExpense(
        id: Long,
        title: String,
        amount: Double,
        category: String,
        dateTimeMillis: Long,
        notes: String,
        onComplete: () -> Unit = {}
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updated = ExpenseEntity(
                id = id,
                userEmail = user.email,
                title = title,
                amount = amount,
                category = category,
                dateTimeMillis = dateTimeMillis,
                notes = notes
            )
            repository.updateExpense(updated)
            onComplete()
        }
    }

    fun deleteExpense(id: Long, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteExpense(id)
            onComplete()
        }
    }

    // --- GENERATIVE AI REPORTS CALLS ---
    fun fetchAiSuggestions() {
        val user = _currentUser.value ?: return
        _aiAdvice.value = AiAdviceState.Loading
        viewModelScope.launch {
            val advice = repository.getSuggestions(user.email)
            _aiAdvice.value = AiAdviceState.Success(advice)
        }
    }

    // --- DATE HELPER CHECKS FOR FILTERS ---
    private fun isToday(millis: Long): Boolean {
        val now = Calendar.getInstance()
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        return now.get(Calendar.YEAR) == c.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR)
    }

    private fun isThisWeek(millis: Long): Boolean {
        val now = Calendar.getInstance()
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        return now.get(Calendar.YEAR) == c.get(Calendar.YEAR) &&
                now.get(Calendar.WEEK_OF_YEAR) == c.get(Calendar.WEEK_OF_YEAR)
    }

    private fun isThisMonth(millis: Long): Boolean {
        val now = Calendar.getInstance()
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        return now.get(Calendar.YEAR) == c.get(Calendar.YEAR) &&
                now.get(Calendar.MONTH) == c.get(Calendar.MONTH)
    }
}
