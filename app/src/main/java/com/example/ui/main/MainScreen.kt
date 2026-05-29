package com.example.ui.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExpenseEntity
import com.example.ui.AiAdviceState
import com.example.ui.ExpenseViewModel
import com.example.ui.components.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ExpenseViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val context = LocalContext.current

    val currentUser by viewModel.currentUser.collectAsState()
    val expenses by viewModel.filteredExpenses.collectAsState()

    // Since we want raw totals to be precise even when the user performs searches/filters in the transaction tab,
    // let's derive the calculations directly from the active user's total database list.
    var userExpensesList by remember { mutableStateOf<List<ExpenseEntity>>(emptyList()) }
    val userFlow = currentUser?.email
    LaunchedEffect(userFlow, expenses) {
        userFlow?.let { email ->
            // Update backing list whenever database changes
            viewModel.filteredExpenses.collect {
                // For simplicity, we compute aggregates from the reactive expenses list while matching the active user!
                userExpensesList = it
            }
        }
    }

    // Active Sidebar Tab State (0 = Dashboard, 1 = Analytics, 2 = Transactions, 3 = Settings)
    var activeTab by remember { mutableStateOf(0) }

    // Dialog state controllers
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedExpenseForEdit by remember { mutableStateOf<ExpenseEntity?>(null) }

    val userCurrency = currentUser?.activeCurrencySymbol ?: "$"
    val userMonthlyBudget = currentUser?.monthlyBudgetLimit ?: 1000.0

    // Compute basic stats
    val totalToday = remember(userExpensesList) {
        computeTotalSpentInPeriod(userExpensesList, Period.Today)
    }
    val totalThisWeek = remember(userExpensesList) {
        computeTotalSpentInPeriod(userExpensesList, Period.ThisWeek)
    }
    val totalThisMonth = remember(userExpensesList) {
        computeTotalSpentInPeriod(userExpensesList, Period.ThisMonth)
    }

    // Render Side Menu modal drawer
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Profile Header Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (currentUser?.name?.take(1)?.uppercase() ?: "U"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = currentUser?.name ?: "Expense Tracker Pro",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentUser?.email ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = "cloud check",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Offline Safe Sync",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.padding(horizontal = 20.dp))

                Spacer(modifier = Modifier.height(12.dp))

                // Drawer clickable lists
                NavigationDrawerItem(
                    label = { Text("Dashboard") },
                    selected = activeTab == 0,
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    onClick = {
                        activeTab = 0
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Analytics Charts") },
                    selected = activeTab == 1,
                    icon = { Icon(Icons.Default.PieChart, contentDescription = null) },
                    onClick = {
                        activeTab = 1
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Manage Transactions") },
                    selected = activeTab == 2,
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    onClick = {
                        activeTab = 2
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("App Settings") },
                    selected = activeTab == 3,
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    onClick = {
                        activeTab = 3
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Logout Bottom action
                Divider(modifier = Modifier.padding(horizontal = 20.dp))
                NavigationDrawerItem(
                    label = { Text("Logout Profile") },
                    selected = false,
                    icon = { Icon(Icons.Default.Logout, contentDescription = null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.logout()
                        onLogout()
                    },
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .testTag("logout_drawer_button")
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (activeTab) {
                                0 -> "Dashboard Summary"
                                1 -> "Advanced Analytics"
                                2 -> "Manage Book"
                                else -> "Settings & Data"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("sidebar_hamburger_button")
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Sidebar Navigation")
                        }
                    },
                    actions = {
                        // Quick reminder indicator for standard notifications within app
                        IconButton(onClick = {
                            // Quick simulation toast reminder trigger
                            android.widget.Toast.makeText(
                                context,
                                "Daily Expense Check: Recording daily transactions helps Gemini analyze your budget accurately!",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = "Reminders")
                        }
                    }
                )
            },
            bottomBar = {
                Surface(
                    color = Color(0xFFF3F3F7),
                    tonalElevation = 0.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tab 0: Dashboard Summary
                        IconButtonWithLabel(
                            icon = Icons.Default.Dashboard,
                            label = "Dashboard",
                            isSelected = activeTab == 0,
                            onClick = { activeTab = 0 }
                        )

                        // Tab 1: Analytics charts
                        IconButtonWithLabel(
                            icon = Icons.Default.InsertChartOutlined,
                            label = "Stats",
                            isSelected = activeTab == 1,
                            onClick = { activeTab = 1 }
                        )

                        // Center FAB
                        Box(
                            modifier = Modifier
                                .offset(y = (-15).dp)
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0061A4))
                                .clickable { showAddDialog = true }
                                .testTag("fab_add_expense"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Expense",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Tab 2: Ledger
                        IconButtonWithLabel(
                            icon = Icons.Default.AccountBalanceWallet,
                            label = "Budget",
                            isSelected = activeTab == 2,
                            onClick = { activeTab = 2 }
                        )

                        // Tab 3: Settings
                        IconButtonWithLabel(
                            icon = Icons.Default.Settings,
                            label = "Settings",
                            isSelected = activeTab == 3,
                            onClick = { activeTab = 3 }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (activeTab) {
                    0 -> DashboardTab(
                        viewModel = viewModel,
                        totalToday = totalToday,
                        totalThisWeek = totalThisWeek,
                        totalThisMonth = totalThisMonth,
                        expenses = userExpensesList,
                        userCurrency = userCurrency,
                        userMonthlyBudget = userMonthlyBudget,
                        onEditExpense = { selectedExpenseForEdit = it }
                    )
                    1 -> AnalyticsTab(
                        expenses = userExpensesList,
                        currencySymbol = userCurrency
                    )
                    2 -> TransactionsTab(
                        viewModel = viewModel,
                        expenses = expenses,
                        currencySymbol = userCurrency,
                        onEditExpense = { selectedExpenseForEdit = it }
                    )
                    3 -> SettingsTab(
                        viewModel = viewModel,
                        currentUser = currentUser,
                        expensesCount = userExpensesList.size,
                        expenses = userExpensesList
                    )
                }
            }
        }
    }

    // ADD DIALOG
    if (showAddDialog) {
        ExpenseInputFormDialog(
            titleLabel = "Record Daily Expense",
            onDismiss = { showAddDialog = false },
            onSave = { title, amt, cat, date, notes ->
                viewModel.addExpense(title, amt, cat, date, notes)
                showAddDialog = false
            },
            currencySymbol = userCurrency
        )
    }

    // EDIT DIALOG
    selectedExpenseForEdit?.let { item ->
        ExpenseInputFormDialog(
            titleLabel = "Edit Expense Details",
            initialExpense = item,
            onDismiss = { selectedExpenseForEdit = null },
            onSave = { title, amt, cat, date, notes ->
                viewModel.updateExpense(item.id, title, amt, cat, date, notes)
                selectedExpenseForEdit = null
            },
            onDelete = {
                viewModel.deleteExpense(item.id)
                selectedExpenseForEdit = null
            },
            currencySymbol = userCurrency
        )
    }
}

// --- TAB SUB-COMPONENTS ---

enum class Period { Today, ThisWeek, ThisMonth }

fun computeTotalSpentInPeriod(expenses: List<ExpenseEntity>, period: Period): Double {
    val now = Calendar.getInstance()
    now.set(Calendar.HOUR_OF_DAY, 0)
    now.set(Calendar.MINUTE, 0)
    now.set(Calendar.SECOND, 0)
    now.set(Calendar.MILLISECOND, 0)

    val todayStart = now.timeInMillis

    val weekStart = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val monthStart = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    return expenses.filter {
        when (period) {
            Period.Today -> it.dateTimeMillis >= todayStart
            Period.ThisWeek -> it.dateTimeMillis >= weekStart
            Period.ThisMonth -> it.dateTimeMillis >= monthStart
        }
    }.sumOf { it.amount }
}

@Composable
fun DashboardTab(
    viewModel: ExpenseViewModel,
    totalToday: Double,
    totalThisWeek: Double,
    totalThisMonth: Double,
    expenses: List<ExpenseEntity>,
    userCurrency: String,
    userMonthlyBudget: Double,
    onEditExpense: (ExpenseEntity) -> Unit
) {
    val aiState by viewModel.aiAdvice.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. WELCOME HEADER (From Clean Minimalism Template) ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Welcome back",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF44474E),
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = currentUser?.name ?: "User Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1C1E)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFD6E2FF), CircleShape)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (currentUser?.name?.take(2)?.uppercase() ?: "AR"),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF001A41)
                    )
                }
            }
        }

        // --- 2. THE CHIC SPENT CARD (From Clean Minimalism Template) ---
        val limitPercent = if (userMonthlyBudget > 0) (totalThisMonth / userMonthlyBudget) else 0.0
        val isOverBudget = totalThisMonth > userMonthlyBudget

        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOverBudget) Color(0xFFFFDAD6) else Color(0xFFD6E2FF)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "Spent this month",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isOverBudget) Color(0xFF410002).copy(alpha = 0.8f) else Color(0xFF001A41).copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "$userCurrency${"%.2f".format(totalThisMonth)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isOverBudget) Color(0xFF410002) else Color(0xFF001A41)
                            )
                        }
                        
                        // Limit indicator pill
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isOverBudget) Color(0xFFBA1A1A) else Color(0xFF001A41),
                                    shape = RoundedCornerShape(100.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isOverBudget) "Overspent!" else "${"%.0f".format(limitPercent * 100)}% limit",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Bottom info block with Daily Avg & Limit Left (Semi-transparent inner cards)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Daily Average Block
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Daily Avg",
                                    fontSize = 10.sp,
                                    color = if (isOverBudget) Color(0xFF410002).copy(alpha = 0.7f) else Color(0xFF001A41).copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$userCurrency${"%.2f".format(totalThisMonth / 30.0)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOverBudget) Color(0xFF410002) else Color(0xFF001A41)
                                )
                            }
                        }

                        // Limit Left / remaining warning Block
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = if (isOverBudget) "Exceeded By" else "Limit Left",
                                    fontSize = 10.sp,
                                    color = if (isOverBudget) Color(0xFF410002).copy(alpha = 0.7f) else Color(0xFF001A41).copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isOverBudget) {
                                        "$userCurrency${"%.2f".format(totalThisMonth - userMonthlyBudget)}"
                                    } else {
                                        "$userCurrency${"%.2f".format((userMonthlyBudget - totalThisMonth).coerceAtLeast(0.0))}"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOverBudget) Color(0xFF410002) else Color(0xFF001A41)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 3. CATEGORY INSTANTS GRID (From Clean Minimalism Template) ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Aesthetic Quick Filters",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1C1E)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val quickCats = listOf(
                        Triple("Food", "🍔", "Food"),
                        Triple("Travel", "✈️", "Travel"),
                        Triple("Shop", "🛍️", "Shopping"),
                        Triple("Bills", "💡", "Bills")
                    )
                    quickCats.forEach { (label, emoji, fullCategory) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    viewModel.selectedCategoryFilter.value = fullCategory
                                    android.widget.Toast.makeText(
                                        context,
                                        "Applied dynamic search filter: $fullCategory",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color(0xFFE2E2E6), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1C1E)
                            )
                        }
                    }
                }
            }
        }

        // --- 4. THE POWERFUL INTERACTIVE GEMINI COMPANION ---
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F3F7)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Color(0xFF0061A4).copy(alpha = 0.15f),
                        RoundedCornerShape(20.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Gemini Companion Optimizer",
                                tint = Color(0xFF0061A4),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "AI Balance Coach",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0061A4)
                            )
                        }

                        Button(
                            onClick = { viewModel.fetchAiSuggestions() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Analyze", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    when (aiState) {
                        is AiAdviceState.Idle -> {
                            Text(
                                "Unleash Gemini to parse transaction sequences and suggest neat minimal spending micro-saves.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF44474E)
                            )
                        }
                        is AiAdviceState.Loading -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF0061A4))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Heuristics modeling and analysis...", style = MaterialTheme.typography.labelSmall, color = Color(0xFF44474E))
                            }
                        }
                        is AiAdviceState.Success -> {
                            Text(
                                text = (aiState as AiAdviceState.Success).advice,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF1A1C1E)
                            )
                        }
                        is AiAdviceState.Error -> {
                            Text(
                                text = (aiState as AiAdviceState.Error).error,
                                color = Color(0xFFBA1A1A),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        // --- 5. RECENT TRANSACTIONS HEADER ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recent Transactions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1C1E)
                )
            }
        }

        if (expenses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Color(0xFF74777F).copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Securely offline. No transactions recorded yet.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF74777F)
                        )
                    }
                }
            }
        } else {
            items(expenses.take(4)) { expense ->
                ExpenseCardItem(
                    expense = expense,
                    currencySymbol = userCurrency,
                    onClick = { onEditExpense(expense) }
                )
            }
        }
    }
}

@Composable
fun AnalyticsTab(
    expenses: List<ExpenseEntity>,
    currencySymbol: String
) {
    val totalMap = expenses.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }

    val highestCategory = remember(totalMap) {
        if (totalMap.isEmpty()) "None" else totalMap.maxByOrNull { it.value }?.key ?: "None"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Highest Spending Category highlight panel
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Highest Spending Category",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = highestCategory,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (totalMap.isNotEmpty()) {
                            val highestSpent = totalMap[highestCategory] ?: 0.0
                            Text(
                                "Total Spent: $currencySymbol${"%.2f".format(highestSpent)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Category Pie Donut Chart
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    CategoryPieChart(expenses = expenses, currencySymbol = currencySymbol)
                }
            }
        }

        // Daily Line Trend Chart
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DailyTrendLineChart(expenses = expenses, currencySymbol = currencySymbol)
                }
            }
        }

        // Monthly Bar Chart
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    MonthlyBarChart(expenses = expenses, currencySymbol = currencySymbol)
                }
            }
        }
    }
}

@Composable
fun TransactionsTab(
    viewModel: ExpenseViewModel,
    expenses: List<ExpenseEntity>,
    currencySymbol: String,
    onEditExpense: (ExpenseEntity) -> Unit
) {
    val search by viewModel.searchQuery.collectAsState()
    val activeCat by viewModel.selectedCategoryFilter.collectAsState()
    val activeDateRange by viewModel.selectedDateFilter.collectAsState()
    val filterMin by viewModel.minAmountFilter.collectAsState()
    val filterMax by viewModel.maxAmountFilter.collectAsState()

    var showFiltersPanel by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Search title...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showFiltersPanel = !showFiltersPanel }) {
                        Icon(
                            imageVector = if (showFiltersPanel) Icons.Default.FilterListOff else Icons.Default.FilterList,
                            contentDescription = "Toggle Advance Filters"
                        )
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("transactions_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Expandable Filters Shelf panel
        AnimatedVisibility(
            visible = showFiltersPanel,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Advance Filters", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                    // Row showing Category & Date dropdown filters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Category choices dropdown
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Category", style = MaterialTheme.typography.labelSmall)
                            var expandedCatMenu by remember { mutableStateOf(false) }
                            val categoriesOptions = listOf("All", "Food", "Travel", "Shopping", "Bills", "Entertainment", "Education", "Health", "Other")
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedCatMenu = true }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(activeCat, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                DropdownMenu(expanded = expandedCatMenu, onDismissRequest = { expandedCatMenu = false }) {
                                    categoriesOptions.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt) },
                                            onClick = {
                                                viewModel.selectedCategoryFilter.value = opt
                                                expandedCatMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Date dropdown filters
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Date Range", style = MaterialTheme.typography.labelSmall)
                            var expandedDateMenu by remember { mutableStateOf(false) }
                            val datesOptions = listOf("All", "Today", "This Week", "This Month")
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedDateMenu = true }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(activeDateRange, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                DropdownMenu(expanded = expandedDateMenu, onDismissRequest = { expandedDateMenu = false }) {
                                    datesOptions.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt) },
                                            onClick = {
                                                viewModel.selectedDateFilter.value = opt
                                                expandedDateMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Row showing min - max filter range entries
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = filterMin,
                            onValueChange = { viewModel.minAmountFilter.value = it },
                            label = { Text("Min Amount") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = filterMax,
                            onValueChange = { viewModel.maxAmountFilter.value = it },
                            label = { Text("Max Amount") },
                            modifier = Modifier.weight(1f).testTag("max_amount_filter_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    // Reset Filters Button
                    TextButton(
                        onClick = {
                            viewModel.searchQuery.value = ""
                            viewModel.selectedCategoryFilter.value = "All"
                            viewModel.selectedDateFilter.value = "All"
                            viewModel.minAmountFilter.value = ""
                            viewModel.maxAmountFilter.value = ""
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Reset Filters")
                    }
                }
            }
        }

        // Ledger List
        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No transactions found matching filters.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(expenses) { expense ->
                    ExpenseCardItem(
                        expense = expense,
                        currencySymbol = currencySymbol,
                        onClick = { onEditExpense(expense) }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsTab(
    viewModel: ExpenseViewModel,
    currentUser: com.example.data.UserEntity?,
    expensesCount: Int,
    expenses: List<ExpenseEntity>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var editLimit by remember { mutableStateOf(currentUser?.monthlyBudgetLimit?.toString() ?: "1000") }
    var editCurrency by remember { mutableStateOf(currentUser?.activeCurrencySymbol ?: "$") }
    var isCurrencyMenuExpanded by remember { mutableStateOf(false) }
    val currencyOptions = listOf("$", "€", "₹", "£", "¥", "₩")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Profile Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Currency Dropdown
                var expandedCurMenu by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Reporting Currency", fontWeight = FontWeight.Medium)
                    Box(modifier = Modifier.clickable { expandedCurMenu = true }) {
                        Text(editCurrency, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                        DropdownMenu(expanded = expandedCurMenu, onDismissRequest = { expandedCurMenu = false }) {
                            currencyOptions.forEach { symbol ->
                                DropdownMenuItem(
                                    text = { Text(symbol) },
                                    onClick = {
                                        editCurrency = symbol
                                        expandedCurMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Monthly Budget Field
                OutlinedTextField(
                    value = editLimit,
                    onValueChange = { editLimit = it },
                    label = { Text("Monthly Budget Limit") },
                    modifier = Modifier.fillMaxWidth().testTag("settings_budget_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val limitVal = editLimit.toDoubleOrNull() ?: 1000.0
                        viewModel.updateSettings(editCurrency, limitVal) {
                            android.widget.Toast.makeText(context, "Settings updated safely!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.align(Alignment.End).testTag("save_settings_btn")
                ) {
                    Text("Save Changes")
                }
            }
        }

        // Data utilities panel
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Data Utilities",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    "Database has $expensesCount transaction records logged. Safe offline backup.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Export to CSV Actions
                Button(
                    onClick = { exportExpensesToCsv(context, expenses, editCurrency) },
                    modifier = Modifier.fillMaxWidth().testTag("export_csv_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Export Ledger to CSV Sheet")
                }

                Spacer(modifier = Modifier.height(10.dp))

                // SEED DUMMY RECORDS ACTION
                Button(
                    onClick = {
                        viewModel.seedDummyData()
                        android.widget.Toast.makeText(context, "Dummy transaction feed generated!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("seed_data_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Populate Realistic Dummy Data")
                }
            }
        }
    }
}

// --- UTILITY ITEM CARD ---

@Composable
fun ExpenseCardItem(
    expense: ExpenseEntity,
    currencySymbol: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("expense_item_${expense.id}")
            .border(
                1.dp,
                Color(0xFF74777F).copy(alpha = 0.1f),
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Block with rich specific emojis (Clean Minimalism matching)
            val categoryEmoji = when (expense.category.lowercase(Locale.getDefault())) {
                "food" -> "🍔"
                "travel" -> "🚗"
                "shopping" -> "🛍️"
                "bills" -> "💡"
                "entertainment" -> "🎬"
                "education" -> "📚"
                "health" -> "❤️"
                else -> "💸"
            }
            
            val categoryBg = when (expense.category.lowercase(Locale.getDefault())) {
                "food" -> Color(0xFFFFDAD6)
                "travel" -> Color(0xFFD1E4FF)
                "shopping" -> Color(0xFFE2E2E6)
                "bills" -> Color(0xFFF3F3F7)
                "entertainment" -> Color(0xFFE8DDFF)
                "education" -> Color(0xFFD1F2D9)
                "health" -> Color(0xFFFAD2E1)
                else -> Color(0xFFE2E2E6)
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(categoryBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = categoryEmoji, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Main Text block details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF1A1C1E),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(expense.dateTimeMillis)),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF74777F)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Amount block details with right-aligned metadata
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "-$currencySymbol${"%.2f".format(expense.amount)}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFFBA1A1A)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = expense.category,
                    fontSize = 10.sp,
                    color = Color(0xFF74777F),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}


// --- SHARED EXPENSE ADD / EDIT DIALOG FORM WITH VOICE TRANSCRIPTION ACTIONS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseInputFormDialog(
    titleLabel: String,
    initialExpense: ExpenseEntity? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, category: String, dateTime: Long, notes: String) -> Unit,
    onDelete: (() -> Unit)? = null,
    currencySymbol: String
) {
    val context = LocalContext.current

    var editTitle by remember { mutableStateOf(initialExpense?.title ?: "") }
    var editAmount by remember { mutableStateOf(initialExpense?.amount?.toString() ?: "") }
    var editCategory by remember { mutableStateOf(initialExpense?.category ?: "Food") }
    var editNotes by remember { mutableStateOf(initialExpense?.notes ?: "") }
    var editDateTime by remember { mutableStateOf(initialExpense?.dateTimeMillis ?: System.currentTimeMillis()) }

    var expandedCategoryMenu by remember { mutableStateOf(false) }
    val categoriesList = listOf("Food", "Travel", "Shopping", "Bills", "Entertainment", "Education", "Health", "Other")

    // Speech-to-Text integration parsing
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val words = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val spokenText = words?.firstOrNull()?.lowercase(Locale.getDefault()) ?: ""
                
                if (spokenText.isNotEmpty()) {
                    // Sophisticated natural language regex heuristic parsing on voice inputs
                    // e.g. "spent 52 dollars on groceries" or "Uber ride 25 bucks"
                    val parsedAmt = extractNumberFromString(spokenText)
                    if (parsedAmt != null) {
                        editAmount = "%.2f".format(parsedAmt)
                    }

                    // Simple keyword checks for category heuristics
                    val detectedCategory = autoDetectCategoryFromKeywords(spokenText)
                    if (detectedCategory != null) {
                        editCategory = detectedCategory
                    }

                    // Feed title from command
                    editTitle = spokenText.capitalize(Locale.getDefault())
                }
            }
        }
    )

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(titleLabel, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Speech assist narrations bar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Narration: spent [amount] on [details]")
                                    }
                                    speechLauncher.launch(intent)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Voice input requires direct speech-to-text service.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Trigger narration recognition", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Voice Entry Assistant", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("Tap & Speak e.g: '52 dollars on sushi'", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("Title / Name") },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_expense_title"),
                    singleLine = true
                )

                // Amount Input
                OutlinedTextField(
                    value = editAmount,
                    onValueChange = { editAmount = it },
                    label = { Text("Amount ($currencySymbol)") },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_expense_amount"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                // Category Selection Droplist
                ExposedDropdownMenuBox(
                    expanded = expandedCategoryMenu,
                    onExpandedChange = { expandedCategoryMenu = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = editCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoryMenu) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategoryMenu,
                        onDismissRequest = { expandedCategoryMenu = false }
                    ) {
                        categoriesList.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    editCategory = opt
                                    expandedCategoryMenu = false
                                }
                            )
                        }
                    }
                }

                // Notes optional
                OutlinedTextField(
                    value = editNotes,
                    onValueChange = { editNotes = it },
                    label = { Text("Add Notes (optional)") },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_expense_notes"),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amtDouble = editAmount.toDoubleOrNull()
                    if (editTitle.isBlank() || amtDouble == null || amtDouble <= 0.0) {
                        android.widget.Toast.makeText(context, "Please supply a valid numeric transaction amount.", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        onSave(editTitle, amtDouble, editCategory, editDateTime, editNotes)
                    }
                },
                modifier = Modifier.testTag("dialog_expense_save")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(
                        onClick = { onDelete() },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("dialog_expense_delete")
                    ) {
                        Text("Delete")
                    }
                }
                TextButton(onClick = { onDismiss() }) {
                    Text("Cancel")
                }
            }
        }
    )
}

// Heuristic voice speech parsers
fun extractNumberFromString(input: String): Double? {
    // Looks for standard continuous digit structures like "25" or decimals "12.50"
    val regex = """\d+(\.\d+)?""".toRegex()
    val match = regex.find(input)
    if (match != null) {
        return match.value.toDoubleOrNull()
    }
    
    // Fallback checks for written English digits
    val wordMap = mapOf(
        "one" to 1.0, "two" to 2.0, "three" to 3.0, "four" to 4.0, "five" to 5.0,
        "six" to 6.0, "seven" to 7.0, "eight" to 8.0, "nine" to 9.0, "ten" to 10.0,
        "twenty" to 20.0, "thirty" to 30.0, "forty" to 40.0, "fifty" to 50.0,
        "hundred" to 100.0
    )
    for ((word, valDouble) in wordMap) {
        if (input.contains(word)) {
            return valDouble
        }
    }
    return null
}

fun autoDetectCategoryFromKeywords(speech: String): String? {
    if (speech.contains("food") || speech.contains("eat") || speech.contains("lunch") || speech.contains("dinner") || speech.contains("restaurant") || speech.contains("coffee") || speech.contains("sushi") || speech.contains("burger")) return "Food"
    if (speech.contains("taxi") || speech.contains("uber") || speech.contains("bus") || speech.contains("train") || speech.contains("gas") || speech.contains("ticket") || speech.contains("flight") || speech.contains("fuel") || speech.contains("drive")) return "Travel"
    if (speech.contains("clothes") || speech.contains("zara") || speech.contains("amazon") || speech.contains("shoes") || speech.contains("shirt") || speech.contains("buy") || speech.contains("mall")) return "Shopping"
    if (speech.contains("rent") || speech.contains("electricity") || speech.contains("heating") || speech.contains("subscription") || speech.contains("netflix") || speech.contains("spotify") || speech.contains("bill") || speech.contains("vps")) return "Bills"
    if (speech.contains("movie") || speech.contains("cinema") || speech.contains("concert") || speech.contains("game") || speech.contains("bar") || speech.contains("club") || speech.contains("bowling")) return "Entertainment"
    if (speech.contains("course") || speech.contains("book") || speech.contains("tutoring") || speech.contains("udemy") || speech.contains("school") || speech.contains("guide")) return "Education"
    if (speech.contains("gym") || speech.contains("fitness") || speech.contains("doctor") || speech.contains("dentist") || speech.contains("medicine") || speech.contains("pills")) return "Health"
    return null
}

fun exportExpensesToCsv(context: Context, expenses: List<ExpenseEntity>, currency: String) {
    val csvHeader = "ID,Title,Amount,Category,Date,Notes\n"
    val csvRows = expenses.joinToString("\n") { expense ->
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(expense.dateTimeMillis))
        "${expense.id},\"${expense.title.replace("\"", "\"\"")}\",${expense.amount},\"${expense.category}\",\"$dateStr\",\"${expense.notes.replace("\"", "\"\"")}\""
    }
    val csvContent = csvHeader + csvRows
    
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_SUBJECT, "Expense Ledger CSV Export")
        putExtra(Intent.EXTRA_TEXT, csvContent)
    }
    context.startActivity(Intent.createChooser(intent, "Export CSV Ledger via"))
}

@Composable
fun IconButtonWithLabel(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(width = 50.dp, height = 28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFD6E2FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color(0xFF001A41),
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF44474E).copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color(0xFF001A41) else Color(0xFF44474E).copy(alpha = 0.7f)
        )
    }
}

