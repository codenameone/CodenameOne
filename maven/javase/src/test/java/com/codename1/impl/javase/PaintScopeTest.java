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
package com.codename1.impl.javase;

import com.codename1.io.Log;
import com.codename1.testing.junit.CodenameOneTest;
import com.codename1.testing.junit.RunOnEdt;
import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Painter;
import com.codename1.ui.geom.Rectangle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the simulator's strict paint-scope checks added for issue #5058:
/// excess `popClip` must throw, and a paint scope that leaks Graphics state
/// must auto-restore the state at end-of-scope.
@CodenameOneTest
@DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
public class PaintScopeTest {

    private JavaSEPort port() {
        return JavaSEPort.instance;
    }

    @Test
    public void excessPopClipThrows() {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            assertThrows(IllegalStateException.class, () -> port().popClip(g));
        } finally {
            g.dispose();
        }
    }

    @Test
    public void overPopAfterMatchingPushThrows() {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            port().pushClip(g);
            port().popClip(g);
            // Second pop has no matching push.
            assertThrows(IllegalStateException.class, () -> port().popClip(g));
        } finally {
            port().disposeGraphics(g);
            g.dispose();
        }
    }

    @Test
    public void scopeAutoRestoresClipStackDepth() {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            Object owner = new Object();
            port().beginPaintScope(g, owner);
            port().pushClip(g);
            port().pushClip(g);
            // Deliberately forget to pop.
            port().endPaintScope(g, owner);
            // Auto-restore must drain the stack so the next pop fails cleanly
            // rather than corrupting subsequent paints (the issue #5058 leak).
            assertThrows(IllegalStateException.class, () -> port().popClip(g));
        } finally {
            port().disposeGraphics(g);
            g.dispose();
        }
    }

    @Test
    public void scopeAutoRestoresTransformAndColor() {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            AffineTransform initialTx = new AffineTransform(g.getTransform());
            Color initialColor = g.getColor();

            Object owner = new Object();
            port().beginPaintScope(g, owner);
            g.translate(50, 50);
            g.setColor(Color.MAGENTA);
            port().endPaintScope(g, owner);

            assertEquals(initialTx, g.getTransform(),
                    "transform must be restored to its pre-scope value");
            assertEquals(initialColor, g.getColor(),
                    "color must be restored to its pre-scope value");
        } finally {
            port().disposeGraphics(g);
            g.dispose();
        }
    }

    @Test
    public void cleanScopeProducesNoChange() {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            AffineTransform initialTx = new AffineTransform(g.getTransform());
            Color initialColor = g.getColor();

            Object owner = new Object();
            port().beginPaintScope(g, owner);
            // Mimic a well-behaved paint: push, draw, pop -- all balanced.
            port().pushClip(g);
            g.translate(10, 10);
            g.setColor(Color.RED);
            g.translate(-10, -10);
            g.setColor(initialColor);
            port().popClip(g);
            port().endPaintScope(g, owner);

            assertEquals(initialTx, g.getTransform());
            assertEquals(initialColor, g.getColor());
            // A balanced scope leaves the stack empty -- a follow-up pop must
            // still throw to flag any *new* unbalanced caller.
            assertThrows(IllegalStateException.class, () -> port().popClip(g));
        } finally {
            port().disposeGraphics(g);
            g.dispose();
        }
    }

    /// Capturing Log used by the dedup test below. It installs itself in
    /// `install()`, restores the previous instance in `restore()`, and records
    /// only `paint-scope:` lines so unrelated output stays out of the way.
    private static final class CapturingLog extends Log {
        final List<String> messages = new ArrayList<>();
        private Log previous;
        void install() {
            previous = Log.getInstance();
            Log.install(this);
        }
        void restore() {
            Log.install(previous);
        }
        @Override
        protected void print(String text, int level) {
            if (text != null && text.startsWith("paint-scope:")) {
                messages.add(text);
            }
        }
    }

    @Test
    public void repeatedLeakLogsOncePerSignature() {
        // Issue #5102: a component that leaks state every paint used to flood
        // the EDT log -- one warning per repaint at ~60Hz. The dedup keeps the
        // diagnostic value (first occurrence reported) while collapsing the
        // tail of identical reports.
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        CapturingLog log = new CapturingLog();
        log.install();
        try {
            // Class with a distinct fully-qualified name so this test's
            // signature is unique even if other tests run in the same JVM and
            // pollute the shared dedup set on JavaSEPort.instance.
            class LeakySignatureA_5102 {}
            class LeakySignatureB_5102 {}
            Object ownerA = new LeakySignatureA_5102();
            Object ownerB = new LeakySignatureB_5102();

            for (int i = 0; i < 5; i++) {
                port().beginPaintScope(g, ownerA);
                g.setColor(Color.MAGENTA);
                port().endPaintScope(g, ownerA);
            }

            int afterA = log.messages.size();
            assertEquals(1, afterA,
                    "repeating the same (owner-class, leak-kind) must log only once");

            // A different owner class with the same leak kind is a new
            // signature -- we still want one warning so devs see it.
            port().beginPaintScope(g, ownerB);
            g.setColor(Color.MAGENTA);
            port().endPaintScope(g, ownerB);
            assertEquals(2, log.messages.size(),
                    "a different owner class must produce its own warning");

            // Even after dedup the auto-restore must keep working -- the log
            // suppression must not gate the state-fix-up.
            Color initialColor = g.getColor();
            port().beginPaintScope(g, ownerA);
            g.setColor(Color.GREEN);
            port().endPaintScope(g, ownerA);
            assertEquals(initialColor, g.getColor(),
                    "auto-restore must run even when the warning is suppressed");
        } finally {
            port().disposeGraphics(g);
            g.dispose();
            log.restore();
        }
    }

    @Test
    @RunOnEdt
    public void componentPaintLeakIsAbsorbedByFrameworkWrapper() {
        // Issue #5102 root-cause fix: framework callers in Component/Form save
        // and restore color/font/alpha around each Painter / paint() dispatch,
        // so a leaky paint method can no longer poison the next draw on iOS or
        // spam the simulator log. This test paints a real Component whose
        // paint() deliberately leaks all three properties and verifies:
        //   1) the outer Graphics state is unchanged afterwards (real bug fix),
        //   2) the simulator's paint-scope checker never logs a warning
        //      (the spam in the bug report is gone).
        CapturingLog log = new CapturingLog();
        log.install();
        try {
            final int leakColor = 0xff00ff;
            final int leakAlpha = 77;
            final Font leakFont = Font.createSystemFont(Font.FACE_MONOSPACE,
                    Font.STYLE_BOLD, Font.SIZE_LARGE);

            Component leaky = new Component() {
                @Override
                public void paint(Graphics g) {
                    g.setColor(leakColor);
                    g.setAlpha(leakAlpha);
                    g.setFont(leakFont);
                }

                @Override
                protected void paintBackground(Graphics g) {
                    g.setColor(leakColor);
                    g.setAlpha(leakAlpha);
                    g.setFont(leakFont);
                }
            };
            leaky.setX(0);
            leaky.setY(0);
            leaky.setWidth(40);
            leaky.setHeight(40);

            Image img = Image.createImage(40, 40);
            Graphics g = img.getGraphics();
            int outerColor = 0x123456;
            int outerAlpha = 200;
            Font outerFont = Font.createSystemFont(Font.FACE_SYSTEM,
                    Font.STYLE_PLAIN, Font.SIZE_SMALL);
            g.setColor(outerColor);
            g.setAlpha(outerAlpha);
            g.setFont(outerFont);

            leaky.paintComponent(g, true);

            assertEquals(outerColor, g.getColor(),
                    "framework wrapper must restore color after a leaky paint");
            assertEquals(outerAlpha, g.getAlpha(),
                    "framework wrapper must restore alpha after a leaky paint");
            assertEquals(outerFont, g.getFont(),
                    "framework wrapper must restore font after a leaky paint");
            assertTrue(log.messages.isEmpty(),
                    "framework restore must happen before endPaintScope so the"
                            + " simulator checker never sees a leak (log was: "
                            + log.messages + ")");
        } finally {
            log.restore();
        }
    }

    @Test
    @RunOnEdt
    public void bgPainterLeakIsAbsorbedByFrameworkWrapper() {
        // Same protection as above but on the Painter.paint dispatch used for
        // a Style's BgPainter -- that was the most common offender in the bug
        // report (`com.codename1.ui.Component$BGPainter`).
        CapturingLog log = new CapturingLog();
        log.install();
        try {
            final int leakColor = 0x00ff00;
            final int leakAlpha = 88;
            final Font leakFont = Font.createSystemFont(Font.FACE_MONOSPACE,
                    Font.STYLE_ITALIC, Font.SIZE_LARGE);

            Painter leakyPainter = new Painter() {
                @Override
                public void paint(Graphics g, Rectangle rect) {
                    g.setColor(leakColor);
                    g.setAlpha(leakAlpha);
                    g.setFont(leakFont);
                }
            };

            Component host = new Component() {};
            host.setX(0);
            host.setY(0);
            host.setWidth(40);
            host.setHeight(40);
            host.getUnselectedStyle().setBgPainter(leakyPainter);

            Image img = Image.createImage(40, 40);
            Graphics g = img.getGraphics();
            int outerColor = 0x654321;
            int outerAlpha = 210;
            Font outerFont = Font.createSystemFont(Font.FACE_SYSTEM,
                    Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
            g.setColor(outerColor);
            g.setAlpha(outerAlpha);
            g.setFont(outerFont);

            host.paintComponent(g, true);

            assertEquals(outerColor, g.getColor());
            assertEquals(outerAlpha, g.getAlpha());
            assertEquals(outerFont, g.getFont());
            assertTrue(log.messages.isEmpty(),
                    "BgPainter leaks must be absorbed silently (log was: "
                            + log.messages + ")");
        } finally {
            log.restore();
        }
    }

    @Test
    public void nestedScopesLifoBalance() {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            Object outer = new Object();
            Object inner = new Object();
            port().beginPaintScope(g, outer);
            g.translate(5, 5);
            port().beginPaintScope(g, inner);
            g.translate(7, 7);
            // Inner forgets to undo its translate.
            port().endPaintScope(g, inner);
            // After inner, translate should be back to (5, 5).
            assertTrue(g.getTransform().getTranslateX() == 5.0
                    && g.getTransform().getTranslateY() == 5.0,
                    "inner scope auto-restore should leave outer translate intact");
            port().endPaintScope(g, outer);
            // After outer, transform should be identity again.
            assertTrue(g.getTransform().isIdentity(),
                    "outer scope auto-restore should reach the original identity");
        } finally {
            port().disposeGraphics(g);
            g.dispose();
        }
    }
}
