package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.spinner.Picker;
import com.codename1.ui.util.UITimer;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * Regression test for the staging behavior layered on top of #4897:
 * `setX` calls made via custom lightweight popup buttons while the
 * picker is showing must be staged. If the user dismisses with Cancel
 * the staged value rolls back to whatever the picker held before the
 * popup was shown; if the user presses Done the staged value is
 * committed. Drives the picker programmatically and asserts
 * {@code getDate()} after each dismiss path.
 */
public class PickerCancelRestoreTest extends BaseTest {
    private Form form;
    private Picker picker;
    private Date initialDate;
    private Date stagedDate;

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        initialDate = toDate(LocalDate.of(2026, 4, 11));
        stagedDate = toDate(LocalDate.of(2026, 4, 18));

        form = new Form("Picker Cancel Restore", BoxLayout.y()) {
            @Override
            protected void onShowCompleted() {
                runCancelScenario();
            }
        };
        picker = new Picker();
        picker.setType(Display.PICKER_TYPE_DATE);
        picker.setUseLightweightPopup(true);
        picker.setDate(initialDate);
        picker.addLightweightPopupButton("+7 Days", new Runnable() {
            @Override
            public void run() {
                picker.setDate(stagedDate);
            }
        });
        form.add(picker);
        form.show();
        return true;
    }

    private void runCancelScenario() {
        picker.startEditingAsync();
        UITimer.timer(600, false, form, new Runnable() {
            @Override
            public void run() {
                if (!clickPopupButton("+7 Days")) {
                    return;
                }
                if (!datesEqual(stagedDate, picker.getDate())) {
                    fail("Staged setDate did not take effect during edit. Expected "
                            + stagedDate + " but got " + picker.getDate());
                    return;
                }
                if (!clickPopupButton("Cancel")) {
                    return;
                }
                // Cancel dismisses the popup; give the dialog a frame to
                // settle so the picker has run its rollback before we read.
                UITimer.timer(600, false, form, new Runnable() {
                    @Override
                    public void run() {
                        if (!datesEqual(initialDate, picker.getDate())) {
                            fail("Cancel did not restore initial date. Expected "
                                    + initialDate + " but got " + picker.getDate());
                            return;
                        }
                        runDoneScenario();
                    }
                });
            }
        });
    }

    private void runDoneScenario() {
        picker.startEditingAsync();
        UITimer.timer(600, false, form, new Runnable() {
            @Override
            public void run() {
                if (!clickPopupButton("+7 Days")) {
                    return;
                }
                if (!clickPopupButton("Done")) {
                    return;
                }
                UITimer.timer(600, false, form, new Runnable() {
                    @Override
                    public void run() {
                        if (!datesEqual(stagedDate, picker.getDate())) {
                            fail("Done did not commit staged date. Expected "
                                    + stagedDate + " but got " + picker.getDate());
                            return;
                        }
                        done();
                    }
                });
            }
        });
    }

    private boolean clickPopupButton(String text) {
        Button btn = findButtonByText(form, text);
        if (btn == null) {
            fail("Could not find button with text '" + text + "' in the picker popup");
            return false;
        }
        btn.pressed();
        btn.released();
        return true;
    }

    private static Button findButtonByText(Container c, String text) {
        for (Component child : c) {
            if (child instanceof Button && matchesButtonText((Button) child, text)) {
                return (Button) child;
            }
            if (child instanceof Container) {
                Button result = findButtonByText((Container) child, text);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static boolean matchesButtonText(Button btn, String text) {
        // Button.setText() uppercases when capsTextDefault is on (Material
        // theme default), so a case-insensitive compare matches both
        // "Cancel" and "CANCEL"; the pre-caps original is also stashed in
        // a client property which we check as a second chance.
        if (text.equalsIgnoreCase(btn.getText())) {
            return true;
        }
        Object orig = btn.getClientProperty("cn1$origText");
        return orig instanceof String && text.equalsIgnoreCase((String) orig);
    }

    private static boolean datesEqual(Date expected, Date actual) {
        if (expected == actual) {
            return true;
        }
        if (expected == null || actual == null) {
            return false;
        }
        // The picker's DateSpinner3D commits midnight-of-day, so a tolerant
        // day-granularity compare keeps the test stable against the wheel
        // wrapping the time component when Done is pressed.
        return expected.getTime() / (24L * 60 * 60 * 1000)
                == actual.getTime() / (24L * 60 * 60 * 1000);
    }

    private static Date toDate(LocalDate date) {
        return new Date(date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
}
