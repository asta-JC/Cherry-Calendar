package com.asta.cherrycalendar
import org.junit.Assert.*
import org.junit.Test
import java.time.*
class CalendarEventTest {
 private fun event(date:String="2026-01-31",repeat:String="Never",reminder:Int=-1)=CalendarEvent(title="Test",date=LocalDate.parse(date),start=LocalTime.of(10,0),end=LocalTime.of(11,0),repeat=repeat,reminder=reminder)
 @Test fun monthlyClampsAtMonthEndWithoutDrift(){val e=event(repeat="Monthly");assertTrue(e.occursOn(LocalDate.parse("2026-02-28")));assertTrue(e.occursOn(LocalDate.parse("2026-03-31")));assertFalse(e.occursOn(LocalDate.parse("2026-03-28")))}
 @Test fun weeklyNeverOccursBeforeStart(){val e=event(repeat="Weekly");assertFalse(e.occursOn(e.date.minusWeeks(1)));assertTrue(e.occursOn(e.date.plusWeeks(1)));assertFalse(e.occursOn(e.date.plusDays(1)))}
 @Test fun leapYearAnniversary(){val e=event("2024-02-29","Yearly");assertTrue(e.occursOn(LocalDate.parse("2025-02-28")));assertTrue(e.occursOn(LocalDate.parse("2028-02-29")));assertFalse(e.occursOn(LocalDate.parse("2028-02-28")))}
 @Test fun recurringReminderFindsNextOccurrence(){val now=ZonedDateTime.of(2026,1,31,10,0,0,0,ZoneId.of("UTC"));val e=event(repeat="Daily",reminder=15);assertEquals(now.plusDays(1).minusMinutes(15).toInstant().toEpochMilli(),e.nextReminder(now))}
 @Test fun pastOneOffAndDisabledRemindersAreNotScheduled(){val now=ZonedDateTime.of(2026,2,1,0,0,0,0,ZoneId.of("UTC"));assertNull(event(reminder=0).nextReminder(now));assertNull(event(repeat="Daily").nextReminder(now))}
 @Test fun daylightSavingUsesLocalWallClock(){val now=ZonedDateTime.of(2026,3,7,12,0,0,0,ZoneId.of("America/New_York"));val e=event("2026-03-07","Daily",0);assertEquals(ZonedDateTime.of(2026,3,8,10,0,0,0,now.zone).toInstant().toEpochMilli(),e.nextReminder(now))}
}
