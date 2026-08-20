package com.gffh.mobile.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private fun twoDigit(n: Int): String = if (n < 10) "0$n" else "$n"

/** Renders "HH:MM" and opens a Material3 TimePicker dialog on tap. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(
    label: String,
    hour: Int,
    minute: Int,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }

    OutlinedButton(onClick = { open = true }, modifier = modifier) {
        Text("$label: ${twoDigit(hour)}:${twoDigit(minute)}")
    }

    if (open) {
        val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("Select $label") },
            text = { TimePicker(state = state, modifier = Modifier.padding(8.dp)) },
            confirmButton = {
                TextButton(onClick = {
                    onTimeSelected(state.hour, state.minute)
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } }
        )
    }
}

/** Parses a "HH:MM" string into (hour, minute), defaulting to 10:00 on malformed input. */
fun parseHourMinute(value: String): Pair<Int, Int> {
    val parts = value.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 10
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return hour to minute
}

fun formatHourMinute(hour: Int, minute: Int): String = "${twoDigit(hour)}:${twoDigit(minute)}"
