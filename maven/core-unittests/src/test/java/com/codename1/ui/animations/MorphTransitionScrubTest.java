package com.codename1.ui.animations;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Validates the {@link MorphTransition} extensions: opacity / rotation / scale
/// tweening, scrubbing to an arbitrary normalized progress (deterministic,
/// order-independent, reversible) and morphing arbitrary rendered elements
/// (here backed by an {@link Image}) rather than only named form components.
///
/// The tests assert the interpolated state (queried through the public
/// `getCurrent*` accessors) rather than rendered pixels so they stay
/// platform-independent; the on-screen rendering is covered by the
/// screenshot animation tests under {@code scripts/hellocodenameone}.
public class MorphTransitionScrubTest extends UITestBase {
    private static final float EPS = 0.02f;

    private static Image image(int w, int h) {
        return Image.createImage(w, h, 0xffff0000);
    }

    private static MorphTransition.MorphElement movingElement() {
        return MorphTransition.MorphElement.create(image(20, 20))
                .from(0, 0, 20, 20)
                .to(100, 50, 40, 60)
                .opacity(1f, 0f)
                .rotation(0f, 90f)
                .scale(1f, 2f);
    }

    private static Form blankForm() {
        Form f = new Form("blank", new BorderLayout());
        f.setWidth(320);
        f.setHeight(480);
        f.setVisible(true);
        return f;
    }

    private static Form formWithCard(String name, String region, int pad) {
        Form f = new Form(name, new BorderLayout());
        f.setWidth(320);
        f.setHeight(480);
        f.setVisible(true);
        Label card = new Label("card");
        card.setName("card");
        card.getAllStyles().setPadding(pad, pad, pad, pad);
        if (BorderLayout.NORTH.equals(region)) {
            f.add(BorderLayout.NORTH, card);
        } else {
            Container row = new Container(new BorderLayout());
            row.add(BorderLayout.WEST, card);
            f.add(BorderLayout.SOUTH, row);
        }
        return f;
    }

    private static Label cardOf(Form f) {
        return (Label) findByName(f, "card");
    }

    private static com.codename1.ui.Component findByName(Container root, String n) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            com.codename1.ui.Component c = root.getComponentAt(i);
            if (n.equals(c.getName())) {
                return c;
            }
            if (c instanceof Container) {
                com.codename1.ui.Component r = findByName((Container) c, n);
                if (r != null) {
                    return r;
                }
            }
        }
        return null;
    }

    // ---- arbitrary rendered element morphing ------------------------------

    @FormTest
    public void elementEndpointsAreExact() {
        MorphTransition.MorphElement e = movingElement();
        MorphTransition t = MorphTransition.create(500).morph(e);

        t.setProgress(0);
        assertEquals(0f, e.getCurrentX(), EPS);
        assertEquals(0f, e.getCurrentY(), EPS);
        assertEquals(20f, e.getCurrentWidth(), EPS);
        assertEquals(20f, e.getCurrentHeight(), EPS);
        assertEquals(1f, e.getCurrentOpacity(), EPS);
        assertEquals(0f, e.getCurrentRotation(), EPS);
        assertEquals(1f, e.getCurrentScale(), EPS);
        assertEquals(0.0, t.getProgress(), 1e-9);
        assertTrue(t.isScrubbing());

        t.setProgress(1);
        assertEquals(100f, e.getCurrentX(), EPS);
        assertEquals(50f, e.getCurrentY(), EPS);
        assertEquals(40f, e.getCurrentWidth(), EPS);
        assertEquals(60f, e.getCurrentHeight(), EPS);
        assertEquals(0f, e.getCurrentOpacity(), EPS);
        assertEquals(90f, e.getCurrentRotation(), EPS);
        assertEquals(2f, e.getCurrentScale(), EPS);
        assertEquals(1.0, t.getProgress(), 1e-9);
    }

    @FormTest
    public void elementMidpointIsStrictlyBetweenEndpoints() {
        MorphTransition.MorphElement e = movingElement();
        MorphTransition t = MorphTransition.create(500).morph(e);
        t.setProgress(0.5);
        assertTrue(e.getCurrentX() > 0f && e.getCurrentX() < 100f, "x=" + e.getCurrentX());
        assertTrue(e.getCurrentY() > 0f && e.getCurrentY() < 50f, "y=" + e.getCurrentY());
        assertTrue(e.getCurrentWidth() > 20f && e.getCurrentWidth() < 40f, "w=" + e.getCurrentWidth());
        assertTrue(e.getCurrentHeight() > 20f && e.getCurrentHeight() < 60f, "h=" + e.getCurrentHeight());
        assertTrue(e.getCurrentOpacity() > 0f && e.getCurrentOpacity() < 1f, "o=" + e.getCurrentOpacity());
        assertTrue(e.getCurrentRotation() > 0f && e.getCurrentRotation() < 90f, "r=" + e.getCurrentRotation());
        assertTrue(e.getCurrentScale() > 1f && e.getCurrentScale() < 2f, "s=" + e.getCurrentScale());
    }

    @FormTest
    public void progressIsClampedAndMonotonic() {
        MorphTransition.MorphElement e = movingElement();
        MorphTransition t = MorphTransition.create(500).morph(e);

        // out-of-range progress is clamped
        t.setProgress(-3.0);
        assertEquals(0.0, t.getProgress(), 1e-9);
        assertEquals(0f, e.getCurrentX(), EPS);
        t.setProgress(7.0);
        assertEquals(1.0, t.getProgress(), 1e-9);
        assertEquals(100f, e.getCurrentX(), EPS);

        float prev = -1f;
        for (int i = 0; i <= 20; i++) {
            t.setProgress(i / 20.0);
            float x = e.getCurrentX();
            assertTrue(x >= prev - EPS, "non-monotonic x at i=" + i + " (" + x + " < " + prev + ")");
            prev = x;
        }
    }

    @FormTest
    public void scrubIsDeterministicAndReversible() {
        MorphTransition.MorphElement e = movingElement();
        MorphTransition t = MorphTransition.create(500).morph(e);

        t.setProgress(0.3);
        float x30 = e.getCurrentX();
        float r30 = e.getCurrentRotation();

        // jump to the end then back: the state must be identical to the first
        // visit of 0.3 -- scrubbing never depends on history.
        t.setProgress(0.9);
        t.setProgress(0.3);
        assertEquals(x30, e.getCurrentX(), EPS);
        assertEquals(r30, e.getCurrentRotation(), EPS);

        // and a full reset returns exactly to the source state
        t.setProgress(0);
        assertEquals(0f, e.getCurrentX(), EPS);
        assertEquals(0f, e.getCurrentRotation(), EPS);
    }

    @FormTest
    public void scrubIsOrderIndependentForExportLoop() {
        MorphTransition.MorphElement e = MorphTransition.MorphElement.create(image(10, 10))
                .from(0, 0, 10, 10).to(80, 40, 30, 20)
                .opacity(1f, 0f).rotation(0f, 180f).scale(1f, 3f);
        MorphTransition t = MorphTransition.create(500).morph(e);

        double[] frames = {0.0, 0.25, 0.5, 0.75, 1.0};
        float[] fwdX = new float[frames.length];
        float[] fwdRot = new float[frames.length];
        float[] fwdScale = new float[frames.length];
        for (int i = 0; i < frames.length; i++) {
            t.setProgress(frames[i]);
            fwdX[i] = e.getCurrentX();
            fwdRot[i] = e.getCurrentRotation();
            fwdScale[i] = e.getCurrentScale();
        }
        // replay the same frames in reverse order -- a frame export loop must be
        // able to render any frame at any time and get the same pixels.
        for (int i = frames.length - 1; i >= 0; i--) {
            t.setProgress(frames[i]);
            assertEquals(fwdX[i], e.getCurrentX(), EPS, "x mismatch at frame " + i);
            assertEquals(fwdRot[i], e.getCurrentRotation(), EPS, "rotation mismatch at frame " + i);
            assertEquals(fwdScale[i], e.getCurrentScale(), EPS, "scale mismatch at frame " + i);
        }
    }

    @FormTest
    public void elementWithoutRectStaysInPlaceAndOnlyFades() {
        Image img = image(30, 40);
        MorphTransition.MorphElement e = MorphTransition.MorphElement.create(img).opacity(1f, 0f);
        Form src = blankForm();
        Form dst = blankForm();
        MorphTransition t = MorphTransition.create(400).morph(e);
        t.init(src, dst);
        t.initTransition();

        t.setProgress(0.5);
        // resolve() defaulted the rectangle to the image bounds and the
        // destination to the same rect, so only opacity moves.
        assertEquals(0f, e.getCurrentX(), EPS);
        assertEquals(0f, e.getCurrentY(), EPS);
        assertEquals(30f, e.getCurrentWidth(), EPS);
        assertEquals(40f, e.getCurrentHeight(), EPS);
        assertTrue(e.getCurrentOpacity() > 0f && e.getCurrentOpacity() < 1f);
        t.cleanup();
    }

    // ---- named component pair scrubbing -----------------------------------

    @FormTest
    public void namedPairScrubMovesComponentDeterministically() {
        Form src = formWithCard("src", BorderLayout.SOUTH, 4);
        Form dst = formWithCard("dst", BorderLayout.NORTH, 20);
        Label card = cardOf(src);

        MorphTransition t = MorphTransition.create(600).morph("card");
        t.init(src, dst);
        t.initTransition();

        t.setProgress(0);
        int x0 = card.getX(), y0 = card.getY(), w0 = card.getWidth(), h0 = card.getHeight();
        t.setProgress(1);
        int x1 = card.getX(), y1 = card.getY(), w1 = card.getWidth(), h1 = card.getHeight();
        assertTrue(x0 != x1 || y0 != y1 || w0 != w1 || h0 != h1,
                "morph endpoints are identical - layout did not differ");

        t.setProgress(0.5);
        assertBetween(x0, card.getX(), x1, "x");
        assertBetween(y0, card.getY(), y1, "y");
        assertBetween(w0, card.getWidth(), w1, "w");
        assertBetween(h0, card.getHeight(), h1, "h");

        // reversible back to the exact source bounds
        t.setProgress(0);
        assertEquals(x0, card.getX());
        assertEquals(y0, card.getY());
        assertEquals(w0, card.getWidth());
        assertEquals(h0, card.getHeight());
        t.cleanup();
    }

    @FormTest
    public void namedPairOpacityRotationScaleInterpolate() {
        Form src = formWithCard("src", BorderLayout.SOUTH, 4);
        Form dst = formWithCard("dst", BorderLayout.NORTH, 20);

        MorphTransition t = MorphTransition.create(600)
                .morph("card")
                .opacity("card", 1f, 0f)
                .rotation("card", 0f, 45f)
                .scale("card", 1f, 1.5f);
        t.init(src, dst);
        t.initTransition();

        t.setProgress(0);
        assertEquals(1f, t.getCurrentOpacity("card"), EPS);
        assertEquals(0f, t.getCurrentRotation("card"), EPS);
        assertEquals(1f, t.getCurrentScale("card"), EPS);

        t.setProgress(1);
        assertEquals(0f, t.getCurrentOpacity("card"), EPS);
        assertEquals(45f, t.getCurrentRotation("card"), EPS);
        assertEquals(1.5f, t.getCurrentScale("card"), EPS);

        t.setProgress(0.5);
        assertTrue(t.getCurrentOpacity("card") > 0f && t.getCurrentOpacity("card") < 1f);
        assertTrue(t.getCurrentRotation("card") > 0f && t.getCurrentRotation("card") < 45f);
        assertTrue(t.getCurrentScale("card") > 1f && t.getCurrentScale("card") < 1.5f);
        t.cleanup();
    }

    // ---- copy / reverse semantics -----------------------------------------

    @FormTest
    public void reverseSwapsElementEndpoints() {
        MorphTransition.MorphElement e = MorphTransition.MorphElement.create(image(10, 10))
                .from(0, 0, 10, 10).to(100, 0, 20, 20)
                .opacity(1f, 0f).rotation(0f, 90f).scale(1f, 2f);
        // package-private copy is reachable from an in-package test
        MorphTransition.MorphElement r = e.copy(true);

        r.applyFraction(0f);
        assertEquals(100f, r.getCurrentX(), EPS);
        assertEquals(20f, r.getCurrentWidth(), EPS);
        assertEquals(0f, r.getCurrentOpacity(), EPS);
        assertEquals(90f, r.getCurrentRotation(), EPS);
        assertEquals(2f, r.getCurrentScale(), EPS);

        r.applyFraction(1f);
        assertEquals(0f, r.getCurrentX(), EPS);
        assertEquals(10f, r.getCurrentWidth(), EPS);
        assertEquals(1f, r.getCurrentOpacity(), EPS);
        assertEquals(0f, r.getCurrentRotation(), EPS);
        assertEquals(1f, r.getCurrentScale(), EPS);
    }

    @FormTest
    public void copyPreservesForwardElementEndpoints() {
        MorphTransition.MorphElement e = movingElement();
        MorphTransition.MorphElement f = e.copy(false);
        f.applyFraction(0f);
        assertEquals(0f, f.getCurrentX(), EPS);
        assertEquals(1f, f.getCurrentOpacity(), EPS);
        f.applyFraction(1f);
        assertEquals(100f, f.getCurrentX(), EPS);
        assertEquals(0f, f.getCurrentOpacity(), EPS);
    }

    @FormTest
    public void freshTransitionIsNotScrubbing() {
        MorphTransition t = MorphTransition.create(300).morph("card");
        assertFalse(t.isScrubbing());
        assertEquals(0.0, t.getProgress(), 1e-9);
    }

    // ---- render smoke (the paint path must not throw) ---------------------

    @FormTest
    public void paintingElementMorphDoesNotThrow() {
        MorphTransition.MorphElement e = MorphTransition.MorphElement.create(image(24, 24))
                .from(0, 0, 24, 24).to(120, 80, 60, 60)
                .opacity(1f, 0.5f).rotation(0f, 90f).scale(1f, 1.4f);
        Form src = blankForm();
        Form dst = blankForm();
        MorphTransition t = MorphTransition.create(400).morph(e);
        t.init(src, dst);
        t.initTransition();

        Image canvas = Image.createImage(200, 320, 0xff000000);
        for (double p = 0; p <= 1.0; p += 0.2) {
            t.setProgress(p);
            t.paint(canvas.getGraphics()); // exercises paintElements + drawTransformed
        }
        t.cleanup();
    }

    @FormTest
    public void paintingSnapshotNamedPairWithTransformsDoesNotThrow() {
        Form src = formWithCard("src", BorderLayout.SOUTH, 4);
        Form dst = formWithCard("dst", BorderLayout.NORTH, 20);
        MorphTransition t = MorphTransition.create(400)
                .snapshotMode(true)
                .morph("card")
                .rotation("card", 0f, 30f)
                .scale("card", 1f, 1.2f)
                .opacity("card", 1f, 0.6f);
        t.init(src, dst);
        t.initTransition(); // captures the source/dest snapshots

        Image canvas = Image.createImage(320, 480, 0xff000000);
        for (double p = 0; p <= 1.0; p += 0.25) {
            t.setProgress(p);
            t.paint(canvas.getGraphics()); // exercises paintSnapshots + drawTransformed
        }
        t.cleanup();
    }

    private static void assertBetween(int a, int mid, int b, String label) {
        int lo = Math.min(a, b) - 1;
        int hi = Math.max(a, b) + 1;
        assertTrue(mid >= lo && mid <= hi,
                label + " midpoint " + mid + " not within [" + lo + ", " + hi + "]");
    }
}
