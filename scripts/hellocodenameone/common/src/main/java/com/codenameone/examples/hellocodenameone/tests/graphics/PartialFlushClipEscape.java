package com.codenameone.examples.hellocodenameone.tests.graphics;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.hellocodenameone.tests.BaseTest;

/// Regression guard for issue #5273 -- "iOS: Fixed Toolbar and North Container
/// Become Blank While Scrolling a Separate Center Container".
///
/// The existing clip tests ({@code graphics-clip}, {@code graphics-empty-clip},
/// {@code graphics-clip-under-rotation}) all paint a whole Form in one shot, so
/// the framework issues a FULL-screen flush. The iOS native backends only clamp
/// a clip to the current flush region (ClipRect.m's {@code drawingRect}); when
/// the flush region is the whole screen that clamp is a no-op, so a full-screen
/// test can never exercise it. The bug only appears on a PARTIAL flush: when an
/// independently scrollable {@code BorderLayout.CENTER} repaints, paintDirty()
/// flushes only that component's bounds, but a clip emitted during that flush can
/// still extend past it into a fixed band. The GL backend clamps every clip to
/// {@code drawingRect}; the Metal backend was missing that clamp, and because
/// Metal renders into a PERSISTENT screenTexture (MTLLoadActionLoad) the escaping
/// fill overwrote the fixed band and it stayed blank until a full repaint.
///
/// This test reproduces that path deterministically. A fixed red band lives in
/// {@code BorderLayout.SOUTH}; the scrollable area is the {@code CENTER}. After a
/// correct full paint (red SOUTH, white CENTER) it repaints ONLY the CENTER -- a
/// real partial flush whose {@code drawingRect} is the CENTER's bounds -- during
/// which the CENTER sets a clip that reaches DOWN past its own bottom into the
/// SOUTH band and fills it magenta. A backend that clamps the clip to
/// {@code drawingRect} (GL, JavaSE, Android, the JS port, and Metal after the
/// #5273 fix) keeps the magenta inside the CENTER and the SOUTH band stays red.
/// The broken Metal backend lets the magenta escape into the SOUTH band.
///
/// SOUTH (not NORTH) is the fixed band on purpose: the simulator's title/status
/// bar repaints every frame, so paintDirty() unions that region with the CENTER
/// repaint -- but only ever upward, never below the CENTER. A band placed BELOW
/// the CENTER is therefore always outside the flush bounding box, which keeps the
/// partial-flush precondition intact regardless of any top-bar animation. The
/// fix is symmetric, so guarding the SOUTH direction guards the reported NORTH
/// case too.
///
/// Clamping a clip to the flush region is a portable correctness property, so the
/// test runs on every platform (including tvOS, which is also Metal and gets the
/// same fix). On any backend that clamps correctly the golden is "magenta CENTER +
/// red SOUTH"; a backend that lets the fill escape the flush region (the regressed
/// iOS Metal path) turns the SOUTH band magenta and fails the comparison.
public class PartialFlushClipEscape extends BaseTest {
    private static final String NAME = "graphics-partial-flush-clip-escape";

    private static final int SOUTH_COLOR = 0xd32f2f;   // red -- the "fixed" band
    private static final int CENTER_COLOR = 0xffffff;  // white -- benign CENTER
    private static final int ESCAPE_COLOR = 0xff00ff;  // magenta -- the escaping fill

    /// Flipped on right before the partial repaint so the very first (full) paint
    /// stays benign and only the partial flush attempts the clip escape.
    private boolean escaping;

    private final SolidComponent south = new SolidComponent(SOUTH_COLOR);
    private final EscapeComponent center = new EscapeComponent();

    @Override
    public boolean runTest() {
        // Empty title: a non-empty Form title renders a Label that can ticker-
        // animate (and varies per capture), so an empty title keeps the golden
        // deterministic. The fixed SOUTH band, not the title, is what the test
        // verifies.
        Form form = new Form("", new BorderLayout()) {
            @Override
            protected void onShowCompleted() {
                registerReadyCallback(this, () -> triggerEscapeThenCapture(this));
            }
        };
        form.setUIID("GraphicsForm");
        zeroInsets(form.getContentPane());

        // A tall, fixed band at the bottom. Both dimensions set explicitly so
        // BorderLayout.SOUTH always gives it real height.
        int bandH = Math.max(120, CN.getDisplayHeight() / 4);
        south.setPreferredSize(new Dimension(CN.getDisplayWidth(), bandH));

        form.add(BorderLayout.CENTER, center);
        form.add(BorderLayout.SOUTH, south);
        form.show();
        return true;
    }

    private void triggerEscapeThenCapture(Form form) {
        escaping = true;
        // Repaint ONLY the CENTER: paintDirty() flushes its bounds (unioned at
        // most with the always-animating top bar, never below) as the partial
        // drawingRect, which is the precondition for the #5273 clip escape.
        center.repaint();
        UITimer.timer(600, false, form, () -> captureWhenSettled(form, NAME, this::done));
    }

    private static void zeroInsets(Component c) {
        c.getAllStyles().setPadding(0, 0, 0, 0);
        c.getAllStyles().setMargin(0, 0, 0, 0);
    }

    /// Fills its whole bounds with one solid colour, nothing else.
    private static final class SolidComponent extends Component {
        private final int color;

        SolidComponent(int color) {
            this.color = color;
            zeroInsets(this);
        }

        @Override
        public void paint(Graphics g) {
            g.setColor(color);
            g.fillRect(getX(), getY(), getWidth(), getHeight());
        }
    }

    private final class EscapeComponent extends Component {
        EscapeComponent() {
            zeroInsets(this);
        }

        @Override
        public void paint(Graphics g) {
            if (!escaping) {
                g.setColor(CENTER_COLOR);
                g.fillRect(getX(), getY(), getWidth(), getHeight());
                return;
            }
            // Set a clip that starts at this component's own top and extends a
            // full display height downward -- well past its own bottom, over the
            // SOUTH band -- then fill it. The partial flush's drawingRect is only
            // this component's bounds, so a backend that clamps the clip to
            // drawingRect keeps the fill inside the CENTER; a backend that does
            // not lets it overwrite the SOUTH band below.
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = CN.getDisplayHeight();
            g.setColor(ESCAPE_COLOR);
            g.setClip(x, y, w, h);
            g.fillRect(x, y, w, h);
        }
    }
}
