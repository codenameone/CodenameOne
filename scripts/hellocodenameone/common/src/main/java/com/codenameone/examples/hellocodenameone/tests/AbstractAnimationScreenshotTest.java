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

import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.animations.AnimationTime;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;

/// Base class for tests that capture an animation as a single screenshot
/// containing a 2x3 grid of frames (start, four intermediate, end).
///
/// Subclasses override [renderFrame(Graphics, int, int, double, int)] which paints
/// one frame at a given progress fraction (0.0 - 1.0). The base class drives
/// [AnimationTime] to deterministic values around each frame so any animations
/// reading the clock land on identical pixels regardless of the runtime.
public abstract class AbstractAnimationScreenshotTest extends BaseTest {
    private static final int FRAME_COUNT = 6;
    protected static final int GRID_COLS = 2;
    protected static final int GRID_ROWS = 3;
    private static final long ANIM_BASE_TIME = 5_000_000L;

    private Form host;

    protected final Form getHostForm() {
        return host;
    }

    @Override
    protected void registerReadyCallback(Form parent, Runnable run) {
        // BaseTest's default of 1500ms is needed to let real form contents
        // settle before the screenshot fires (Android in particular drops
        // images when the wait is shorter). Animation/transition tests render
        // entirely off-screen into an Image and don't depend on the host form's
        // contents - shrinking this to 200ms saves ~22s across the 17 grid
        // tests, keeping the iOS suite under its 300s end-marker budget.
        UITimer.timer(200, false, parent, run);
    }

    @Override
    public boolean runTest() throws Exception {
        host = new Form(getDisplayTitle(), new BorderLayout()) {
            @Override
            protected void onShowCompleted() {
                registerReadyCallback(this, AbstractAnimationScreenshotTest.this::captureAndEmit);
            }
        };
        host.show();
        return true;
    }

    private void captureAndEmit() {
        captureAndEmit(0);
    }

    private void captureAndEmit(final int waitedMs) {
        // The grid is composed at the HOST FORM's current size, so a landscape
        // orientation leaked by an earlier test ships a sideways grid against a
        // portrait golden (observed: VideoIODecodedFrames at 2556x1179 on iOS
        // Metal). Wait -- while the guard re-asserts the portrait lock -- until
        // the device is back in the suite's baseline orientation, with the same
        // 15s budget as the BaseTest settle path, then compose regardless.
        if (waitedMs < 15000 && captureBlockedByOrientation(getImageName(), waitedMs)) {
            UITimer.timer(250, false, host, new Runnable() {
                public void run() {
                    captureAndEmit(waitedMs + 250);
                }
            });
            return;
        }
        int width = Math.max(1, host.getWidth());
        int height = Math.max(1, host.getHeight());
        Image grid;
        try {
            grid = buildScreenshot(width, height);
        } catch (Throwable t) {
            System.out.println("CN1SS:ERR:test=" + getImageName() + " animation_grid_failed=" + t);
            t.printStackTrace();
            grid = Image.createImage(width, height, 0xff202020);
        } finally {
            AnimationTime.reset();
        }
        markCaptureStarted();
        Cn1ssDeviceRunnerHelper.emitImage(grid, getImageName(), new Runnable() {
            @Override
            public void run() {
                if (blankFilmstripMessage != null) {
                    // fail() calls done(), so this finalises the test exactly once.
                    fail(blankFilmstripMessage);
                    return;
                }
                done();
            }
        });
    }

    /// Build the final screenshot Image. The default implementation runs the
    /// per-frame grid composition (calling [renderFrame] six times); subclasses
    /// with a different capture strategy (e.g. composing six in-place animation
    /// instances onto a single form paint) can override this to skip the
    /// per-frame loop entirely.
    protected Image buildScreenshot(int width, int height) {
        return buildGrid(width, height);
    }

    /// Set when every frame came out a single flat colour. Recorded rather than failed on
    /// the spot: BaseTest.fail calls done(), and finalising the test from inside the compose
    /// would end it before the image is emitted -- the runner would then advance and the
    /// late emit would land on whatever screen came next, which is the exact failure the
    /// DualAppearance gate was written for. The image is worth having either way; it is the
    /// evidence.
    private String blankFilmstripMessage;

    private Image buildGrid(int width, int height) {
        int cellW = width / GRID_COLS;
        int cellH = height / GRID_ROWS;
        if (cellW <= 0 || cellH <= 0) {
            cellW = Math.max(1, cellW);
            cellH = Math.max(1, cellH);
        }
        int frameWidth = getFrameWidth(width);
        int frameHeight = getFrameHeight(height);
        Image composite = Image.createImage(width, height, 0xff101010);
        Graphics cg = composite.getGraphics();
        cg.setColor(0x101010);
        cg.fillRect(0, 0, width, height);
        prepareCapture(frameWidth, frameHeight);
        int blankFrames = 0;
        try {
            for (int i = 0; i < FRAME_COUNT; i++) {
                double progress = (double) i / (double) (FRAME_COUNT - 1);
                Image frame = Image.createImage(frameWidth, frameHeight, 0xffffffff);
                Graphics fg = frame.getGraphics();
                fg.setColor(0xffffff);
                fg.fillRect(0, 0, frameWidth, frameHeight);
                AnimationTime.setTime(timeForProgress(progress));
                renderFrame(fg, frameWidth, frameHeight, progress, i);
                Image scaled;
                if (frameWidth == cellW && frameHeight == cellH) {
                    scaled = frame;
                } else {
                    scaled = frame.scaled(cellW, cellH);
                }
                if (isSingleColour(frame, frameWidth, frameHeight)) {
                    blankFrames++;
                }
                int row = i / GRID_COLS;
                int col = i % GRID_COLS;
                cg.drawImage(scaled, col * cellW, row * cellH);
                drawCellOverlay(cg, col * cellW, row * cellH, cellW, cellH, i, progress);
                if (scaled != frame) {
                    scaled.dispose();
                }
                frame.dispose();
            }
        } finally {
            finishCapture();
        }
        if (blankFrames == FRAME_COUNT) {
            // Every frame is one flat colour, so the filmstrip has no content in it at all.
            //
            // This is a picture, which is the whole problem: the capture succeeds, the
            // comparison runs, and the only thing that can tell a blank filmstrip from a
            // real one is a person looking at it. That is how ten of these emitted six
            // empty cells in the host Form's background colour -- a layout invalidation
            // that these captures had been getting by accident stopped happening -- and
            // the goldens would have recorded the blank as the new truth.
            //
            // The condition is deliberately all six rather than any: a single flat frame
            // can be legitimate at one end of an animation, six cannot.
            blankFilmstripMessage = getImageName() + " produced " + FRAME_COUNT
                    + " frames and every one of them is a single flat colour."
                    + " The animation host painted its background and none of its children;"
                    + " see BaseTest.layoutOffScreen.";
            System.out.println("CN1SS:ERR:test=" + getImageName()
                    + " blank_filmstrip=" + blankFilmstripMessage);
        }
        drawGridLines(cg, width, height, cellW, cellH);
        return composite;
    }

    /// True when every pixel of the image is the same colour.
    ///
    /// #### Parameters
    ///
    /// - `img`: the frame to inspect
    ///
    /// - `w`: its width
    ///
    /// - `h`: its height
    ///
    /// #### Returns
    ///
    /// true when the frame carries exactly one colour
    private static boolean isSingleColour(Image img, int w, int h) {
        if (w <= 0 || h <= 0) {
            return true;
        }
        // getRGB() rather than the region overload, which is not public outside the
        // com.codename1.ui package.
        int[] pixels = img.getRGB();
        if (pixels == null || pixels.length == 0) {
            return true;
        }
        int first = pixels[0];
        for (int i = 1; i < pixels.length; i++) {
            if (pixels[i] != first) {
                return false;
            }
        }
        return true;
    }

    private void drawGridLines(Graphics g, int width, int height, int cellW, int cellH) {
        g.setColor(0x303030);
        for (int c = 1; c < GRID_COLS; c++) {
            int x = c * cellW;
            g.drawLine(x, 0, x, height - 1);
        }
        for (int r = 1; r < GRID_ROWS; r++) {
            int y = r * cellH;
            g.drawLine(0, y, width - 1, y);
        }
    }

    private void drawCellOverlay(Graphics g, int x, int y, int cellW, int cellH, int frameIndex, double progress) {
        String label = "F" + (frameIndex + 1) + " " + percentLabel(progress);
        g.setColor(0x000000);
        int textY = y + 2;
        int textX = x + 4;
        g.drawString(label, textX + 1, textY + 1);
        g.setColor(0xffe066);
        g.drawString(label, textX, textY);
    }

    private String percentLabel(double progress) {
        int pct = (int) Math.round(progress * 100);
        return pct + "%";
    }

    /// Maps a frame's progress fraction to an animation clock time. The base
    /// time is fixed so motions started during prepareCapture see identical
    /// time deltas across runs.
    private long timeForProgress(double progress) {
        return ANIM_BASE_TIME + (long) Math.round(progress * (double) getAnimationDurationMillis());
    }

    /// Frame buffer width. Default is the full display width so frames render
    /// at their natural size before being scaled into the grid cell.
    protected int getFrameWidth(int displayWidth) {
        return Math.max(1, displayWidth);
    }

    /// Frame buffer height. Default is the full display height.
    protected int getFrameHeight(int displayHeight) {
        return Math.max(1, displayHeight);
    }

    /// Animation duration (in ms) used to map progress to AnimationTime.
    /// Subclasses should match this with the duration they pass to the
    /// transition or container animation under test.
    protected int getAnimationDurationMillis() {
        return 1000;
    }

    /// Anchor for AnimationTime that prepareCapture sees - frames are rendered
    /// at progress fractions of [getAnimationDurationMillis()] beyond this.
    protected long getAnimationStartTime() {
        return ANIM_BASE_TIME;
    }

    /// Allow subclasses to set up state (e.g. start an animation) before any
    /// frame is rendered. The clock is held at [getAnimationStartTime()] when
    /// this is called so any motions started here align with the first frame.
    protected void prepareCapture(int frameWidth, int frameHeight) {
        AnimationTime.setTime(getAnimationStartTime());
    }

    /// Tear-down hook invoked after all frames have been rendered.
    protected void finishCapture() {
    }

    /// Paint a single animation frame. Subclasses using the default per-frame
    /// grid strategy must override this; subclasses overriding [buildScreenshot]
    /// can leave this as a no-op since the grid loop won't be invoked.
    protected void renderFrame(Graphics g, int width, int height, double progress, int frameIndex) {
    }

    protected String getImageName() {
        return getClass().getSimpleName();
    }

    protected String getDisplayTitle() {
        return getClass().getSimpleName();
    }

}
