package com.asta.cherrycalendar

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.*
import java.time.format.DateTimeFormatter

private fun categoryIcon(category: String) = when(category) {
    "Work" -> Icons.Outlined.WorkOutline
    "Family" -> Icons.Outlined.Groups
    "Health" -> Icons.Outlined.FavoriteBorder
    else -> Icons.Outlined.PersonOutline
}
internal fun reminderLabel(minutes: Int): String = when(minutes) {
    -1 -> "No reminder"
    0 -> "At event time"
    60 -> "1 hour before"
    1440 -> "1 day before"
    else -> "$minutes minutes before"
}

@Composable private fun EditorField(value: String, change: (String) -> Unit, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, minLines: Int = 1, error: Boolean = false) {
    OutlinedTextField(value, change, Modifier.fillMaxWidth(), label = { Text(label) }, leadingIcon = { CherryIcon(icon) }, minLines = minLines, shape = RoundedCornerShape(18.dp), isError = error,
        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surface, focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun EventEditor(e: CalendarEvent, busy: Boolean, dismiss: () -> Unit, save: (CalendarEvent) -> Unit) {
    val context = LocalContext.current
    val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
    val format = remember(is24Hour) { DateTimeFormatter.ofPattern(if(is24Hour) "HH:mm" else "h:mm a") }
    var title by rememberSaveable(e.id) { mutableStateOf(e.title) }
    var ds by rememberSaveable(e.id) { mutableStateOf(e.date.toString()) }
    var ss by rememberSaveable(e.id) { mutableStateOf(e.start.toString()) }
    var es by rememberSaveable(e.id) { mutableStateOf(e.end.toString()) }
    var all by rememberSaveable(e.id) { mutableStateOf(e.allDay) }
    var category by rememberSaveable(e.id) { mutableStateOf(e.category) }
    var location by rememberSaveable(e.id) { mutableStateOf(e.location) }
    var notes by rememberSaveable(e.id) { mutableStateOf(e.notes) }
    var repeat by rememberSaveable(e.id) { mutableStateOf(e.repeat) }
    var reminder by rememberSaveable(e.id) { mutableIntStateOf(e.reminder) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var picker by rememberSaveable { mutableStateOf("") }
    val date = LocalDate.parse(ds)
    val start = LocalTime.parse(ss)
    val end = LocalTime.parse(es)
    val invalid = !all && end <= start
    CherrySheet(onDismiss = dismiss, dismissEnabled = !busy) {
        // ModalBottomSheet already handles the IME inset. Applying it again causes relayout jumps.
        Column(Modifier.fillMaxWidth().fillMaxHeight(.92f)) {
        Column(Modifier.weight(1f).clipToBounds().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SheetHeading(if(e.id == 0L) "A new plan" else "Edit your plan", "Make time for what matters", Icons.Outlined.EditCalendar) { if(!busy) dismiss() }
            EditorField(title, { title = it.take(200) }, "Event title", Icons.Outlined.Title, error = submitted && title.isBlank())
            if(submitted && title.isBlank()) Text("Give your plan a name", color = MaterialTheme.colorScheme.error)
            SectionLabel("When")
            CherryGroup {
                InfoRow(Icons.Outlined.CalendarMonth, "Date", date.format(longDate)) { picker = "date" }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Icon(Icons.Outlined.WbSunny, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) { Text("All day", fontWeight = FontWeight.Medium); Text("No specific start or end time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Switch(all, { all = it })
                }
            }
            if(!all) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TimeTile("Starts", start.format(format), Modifier.weight(1f)) { picker = "start" }
                    TimeTile("Ends", end.format(format), Modifier.weight(1f)) { picker = "end" }
                }
                if(invalid) Text("Choose an end time after the start on the same day.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            SectionLabel("A little context")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Personal", "Work", "Family", "Health")) { name ->
                    FilterChip(category == name, { category = name }, label = { Text(name) }, leadingIcon = { CherryIcon(if(category == name) Icons.Outlined.Check else categoryIcon(name)) })
                }
            }
            EditorField(location, { location = it.take(500) }, "Location (optional)", Icons.Outlined.LocationOn)
            EditorField(notes, { notes = it.take(5000) }, "Notes (optional)", Icons.Outlined.Notes, minLines = 2)
            SectionLabel("Keep it on your radar")
            CherryGroup {
                SelectionRow("Repeat", repeat, Icons.Outlined.Repeat, listOf("Never", "Daily", "Weekly", "Monthly", "Yearly")) { repeat = it }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                val reminders = listOf(-1, 0, 5, 15, 30, 60, 1440)
                SelectionRow("Reminder", reminderLabel(reminder), Icons.Outlined.NotificationsNone, reminders.map(::reminderLabel)) { label -> reminder = reminders.first { reminderLabel(it) == label } }
            }
            if(all && reminder >= 0) Text("All-day reminders are based on 09:00 local time.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if(e.repeat != "Never") Text("Changes apply to the entire repeating event.", style = MaterialTheme.typography.bodySmall)
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(enabled = !busy, onClick = {
                submitted = true
                if(title.isNotBlank() && !invalid) save(e.copy(title = title.trim(), date = date, start = start, end = end, allDay = all, category = category, location = location.trim(), notes = notes.trim(), repeat = repeat, reminder = reminder))
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
                CherryIcon(Icons.Outlined.Check); Spacer(Modifier.width(8.dp)); Text(if(busy) "Saving…" else "Save event")
            }
            TextButton(onClick = dismiss, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
        }
    }
    when(picker) {
        "date" -> CherryDatePicker(date, { picker = "" }) { ds = it.toString(); picker = "" }
        "start" -> CherryTimePicker(start, is24Hour, "Start time", { picker = "" }) {
            val duration = Duration.between(start, end).toMinutes().coerceAtLeast(15)
            ss = it.toString()
            if(it >= end && it.hour < 23) es = it.plusMinutes(duration.coerceAtMost((23 * 60 + 59 - it.hour * 60 - it.minute).toLong())).toString()
            picker = ""
        }
        "end" -> CherryTimePicker(end, is24Hour, "End time", { picker = "" }) { es = it.toString(); picker = "" }
    }
}

@Composable private fun TimeTile(label: String, time: String, modifier: Modifier, click: () -> Unit) {
    Surface(onClick = click, modifier = modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { CherryIcon(Icons.Outlined.Schedule); Text(label, style = MaterialTheme.typography.labelLarge) }
            Text(time, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable private fun SelectionRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, options: List<String>, select: (String) -> Unit) {
    var opened by rememberSaveable { mutableStateOf(false) }
    InfoRow(icon, label, value) { opened = true }
    if(opened) AlertDialog(onDismissRequest = { opened = false }, icon = { CherryIcon(icon) }, title = { Text(label) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            options.forEach { option ->
                Row(Modifier.fillMaxWidth().clickable { select(option); opened = false }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(option == value, onClick = { select(option); opened = false }); Text(option)
                }
            }
        }
    }, confirmButton = { TextButton(onClick = { opened = false }) { Text("Cancel") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun EventDetails(e: CalendarEvent, close: () -> Unit, edit: () -> Unit, delete: () -> Unit) {
    val use24 = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    val timeFormat = DateTimeFormatter.ofPattern(if(use24) "HH:mm" else "h:mm a")
    CherrySheet(onDismiss = close) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.92f)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconBadge(categoryIcon(e.category)); Spacer(Modifier.width(12.dp))
                Text(e.category, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                IconButton(onClick = close) { CherryIcon(Icons.Outlined.Close, "Close") }
            }
            Text(e.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("A moment in your calendar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            CherryGroup {
                InfoRow(Icons.Outlined.CalendarMonth, if(e.repeat == "Never") "Date" else "Repeats from", e.date.format(longDate))
                InfoRow(Icons.Outlined.Schedule, "Time", if(e.allDay) "All day" else "${e.start.format(timeFormat)} – ${e.end.format(timeFormat)}")
                if(e.location.isNotBlank()) InfoRow(Icons.Outlined.LocationOn, "Location", e.location)
            }
            if(e.repeat != "Never" || e.reminder >= 0) CherryGroup {
                if(e.repeat != "Never") InfoRow(Icons.Outlined.Repeat, "Repeat", e.repeat)
                if(e.reminder >= 0) InfoRow(Icons.Outlined.NotificationsNone, "Reminder", reminderLabel(e.reminder))
            }
            if(e.notes.isNotBlank()) CherryGroup { InfoRow(Icons.Outlined.Notes, "Notes", e.notes) }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = edit, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) { CherryIcon(Icons.Outlined.Edit); Spacer(Modifier.width(8.dp)); Text("Edit") }
            OutlinedButton(onClick = delete, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { CherryIcon(Icons.Outlined.DeleteOutline); Spacer(Modifier.width(8.dp)); Text("Delete event") }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun SettingsSheet(mode: String, change: (String) -> Unit, close: () -> Unit) {
    CherrySheet(onDismiss = close) {
        Column(Modifier.clipToBounds().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            SheetHeading("Make it yours", "A calmer calendar, your way", Icons.Outlined.Settings, close)
            SectionLabel("Appearance")
            CherryGroup {
                listOf("System", "Light", "Dark").forEach { option ->
                    val icon = when(option) { "Light" -> Icons.Outlined.LightMode; "Dark" -> Icons.Outlined.DarkMode; else -> Icons.Outlined.BrightnessAuto }
                    Row(Modifier.fillMaxWidth().clickable { change(option) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconBadge(icon)
                        Column(Modifier.weight(1f)) { Text(option, fontWeight = FontWeight.Medium); Text(when(option) { "Light" -> "Warm cream & soft rose"; "Dark" -> "Quiet plum & cherry pink"; else -> "Follow your device" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        RadioButton(mode == option, { change(option) })
                    }
                }
            }
            CherryGroup { InfoRow(Icons.Outlined.PrivacyTip, "Only yours", "Your events stay on this device. No account or internet required.") }
            CherryGroup { InfoRow(Icons.Outlined.NotificationsNone, "A gentle heads-up", "Reminder times are approximate. Android battery restrictions may delay notifications.") }
            Button(onClick = close, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Done") }
        }
    }
}
