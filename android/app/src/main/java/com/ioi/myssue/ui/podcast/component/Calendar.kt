package com.ioi.myssue.ui.podcast.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ioi.myssue.R
import com.ioi.myssue.designsystem.theme.AppColors
import com.ioi.myssue.designsystem.theme.BackgroundColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun Calendar(
    modifier: Modifier = Modifier,
    isMonthlyView: Boolean,
    selectedDate: LocalDate,
    onToggleView: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()

    Column(modifier = modifier.animateContentSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(4.dp)
                .shadow(2.dp, shape = RoundedCornerShape(8.dp))
                .background(color = BackgroundColors.Background50, shape = RoundedCornerShape(8.dp))
                .clickable(onClick = onToggleView)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_calendar),
                contentDescription = null,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${
                    today.month.getDisplayName(TextStyle.FULL, Locale.US).uppercase()
                }, ${today.year}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(4.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        if (isMonthlyView) {
            BackHandler {
                onToggleView()
            }
            MonthCalendar(
                currentDate = today,
                selectedDate = selectedDate,
                onDateSelected = onDateSelected
            )
        } else {
            WeekCalendarRow(
                selectedDate = selectedDate,
                onDateSelected = onDateSelected
            )
        }
    }
}

@Composable
private fun WeekCalendarRow(
    daysToShow: Int = 5,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val dates = remember(today, daysToShow) {
        List(daysToShow) { i -> today.minusDays((daysToShow - 1 - i).toLong()) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dates.forEach { date ->
            DayBoxCell(
                date = date,
                selected = date == selectedDate,
                isMonthly = false,
                onClick = { onDateSelected(date) },
                isEnabled = today > date && date > LocalDate.of(2025, 9, 20),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MonthCalendar(
    currentDate: LocalDate,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val firstDayOfMonth = currentDate.withDayOfMonth(1)
    val daysInMonth = currentDate.lengthOfMonth()

    val daysThisMonth = remember(firstDayOfMonth) {
        (0 until daysInMonth).map { firstDayOfMonth.plusDays(it.toLong()) }
    }

    val firstDowSundayIndex = firstDayOfMonth.dayOfWeek.value % 7
    val leadingCount = firstDowSundayIndex

    val prevMonth = currentDate.minusMonths(1)
    val prevLast = prevMonth.withDayOfMonth(prevMonth.lengthOfMonth())
    val leadingDays = remember(currentDate, leadingCount) {
        List(leadingCount) { i -> prevLast.minusDays((leadingCount - 1 - i).toLong()) }
    }

    val totalSoFar = leadingDays.size + daysThisMonth.size
    val trailingCount = (7 - (totalSoFar % 7)).let { if (it == 7) 0 else it }
    val nextMonthFirst = currentDate.plusMonths(1).withDayOfMonth(1)
    val trailingDays = remember(currentDate, trailingCount) {
        List(trailingCount) { i -> nextMonthFirst.plusDays(i.toLong()) }
    }

    val fullDays = leadingDays + daysThisMonth + trailingDays

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        fullDays.chunked(7).forEach { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                week.forEach { date ->
                    DayBoxCell(
                        date = date,
                        selected = date == selectedDate,
                        isMonthly = true,
                        isEnabled = currentDate > date && date > LocalDate.of(2025, 9, 20),
                        onClick = { onDateSelected(date) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayBoxCell(
    modifier: Modifier = Modifier,
    date: LocalDate,
    selected: Boolean,
    isMonthly: Boolean,
    isEnabled: Boolean = true,
    onClick: () -> Unit,
) {
    val today = LocalDate.now()
    val bg = when {
        date == today -> BackgroundColors.Background600
        selected -> AppColors.Primary400
        !isMonthly -> BackgroundColors.Background50
        else -> Color.Transparent
    }
    date.dayOfWeek == DayOfWeek.SUNDAY
    val dayTextColor =
        if (selected || date == today) BackgroundColors.Background50
        else if (!isEnabled) BackgroundColors.Background300
        else BackgroundColors.Background700

    Surface(
        color = bg,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .padding(vertical = if (isMonthly) 0.dp else 4.dp)
            .aspectRatio(1f),
        shadowElevation = if (isMonthly) 0.dp else 2.dp,
        onClick = { if (isEnabled) onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = if (!isMonthly) MaterialTheme.typography.titleLarge
                else MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = dayTextColor
            )
            if (!isMonthly) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color =
                        if (selected || date == today) BackgroundColors.Background200
                        else if (!isEnabled) BackgroundColors.Background300
                        else BackgroundColors.Background500
                )
            }
        }
    }
}
