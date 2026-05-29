package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExpenseEntity
import java.text.SimpleDateFormat
import java.util.*

// --- CATEGORY BRAND COLORS ---
val FoodColor = Color(0xFFF25C54)        // Coral Red
val TravelColor = Color(0xFF48CAE4)      // Ocean Cyan
val ShoppingColor = Color(0xFF7209B7)    // Dark Violet
val BillsColor = Color(0xFF4361EE)       // Indigo Blue
val EntertainmentColor = Color(0xFFFFB703) // Amber Yellow
val EducationColor = Color(0xFF2EC4B6)   // Mint Green
val HealthColor = Color(0xFFE91E63)      // Crimson Pink
val OtherColor = Color(0xFFADA79B)       // Sand Gray

fun getCategoryColor(category: String): Color {
    return when (category.lowercase(Locale.getDefault())) {
        "food" -> FoodColor
        "travel" -> TravelColor
        "shopping" -> ShoppingColor
        "bills" -> BillsColor
        "entertainment" -> EntertainmentColor
        "education" -> EducationColor
        "health" -> HealthColor
        else -> OtherColor
    }
}

@Composable
fun CategoryPieChart(
    expenses: List<ExpenseEntity>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val totalMap = expenses.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
    
    val overallTotal = totalMap.values.sum()
    if (overallTotal <= 0) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No transaction records available for Pie breakdown.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val sortedCategories = totalMap.entries.sortedByDescending { it.value }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Category Ratio Breakdown",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Pie Drawing
            Canvas(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .padding(8.dp)
            ) {
                val diameter = size.minDimension * 0.9f
                val left = (size.width - diameter) / 2f
                val top = (size.height - diameter) / 2f
                val bounds = Size(diameter, diameter)

                var currentStartAngle = -90f
                sortedCategories.forEach { (cat, amt) ->
                    val sweepAngle = ((amt / overallTotal) * 360f).toFloat()
                    drawArc(
                        color = getCategoryColor(cat),
                        startAngle = currentStartAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(left, top),
                        size = bounds,
                        style = Stroke(width = diameter * 0.22f) // Donut Style
                    )
                    currentStartAngle += sweepAngle
                }
            }

            // Right: Interactive legends
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                sortedCategories.take(4).forEach { (cat, amt) ->
                    val percentage = (amt / overallTotal) * 100
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(getCategoryColor(cat), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$currencySymbol${"%.2f".format(amt)} (${"%.1f".format(percentage)}%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (sortedCategories.size > 4) {
                    val otherAmt = sortedCategories.drop(4).sumOf { it.value }
                    val otherPercentage = (otherAmt / overallTotal) * 100
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(OtherColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Others",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$currencySymbol${"%.2f".format(otherAmt)} (${"%.1f".format(otherPercentage)}%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyBarChart(
    expenses: List<ExpenseEntity>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    if (expenses.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No records to draw monthly chart.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Group by Month ("MMM yyyy")
    val df = SimpleDateFormat("MMM", Locale.getDefault())
    val monthlyData = expenses.groupBy {
        val cal = Calendar.getInstance()
        cal.timeInMillis = it.dateTimeMillis
        df.format(cal.time)
    }.mapValues { it.value.sumOf { exp -> exp.amount } }

    val sortedMonths = monthlyData.entries.take(5).reversed() // Show up to past 5 months
    val maxMonthValue = sortedMonths.maxOfOrNull { it.value } ?: 1.0

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Monthly Spending Flow",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillModifier()
                .height(180.dp)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            if (sortedMonths.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No months parsed yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                sortedMonths.forEach { (monthLabel, totalSpent) ->
                    val ratio = (totalSpent / maxMonthValue).toFloat().coerceIn(0.05f, 1f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "$currencySymbol${"%.0f".format(totalSpent)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        // Visual Bar
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(0.75f * ratio)
                                .width(32.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = monthLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DailyTrendLineChart(
    expenses: List<ExpenseEntity>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    if (expenses.isEmpty()) {
        return
    }

    // Group expenses of past 7 days
    val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
    val cal = Calendar.getInstance()
    
    // Build a map of dates for the past 7 days to ensure continuous graph
    val past7Days = (0..6).map { offset ->
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, -offset)
        sdf.format(c.time)
    }.reversed()

    val dateSpendMap = expenses.groupBy {
        sdf.format(Date(it.dateTimeMillis))
    }.mapValues { it.value.sumOf { e -> e.amount } }

    val dataPoints = past7Days.map { dateLabel ->
        dateSpendMap[dateLabel] ?: 0.0
    }

    val maxPoint = dataPoints.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Daily Trend (Past 7 Days)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(vertical = 12.dp, horizontal = 8.dp)
        ) {
            val width = size.width
            val height = size.height
            val spacing = width / (dataPoints.size - 1)

            val points = dataPoints.mapIndexed { idx, value ->
                val x = idx * spacing
                val y = height - ((value / maxPoint) * height).toFloat()
                Offset(x, y)
            }

            // Draw line
            val path = Path().apply {
                if (points.isNotEmpty()) {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        // Smooth curves using cubic bezier
                        val pClicked = points[i - 1]
                        val pNext = points[i]
                        val controlX = (pClicked.x + pNext.x) / 2f
                        cubicTo(controlX, pClicked.y, controlX, pNext.y, pNext.x, pNext.y)
                    }
                }
            }

            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 4.dp.toPx())
            )

            // Draw points
            points.forEach { offset ->
                drawCircle(
                    color = primaryColor,
                    radius = 5.dp.toPx(),
                    center = offset
                )
            }
        }

        // Horizontal trend labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            past7Days.forEach { dateLabel ->
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun Modifier.fillModifier(): Modifier {
    return this.fillMaxWidth()
}
