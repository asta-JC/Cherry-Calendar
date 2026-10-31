package com.asta.cherrycalendar

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import org.json.JSONObject
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalContext
import com.asta.cherrycalendar.ui.theme.CherryCalendarTheme
import kotlinx.coroutines.*
import java.time.*
import java.time.format.DateTimeFormatter

class MainActivity:ComponentActivity() {
private var openId by mutableLongStateOf(-1)
override fun onCreate(b:Bundle?){super.onCreate(b);openId=intent.getLongExtra("event_id",-1);enableEdgeToEdge();setContent{CalendarApp(openId)}}
override fun onNewIntent(i:Intent){super.onNewIntent(i);setIntent(i);openId=i.getLongExtra("event_id",-1)}
}

private val eventSaver = Saver<CalendarEvent?, String>(
    save = { e -> if(e == null) "" else JSONObject().apply {
        put("id",e.id);put("title",e.title);put("date",e.date.toString());put("start",e.start.toString());put("end",e.end.toString());put("all",e.allDay);put("category",e.category);put("location",e.location);put("notes",e.notes);put("repeat",e.repeat);put("reminder",e.reminder)
    }.toString() },
    restore = { raw -> if(raw.isEmpty()) null else JSONObject(raw).let { j -> CalendarEvent(j.getLong("id"),j.getString("title"),LocalDate.parse(j.getString("date")),LocalTime.parse(j.getString("start")),LocalTime.parse(j.getString("end")),j.getBoolean("all"),j.getString("category"),j.getString("location"),j.getString("notes"),j.getString("repeat"),j.getInt("reminder")) } }
)

private val dateFormat=DateTimeFormatter.ofPattern("EEE, d MMM yyyy")
private val timeFormat=DateTimeFormatter.ofPattern("HH:mm")
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CalendarApp(openId:Long, demoEvents:List<CalendarEvent>?=null){
val c=LocalContext.current;val scope=rememberCoroutineScope();val prefs=remember{c.getSharedPreferences("settings",0)}
var mode by rememberSaveable{mutableStateOf(prefs.getString("theme","System")?:"System")}
val dark=when(mode){"Light"->false;"Dark"->true;else->isSystemInDarkTheme()}
val store=remember{EventStore(c.applicationContext)}
var events by remember{mutableStateOf<List<CalendarEvent>>(emptyList())};var loading by remember{mutableStateOf(true)};var failure by remember{mutableStateOf(false)}
var dayText by rememberSaveable{mutableStateOf(LocalDate.now().toString())};val day=LocalDate.parse(dayText)
var monthText by rememberSaveable{mutableStateOf(YearMonth.now().toString())};val month=YearMonth.parse(monthText)
var search by rememberSaveable{mutableStateOf(false)};var query by rememberSaveable{mutableStateOf("")};var agenda by rememberSaveable{mutableStateOf(false)}
var settings by remember{mutableStateOf(false)};var editor by rememberSaveable(stateSaver=eventSaver){mutableStateOf<CalendarEvent?>(null)};var detail by rememberSaveable(stateSaver=eventSaver){mutableStateOf<CalendarEvent?>(null)};var deleting by remember{mutableStateOf(false)};var busy by remember{mutableStateOf(false)}
val snack=remember{SnackbarHostState()}
val focusManager=LocalFocusManager.current
val keyboard=LocalSoftwareKeyboardController.current
val calendarScroll=rememberLazyListState()
val searchScroll=rememberLazyListState()
fun closeSearch(){search=false;query="";focusManager.clearFocus();keyboard?.hide()}
BackHandler(enabled=search && editor==null && detail==null && !settings && !deleting){closeSearch()}
suspend fun load(){events=withContext(Dispatchers.IO){demoEvents ?: store.events()};loading=false;failure=false}
LaunchedEffect(Unit){try{load();if(demoEvents==null)withContext(Dispatchers.IO){Reminders.reschedule(c)}}catch(_:Exception){loading=false;failure=true}}
LaunchedEffect(openId,events){if(openId>0)detail=events.find{it.id==openId}}
val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){ok->if(!ok)scope.launch{snack.showSnackbar("Enable notifications in Android settings to receive reminders.")}}
val visible by produceState<List<Pair<LocalDate,CalendarEvent>>>(emptyList(),events,dayText,query,agenda){value=withContext(Dispatchers.Default){if(query.isNotBlank())events.filter{(it.title+" "+it.notes+" "+it.location).contains(query,true)}.map{it.date to it}else (0L..if(agenda)30L else 0L).flatMap{offset->val d=day.plusDays(offset);events.filter{it.occursOn(d)}.map{d to it}}}.sortedWith(compareBy<Pair<LocalDate,CalendarEvent>>{it.first}.thenBy{!it.second.allDay}.thenBy{it.second.start})}
fun create(){val s=LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0);editor=CalendarEvent(title="",date=day,start=s,end=if(s.hour==23)LocalTime.of(23,59) else s.plusHours(1))}
CherryCalendarTheme(dark){
SideEffect{(c as? ComponentActivity)?.enableEdgeToEdge(statusBarStyle=if(dark)SystemBarStyle.dark(0)else SystemBarStyle.light(0,0),navigationBarStyle=if(dark)SystemBarStyle.dark(0)else SystemBarStyle.light(0,0))}
Scaffold(containerColor=MaterialTheme.colorScheme.background,snackbarHost={SnackbarHost(snack)},topBar={Column(Modifier.statusBarsPadding()){
Row(Modifier.fillMaxWidth().padding(start=20.dp,end=8.dp),verticalAlignment=Alignment.CenterVertically){Image(painterResource(if(dark)R.drawable.cherryapps_logo_full_dark else R.drawable.cherryapps_logo_full),null,Modifier.size(32.dp).padding(2.dp));Text("Cherry Calendar",Modifier.weight(1f).padding(start=8.dp),fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge);IconButton(onClick={if(search)closeSearch() else search=true}){CherryIcon(Icons.Outlined.Search,"Search")};IconButton(onClick={settings=true}){CherryIcon(Icons.Outlined.Settings,"Settings")}}
if(search)OutlinedTextField(query,{query=it},Modifier.fillMaxWidth().padding(horizontal=20.dp),label={Text("Search title, place, notes")},singleLine=true,trailingIcon={IconButton(onClick={closeSearch()}){CherryIcon(Icons.Outlined.Close,"Close search")}})
}},floatingActionButton={ExtendedFloatingActionButton(onClick={create()},containerColor=MaterialTheme.colorScheme.primary,contentColor=MaterialTheme.colorScheme.onPrimary){CherryIcon(Icons.Outlined.Add);Spacer(Modifier.width(8.dp));Text("New event")}}){padding->
LazyColumn(Modifier.fillMaxSize().padding(padding),state=if(search)searchScroll else calendarScroll,contentPadding=PaddingValues(20.dp,16.dp,20.dp,100.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
item{Column(verticalArrangement=Arrangement.spacedBy(6.dp)){Text("Make room for a good day.",color=MaterialTheme.colorScheme.onSurfaceVariant);Text("Your day, at a glance",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text(day.format(dateFormat),color=MaterialTheme.colorScheme.onSurfaceVariant)}}
if(!search)item{MonthView(month,day,events,{monthText=it.toString()},{dayText=it.toString()})}
item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(if(search)"Search results"else "Your schedule",Modifier.weight(1f),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);TextButton(onClick={dayText=LocalDate.now().toString();monthText=YearMonth.now().toString()}){Text("Today")}}}
if(!search)item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(!agenda,{agenda=false},label={Text("Day")});FilterChip(agenda,{agenda=true},label={Text("Agenda · 31 days")})}}
if(loading)item{LinearProgressIndicator(Modifier.fillMaxWidth())}
if(failure)item{TextButton(onClick={scope.launch{try{load()}catch(_:Exception){}}}){Text("Couldn’t load events. Tap to retry.")}}
if(!loading&&visible.isEmpty())item{Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){Column(Modifier.fillMaxWidth().padding(24.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(if(search)"No matching events"else "A little room to breathe",style=MaterialTheme.typography.titleMedium);Text(if(search)"Try a different title, place, or note."else "Nothing scheduled. Add something to look forward to.",color=MaterialTheme.colorScheme.onSurfaceVariant);if(!search)TextButton(onClick={create()}){Text("Create an event")}}}}
items(visible,key={"${it.first}-${it.second.id}"}){(d,e)->Column(verticalArrangement=Arrangement.spacedBy(6.dp)){if(agenda||search)Text(d.format(dateFormat),style=MaterialTheme.typography.labelLarge);EventCard(e){detail=e}}}
if(!agenda&&!search){val tomorrow=events.filter{it.occursOn(day.plusDays(1))};if(tomorrow.isNotEmpty()){item{Text("For tomorrow",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)};items(tomorrow,key={"tomorrow-${it.id}"}){e->EventCard(e){detail=e}}}}
}}
if(settings) SettingsSheet(mode, { mode=it;prefs.edit().putString("theme",it).apply() }, { settings=false })
detail?.let { e -> EventDetails(e, { detail=null }, { detail=null;editor=e }, { deleting=true }) }
if(deleting)AlertDialog(onDismissRequest={deleting=false},title={Text("Delete event?")},text={Text("Removes this event and all its repetitions from Cherry Calendar.")},confirmButton={TextButton(enabled=!busy,onClick={val e=detail?:return@TextButton;busy=true;scope.launch{try{withContext(Dispatchers.IO){store.delete(e.id);Reminders.cancel(c,e.id)};load();detail=null;deleting=false}catch(_:Exception){snack.showSnackbar("Couldn’t delete. Try again.")}finally{busy=false}}}){CherryIcon(Icons.Outlined.DeleteOutline);Spacer(Modifier.width(8.dp));Text("Delete")}},dismissButton={TextButton(onClick={deleting=false}){Text("Cancel")}})
editor?.let{e->EventEditor(e,busy,{if(!busy)editor=null}){saved->busy=true;scope.launch{try{val stored=withContext(Dispatchers.IO){store.save(saved)};withContext(Dispatchers.IO){Reminders.schedule(c,stored)};load();editor=null;dayText=saved.date.toString();monthText=YearMonth.from(saved.date).toString();if(saved.reminder>=0&&Build.VERSION.SDK_INT>=33)permission.launch(Manifest.permission.POST_NOTIFICATIONS)}catch(_:Exception){snack.showSnackbar("Couldn’t save. Please try again.")}finally{busy=false}}}}
}}
@Composable private fun MonthView(month:YearMonth,selected:LocalDate,events:List<CalendarEvent>,move:(YearMonth)->Unit,pick:(LocalDate)->Unit){
val start=remember(month){month.atDay(1).minusDays((month.atDay(1).dayOfWeek.value-1).toLong())}
val marks=remember(events,month){(0L..41L).map{start.plusDays(it)}.filter{d->events.any{it.occursOn(d)}}.toSet()}
Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){Column(Modifier.fillMaxWidth().padding(12.dp)){
Row(verticalAlignment=Alignment.CenterVertically){Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),Modifier.weight(1f),fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium);IconButton(onClick={move(month.minusMonths(1))}){CherryIcon(Icons.Outlined.ChevronLeft,"Previous month")};IconButton(onClick={move(month.plusMonths(1))}){CherryIcon(Icons.Outlined.ChevronRight,"Next month")}}
Row{listOf("M","T","W","T","F","S","S").forEach{Text(it,Modifier.weight(1f),textAlign=TextAlign.Center,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
repeat(6){w->Row{repeat(7){col->val d=start.plusDays((w*7+col).toLong());val active=d==selected;Column(Modifier.weight(1f).heightIn(min=48.dp).clip(RoundedCornerShape(14.dp)).background(if(active)MaterialTheme.colorScheme.primary else Color.Transparent).clickable{pick(d)}.semantics{contentDescription=d.format(dateFormat);this.selected=active}.padding(vertical=5.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(d.dayOfMonth.toString(),color=if(active)MaterialTheme.colorScheme.onPrimary else if(YearMonth.from(d)!=month)MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,fontWeight=if(d==LocalDate.now())FontWeight.Bold else FontWeight.Normal);Text(if(d in marks)"•"else " ",fontSize=10.sp,lineHeight=10.sp,color=if(active)MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)}}}}
}}}
@Composable private fun EventCard(e:CalendarEvent,click:()->Unit){Card(onClick=click,shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer,contentColor=MaterialTheme.colorScheme.onPrimaryContainer)){Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.spacedBy(14.dp)){Text(if(e.allDay)"All day"else e.start.format(timeFormat),style=MaterialTheme.typography.labelLarge);Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)){Text(e.title,style=MaterialTheme.typography.titleMedium);Text((if(e.allDay)"All day"else "${e.start.format(timeFormat)} – ${e.end.format(timeFormat)}")+" · ${e.category}",style=MaterialTheme.typography.bodySmall);if(e.location.isNotBlank())Text(e.location,style=MaterialTheme.typography.bodySmall);if(e.repeat!="Never")Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){Icon(Icons.Outlined.Repeat,null,Modifier.size(16.dp));Text(e.repeat,style=MaterialTheme.typography.labelSmall)}}}}}
