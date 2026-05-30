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
