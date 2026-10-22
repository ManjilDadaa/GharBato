package com.example.gharbato

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gharbato.ui.theme.GharBatoTheme
import com.example.gharbato.view.ListingBody
import com.example.gharbato.view.ListingProgressHeader
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ListingLayoutInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun introScrollsAwayAndProgressPinsBelowTheFixedAppBar() {
        val draftId = "listing-layout-test-${UUID.randomUUID()}"
        var pixelsPerDp = 1f
        compose.setContent {
            pixelsPerDp = LocalDensity.current.density
            GharBatoTheme(darkTheme = false) {
                Box(Modifier.width(320.dp).height(560.dp)) { ListingBody(propertyId = draftId) }
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("listing_form_scroll").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("List your property").assertDoesNotExist()
        compose.onNodeWithText("List Your Property").assertIsDisplayed()
        compose.onNodeWithText("Reach thousands of potential buyers and renters").assertIsDisplayed()
        compose.onNodeWithContentDescription("Property icon").assertIsDisplayed()
        compose.onNodeWithText("Save draft").assertDoesNotExist()
        val appBar = compose.onNodeWithTag("listing_app_bar")
        val form = compose.onNodeWithTag("listing_form_scroll")
        val intro = compose.onNodeWithTag("listing_intro")
        val progress = compose.onNodeWithTag("listing_progress")
        val initialAppBar = appBar.fetchSemanticsNode().boundsInRoot
        val initialIntro = intro.fetchSemanticsNode().boundsInRoot
        val initialTop = progress.fetchSemanticsNode().boundsInRoot.top
        assertTrue("The intro belongs below the fixed app bar", initialIntro.top >= initialAppBar.bottom - 1f)
        assertTrue("Progress starts after the full intro", initialTop >= initialIntro.bottom - 1f)
        capture("listing-first-page-intro")
        fun titleLuminance(): Double {
            val pixels = compose.onNodeWithText("List Your Property").captureToImage().toPixelMap()
            var luminance = 0.0
            for (y in 0 until pixels.height) {
                for (x in 0 until pixels.width) {
                    luminance += pixels[x, y].luminance()
                }
            }
            return luminance / (pixels.width * pixels.height)
        }
        val initialTitleLuminance = titleLuminance()

        fun assertAppBarFixed() {
            val bounds = appBar.fetchSemanticsNode().boundsInRoot
            assertEquals(initialAppBar.top, bounds.top, 1f)
            assertEquals(initialAppBar.bottom, bounds.bottom, 1f)
            compose.onNodeWithContentDescription("Exit listing").assertIsDisplayed()
        }
        fun assertProgressPinned(): Float {
            assertAppBarFixed()
            progress.assertIsDisplayed()
            val top = progress.fetchSemanticsNode().boundsInRoot.top
            assertTrue("Pinned progress must stay below the app bar", top >= initialAppBar.bottom - 1f)
            assertTrue("Pinned progress must sit directly under the app bar", top <= initialAppBar.bottom + 16f * pixelsPerDp)
            return top
        }

        form.performSemanticsAction(SemanticsActions.ScrollBy) { scroll ->
            assertTrue(scroll(0f, initialIntro.height * 0.25f))
        }
        compose.waitForIdle()
        assertAppBarFixed()
        val fadingTop = progress.fetchSemanticsNode().boundsInRoot.top
        assertTrue("Progress must move upward during the fade", fadingTop < initialTop - 1f)
        assertTrue("Progress must still be below its pinned position during the fade", fadingTop > initialAppBar.bottom + 16f * pixelsPerDp)
        compose.onNodeWithText("List Your Property").assertIsDisplayed()
        val fadingTitleLuminance = titleLuminance()
        assertTrue(
            "The visible title should fade toward the light background: before=$initialTitleLuminance, during=$fadingTitleLuminance",
            fadingTitleLuminance > initialTitleLuminance + 0.015
        )
        capture("listing-first-page-fading")
        form.performScrollToIndex(0)
        assertEquals(initialTop, progress.fetchSemanticsNode().boundsInRoot.top, 1f)

        form.performScrollToNode(hasText("Studio"))
        compose.onNodeWithText("Studio").assertIsDisplayed().performClick()
        val pinnedTop = assertProgressPinned()
        assertTrue("Progress should move upward as the intro scrolls away", pinnedTop < initialTop - 1f)
        if (compose.onAllNodesWithTag("listing_intro").fetchSemanticsNodes().isNotEmpty()) {
            intro.assertIsNotDisplayed()
        }
        form.performTouchInput { swipeUp(startY = height * 0.8f, endY = height * 0.55f, durationMillis = 500) }
        assertEquals(pinnedTop, assertProgressPinned(), 1f)
        form.performTouchInput { swipeDown(startY = height * 0.3f, endY = height * 0.42f, durationMillis = 500) }
        assertEquals("Reversing direction below the intro must keep progress pinned", pinnedTop, assertProgressPinned(), 1f)
        capture("listing-first-page-scrolled")

        form.performScrollToIndex(0)
        compose.onNodeWithText("List Your Property").assertIsDisplayed()
        compose.onNodeWithText("Reach thousands of potential buyers and renters").assertIsDisplayed()
        compose.onNodeWithContentDescription("Property icon").assertIsDisplayed()
        assertAppBarFixed()
        assertEquals(initialIntro.top, intro.fetchSemanticsNode().boundsInRoot.top, 1f)
        assertEquals("Returning to the top must restore progress below the intro", initialTop, progress.fetchSemanticsNode().boundsInRoot.top, 1f)
        capture("listing-first-page-reexpanded")

        compose.onNodeWithText("Continue").assertIsDisplayed()
        form.performScrollToNode(hasText("Rent Out Property"))
        compose.onNodeWithText("Rent Out Property").performClick()
        progress.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Step 1 of 5"))
        form.performScrollToNode(hasText("Studio"))
        compose.onNodeWithText("Studio").assertIsSelected()
        assertEquals(pinnedTop, assertProgressPinned(), 1f)
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithText("Property Details").assertIsDisplayed()
        progress.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Step 2 of 5"))
        assertEquals("The next page keeps progress under the same app bar", pinnedTop, assertProgressPinned(), 1f)
        Espresso.pressBack()
        progress.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Step 1 of 5"))
        compose.onNodeWithText("List Your Property").assertIsDisplayed()
        assertEquals(initialTop, progress.fetchSemanticsNode().boundsInRoot.top, 1f)
        assertAppBarFixed()
        form.performScrollToNode(hasText("Studio"))
        compose.onNodeWithText("Studio").assertIsSelected()
        compose.onNodeWithContentDescription("Exit listing").performClick()
        compose.onNodeWithText("Save your progress?").assertIsDisplayed()
        capture("listing-exit-prompt")
        compose.onNodeWithText("Keep editing").performClick()
        compose.onNodeWithText("Save your progress?").assertDoesNotExist()
        progress.assertIsDisplayed()
    }

    @Test fun progressAnimatesBetweenStepsAndBackForBothStepCounts() {
        var step by mutableIntStateOf(1)
        var rental by mutableStateOf(false)
        compose.setContent { GharBatoTheme { ListingProgressHeader(step, rental, modifier = Modifier.width(320.dp)) } }
        compose.mainClock.autoAdvance = false
        val progress = compose.onNodeWithTag("listing_progress")
        fun value() = progress.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current
        compose.runOnIdle { step = 2 }
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(200)
        assertTrue(value() > 0f && value() < 1f / 3)
        compose.mainClock.advanceTimeBy(500)
        assertEquals(1f / 3, value(), 0.001f)
        compose.runOnIdle { rental = true; step = 5 }
        compose.mainClock.advanceTimeBy(600)
        assertEquals(1f, value(), 0.001f)
        compose.runOnIdle { step = 1 }
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(200)
        assertTrue(value() > 0f && value() < 1f)
        compose.mainClock.advanceTimeBy(500)
        assertEquals(0f, value(), 0.001f)
        compose.mainClock.autoAdvance = true
    }

    private fun capture(name: String) {
        val folder = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "ui-review").apply { mkdirs() }
        val node = if (compose.onAllNodes(isDialog()).fetchSemanticsNodes().isNotEmpty()) compose.onNode(isDialog()) else compose.onRoot()
        node.captureToImage().asAndroidBitmap().let { bitmap ->
            File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
