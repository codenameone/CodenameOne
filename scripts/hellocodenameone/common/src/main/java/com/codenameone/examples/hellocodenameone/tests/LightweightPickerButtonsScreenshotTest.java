package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.spinner.Picker;
import com.codename1.ui.spinner.Picker.LightweightPopupButtonPlacement;
import com.codename1.ui.util.UITimer;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * Captures the lightweight Picker popup with custom buttons in every
 * supported placement / alignment combination. Originally a single
 * baseline shot ({@code LightweightPickerButtons}); extended to a suite
 * after issue #4819 where {@code Component.CENTER} alignment silently
 * left-aligned the buttons. The expanded variants are the regression
 * fence: each placement and each alignment now has its own golden so a
 * future change to the row layout cannot ship without one of the
 * screenshots disagreeing.
 */
public class LightweightPickerButtonsScreenshotTest extends BaseTest {
    private Form form;
    private Picker picker;
    private Date fixedDate;
    private Date fixedDatePlus7;
    private Variant[] variants;

    @Override
    public boolean runTest() {
        fixedDate = toDate(LocalDate.of(2026, 4, 11));
        fixedDatePlus7 = toDate(LocalDate.of(2026, 4, 18));
        variants = buildVariants();

        form = new Form("Picker Quick Buttons", BoxLayout.y()) {
            @Override
            protected void onShowCompleted() {
                runVariantsFrom(0);
            }
        };
        picker = new Picker();
        picker.setType(Display.PICKER_TYPE_DATE);
        picker.setUseLightweightPopup(true);
        picker.setDate(fixedDate);
        form.add(picker);
        form.show();
        return true;
    }

    private Variant[] buildVariants() {
        // Order matters for telemetry but not for correctness. The
        // first entry's image name is "LightweightPickerButtons" to
        // preserve the pre-existing golden (Today / +7 Days, both LEFT
        // aligned) checked in under scripts/<port>/screenshots/.
        return new Variant[] {
                new Variant("LightweightPickerButtons", new Runnable() {
                    @Override
                    public void run() {
                        addToday(LightweightPopupButtonPlacement.BETWEEN_CANCEL_AND_DONE, Component.LEFT);
                        addPlus7(LightweightPopupButtonPlacement.BELOW_SPINNER, Component.LEFT);
                    }
                }),
                new Variant("LightweightPickerButtons_between_center", new Runnable() {
                    @Override
                    public void run() {
                        addToday(LightweightPopupButtonPlacement.BETWEEN_CANCEL_AND_DONE, Component.CENTER);
                    }
                }),
                new Variant("LightweightPickerButtons_between_right", new Runnable() {
                    @Override
                    public void run() {
                        addToday(LightweightPopupButtonPlacement.BETWEEN_CANCEL_AND_DONE, Component.RIGHT);
                    }
                }),
                new Variant("LightweightPickerButtons_between_mixed", new Runnable() {
                    @Override
                    public void run() {
                        // Same placement, three alignments: a regression here
                        // would visibly collapse one column toward another.
                        picker.addLightweightPopupButton("L", null,
                                LightweightPopupButtonPlacement.BETWEEN_CANCEL_AND_DONE, Component.LEFT);
                        picker.addLightweightPopupButton("C", null,
                                LightweightPopupButtonPlacement.BETWEEN_CANCEL_AND_DONE, Component.CENTER);
                        picker.addLightweightPopupButton("R", null,
                                LightweightPopupButtonPlacement.BETWEEN_CANCEL_AND_DONE, Component.RIGHT);
                    }
                }),
                new Variant("LightweightPickerButtons_above_center", new Runnable() {
                    @Override
                    public void run() {
                        addToday(LightweightPopupButtonPlacement.ABOVE_SPINNER, Component.CENTER);
                    }
                }),
                new Variant("LightweightPickerButtons_below_right", new Runnable() {
                    @Override
                    public void run() {
                        addPlus7(LightweightPopupButtonPlacement.BELOW_SPINNER, Component.RIGHT);
                    }
                }),
        };
    }

    private void addToday(int placement, int alignment) {
        picker.addLightweightPopupButton("Today", new Runnable() {
            @Override
            public void run() {
                picker.setDate(fixedDate);
            }
        }, placement, alignment);
    }

    private void addPlus7(int placement, int alignment) {
        picker.addLightweightPopupButton("+7 Days", new Runnable() {
            @Override
            public void run() {
                picker.setDate(fixedDatePlus7);
            }
        }, placement, alignment);
    }

    private void runVariantsFrom(final int index) {
        if (index >= variants.length) {
            done();
            return;
        }
        final Variant variant = variants[index];
        picker.clearLightweightPopupButtons();
        picker.setDate(fixedDate);
        variant.configure.run();
        picker.startEditingAsync();
        // Wait for the popup to slide up and the spinner cells to settle
        // before grabbing the screenshot. 1000ms matches the Android budget
        // we use on the original single-shot version of this test.
        UITimer.timer(1000, false, form, new Runnable() {
            @Override
            public void run() {
                Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshot(variant.imageName, new Runnable() {
                    @Override
                    public void run() {
                        picker.stopEditing(new Runnable() {
                            @Override
                            public void run() {
                                runVariantsFrom(index + 1);
                            }
                        });
                    }
                });
            }
        });
    }

    private static Date toDate(LocalDate date) {
        return new Date(date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }

    private static final class Variant {
        final String imageName;
        final Runnable configure;

        Variant(String imageName, Runnable configure) {
            this.imageName = imageName;
            this.configure = configure;
        }
    }
}
