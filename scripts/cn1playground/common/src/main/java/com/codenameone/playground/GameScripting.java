package com.codenameone.playground;

import com.codename1.ui.Component;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;

import java.util.function.Consumer;

/// Bridges custom 2D drawing to the single-method lambdas the Playground's
/// BeanShell runner can produce.
///
/// On the JavaScript port the runner cannot subclass `Component` to override
/// `paint(Graphics)` (ParparVM is ahead-of-time compiled, so BeanShell's
/// runtime class generation has nothing to bind to). Single abstract method
/// lambdas DO work, so this compiled adapter lets a script draw with a
/// `(Graphics, Component)` lambda instead of an anonymous subclass:
///
/// ```java
/// Component view = GameScripting.canvas(320, 480, g -> {
///     g.setColor(0x10182a);
///     g.fillRect(0, 0, 320, 480);   // local 0,0-relative coordinates
///     // ... draw the scene ...
/// });
/// ```
///
/// The `Graphics` is translated to the component's origin before the lambda
/// runs, so the lambda draws in local (0,0-relative) coordinates -- it doesn't
/// need the component's absolute position. A single-argument `Consumer` is used
/// because BeanShell's lambda support on the AOT JavaScript port binds
/// single-method functional interfaces; this is the 2D counterpart to
/// [GpuScripting] for the GPU `Renderer`.
public final class GameScripting {
    private GameScripting() {
    }

    /// Builds a `Component` of the given preferred size whose `paint` forwards to
    /// the supplied lambda. `painter` receives a `Graphics` already translated to
    /// the component's top-left, so it draws in local coordinates.
    public static Component canvas(final int width, final int height, final Consumer painter) {
        return new Component() {
            @Override
            protected Dimension calcPreferredSize() {
                return new Dimension(width, height);
            }

            @Override
            @SuppressWarnings("unchecked")
            public void paint(Graphics g) {
                if (painter == null) {
                    return;
                }
                int tx = getX();
                int ty = getY();
                g.translate(tx, ty);
                try {
                    painter.accept(g);
                } finally {
                    g.translate(-tx, -ty);
                }
            }
        };
    }
}
