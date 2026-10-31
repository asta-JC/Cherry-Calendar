package com.asta.cherrycalendar

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlinx.coroutines.launch

internal val longDate = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")
internal val clockFormat = DateTimeFormatter.ofPattern("HH:mm")

@Composable internal fun CherryIcon(icon: ImageVector, description: String? = null, modifier: Modifier = Modifier) {
    Icon(icon, description, modifier.size(22.dp))
}

@Composable internal fun IconBadge(icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(modifier.size(48.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
        Box(contentAlignment = Alignment.Center) { CherryIcon(icon) }
    }
}

@Composable internal fun SheetHeading(title: String, subtitle: String, icon: ImageVector, close: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconBadge(icon)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = close) { CherryIcon(Icons.Outlined.Close, "Close") }
    }
}

@Composable internal fun SectionLabel(title: String) {
    Text(title, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}

@Composable internal fun InfoRow(icon: ImageVector, label: String, value: String, onClick: (() -> Unit)? = null) {
    val modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(16.dp)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
        if (onClick != null) CherryIcon(Icons.Outlined.ChevronRight)
    }
}

@Composable internal fun CherryGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(content = content)
    }
}

@Composable private fun PickerShell(title: String, subtitle: String, icon: ImageVector, dismiss: () -> Unit, confirm: () -> Unit, confirmLabel: String, enabled: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = dismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 440.dp).fillMaxWidth().padding(horizontal = 16.dp).heightIn(max = 700.dp), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SheetHeading(title, subtitle, icon, dismiss)
                content()
                Button(onClick = confirm, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    CherryIcon(Icons.Outlined.Check); Spacer(Modifier.width(8.dp)); Text(confirmLabel)
                }
            }
        }
    }
}

@Composable internal fun CherryDatePicker(initial: LocalDate, dismiss: () -> Unit, choose: (LocalDate) -> Unit) {
    var selectedText by rememberSaveable { mutableStateOf(initial.toString()) }
    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(initial).toString()) }
    var years by rememberSaveable { mutableStateOf(false) }
    val selected = LocalDate.parse(selectedText)
    val month = YearMonth.parse(monthText)
    fun select(day: LocalDate) { selectedText = day.toString(); monthText = YearMonth.from(day).toString() }
    PickerShell("Choose a date", "A little space for your plans", Icons.Outlined.CalendarMonth, dismiss, { choose(selected) }, "Use this date") {
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(selected.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(selected.format(DateTimeFormatter.ofPattern("d MMMM yyyy")), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("Today" to LocalDate.now(), "Tomorrow" to LocalDate.now().plusDays(1), "Next week" to LocalDate.now().plusWeeks(1))) { (label, date) ->
                SuggestionChip(onClick = { select(date) }, label = { Text(label) })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { years = !years }, modifier = Modifier.weight(1f)) {
                Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), fontWeight = FontWeight.Bold)
                CherryIcon(if (years) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore)
            }
            IconButton(onClick = { monthText = month.minusMonths(1).toString() }) { CherryIcon(Icons.Outlined.ChevronLeft, "Previous month") }
            IconButton(onClick = { monthText = month.plusMonths(1).toString() }) { CherryIcon(Icons.Outlined.ChevronRight, "Next month") }
        }
        if (years) {
            val yearList = (1900..maxOf(2100, selected.year + 20)).toList()
            val state = rememberLazyListState(initialFirstVisibleItemIndex = (month.year - 1902).coerceIn(0, yearList.lastIndex))
            LazyColumn(Modifier.height(288.dp).fillMaxWidth(), state = state) {
                items(yearList, key = { it }) { year ->
                    Row(Modifier.fillMaxWidth().selectableYear(year == month.year) { monthText = month.withYear(year).toString(); years = false }.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(year.toString()); if (year == month.year) CherryIcon(Icons.Outlined.Check)
                    }
                }
            }
        } else {
            Row { listOf("M", "T", "W", "T", "F", "S", "S").forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            val first = month.atDay(1).minusDays((month.atDay(1).dayOfWeek.value - 1).toLong())
            Column {
                repeat(6) { week -> Row {
                    repeat(7) { column ->
                        val day = first.plusDays((week * 7 + column).toLong())
                        val active = day == selected
                        Box(Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(13.dp)).background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent).clickable { select(day) }.semantics { contentDescription = day.format(longDate); this.selected = active }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text(day.dayOfMonth.toString(), fontWeight = if (active || day == LocalDate.now()) FontWeight.Bold else FontWeight.Normal, color = if (active) MaterialTheme.colorScheme.onPrimary else if (day.month != month.month) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                } }
            }
        }
    }
}

private fun Modifier.selectableYear(selected: Boolean, click: () -> Unit) = this.clickable(onClick = click).semantics { this.selected = selected }

@Composable private fun NumberWheel(label: String, values: List<String>, initial: Int, onChange: (Int) -> Unit, state: LazyListState) {
    val scope = rememberCoroutineScope()
    val centerIndex by remember { derivedStateOf {
        val info = state.layoutInfo
        val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
        info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index ?: initial
    } }
    LaunchedEffect(centerIndex) { onChange(centerIndex) }
    Column(Modifier.widthIn(min = 84.dp, max = 140.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.height(240.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(Modifier.fillMaxWidth().height(48.dp), color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(14.dp)) {}
            LazyColumn(state = state, contentPadding = PaddingValues(vertical = 96.dp), flingBehavior = rememberSnapFlingBehavior(state), modifier = Modifier.fillMaxSize().semantics { contentDescription = label }) {
                itemsIndexed(values) { index, number ->
                    Box(Modifier.fillMaxWidth().height(48.dp).clickable { scope.launch { state.animateScrollToItem(index) } }.semantics { contentDescription = "$label $number"; selected = index == centerIndex }, contentAlignment = Alignment.Center) {
                        Text(number, style = MaterialTheme.typography.headlineSmall, fontWeight = if (index == centerIndex) FontWeight.Bold else FontWeight.Normal, color = if (index == centerIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .65f))
                    }
                }
            }
        }
    }
}

@Composable internal fun CherryTimePicker(initial: LocalTime, use24Hour: Boolean, title: String, dismiss: () -> Unit, choose: (LocalTime) -> Unit) {
    var hour by rememberSaveable { mutableIntStateOf(if (use24Hour) initial.hour else (initial.hour + 11) % 12) }
    var minute by rememberSaveable { mutableIntStateOf(initial.minute) }
    var afternoon by rememberSaveable { mutableStateOf(initial.hour >= 12) }
    val hours = remember(use24Hour) { if (use24Hour) (0..23).map { "%02d".format(it) } else (1..12).map { it.toString() } }
    val minutes = remember { (0..59).map { "%02d".format(it) } }
    val hourState = rememberLazyListState(initialFirstVisibleItemIndex = hour)
    val minuteState = rememberLazyListState(initialFirstVisibleItemIndex = minute)
    val scope = rememberCoroutineScope()
    val time = LocalTime.of(if (use24Hour) hour else ((hour + 1) % 12) + if (afternoon) 12 else 0, minute)
    PickerShell(title, "Scroll to find your perfect time", Icons.Outlined.Schedule, dismiss, { choose(time) }, "Use this time", !hourState.isScrollInProgress && !minuteState.isScrollInProgress) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Text(time.format(DateTimeFormatter.ofPattern(if (use24Hour) "HH:mm" else "h:mm a")), Modifier.fillMaxWidth().padding(20.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { NumberWheel("Hour", hours, hour, { hour = it }, hourState) }
            Text(":", Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.headlineMedium)
            Box(Modifier.weight(1f)) { NumberWheel("Minute", minutes, minute, { minute = it }, minuteState) }
        }
        if (!use24Hour) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            FilterChip(!afternoon, { afternoon = false }, label = { Text("AM") }); Spacer(Modifier.width(12.dp)); FilterChip(afternoon, { afternoon = true }, label = { Text("PM") })
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(0, 15, 30, 45)) { value -> SuggestionChip(onClick = { scope.launch { minuteState.animateScrollToItem(value) } }, label = { Text(":%02d".format(value)) }) }
        }
    }
}
