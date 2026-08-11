package com.gffh.mobile.feature.availability

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.SlotView
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.AvailabilityRepository
import com.gffh.mobile.session.CurrentTeamStore
import kotlinx.coroutines.launch
import kotlinx.datetime.*

/**
 * SCR-AV-01 Calendar (month). Purpose: show at a glance when the team is
 * available, booked or being asked about.
 *
 * Only the availability dot is wired up in this pass - the navy (confirmed
 * fixture) and amber (pending request) indicators need the Arrange slice's
 * client-side repositories, which don't exist yet. The list-view parity
 * requirement from the spec's global conventions is met: [showAsList] is a
 * first-class alternative, not an afterthought.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    availabilityRepository: AvailabilityRepository,
    currentTeamStore: CurrentTeamStore,
    navigator: Navigator
) {
    val team = currentTeamStore.active.collectAsState().value
    var monthCursor by remember { mutableStateOf(todayDate()) }
    var slots by remember { mutableStateOf<List<SlotView>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAsList by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val monthStart = LocalDate(monthCursor.year, monthCursor.month, 1)
    val monthEnd = monthStart.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)

    fun reload() {
        val t = team ?: return
        loading = true
        scope.launch {
            val result = availabilityRepository.list(t.teamId, monthStart.toString(), monthEnd.toString())
            loading = false
            when (result) {
                is ApiResult.Success -> { slots = result.value; errorMessage = null }
                is ApiResult.Failure -> errorMessage = result.message
            }
        }
    }

    LaunchedEffect(team?.teamId, monthCursor) { reload() }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(team?.teamName ?: "Calendar") },
            actions = {
                TextButton(onClick = { showAsList = !showAsList }) {
                    Text(if (showAsList) "Grid" else "List")
                }
            }
        )

        if (team == null) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("No active team. Create or select a team first.")
            }
            return
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { monthCursor = monthStart.minus(1, DateTimeUnit.MONTH) }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
            }
            Text("${monthStart.month.name} ${monthStart.year}", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { monthCursor = monthStart.plus(1, DateTimeUnit.MONTH) }) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
            }
        }

        val datesWithSlots = slots.map { it.date }.toSet()
        Text(
            "${datesWithSlots.size} date${if (datesWithSlots.size == 1) "" else "s"} published this month",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(8.dp))
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            LegendDot(Color(0xFF4CAF50)); Text(" Available   ", style = MaterialTheme.typography.bodySmall)
            LegendDot(Color(0xFF1A237E)); Text(" Confirmed   ", style = MaterialTheme.typography.bodySmall)
            LegendDot(Color(0xFFFFA000)); Text(" Pending", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(8.dp))

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
        }

        if (loading) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (showAsList) {
            DayList(monthStart, monthEnd, datesWithSlots) { date -> navigator.push(Route.DayDetail(date.toString())) }
        } else {
            MonthGrid(monthStart, datesWithSlots) { date -> navigator.push(Route.DayDetail(date.toString())) }
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = { navigator.push(Route.EditAvailabilitySlot(date = todayDate().toString())) },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add availability")
        }
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(Modifier.size(10.dp).clip(CircleShape).background(color))
}

@Composable
private fun MonthGrid(monthStart: LocalDate, datesWithSlots: Set<String>, onDayClick: (LocalDate) -> Unit) {
    val firstDayOffset = monthStart.dayOfWeek.isoDayNumber - 1 // Monday = 0
    val daysInMonth = monthStart.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).dayOfMonth
    val today = todayDate()

    LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.padding(horizontal = 16.dp)) {
        items(firstDayOffset) { Box(Modifier.aspectRatio(1f)) }
        items(daysInMonth) { i ->
            val date = LocalDate(monthStart.year, monthStart.month, i + 1)
            val hasSlot = datesWithSlots.contains(date.toString())
            val isToday = date == today
            Box(
                Modifier.aspectRatio(1f).padding(2.dp)
                    .clip(CircleShape)
                    .background(if (isToday) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .clickable { onDayClick(date) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text((i + 1).toString(), textAlign = TextAlign.Center)
                    if (hasSlot) LegendDot(Color(0xFF4CAF50))
                }
            }
        }
    }
}

@Composable
private fun DayList(monthStart: LocalDate, monthEnd: LocalDate, datesWithSlots: Set<String>, onDayClick: (LocalDate) -> Unit) {
    val days = generateSequence(monthStart) { d -> if (d < monthEnd) d.plus(1, DateTimeUnit.DAY) else null }
        .filter { datesWithSlots.contains(it.toString()) }
        .toList()

    if (days.isEmpty()) {
        Text(
            "Nothing published this month yet.",
            modifier = Modifier.padding(24.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }

    Column(Modifier.padding(horizontal = 16.dp)) {
        days.forEach { date ->
            Row(
                Modifier.fillMaxWidth().clickable { onDayClick(date) }.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(Color(0xFF4CAF50))
                Spacer(Modifier.width(12.dp))
                Text(date.toString())
            }
            HorizontalDivider()
        }
    }
}

private fun todayDate(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
