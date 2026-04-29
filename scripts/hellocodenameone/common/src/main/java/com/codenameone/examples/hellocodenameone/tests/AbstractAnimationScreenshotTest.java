package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.animations.AnimationTime;
import com.codename1.ui.layouts.BorderLayout;

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
        Cn1ssDeviceRunnerHelper.emitImage(grid, getImageName(), this::done);
    }

    /// Build the final screenshot Image. The default implementation runs the
    /// per-frame grid composition (calling [renderFrame] six times); subclasses
    /// with a different capture strategy (e.g. composing six in-place animation
    /// instances onto a single form paint) can override this to skip the
    /// per-frame loop entirely.
    protected Image buildScreenshot(int width, int height) {
        return buildGrid(width, height);
    }

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
        drawGridLines(cg, width, height, cellW, cellH);
        return composite;
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
