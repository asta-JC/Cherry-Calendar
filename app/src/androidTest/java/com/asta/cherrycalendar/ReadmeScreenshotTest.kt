package com.asta.cherrycalendar

import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import java.time.*
import java.io.File

/** Production composables with an in-memory event source; no demo events are stored. */
class ReadmeScreenshotTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun captureThreeLightScreens() {
        val i = InstrumentationRegistry.getInstrumentation()
        val prefs = i.targetContext.getSharedPreferences("settings",0)
        val previous = prefs.getString("theme","System")
        val day = LocalDate.now()
        val examples = listOf(
            CalendarEvent(101,"Team catch-up",day,LocalTime.of(9,0),LocalTime.of(10,0),category="Work",location="The studio",notes="Share ideas and plan the week ahead.",repeat="Weekly",reminder=15),
            CalendarEvent(102,"A little time outdoors",day,LocalTime.of(14,0),LocalTime.of(15,0),category="Health"),
            CalendarEvent(103,"Dinner with family",day.plusDays(1),LocalTime.of(18,0),LocalTime.of(19,0),category="Family",reminder=30),
        )
        try {
            rule.activityRule.scenario.onActivity { activity -> activity.setContent { CalendarApp(-1,examples) } }
            rule.onNodeWithContentDescription("Settings").performClick()
            rule.onNodeWithText("Light",useUnmergedTree=true).performScrollTo().performClick()
            rule.onNodeWithText("Done").performScrollTo().performClick()
            rule.onNode(hasScrollToIndexAction()).performScrollToIndex(1)
            capture("calendar-light.png")
            rule.onNodeWithText("Team catch-up").performScrollTo().performClick()
            rule.onNodeWithText("Edit").performClick()
            rule.onNodeWithText("Starts").performScrollTo().performClick()
            capture("time-picker-light.png")
            rule.onNodeWithText("Use this time").performScrollTo().performClick()
            rule.onNodeWithText("Cancel").performClick()
            rule.onNodeWithContentDescription("Settings").performClick()
            capture("settings-light.png")
            rule.onNodeWithText("Done").performScrollTo().performClick()
        } finally { prefs.edit().putString("theme",previous).commit() }
    }
    private fun capture(name:String) {
        rule.waitForIdle();android.os.SystemClock.sleep(500)
        val i=InstrumentationRegistry.getInstrumentation()
        val bitmap=i.uiAutomation.takeScreenshot()
        val insets=rule.activity.window.decorView.rootWindowInsets.getInsets(android.view.WindowInsets.Type.systemBars())
        val app=android.graphics.Bitmap.createBitmap(bitmap,0,insets.top,bitmap.width,bitmap.height-insets.top-insets.bottom)
        File(i.targetContext.getExternalFilesDir(null),name).outputStream().use { app.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
        app.recycle();bitmap.recycle()
    }
}
