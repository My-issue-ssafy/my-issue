package com.ioi.myssue.ui.cartoon

import android.R.attr.textColor
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ioi.myssue.R
import com.ioi.myssue.designsystem.theme.AppColors
import com.ioi.myssue.designsystem.theme.BackgroundColors
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun Calendar(
    modifier: Modifier = Modifier,
    onDateSelected: (LocalDate) -> Unit = {}
) {
    var isMonthlyView by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    var selectedDate by remember { mutableStateOf(today) }

    Column(
        modifier = modifier
            .animateContentSize() // 확장/축소 시 부드럽게
    ) {
        // 헤더 (클릭 시 주간/월간 토글)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .combinedClickable(
                    onClick = { isMonthlyView = !isMonthlyView }
                )
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
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(4.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        if (isMonthlyView) {
            MonthCalendar(
                currentMonth = today,
                selectedDate = selectedDate,
                onDateSelected = {
                    selectedDate = it
                    onDateSelected(it)
                }
            )
        } else {
            WeekCalendarRow(
                daysToShow = 5,
                selectedDate = selectedDate,
                onDateSelected = {
                    selectedDate = it
                    onDateSelected(it)
                }
            )
        }
    }
}

@Composable
private fun WeekCalendarRow(
    daysToShow: Int,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val dates = remember(today, daysToShow) {
        List(daysToShow) { i -> today.minusDays((daysToShow - 1 - i).toLong()) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
    val lastDayOfMonth = currentMonth.withDayOfMonth(currentMonth.lengthOfMonth())
    val days = (0 until lastDayOfMonth.dayOfMonth).map { firstDayOfMonth.plusDays(it.toLong()) }

    // 🔥 7의 배수 맞추기 (마지막 줄 균등 분배)
    val paddedDays = remember(days) {
        val total = if (days.size % 7 == 0) days.size else days.size + (7 - days.size % 7)
        List(total) { i -> days.getOrNull(i) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        paddedDays.chunked(7).forEach { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                week.forEach { date ->
                    if (date != null) {
                        DayBoxCell(
                            date = date,
                            selected = date == selectedDate,
                            isMonthly = true, // 월간 모드
                            onClick = { onDateSelected(date) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayBoxCell(
    date: LocalDate,
    selected: Boolean,
    isMonthly: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = when {
        selected -> AppColors.Primary400
        !isMonthly -> BackgroundColors.Background50
        else -> Color.Transparent
    }


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
                style = if(!isMonthly) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (selected) BackgroundColors.Background50 else BackgroundColors.Background700
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
