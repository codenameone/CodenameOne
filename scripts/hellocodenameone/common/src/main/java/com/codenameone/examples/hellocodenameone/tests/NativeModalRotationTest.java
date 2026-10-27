/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.system.NativeLookup;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.hellocodenameone.NativeModalRotationNative;

/// Issue #6080: rotate the device while a full-screen native modal that only
/// supports portrait covers the Codename One view -- the iOS camera is one --
/// and dismiss it. The interface rotates to landscape as the modal goes away,
/// and the framework has to learn the new size then; when it did not, every
/// form drew with the portrait width stretched across the landscape view until
/// the device was rotated again.
///
/// Asserts that once the modal is gone and the view is landscape, the display
/// size the framework uses equals the view's size in pixels. iOS only; other
/// platforms have no such modal and finish at once.
///
/// A second case starts in landscape and stays there: presenting the
/// portrait-only modal announces a portrait size to the covered view, whose
/// bounds never change, so nothing corrected it after the dismissal.
///
/// No screenshot -- this is an assertion test.
public class NativeModalRotationTest extends BaseTest {
    private static final int POLL_INTERVAL_MS = 100;
    // The modal is presented without animation, the dismissal is animated and
    // the rotation follows it; 5s is far beyond either on a starved runner.
    private static final int POLL_ATTEMPTS = 50;
    // Lets a rotation that the modal failed to hold back land before checking.
    private static final int UNDER_MODAL_SETTLE_MS = 1500;
    private static final int TOLERANCE_PX = 2;

    private NativeModalRotationNative modal;
    private boolean landscapeCase;

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        modal = NativeLookup.create(NativeModalRotationNative.class);
        if (modal == null || !modal.isSupported() || !CN.canForceOrientation()) {
            done();
            return true;
        }
        Form form = new Form("Native Modal Rotation", new BoxLayout(BoxLayout.Y_AXIS)) {
            @Override
            protected void onShowCompleted() {
                start(this);
            }
        };
        form.add(new Label("Rotating under a native modal..."));
        form.show();
        return true;
    }

    private void start(final Form form) {
        CN.lockOrientation(true);
        poll(form, POLL_ATTEMPTS, new Condition() {
            public boolean holds() {
                return modal.getViewPixelHeight() > modal.getViewPixelWidth();
            }
        }, "the device to start in portrait", new Runnable() {
            public void run() {
                modal.presentPortraitOnlyModal();
                poll(form, POLL_ATTEMPTS, new Condition() {
                    public boolean holds() {
                        return modal.isModalPresented();
                    }
                }, "the native modal to be presented", new Runnable() {
                    public void run() {
                        rotateUnderModal(form);
                    }
                });
            }
        });
    }

    private void rotateUnderModal(final Form form) {
        // The device turns to landscape; the modal keeps the interface
        // portrait, exactly as the camera does.
        CN.lockOrientation(false);
        UITimer.timer(UNDER_MODAL_SETTLE_MS, false, form, new Runnable() {
            public void run() {
                // Portrait here is what makes this the camera's path: a modal
                // that let the interface rotate would test an ordinary rotation.
                System.out.println("CN1SS:INFO:test=NativeModalRotationTest under the modal view="
                        + modal.getViewPixelWidth() + "x" + modal.getViewPixelHeight()
                        + " display=" + Display.getInstance().getDisplayWidth()
                        + "x" + Display.getInstance().getDisplayHeight());
                modal.dismissModal();
                poll(form, POLL_ATTEMPTS, new Condition() {
                    public boolean holds() {
                        return !modal.isModalPresented()
                                && modal.getViewPixelWidth() > modal.getViewPixelHeight();
                    }
                }, "the modal to close and the view to turn landscape", new Runnable() {
                    public void run() {
                        awaitDisplayMatchesView(form, POLL_ATTEMPTS);
                    }
                });
            }
        });
    }

    /// Polls rather than checking once: the size reaches the EDT as an event,
    /// so a correct port may need a frame or two. A port that never delivers it
    /// keeps the portrait size for the whole budget.
    private void awaitDisplayMatchesView(final Form form, final int attemptsLeft) {
        int viewW = modal.getViewPixelWidth();
        int viewH = modal.getViewPixelHeight();
        int displayW = Display.getInstance().getDisplayWidth();
        int displayH = Display.getInstance().getDisplayHeight();
        if (Math.abs(viewW - displayW) <= TOLERANCE_PX && Math.abs(viewH - displayH) <= TOLERANCE_PX) {
            if (landscapeCase) {
                finish(form, null);
            } else {
                landscapeCase = true;
                startLandscape(form);
            }
            return;
        }
        if (attemptsLeft <= 0) {
            finish(form, (landscapeCase ? "landscape case: " : "")
                    + "after a native modal closed over a rotation the view is "
                    + viewW + "x" + viewH + " pixels but the display is "
                    + displayW + "x" + displayH + " (issue #6080)");
            return;
        }
        UITimer.timer(POLL_INTERVAL_MS, false, form, new Runnable() {
            public void run() {
                awaitDisplayMatchesView(form, attemptsLeft - 1);
            }
        });
    }

    /// Landscape before the modal, landscape under it, landscape after it.
    private void startLandscape(final Form form) {
        CN.lockOrientation(false);
        poll(form, POLL_ATTEMPTS, new Condition() {
            public boolean holds() {
                return !modal.isModalPresented()
                        && modal.getViewPixelWidth() > modal.getViewPixelHeight();
            }
        }, "the device to start the landscape case in landscape", new Runnable() {
            public void run() {
                // Unlocked, as the reporter's app is: under a landscape lock
                // UIKit keeps the interface landscape even with the portrait-only
                // modal up, so the covered view is never told portrait and the
                // bug cannot happen. The device itself stays landscape.
                CN.unlockOrientation();
                UITimer.timer(UNDER_MODAL_SETTLE_MS, false, form, new Runnable() {
                    public void run() {
                        presentOverLandscape(form);
                    }
                });
            }
        });
    }

    private void presentOverLandscape(final Form form) {
        poll(form, POLL_ATTEMPTS, new Condition() {
            public boolean holds() {
                return modal.getViewPixelWidth() > modal.getViewPixelHeight();
            }
        }, "the unlocked view to stay landscape", new Runnable() {
            public void run() {
                modal.presentPortraitOnlyModal();
                poll(form, POLL_ATTEMPTS, new Condition() {
                    public boolean holds() {
                        return modal.isModalPresented();
                    }
                }, "the native modal to be presented over landscape", new Runnable() {
                    public void run() {
                        UITimer.timer(UNDER_MODAL_SETTLE_MS, false, form, new Runnable() {
                            public void run() {
                                System.out.println("CN1SS:INFO:test=NativeModalRotationTest landscape case under the modal view="
                                        + modal.getViewPixelWidth() + "x" + modal.getViewPixelHeight()
                                        + " display=" + Display.getInstance().getDisplayWidth()
                                        + "x" + Display.getInstance().getDisplayHeight());
                                // This state is the bug's precondition: the covered view
                                // was told portrait while its bounds stayed landscape.
                                // Without it the dismissal below proves nothing.
                                if (Display.getInstance().getDisplayWidth() >= Display.getInstance().getDisplayHeight()
                                        || modal.getViewPixelWidth() <= modal.getViewPixelHeight()) {
                                    finish(form, "landscape case precondition: the modal did not announce portrait"
                                            + " to the covered landscape view; view="
                                            + modal.getViewPixelWidth() + "x" + modal.getViewPixelHeight()
                                            + " display=" + Display.getInstance().getDisplayWidth()
                                            + "x" + Display.getInstance().getDisplayHeight());
                                    return;
                                }
                                modal.dismissModal();
                                poll(form, POLL_ATTEMPTS, new Condition() {
                                    public boolean holds() {
                                        return !modal.isModalPresented()
                                                && modal.getViewPixelWidth() > modal.getViewPixelHeight();
                                    }
                                }, "the modal to close with the view still landscape", new Runnable() {
                                    public void run() {
                                        awaitDisplayMatchesView(form, POLL_ATTEMPTS);
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    private void finish(final Form form, final String failure) {
        modal.dismissModal();
        LandscapeCapture.restorePortrait(form, new Runnable() {
            public void run() {
                if (failure != null) {
                    fail(failure);
                } else {
                    done();
                }
            }
        });
    }

    private interface Condition {
        boolean holds();
    }

    private void poll(final Form form, final int attemptsLeft, final Condition condition,
            final String what, final Runnable then) {
        if (condition.holds()) {
            then.run();
            return;
        }
        if (attemptsLeft <= 0) {
            finish(form, "timed out waiting for " + what + "; view="
                    + modal.getViewPixelWidth() + "x" + modal.getViewPixelHeight());
            return;
        }
        UITimer.timer(POLL_INTERVAL_MS, false, form, new Runnable() {
            public void run() {
                poll(form, attemptsLeft - 1, condition, what, then);
            }
        });
    }
}
