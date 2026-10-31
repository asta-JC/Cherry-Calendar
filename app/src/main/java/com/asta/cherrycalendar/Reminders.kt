package com.asta.cherrycalendar

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors

object Reminders {
    private fun pending(c:Context,id:Long):PendingIntent = PendingIntent.getBroadcast(c,id.toInt(),Intent(c,ReminderReceiver::class.java).setData(android.net.Uri.parse("cherrycalendar://reminder/$id")).putExtra("id",id),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun cancel(c:Context,id:Long) { c.getSystemService(AlarmManager::class.java).cancel(pending(c,id));c.getSystemService(NotificationManager::class.java).cancel(id.toInt()) }
    fun schedule(c:Context,e:CalendarEvent) { cancel(c,e.id);e.nextReminder()?.let { c.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,it,pending(c,e.id)) } }
    fun reschedule(c:Context) { EventStore(c).use { it.events().forEach { e->schedule(c,e) } } }
}
class ReminderReceiver:BroadcastReceiver() {
    override fun onReceive(context:Context,intent:Intent) { val result=goAsync();executor.execute { try { if(intent.action!=null) { Reminders.reschedule(context);return@execute }; val id=intent.getLongExtra("id",-1);val e=EventStore(context).use { it.events().find { e->e.id==id } }?:return@execute
        val manager=context.getSystemService(NotificationManager::class.java)
        if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(NotificationChannel("events","Event reminders",NotificationManager.IMPORTANCE_DEFAULT))
        if(Build.VERSION.SDK_INT<33||ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED) {
            val open=PendingIntent.getActivity(context,id.toInt(),Intent(context,MainActivity::class.java).putExtra("event_id",id),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            manager.notify(id.toInt(),NotificationCompat.Builder(context,"events").setSmallIcon(R.drawable.ic_notification).setContentTitle(e.title).setContentText(if(e.allDay)"All-day event" else e.start.format(DateTimeFormatter.ofPattern("HH:mm"))+if(e.location.isBlank())"" else " · ${e.location}").setContentIntent(open).setAutoCancel(true).build())
        };Reminders.schedule(context,e)
    } finally { result.finish() } } }
    companion object { private val executor=Executors.newSingleThreadExecutor() }
}
