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
 * Captures the lightweight Picker popup with custom buttons across the
 * placement and alignment combinations that matter. Originally a single
 * baseline shot ({@code LightweightPickerButtons}); extended after
 * issue #4819 where {@code Component.CENTER} alignment silently
 * left-aligned the buttons.
 *
 * The variant count is intentionally trimmed to four: each variant
 * cycle (popup show, throttled chunk emission, popup dismiss) costs
 * roughly 5-6s on Android, and {@code Cn1ssDeviceRunner}'s per-test
 * deadline is 30s on native platforms. The {@code _between_mixed}
 * shot subsumes isolated LEFT / CENTER / RIGHT BETWEEN_CANCEL_AND_DONE
 * captures: it lays out all three alignments in the same row with
 * explicit L / C / R labels, so a regression that re-broke any of
 * them would visibly collapse one column toward another.
 *
 * Doubles as a regression test for issue #4897 (live propagation of
 * {@code setDate} to the visible spinner wheels). Each variant opens
 * the popup with {@code new Date()} (so the wheels start at whatever
 * day the test runs), then calls {@code picker.setDate(fixedDate)}
 * after the slide-up to spin them to a known calendar position before
 * the screenshot. The committed baselines all show April 11 2026 -
 * if the live-propagation regresses the wheels will keep showing the
 * runtime "today" instead and the diff will fail every day except by
 * coincidence.
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
        // Initial value is "today" so the wheels open at a per-run date; each
        // variant cycle then spins them to fixedDate via the live-propagation
        // path (#4897) before screenshot, which is what makes the baselines
        // date-independent.
        picker.setDate(new Date());
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
                new Variant("LightweightPickerButtons_between_mixed", new Runnable() {
                    @Override
                    public void run() {
                        // Same placement, three alignments: covers the bug
                        // from #4819 (CENTER had been left-aligning) and
                        // the LEFT / RIGHT siblings in one shot.
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
        // Open at "today" so the wheels start somewhere date-dependent. The
        // setDate(fixedDate) call below (post-show) then exercises the
        // #4897 live-propagation path to drive the wheels to a stable
        // calendar position before we capture.
        picker.setDate(new Date());
        variant.configure.run();
        picker.startEditingAsync();
        // First wait: InteractionDialog slide-up. <300ms in practice; 600ms
        // is the budget the original test used and stays inside the per-test
        // 30s deadline once the second wait below is added.
        UITimer.timer(600, false, form, new Runnable() {
            @Override
            public void run() {
                // Live-propagate the deterministic fixedDate to the visible
                // wheels. Pre-#4897 this had no effect while the popup was
                // showing, so the wheels would stay on "today" and the
                // screenshot diff would fail any day other than April 11.
                picker.setDate(fixedDate);
                // Second wait: give the wheels a couple of frames to settle
                // at the new month/day/year before snapping the PNG.
                UITimer.timer(400, false, form, new Runnable() {
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
