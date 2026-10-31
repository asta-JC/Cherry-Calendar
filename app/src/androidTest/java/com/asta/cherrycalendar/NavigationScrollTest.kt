package com.asta.cherrycalendar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class NavigationScrollTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun backFromSearchRestoresCalendar() {
        rule.onNodeWithContentDescription("Search").performClick()
        rule.onNodeWithText("Search title, place, notes").performTextInput("No matching plan")
        Espresso.closeSoftKeyboard()
        Espresso.pressBack()
        rule.onNodeWithText("Search title, place, notes").assertDoesNotExist()
        rule.onNodeWithText("Your schedule").assertExists()
        rule.onNodeWithText("New event").assertExists()
        assertFalse(rule.activity.isFinishing)
    }

    @Test fun editorScrollCannotDragSheetOrMoveSaveAction() {
        rule.onNodeWithText("New event").performClick()
        val before = rule.onNodeWithText("Save event").getUnclippedBoundsInRoot().top
        rule.onNodeWithText("A new plan").performTouchInput { swipeDown() }
        rule.onNodeWithText("Save event").assertIsDisplayed()
        assertEquals(before.value, rule.onNodeWithText("Save event").getUnclippedBoundsInRoot().top.value, 2f)
        rule.onNodeWithText("Reminder", useUnmergedTree=true).performScrollTo()
        rule.onNodeWithText("Reminder", useUnmergedTree=true).performTouchInput { swipeUp() }
        rule.onNodeWithText("Save event").assertIsDisplayed()
        assertEquals(before.value, rule.onNodeWithText("Save event").getUnclippedBoundsInRoot().top.value, 2f)
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("Your schedule").assertExists()
    }
    @Test fun keyboardAndLongFormScrollKeepDraftAndActionsVisible() {
        rule.onNodeWithText("New event").performClick()
        rule.onNodeWithText("Event title").performTextInput("Scrolling draft")
        rule.onNodeWithText("Notes (optional)").performScrollTo().performTextInput("A longer note that stays intact while the keyboard and form are scrolled.")
        rule.onNodeWithText("Reminder",useUnmergedTree=true).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Save event").assertIsDisplayed()
        Espresso.closeSoftKeyboard()
        rule.onNodeWithText("Event title").performScrollTo()
        rule.onNodeWithText("Scrolling draft").assertExists()
        rule.onNodeWithText("Cancel").assertIsDisplayed().performClick()
        rule.onNodeWithText("New event").assertExists()
    }

}
