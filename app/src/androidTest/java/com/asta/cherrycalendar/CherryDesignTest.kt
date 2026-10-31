package com.asta.cherrycalendar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.*
import java.io.File

class CherryDesignTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun capture(name: String) {
        rule.waitForIdle()
        android.os.SystemClock.sleep(500)
        val i = InstrumentationRegistry.getInstrumentation()
        val bitmap = i.uiAutomation.takeScreenshot()
        File(i.targetContext.getExternalFilesDir(null), name).outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    @Test fun customPickersCommitExactValuesAndDetailsUseIcons() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = EventStore(context)
        val prefs = context.getSharedPreferences("settings",0)
        val previousTheme = prefs.getString("theme","System")
        prefs.edit().putString("theme","Light").commit()
        val event = store.save(CalendarEvent(title = "A little time with friends · QA", date = LocalDate.now(), start = LocalTime.of(9, 0), end = LocalTime.of(10, 0), location = "Our favourite café", notes = "A slow morning and a good conversation.", repeat = "Weekly", reminder = 15))
        try {
            rule.activityRule.scenario.recreate()
            rule.onNodeWithContentDescription("Settings").performClick()
            rule.onNodeWithText("Light",useUnmergedTree=true).performScrollTo().performClick()
            rule.onNodeWithText("Done").performScrollTo().performClick()
            rule.waitUntil(10000) { rule.onAllNodesWithText(event.title).fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithText(event.title).performScrollTo().performClick()
            capture("v11-details-light.png")
            rule.onNodeWithText("Edit").performClick()
            capture("v11-editor-light.png")
            rule.onNodeWithText("Date", useUnmergedTree = true).performClick()
            rule.onNodeWithText("Tomorrow").performClick()
            capture("v11-date-light.png")
            rule.onNodeWithText("Use this date").performScrollTo().performClick()
            rule.onNodeWithText("Starts").performScrollTo().performClick()
            rule.onNodeWithText(":15").performScrollTo().performClick()
            rule.waitForIdle()
            capture("v11-time-light.png")
            rule.onNodeWithText("Use this time").performScrollTo().performClick()
            rule.onNodeWithText("Ends").performScrollTo().performClick()
            rule.onNodeWithContentDescription("Minute").performScrollToIndex(59)
            rule.waitForIdle()
            rule.onNodeWithText("Use this time").performScrollTo().performClick()
            rule.onNodeWithText("Save event").performClick()
            rule.waitUntil(10000) { store.events().firstOrNull { it.id == event.id }?.date == LocalDate.now().plusDays(1) }
            val updated = store.events().first { it.id == event.id }
            assertEquals(LocalTime.of(9, 15), updated.start)
            assertEquals(LocalTime.of(10, 59), updated.end)
            rule.onNodeWithContentDescription("Settings").performClick()
            rule.onNodeWithText("Dark", useUnmergedTree = true).performScrollTo().performClick()
            capture("v11-settings-dark.png")
            rule.onNodeWithText("Done").performScrollTo().performClick()
            rule.onNodeWithText(event.title).performScrollTo().performClick()
            capture("v11-details-dark.png")
            rule.onNodeWithText("Edit").performClick()
            capture("v11-editor-dark.png")
            rule.onNodeWithText("Starts").performScrollTo().performClick()
            capture("v11-time-dark.png")
            rule.onNodeWithText("Use this time").performScrollTo().performClick()
            rule.onNodeWithText("Cancel").performClick()
            rule.onNodeWithContentDescription("Settings").performClick()
            rule.onNodeWithText("System", useUnmergedTree = true).performScrollTo().performClick()
            rule.onNodeWithText("Done").performScrollTo().performClick()
        } finally {
            store.delete(event.id); Reminders.cancel(context, event.id); store.close()
            prefs.edit().putString("theme",previousTheme).commit()
        }
    }
    @Test fun pickerLayoutsAdaptAndInstalledIconMatches() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val drawable = context.packageManager.getApplicationIcon(context.packageName)
        assertTrue(drawable is android.graphics.drawable.AdaptiveIconDrawable)
        if(android.os.Build.VERSION.SDK_INT >= 33) assertNotNull((drawable as android.graphics.drawable.AdaptiveIconDrawable).monochrome)
        val bitmap = android.graphics.Bitmap.createBitmap(432,432,android.graphics.Bitmap.Config.ARGB_8888)
        drawable.setBounds(0,0,432,432);drawable.draw(android.graphics.Canvas(bitmap))
        File(context.getExternalFilesDir(null), "v11-installed-icon.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
        val previous = rule.activity.requestedOrientation
        try {
            rule.activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            rule.waitForIdle()
            rule.onNodeWithText("New event").performClick()
            rule.onNodeWithText("Date",useUnmergedTree=true).performScrollTo().performClick()
            rule.onNodeWithText("Tomorrow").performScrollTo().performClick()
            rule.onNodeWithText("Use this date").performScrollTo().assertIsDisplayed().performClick()
            rule.onNodeWithText("Starts").performScrollTo().performClick()
            rule.onNodeWithText("Use this time").performScrollTo().assertIsDisplayed().performClick()
            rule.onNodeWithText("Save event").assertIsDisplayed()
            rule.onNodeWithText("Cancel").performClick()
        } finally { rule.activity.requestedOrientation = previous }
    }
    @Test fun cancelingDateDoesNotChangeDraft() {
        rule.onNodeWithText("New event").performClick()
        rule.onNodeWithText("Date", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Tomorrow").performClick()
        rule.onAllNodesWithContentDescription("Close").onLast().performClick()
        rule.onNode(hasText(LocalDate.now().format(longDate)) and hasClickAction()).assertExists()
        rule.onNodeWithText("Cancel").performClick()
    }
}
