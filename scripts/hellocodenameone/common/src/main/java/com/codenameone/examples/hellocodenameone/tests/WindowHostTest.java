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

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Desktop;
import com.codename1.ui.Image;
import com.codename1.ui.Window;
import com.codename1.ui.layouts.BorderLayout;

/**
 * Base class for the windowed screenshot suite: it hosts a piece of UI inside a real
 * desktop {@link Window} at a given size, captures <em>that window</em>, and emits the
 * result as a golden.
 *
 * <p>This is the part of the test story that actually demonstrates windowing. A picture
 * of a window proves nothing; re-running representative UI inside one and comparing it
 * against its own baseline proves that layout, theming, scrolling, graphics, peers and
 * native editing all behave on a non-primary surface.</p>
 *
 * <p>Capture goes through {@link Window#capture()} rather than
 * {@code Display.screenshot}, because the ordinary path can only see the application's
 * main framebuffer and a second operating-system window simply is not in it.</p>
 *
 * <p>Ports with no windowing system report that through {@link Desktop#isSupported()};
 * those skip without emitting a golden, so their baselines never contain a picture of
 * something the platform cannot do.</p>
 *
 * @author Shai Almog
 */
public abstract class WindowHostTest extends BaseTest {

    /// How far below the requested size a window may legitimately be.
    /// Chrome costs tens of pixels; a window still carrying another
    /// window's geometry is out by hundreds.
    private static final int CHROME_ALLOWANCE = 64;

    /// How many readiness polls between re-asserting the requested size. The polls run
    /// as fast as the event dispatch thread will re-queue them, so this is a rough
    /// throttle rather than a duration -- often enough to rescue a refused request,
    /// rare enough not to fight a platform that is mid-resize.
    private static final int RESIZE_RETRY_POLLS = 25;

    /// Counts readiness polls for the size re-assert above.
    private int resizeAttempts;

    /// How many windows one size may burn before the case is called a failure.
    ///
    /// Re-asserting the size into a window the platform has already refused does not
    /// always rescue it: on Mac Catalyst a scene can stay pinned to another window's
    /// geometry -- a native editor is one way to pin it -- and every later request on
    /// that scene comes back as the system default. A window that never reached its
    /// size is therefore discarded and asked for again from scratch, which releases
    /// the scene and gets a fresh one.
    private static final int MAX_WINDOW_ATTEMPTS = 2;

    /// Windows opened for the size currently being captured.
    private int windowAttempts;

    /** Window sizes every windowed case is captured at. */
    protected static final int[][] SIZES = new int[][]{
        {400, 300},   // small
        {900, 700},   // large
        {1000, 400},  // deliberately non-square: proves layout follows the window
    };

    /** How long to wait for a newly shown window to become renderable. */
    private static final int WINDOW_READY_TIMEOUT_MS = 10000;

    private Window window;
    /// Previous poll's window size, so readiness can tell a settled window from one
    /// that is still being resized by the platform.
    private int lastWidth = -1;
    private int lastHeight = -1;

    /**
     * The content to host in the window. Invoked once per window opened -- which is
     * once per size, and again for each retry when the platform refuses the size --
     * so an implementation must build a fresh component tree every time rather than
     * caching one. The same component cannot live in two hierarchies, and the window
     * this content went into has already been disposed by the time it is asked for
     * again.
     */
    protected abstract Component createWindowContent(int width, int height);

    /** Golden name stem; the size is appended by the harness. */
    protected abstract String baseImageName();

    /**
     * Sizes this case is captured at. Override to narrow it -- a case that only proves
     * one behaviour does not need three goldens.
     */
    protected int[][] sizes() {
        return SIZES;
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return true;
    }

    /**
     * The window and its content outlive nothing here, but a retry would leave the
     * previous attempt's window open and a second one would then be captured.
     */
    @Override
    public boolean isRetrySafe() {
        return false;
    }

    @Override
    public boolean runTest() throws Exception {
        if (!Desktop.isSupported()) {
            // Reported as SKIPPED, not as a pass. This platform has no windowing
            // system and emits no golden, and a pass here would put a tick against
            // multi-window on the public port status table for a port that cannot
            // open a window at all.
            // Named by test class, not by golden name: the skip marker is parsed
            // with [A-Za-z0-9_]+, which the hyphenated screenshot names would not
            // match, and an unmatched marker is silently dropped -- leaving the
            // very pass this is here to avoid.
            println("CN1SS:INFO:test=" + getClass().getName().substring(
                    getClass().getName().lastIndexOf('.') + 1)
                    + " status=SKIPPED reason=no-windowing-system");
            done();
            return true;
        }
        captureNext(0);
        return true;
    }

    private void captureNext(final int index) {
        int[][] all = sizes();
        if (index >= all.length) {
            done();
            return;
        }
        final int width = all[index][0];
        final int height = all[index][1];

        windowAttempts = 0;
        openWindowFor(index, width, height);
    }

    /// Opens a window for one size and starts waiting for it to become renderable.
    /// Called again when a window has to be discarded and asked for from scratch.
    private void openWindowFor(final int index, final int width, final int height) {
        closeWindow();
        lastWidth = -1;
        lastHeight = -1;
        resizeAttempts = 0;
        windowAttempts++;
        window = new Window(baseImageName(), new BorderLayout());
        window.setResizable(true);
        window.add(BorderLayout.CENTER, createWindowContent(width, height));
        window.setWindowSize(width, height);
        window.show();

        // Wait for the window to actually be renderable rather than for a fixed
        // delay. Some platforms create the native window asynchronously -- Mac
        // Catalyst has to ask the system to activate a scene and is handed one back
        // later -- so a fixed sleep is both too long on the fast ports and too short
        // on the slow ones. The window is also not the current form, so the suite's
        // usual "current form has settled" gate does not apply to it.
        awaitRenderable(index, width, height,
                System.currentTimeMillis() + WINDOW_READY_TIMEOUT_MS);
    }

    /**
     * Polls on the event dispatch thread until the window can actually be rendered.
     * Re-queuing through callSerially rather than sleeping matters: the paint that
     * makes the window renderable happens on this very thread, so blocking it here
     * would prevent the condition from ever becoming true.
     */
    private void awaitRenderable(final int index, final int width, final int height,
                                 final long deadline) {
        // Readiness has four parts, and dropping any one of them produces a golden
        // that silently lies.
        //
        // The window has painted at least once: its raster exists from the moment it
        // is shown, so a capture before the first paint is a blank frame of exactly
        // the right dimensions.
        //
        // Its size has settled: some platforms create the native window
        // asynchronously and only then report a real size back -- Mac Catalyst has to
        // ask the system to activate a scene -- so a capture taken between the request
        // and the answer catches the window mid-resize.
        //
        // The capture is the size the window laid out at. This is the real invariant,
        // and the one that caught a window laying out at 400x300 inside a raster the
        // size of the main display.
        //
        // The window is no larger than what was asked for. setWindowSize() is native
        // geometry and includes the platform's chrome, so the content is legitimately
        // smaller wherever a title bar and border exist -- but it can never be bigger,
        // and a platform that ignored the request and handed back its own size is what
        // that would mean.
        Image probe = window == null ? null : window.capture();
        int windowWidth = window == null ? 0 : window.getWidth();
        int windowHeight = window == null ? 0 : window.getHeight();
        boolean settled = windowWidth == lastWidth && windowHeight == lastHeight;
        lastWidth = windowWidth;
        lastHeight = windowHeight;
        boolean ready = window != null
                && window.hasPaintedOnce()
                && settled
                && windowWidth > 0 && windowHeight > 0
                // Within a chrome-sized allowance of the size asked for, and never
                // larger. The original rule allowed anything down to three quarters,
                // which was meant to reject a window still reporting a previous
                // window's geometry and did not: a 700x500 background came back at a
                // recycled scene's 600x450 and passed. Requiring an exact match instead
                // was worse -- it rejected every window on ports whose reported size is
                // the content inside the chrome, which is most of them.
                //
                // An absolute allowance separates the two. Chrome costs tens of pixels
                // (Windows takes 16 wide and 39 high, Catalyst about 16 high), while a
                // stale geometry is out by hundreds.
                && windowWidth <= width && windowHeight <= height
                && windowWidth >= width - CHROME_ALLOWANCE
                && windowHeight >= height - CHROME_ALLOWANCE
                && probe != null
                && probe.getWidth() == windowWidth
                && probe.getHeight() == windowHeight;
        if (ready || System.currentTimeMillis() >= deadline) {
            captureAndAdvance(index, width, height, ready);
            return;
        }
        // Ask again, periodically, for the size this test wants.
        //
        // A window size is a request the platform may refuse -- Mac Catalyst hands
        // back its 1024x768 default when it ignores one -- and a window that lost the
        // request otherwise stays wrong until the deadline, producing no capture at
        // all. The port retries too, but only for a couple of seconds after creation;
        // this covers a refusal that outlasts that, and costs nothing on a platform
        // that granted the size, because the window is ready and never gets here.
        resizeAttempts++;
        if (window != null && resizeAttempts % RESIZE_RETRY_POLLS == 0
                && (windowWidth != width || windowHeight != height)) {
            window.setWindowSize(width, height);
        }
        CN.callSerially(new Runnable() {
            @Override
            public void run() {
                awaitRenderable(index, width, height, deadline);
            }
        });
    }

    /**
     * True when a capture is mostly unpainted. A window's raster starts out black, so
     * a frame that is largely black was never painted in full -- which is exactly what
     * a window resized ahead of the platform produced: correct dimensions, correct
     * content in one corner, and the rest of the surface untouched. Dimensions alone
     * could not see it, and a green suite hid it twice.
     */
    private static boolean mostlyUnpainted(Image img) {
        int w = img.getWidth();
        int h = img.getHeight();
        if (w <= 0 || h <= 0) {
            return true;
        }
        int[] rgb = img.getRGB();
        int black = 0;
        int sampled = 0;
        // Every eighth row is plenty to tell "half the window is missing" from
        // "this design happens to use dark pixels", and keeps the check cheap.
        for (int y = 0; y < h; y += 8) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                if ((rgb[offset + x] & 0xffffff) == 0) {
                    black++;
                }
                sampled++;
            }
        }
        return sampled > 0 && black * 4 > sampled * 3;
    }

    private void captureAndAdvance(final int index, int width, int height, boolean ready) {
        String name = baseImageName() + "-" + width + "x" + height;
        if (!ready && windowAttempts < MAX_WINDOW_ATTEMPTS) {
            // Throw this window away and ask for another. Re-asserting the size into a
            // window whose scene is pinned to someone else's geometry never wins; a new
            // window gets a new scene. Reported rather than silent, so a port that only
            // ever passes on the second attempt is visible in the log instead of
            // looking clean.
            println("CN1SS:INFO:test=" + getClass().getName().substring(
                    getClass().getName().lastIndexOf('.') + 1)
                    + " note=window-size-refused name=" + name
                    + " got=" + (window == null ? "none"
                            : window.getWidth() + "x" + window.getHeight())
                    + " retrying-with-a-new-window");
            stopEditingThen(new Runnable() {
                @Override
                public void run() {
                    openWindowFor(index, width, height);
                }
            });
            return;
        }
        if (!ready) {
            Image last = window == null ? null : window.capture();
            fail("Window never became renderable at the requested size for " + name
                    + " (showing=" + (window != null && window.isWindowShowing())
                    + " painted=" + (window != null && window.hasPaintedOnce())
                    + " size=" + (window == null ? "none" : window.getWidth() + "x" + window.getHeight())
                    + " capture=" + (last == null ? "none"
                            : last.getWidth() + "x" + last.getHeight()) + ")");
            return;
        }
        Image shot = window.capture();
        if (shot == null) {
            fail("Window capture returned null for " + name);
            return;
        }
        if (mostlyUnpainted(shot)) {
            fail("Window capture for " + name + " is mostly unpainted at "
                    + shot.getWidth() + "x" + shot.getHeight()
                    + "; the window was not painted over its whole surface");
            return;
        }
        Cn1ssDeviceRunnerHelper.emitImage(shot, name, new Runnable() {
            @Override
            public void run() {
                stopEditingThen(new Runnable() {
                    @Override
                    public void run() {
                        captureNext(index + 1);
                    }
                });
            }
        });
    }

    /// Stops any native editor in the current window, then runs the continuation.
    ///
    /// A native editor holds platform state tied to the window it is in -- on Mac
    /// Catalyst it pins the scene, so the *next* window came back at the system's
    /// default size instead of the one requested and never became renderable, which is
    /// why the editing case produced only its first size there. Stopping is
    /// asynchronous, hence the continuation rather than a plain call.
    ///
    /// The window is closed either way before the continuation runs: releasing the
    /// scene is the point, and a window left open would be captured by whatever comes
    /// next.
    private void stopEditingThen(final Runnable after) {
        if (window != null && window.isEditing()) {
            window.stopEditing(new Runnable() {
                @Override
                public void run() {
                    closeWindow();
                    after.run();
                }
            });
            return;
        }
        closeWindow();
        after.run();
    }

    private void closeWindow() {
        if (window != null) {
            window.dispose();
            window = null;
        }
    }

    private static void println(String s) {
        System.out.println(s);
    }
}
