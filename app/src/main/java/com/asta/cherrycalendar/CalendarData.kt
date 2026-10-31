package com.asta.cherrycalendar

import android.content.*
import android.database.sqlite.*
import java.time.*

data class CalendarEvent(val id: Long = 0, val title: String, val date: LocalDate, val start: LocalTime, val end: LocalTime, val allDay: Boolean = false, val category: String = "Personal", val location: String = "", val notes: String = "", val repeat: String = "Never", val reminder: Int = -1) {
    fun occursOn(day: LocalDate): Boolean = day >= date && when (repeat) {
        "Daily" -> true
        "Weekly" -> day.dayOfWeek == date.dayOfWeek
        "Monthly" -> day.dayOfMonth == date.dayOfMonth.coerceAtMost(day.lengthOfMonth())
        "Yearly" -> day.month == date.month && day.dayOfMonth == date.dayOfMonth.coerceAtMost(day.lengthOfMonth())
        else -> day == date
    }
    fun nextReminder(now: ZonedDateTime = ZonedDateTime.now()): Long? {
        if (reminder < 0) return null
        var day = maxOf(date, now.toLocalDate())
        val limit = day.plusYears(5)
        while (day <= limit) {
            if (occursOn(day)) {
                val at = day.atTime(if (allDay) LocalTime.of(9,0) else start).atZone(now.zone).minusMinutes(reminder.toLong())
                if (at.isAfter(now)) return at.toInstant().toEpochMilli()
                if (repeat == "Never") return null
            }
            day = day.plusDays(1)
        }
        return null
    }
}

class EventStore(context: Context) : SQLiteOpenHelper(context, "calendar.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) { db.execSQL("CREATE TABLE events(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,date TEXT NOT NULL,start TEXT NOT NULL,end TEXT NOT NULL,allDay INTEGER NOT NULL,category TEXT NOT NULL,location TEXT NOT NULL,notes TEXT NOT NULL,repeatRule TEXT NOT NULL,reminder INTEGER NOT NULL)") }
    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) = Unit
    fun events(): List<CalendarEvent> = readableDatabase.query("events", null,null,null,null,null,"date,start").use { c -> buildList { while(c.moveToNext()) { fun s(k:String): String { return c.getString(c.getColumnIndexOrThrow(k)) }
 add(CalendarEvent(c.getLong(c.getColumnIndexOrThrow("id")),s("title"),LocalDate.parse(s("date")),LocalTime.parse(s("start")),LocalTime.parse(s("end")),s("allDay")=="1",s("category"),s("location"),s("notes"),s("repeatRule"),s("reminder").toInt())) } } }
    fun save(e: CalendarEvent): CalendarEvent { val v=ContentValues().apply { put("title",e.title);put("date",e.date.toString());put("start",e.start.toString());put("end",e.end.toString());put("allDay",if(e.allDay)1 else 0);put("category",e.category);put("location",e.location);put("notes",e.notes);put("repeatRule",e.repeat);put("reminder",e.reminder) }; return if(e.id==0L)e.copy(id=writableDatabase.insertOrThrow("events",null,v)) else { check(writableDatabase.update("events",v,"id=?",arrayOf(e.id.toString()))==1);e } }
    fun delete(id:Long) { writableDatabase.delete("events","id=?",arrayOf(id.toString())) }
}
