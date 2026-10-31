package com.asta.cherrycalendar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.time.*
import java.io.File

class SheetGestureTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun dragHandleDown() {
        rule.onNodeWithContentDescription("Sheet drag handle").performTouchInput {
            swipeDown(startY = centerY, endY = centerY + 700f, durationMillis = 350)
        }
    }
    private fun assertHandleStaysAt(y: Float) {
        rule.waitForIdle()
        assertEquals(y, rule.onNodeWithContentDescription("Sheet drag handle").getUnclippedBoundsInRoot().top.value, 2f)
    }
    @Test fun editorHandleDismissesAndLongPressHasNoTooltip() {
        rule.onNodeWithText("New event").performClick()
        val y = rule.onNodeWithContentDescription("Sheet drag handle").getUnclippedBoundsInRoot().top.value
        rule.onNodeWithContentDescription("Sheet drag handle").performTouchInput { down(center) }
        android.os.SystemClock.sleep(900)
        val i = InstrumentationRegistry.getInstrumentation()
        val bitmap = i.uiAutomation.takeScreenshot()
        File(i.targetContext.getExternalFilesDir(null), "v112-held-handle.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
        bitmap.recycle()
        rule.onNodeWithContentDescription("Sheet drag handle").performTouchInput { up() }
        assertHandleStaysAt(y)
        rule.onNodeWithText("Save event").assertIsDisplayed()
        dragHandleDown()
        rule.waitUntil(5000) { rule.onAllNodesWithText("Save event").fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithText("New event").assertExists()
    }
    @Test fun fastSettingsContentFlingsStayInsideAndHandleDismisses() {
        rule.onNodeWithContentDescription("Settings").performClick()
        val y = rule.onNodeWithContentDescription("Sheet drag handle").getUnclippedBoundsInRoot().top.value
        repeat(4) {
            rule.onNodeWithText("Make it yours").performScrollTo()
            rule.mainClock.autoAdvance = false
            try {
                rule.onNodeWithText("Make it yours").performTouchInput { swipeDown(durationMillis = 70) }
                repeat(20) { rule.mainClock.advanceTimeByFrame(); assertHandleStaysAt(y) }
            } finally { rule.mainClock.autoAdvance = true }
            assertHandleStaysAt(y)
            rule.onNodeWithText("Done").performScrollTo().performTouchInput { swipeUp(durationMillis = 70) }
            assertHandleStaysAt(y)
        }
        dragHandleDown()
        rule.waitUntil(5000) { rule.onAllNodesWithText("Make it yours").fetchSemanticsNodes().isEmpty() }
    }
    @Test fun detailsContentFlingsStayInsideAndHandleDismisses() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = EventStore(context)
        val e = store.save(CalendarEvent(title="Sheet gesture test",date=LocalDate.now(),start=LocalTime.of(9,0),end=LocalTime.of(10,0),notes="A long note. ".repeat(150),location="Test location",repeat="Weekly",reminder=15))
        try {
            rule.activityRule.scenario.recreate()
            rule.waitUntil(10000) { rule.onAllNodesWithText(e.title).fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithText(e.title).performScrollTo().performClick()
            val y = rule.onNodeWithContentDescription("Sheet drag handle").getUnclippedBoundsInRoot().top.value
            repeat(3) {
                rule.onNodeWithText("A moment in your calendar").performScrollTo().performTouchInput { swipeDown(durationMillis=70) }
                assertHandleStaysAt(y)
                rule.onNodeWithText(e.notes).performScrollTo().performTouchInput { swipeUp(durationMillis=70) }
                assertHandleStaysAt(y)
            }
            rule.onNodeWithText("Edit").assertIsDisplayed()
            dragHandleDown()
            rule.waitUntil(5000) { rule.onAllNodesWithText("Delete event").fetchSemanticsNodes().isEmpty() }
        } finally { store.delete(e.id);Reminders.cancel(context,e.id);store.close() }
    }
}
