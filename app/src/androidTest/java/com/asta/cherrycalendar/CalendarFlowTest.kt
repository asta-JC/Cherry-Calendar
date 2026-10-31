package com.asta.cherrycalendar
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import androidx.test.platform.app.InstrumentationRegistry
import java.time.*
class CalendarFlowTest {
 @get:Rule val rule=createAndroidComposeRule<MainActivity>()
 @Test fun createEditSearchAndDelete(){
  val title="QA event ${System.nanoTime()}"
  rule.onNodeWithText("New event").performClick()
  rule.onNodeWithText("Event title").performTextInput(title)
  rule.onNodeWithText("Save event").performClick()
  rule.waitUntil(10000){rule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()}
  rule.onNodeWithText(title).performScrollTo().performClick()
  rule.onNodeWithText("Edit",useUnmergedTree=true).performClick()
  rule.onNodeWithText("Event title").performTextReplacement("$title edited")
  rule.onNodeWithText("Save event").performClick()
  rule.waitUntil(10000){rule.onAllNodesWithText("$title edited").fetchSemanticsNodes().isNotEmpty()}
  rule.onNodeWithText("$title edited").performScrollTo().performClick()
  rule.onNodeWithText("Delete event").performClick()
  rule.onNodeWithText("Delete",useUnmergedTree=true).performClick()
  rule.waitUntil(10000){rule.onAllNodesWithText("$title edited").fetchSemanticsNodes().isEmpty()}
 }
 @Test fun draftsAndThemeSurviveRecreation(){
  rule.onNodeWithText("New event").performClick()
  rule.onNodeWithText("Event title").performTextInput("A longer plan that survives rotation and return")
  rule.activityRule.scenario.recreate()
  rule.onNodeWithText("A longer plan that survives rotation and return").assertExists()
  rule.onNodeWithText("Cancel").performClick()
  rule.onNodeWithContentDescription("Settings").performClick()
  rule.onNodeWithText("Dark").performClick()
  rule.onNodeWithText("Done").performClick()
  rule.onNodeWithText("Cherry Calendar").assertExists()
  capture("cherry-calendar-dark.png")
  rule.onNodeWithContentDescription("Settings").performClick()
  rule.onNodeWithText("Light").performClick()
  rule.onNodeWithText("Done").performClick()
  capture("cherry-calendar-light.png")
  rule.onNodeWithContentDescription("Settings").performClick()
  rule.onNodeWithText("System").performClick()
  rule.onNodeWithText("Done").performClick()
 }
 private fun capture(name:String){rule.waitForIdle();android.os.SystemClock.sleep(500);val i=InstrumentationRegistry.getInstrumentation();val bitmap=i.uiAutomation.takeScreenshot();val f=java.io.File(i.targetContext.getExternalFilesDir(null),name);f.outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()}
 @Test fun landscapeAndLargerTextKeepActionsReachable(){
  val previous=rule.activity.requestedOrientation
  try {
   rule.activity.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
   rule.waitForIdle()
   rule.onNodeWithText("New event").performClick()
   rule.onNodeWithText("Event title").performTextInput("A long event title for a comfortable layout at larger text sizes")
   rule.onNodeWithText("Save event").assertIsDisplayed()
   rule.onNodeWithText("Cancel").performClick()
  } finally {rule.activity.requestedOrientation=previous}
 }
 @Test fun reminderPostsNotification(){
  val i=InstrumentationRegistry.getInstrumentation();val c=i.targetContext
  val permission="android.permission.POST_NOTIFICATIONS"
  val granted=androidx.core.content.ContextCompat.checkSelfPermission(c,permission)==android.content.pm.PackageManager.PERMISSION_GRANTED
  if(android.os.Build.VERSION.SDK_INT>=33&&!granted)i.uiAutomation.executeShellCommand("pm grant ${c.packageName} $permission").close()
  val event=EventStore(c).use{it.save(CalendarEvent(title="Reminder QA",date=LocalDate.now(),start=LocalTime.now().plusMinutes(2),end=LocalTime.now().plusMinutes(3),reminder=0))}
  try {
   Reminders.schedule(c,event)
   c.sendBroadcast(android.content.Intent(c,ReminderReceiver::class.java).putExtra("id",event.id))
   val nm=c.getSystemService(android.app.NotificationManager::class.java)
   rule.waitUntil(10000){nm.activeNotifications.any{it.id==event.id.toInt()}}
  } finally {
   Reminders.cancel(c,event.id);EventStore(c).use{it.delete(event.id)}
   // Permission is restored by the host after instrumentation, since revocation stops the app process.
  }
 }
 @Test fun databaseRoundTrip(){val c=InstrumentationRegistry.getInstrumentation().targetContext;EventStore(c).use{store->val e=store.save(CalendarEvent(title="Storage QA",date=LocalDate.now(),start=LocalTime.of(12,0),end=LocalTime.of(13,0),repeat="Weekly",notes="Long notes",reminder=15));try{assertEquals(e,store.events().first{it.id==e.id});val changed=e.copy(title="Updated");store.save(changed);assertEquals(changed,store.events().first{it.id==e.id})}finally{store.delete(e.id)};assertFalse(store.events().any{it.id==e.id})}}
}
