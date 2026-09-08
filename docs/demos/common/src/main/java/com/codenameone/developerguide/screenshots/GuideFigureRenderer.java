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

package com.codenameone.developerguide.screenshots;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.ImageIO;
import com.codename1.ui.util.Resources;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;

/// Renders [GuideFigure]s under a native theme at a device profile.
///
/// This is deliberately separate from `PreAdvancedThemingScreenshots`, which
/// draws the schematic layout diagrams. Those are green labelled blocks whose
/// job is to show where a layout manager puts things, and rendering them under
/// a real theme would bury that teaching point under component chrome. They
/// keep their hand-rolled styling and their own pass; this class is for figures
/// whose subject is a real interface.
public final class GuideFigureRenderer {
    private GuideFigureRenderer() {
    }

    /// Where a rendered figure is written. Same shape as the sink the
    /// schematic generator uses, so one output directory holds both passes.
    public interface ScreenshotSink {
        OutputStream open(String fileName) throws IOException;
    }

    /// Renders every variant belonging to `device`.
    ///
    /// The theme is installed once per call. Appearance is not: dark mode is
    /// set per variant, each time followed by `UIManager.refreshTheme()`,
    /// without which the styles stay cached at their light values and every
    /// dark frame silently renders light.
    public static void render(ScreenshotSink sink, FigureDevice device, FigureVariant[] variants)
            throws IOException {
        assertNoSkinIsActive();
        installTheme(device.themeResource());

        StringBuilder failures = new StringBuilder();
        int rendered = 0;
        for (FigureVariant variant : variants) {
            if (variant.device() != device) {
                continue;
            }
            try {
                Display.getInstance().setDarkMode(Boolean.valueOf(variant.darkMode()));
                UIManager.getInstance().refreshTheme();

                Form form = variant.figure().build();
                prepare(form, device);
                verifyAppearance(variant, form);
                write(sink, variant.fileName(), form, device);
                rendered++;
            } catch (Throwable err) {
                // One figure that cannot render must not take the rest with it:
                // the run reports every failure and then fails, which beats
                // stopping at the first and rediscovering the next one build
                // after build.
                failures.append("\n  ").append(variant.fileName()).append(": ")
                        .append(err.getClass().getName()).append(": ").append(err.getMessage());
            }
        }
        System.out.println("Rendered " + rendered + " figure(s) for " + device.key());
        if (failures.length() > 0) {
            throw new IOException("figures failed to render:" + failures);
        }
    }

    /// A skin puts the host machine's fonts back into the output, which is the
    /// one difference that makes a figure impossible to regenerate elsewhere.
    /// Nothing here loads one today; this fails loudly if that ever changes.
    private static void assertNoSkinIsActive() {
        if ("ios".equals(Display.getInstance().getPlatformName())) {
            throw new IllegalStateException(
                    "a skin is active, so native: fonts would resolve to installed system "
                    + "faces and these figures would stop being reproducible off this machine");
        }
    }

    private static void installTheme(String resourcePath) throws IOException {
        Resources res = Resources.open(resourcePath);
        String[] names = res.getThemeResourceNames();
        if (names == null || names.length == 0) {
            throw new IllegalStateException("Theme resource " + resourcePath + " contains no themes");
        }
        Hashtable themeProps = res.getTheme(names[0]);
        UIManager.getInstance().setThemeProps(themeProps);
        UIManager.getInstance().refreshTheme();
    }

    /// Checks the appearance actually took, rather than trusting the file name.
    ///
    /// A dark render that comes out light is the most expensive failure
    /// available here, because it is invisible in review and repeats across
    /// every figure. The port has produced exactly that before, so the dark
    /// frames are required to differ from the theme's light background rather
    /// than merely to have been asked for.
    private static void verifyAppearance(FigureVariant variant, Form form) {
        Boolean reported = Display.getInstance().isDarkMode();
        if (reported == null || reported.booleanValue() != variant.darkMode()) {
            throw new IllegalStateException(variant.fileName() + ": asked for darkMode="
                    + variant.darkMode() + " but Display reports " + reported);
        }
        // Only the dark direction is asserted. The failure worth catching is a
        // dark render that silently comes out light, which repeats across every
        // figure and is invisible in review. The converse is not a failure at
        // all: a figure is free to set a dark background of its own in light
        // mode, and the image masking figure does exactly that -- a red form,
        // which a luminance test reads as "dark" and which this used to reject.
        if (variant.darkMode() && isLight(form.getStyle().getBgColor())) {
            throw new IllegalStateException(variant.fileName()
                    + ": dark mode is set but the form background is "
                    + Integer.toHexString(form.getStyle().getBgColor())
                    + ", which is a light colour");
        }
    }

    private static boolean isLight(int rgb) {
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        return (r * 299 + g * 587 + b * 114) / 1000 > 127;
    }

    private static void prepare(Form form, FigureDevice device) {
        CN.setWindowSize(device.width(), device.height());
        form.setScrollableY(false);
        form.show();
        form.setWidth(device.width());
        form.setHeight(device.height());
        form.getContentPane().setWidth(device.width());
        form.getContentPane().setHeight(
                Math.max(0, device.height() - form.getTitleArea().getHeight()));
        form.revalidate();
    }

    /// Height to keep, so a figure is the part of the screen that has something
    /// on it rather than a phone-shaped picture that is four fifths empty.
    ///
    /// Two shapes have to work at once, and each breaks the obvious reading of
    /// the other. prepare() sizes the content pane to the whole device, so a
    /// child in BorderLayout.CENTER is stretched to fill it: trusting its laid-out
    /// height cropped nothing at all and left the table and tree figures 409x917
    /// with a blank page below the content. But a component that paints itself and
    /// declares no intrinsic size -- the custom Component behind the Hi World
    /// figure -- reports a preferred height of zero while legitimately filling the
    /// whole viewport, and trusting THAT cropped it to a 26-pixel sliver of its
    /// red canvas.
    ///
    /// So: a preferred height is used when the component states one, and a
    /// component that states none is taken at the height it was given.
    private static int figureHeight(Form form, FigureDevice device) {
        Container content = form.getContentPane();
        int bottom = 0;
        for (int i = 0; i < content.getComponentCount(); i++) {
            Component child = content.getComponentAt(i);
            int preferred = child.getPreferredH();
            int height = preferred > 0 ? preferred : child.getHeight();
            bottom = Math.max(bottom, child.getY() + height);
        }
        // No children at all means the figure IS the title area -- the toolbar
        // figures are exactly that -- so the crop stops just below it rather than
        // falling back to the content pane's stretched height, which produced a
        // toolbar with a blank page underneath.
        int used = content.getAbsoluteY() + bottom + content.getStyle().getPaddingBottom();
        return Math.min(device.height(), Math.max(1, used));
    }

    private static void write(ScreenshotSink sink, String fileName, Form form, FigureDevice device)
            throws IOException {
        Image screenshot = Image.createImage(device.width(), device.height(), 0xffffff);
        Graphics graphics = screenshot.getGraphics();
        form.paintComponent(graphics, true);
        screenshot = screenshot.subImage(0, 0, device.width(), figureHeight(form, device), true);
        OutputStream out = sink.open(fileName);
        try {
            ImageIO.getImageIO().save(screenshot, out, ImageIO.FORMAT_PNG, 1);
        } finally {
            out.close();
        }
    }
}
