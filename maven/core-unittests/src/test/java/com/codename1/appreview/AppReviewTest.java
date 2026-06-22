package com.codename1.appreview;

import com.codename1.io.Preferences;
import com.codename1.io.Storage;
import com.codename1.junit.EdtTest;
import com.codename1.junit.UITestBase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link AppReview} engagement scheduler decision logic
 * ({@link AppReview#shouldPrompt()} and the {@link AppReview#registerSession()}
 * bookkeeping). The Preferences keys below intentionally mirror the private
 * constants in {@code AppReview} so the test can seed launch count / install
 * date / last-prompt state directly, without launching real sessions (which on
 * a platform with no native review prompt would pop the modal rating dialog).
 */
class AppReviewTest extends UITestBase {
    private static final String PREF_LAUNCHES = "cn1$appReview$launches";
    private static final String PREF_FIRST_INSTALL = "cn1$appReview$firstInstall";
    private static final String PREF_LAST_PROMPT = "cn1$appReview$lastPrompt";

    private static final long DAY = 24L * 60L * 60L * 1000L;

    private String originalLocation;

    @BeforeEach
    void setUpAppReview() throws Exception {
        Storage.setStorageInstance(null);
        Storage storage = Storage.getInstance();
        storage.clearCache();
        storage.clearStorage();
        implementation.clearStorage();
        originalLocation = Preferences.getPreferencesLocation();
        Preferences.setPreferencesLocation("AppReviewTest-" + System.nanoTime());
        Preferences.clearAll();
        // Reset the singleton config to known defaults for each test.
        AppReview.getInstance()
                .setMinimumLaunches(5)
                .setMinimumDaysInstalled(3)
                .setDaysBetweenPrompts(30)
                .setHighRatingThreshold(4)
                .setStoreUrl(null)
                .setSupportEmail(null)
                .setFeedbackListener(null)
                .reset();
    }

    @AfterEach
    void tearDownAppReview() throws Exception {
        AppReview.getInstance().reset();
        Preferences.clearAll();
        if (originalLocation != null) {
            Preferences.setPreferencesLocation(originalLocation);
        }
        Storage storage = Storage.getInstance();
        storage.clearCache();
        storage.clearStorage();
        implementation.clearStorage();
    }

    @EdtTest
    void doesNotPromptBelowLaunchThreshold() {
        AppReview r = AppReview.getInstance();
        Preferences.set(PREF_LAUNCHES, 4);
        Preferences.set(PREF_FIRST_INSTALL, now() - 10 * DAY);
        assertFalse(r.shouldPrompt(), "Should not prompt below the launch threshold");
    }

    @EdtTest
    void doesNotPromptBeforeMinimumDaysInstalled() {
        AppReview r = AppReview.getInstance();
        Preferences.set(PREF_LAUNCHES, 9);
        Preferences.set(PREF_FIRST_INSTALL, now() - 1 * DAY);
        assertFalse(r.shouldPrompt(), "Should not prompt before the minimum days installed");
    }

    @EdtTest
    void promptsOnceThresholdsAreMet() {
        AppReview r = AppReview.getInstance();
        Preferences.set(PREF_LAUNCHES, 9);
        Preferences.set(PREF_FIRST_INSTALL, now() - 10 * DAY);
        assertTrue(r.shouldPrompt(), "Should prompt once launch + day thresholds are met");
    }

    @EdtTest
    void recentPromptSuppressesWithinCooldown() {
        AppReview r = AppReview.getInstance();
        Preferences.set(PREF_LAUNCHES, 9);
        Preferences.set(PREF_FIRST_INSTALL, now() - 10 * DAY);
        Preferences.set(PREF_LAST_PROMPT, now() - 5 * DAY);
        assertFalse(r.shouldPrompt(), "Should not re-prompt inside daysBetweenPrompts");
    }

    @EdtTest
    void promptsAgainAfterCooldown() {
        AppReview r = AppReview.getInstance();
        Preferences.set(PREF_LAUNCHES, 9);
        Preferences.set(PREF_FIRST_INSTALL, now() - 10 * DAY);
        Preferences.set(PREF_LAST_PROMPT, now() - 31 * DAY);
        assertTrue(r.shouldPrompt(), "Should prompt again after the cool-down window");
    }

    @EdtTest
    void markCompletedSuppressesPermanently() {
        AppReview r = AppReview.getInstance();
        Preferences.set(PREF_LAUNCHES, 9);
        Preferences.set(PREF_FIRST_INSTALL, now() - 10 * DAY);
        assertTrue(r.shouldPrompt());
        r.markCompleted();
        assertFalse(r.shouldPrompt(), "Should never prompt after completion");
    }

    @EdtTest
    void resetClearsSuppression() {
        AppReview r = AppReview.getInstance();
        r.markCompleted();
        r.reset();
        Preferences.set(PREF_LAUNCHES, 9);
        Preferences.set(PREF_FIRST_INSTALL, now() - 10 * DAY);
        assertTrue(r.shouldPrompt(), "reset() should clear the completed flag and state");
    }

    @EdtTest
    void registerSessionAccumulatesLaunchesWithoutPrompting() {
        AppReview r = AppReview.getInstance();
        // A launch threshold high enough that shouldPrompt() never fires here,
        // so registerSession() only performs bookkeeping (no modal dialog).
        r.setMinimumLaunches(1000);

        assertEquals(0, Preferences.get(PREF_LAUNCHES, 0));
        r.registerSession();
        r.registerSession();
        r.registerSession();

        assertEquals(3, Preferences.get(PREF_LAUNCHES, 0), "registerSession should count launches");
        assertTrue(Preferences.get(PREF_FIRST_INSTALL, 0L) > 0L, "registerSession should stamp the install date");
        assertFalse(r.shouldPrompt(), "shouldPrompt stays false below the (high) launch threshold");
    }

    @EdtTest
    void requestReviewRespectsCompleted() {
        AppReview r = AppReview.getInstance();
        r.markCompleted();
        Preferences.set(PREF_LAST_PROMPT, 12345L);
        // Already completed -> requestReview() must no-op (no prompt, no
        // re-stamp). It returns before any UI, so this is safe to call here.
        r.requestReview();
        assertEquals(12345L, Preferences.get(PREF_LAST_PROMPT, 0L),
                "requestReview must not prompt or re-stamp once completed");
    }

    private static long now() {
        return System.currentTimeMillis();
    }
}
