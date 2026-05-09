package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.spinner.Picker;
import com.codename1.ui.util.UITimer;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * Regression test for issue #4897: calling {@code Picker.setDate(...)} while
 * the lightweight popup is on screen must move the visible scroll wheels,
 * not just update the (hidden) committed value. Before the fix the wheels
 * stayed on the original date and only the {@code value} field changed, so
 * a custom popup button doing {@code setDate(getDate() + 7d)} would compute
 * from a stale base.
 *
 * The test opens the popup with April 11 2026, lets the slide-up animation
 * finish, then calls {@code setDate(April 18 2026)}. The screenshot must
 * show the April 18 wheels - if the propagation regresses the wheels will
 * stay on April 11 and the diff will fail.
 */
public class PickerLiveValueScreenshotTest extends BaseTest {
    private Form form;
    private Picker picker;

    @Override
    public boolean runTest() {
        final Date initial = toDate(LocalDate.of(2026, 4, 11));
        final Date updated = toDate(LocalDate.of(2026, 4, 18));

        form = new Form("Picker Live Value", BoxLayout.y()) {
            @Override
            protected void onShowCompleted() {
                picker.startEditingAsync();
                // The InteractionDialog slide-up is <300ms; 600ms matches
                // the wait used by LightweightPickerButtonsScreenshotTest.
                UITimer.timer(600, false, form, new Runnable() {
                    @Override
                    public void run() {
                        picker.setDate(updated);
                        // Give the wheels a frame to redraw at the new date
                        // before capture.
                        UITimer.timer(200, false, form, new Runnable() {
                            @Override
                            public void run() {
                                Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshot(
                                        "PickerLiveValue", new Runnable() {
                                            @Override
                                            public void run() {
                                                picker.stopEditing(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        done();
                                                    }
                                                });
                                            }
                                        });
                            }
                        });
                    }
                });
            }
        };
        picker = new Picker();
        picker.setType(Display.PICKER_TYPE_DATE);
        picker.setUseLightweightPopup(true);
        picker.setDate(initial);
        form.add(picker);
        form.show();
        return true;
    }

    private static Date toDate(LocalDate date) {
        return new Date(date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
}
