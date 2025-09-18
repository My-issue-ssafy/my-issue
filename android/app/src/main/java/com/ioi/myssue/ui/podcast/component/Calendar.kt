package com.ioi.myssue.ui.podcast.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ioi.myssue.R
import com.ioi.myssue.designsystem.theme.AppColors
import com.ioi.myssue.designsystem.theme.BackgroundColors
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
                .align(Alignment.CenterHorizontally)
                .combinedClickable(onClick = onToggleView)
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

        Spacer(Modifier.height(16.dp))

        if (isMonthlyView) {
            BackHandler {
                onToggleView()
            }
            MonthCalendar(
                currentMonth = today,
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dates.forEach { date ->
            DayBoxCell(
                date = date,
                selected = date == selectedDate,
                isMonthly = false,
                onClick = { onDateSelected(date) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MonthCalendar(
    currentMonth: LocalDate,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val firstDayOfMonth = currentMonth.withDayOfMonth(1)
    val daysInMonth = currentMonth.lengthOfMonth()

    val daysThisMonth = remember(firstDayOfMonth) {
        (0 until daysInMonth).map { firstDayOfMonth.plusDays(it.toLong()) }
    }

    val firstDowSundayIndex = firstDayOfMonth.dayOfWeek.value % 7
    val leadingCount = firstDowSundayIndex

    val prevMonth = currentMonth.minusMonths(1)
    val prevLast = prevMonth.withDayOfMonth(prevMonth.lengthOfMonth())
    val leadingDays = remember(currentMonth, leadingCount) {
        List(leadingCount) { i -> prevLast.minusDays((leadingCount - 1 - i).toLong()) }
    }

    val totalSoFar = leadingDays.size + daysThisMonth.size
    val trailingCount = (7 - (totalSoFar % 7)).let { if (it == 7) 0 else it }
    val nextMonthFirst = currentMonth.plusMonths(1).withDayOfMonth(1)
    val trailingDays = remember(currentMonth, trailingCount) {
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
                        isCurrentMonth = date.month == currentMonth.month,
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
    isCurrentMonth: Boolean = true,
    onClick: () -> Unit,
) {
    val bg = when {
        selected -> AppColors.Primary400
        !isMonthly -> BackgroundColors.Background50
        else -> Color.Transparent
    }

    val dayTextColor =
        if (selected) BackgroundColors.Background50
        else if (!isCurrentMonth) BackgroundColors.Background400
        else BackgroundColors.Background700

    Surface(
        color = bg,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.aspectRatio(1f),
        onClick = onClick
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
                    color = if (selected) BackgroundColors.Background200 else BackgroundColors.Background500
                )
            }
        }
    }
}
