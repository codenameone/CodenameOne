/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codenameone.fidelity;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import com.codenameone.fidelity.render.Cn1WidgetRenderer;
import com.codenameone.fidelity.spec.ComponentSpec;
import com.codenameone.fidelity.spec.FidelitySpec;
import com.codenameone.fidelity.spec.FidelitySpecParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Renders the Codename One half of the desktop fidelity comparison.
 *
 * <p>Unlike the mobile runners this one does not stream tiles over a WebSocket from a device.
 * The desktop side runs in the same process as the host, so it writes PNGs straight to a
 * directory and the comparison picks them up from there -- the transport existed to get bytes
 * off a phone, and there is no phone here.</p>
 *
 * <p>The platform is passed in rather than read from the port. JavaSE answers "win", "mac" or
 * "linux" for the HOST it happens to be running on, which is right for an application and
 * different from the fixture identifiers here (windows, macos, gnome). Each fixture must
 * run on its matching host with the native reference font installed.</p>
 *
 * <p>Tiles are named {@code <id>_<state>_<appearance>_cn1.png}, which is the convention
 * ProcessScreenshots pairs against {@code <id>_<state>_<appearance>.png} in the golden
 * directory.</p>
 */
public final class DesktopTileRunner {
    private DesktopTileRunner() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage: DesktopTileRunner <platform> <themeResource> <outDir>");
            System.exit(2);
        }
        final String platform = args[0];
        final String themeRes = args[1];
        final File outDir = new File(args[2]);
        outDir.mkdirs();

        // Native tiles use 1x desktop logical pixels (96 dpi), not the mobile
        // density fallback of 5 px/mm or the host monitor's Retina backing scale.
        // Set these before JavaSEPort initializes its static font/scale defaults.
        System.setProperty("cn1.retinaScale", "1");
        System.setProperty("cn1.javase.pixelMilliRatio", Double.toString(96.0 / 25.4));
        com.codename1.impl.javase.JavaSEPort.setDefaultPixelMilliRatio(Double.valueOf(96.0 / 25.4));
        // Select through the packaged-app entry point so font defaults follow the theme.
        com.codename1.impl.javase.JavaSEPort.setNativeTheme("/" + themeRes + ".res");
        Display.init(new java.awt.Container());
        final int[] written = new int[1];
        final Throwable[] failure = new Throwable[1];
        final java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                try {
                    written[0] = renderAll(platform, themeRes, outDir);
                } catch (Throwable t) {
                    failure[0] = t;
                } finally {
                    done.countDown();
                }
            }
        });

        // The EDT does the work; this thread waits for it. Through a latch rather than a
        // sleep loop over the two arrays: a plain field written on one thread has no
        // happens-before edge to a read on another, so nothing required this thread to ever
        // observe the render finishing. A JVM is free to keep serving the initial values,
        // wait out the entire timeout and report "no tiles were rendered" for a run that
        // rendered everything -- or to swallow the real exception behind that message.
        // await() supplies the edge, so everything the EDT wrote before countDown() is
        // visible here. Bounded rather than open-ended so a genuine hang fails the run
        // instead of holding a CI job until the job timeout.
        if (!done.await(120, java.util.concurrent.TimeUnit.SECONDS)) {
            System.err.println("DesktopTileRunner: the render did not finish within the deadline");
            System.exit(4);
        }
        if (failure[0] != null) {
            failure[0].printStackTrace();
            System.exit(3);
        }
        if (written[0] == 0) {
            System.err.println("DesktopTileRunner: no tiles were rendered");
            System.exit(4);
        }
        System.out.println("CN1SS:INFO desktop tiles written: " + written[0]);
        System.exit(0);
    }

    private static int renderAll(String platform, String themeRes, File outDir) throws Exception {
        java.awt.Font nativeFont = (java.awt.Font) com.codename1.impl.javase.JavaSEPort.instance.loadTrueTypeFont(
                "native:MainRegular", "native:MainRegular");
        int pixelsPer100mm = Display.getInstance().convertToPixels(100f);
        if (pixelsPer100mm != 378) throw new IllegalStateException("Unexpected capture density: " + pixelsPer100mm);
        System.out.println("Desktop capture scale: 1x, 96 dpi (100mm=" + pixelsPer100mm + "px)");
        String family = nativeFont.getFamily();
        System.out.println("Desktop native font: " + nativeFont.getName() + " (family=" + family + ")");
        boolean expected = "gnome".equals(platform) ? "Cantarell".equals(family)
                : ("macos".equals(platform) ? ".AppleSystemUIFont".equals(family)
                : family.startsWith("Segoe UI Variable"));
        if (!expected) throw new IllegalStateException("Native reference font unavailable: " + family);
        FidelitySpec spec = FidelitySpecParser.parse(readSpec());
        int count = 0;
        List appearances = spec.getAppearances();
        if (appearances == null || appearances.isEmpty()) {
            appearances = java.util.Arrays.asList(new String[]{"light"});
        }
        StringBuffer backgrounds = new StringBuffer();
        for (Object ao : appearances) {
            String appearance = (String) ao;
            boolean dark = "dark".equals(appearance);
            // Set the mode BEFORE installing the theme: UIManager resolves the $Dark entries
            // while it builds, and the check-box and radio glyphs are baked during that pass,
            // so flipping afterwards leaves the glyphs coloured for the other scheme.
            CN.setDarkMode(Boolean.valueOf(dark));
            installTheme(themeRes);
            backgrounds.append(appearance).append('=')
                    .append(toHex(tileBackground())).append('\n');

            List components = spec.getComponents();
            for (int i = 0; i < components.size(); i++) {
                ComponentSpec c = (ComponentSpec) components.get(i);
                if (!c.appliesToPlatform(platform) || !Cn1WidgetRenderer.isSupported(c.getId())) {
                    continue;
                }
                int w = spec.tileWidthPx(c);
                int h = spec.tileHeightPx(c);
                List states = c.getStates();
                for (int j = 0; j < states.size(); j++) {
                    String state = (String) states.get(j);
                    if (renderTile(c, state, appearance, w, h, outDir)) {
                        count++;
                    }
                }
            }
        }
        writeBackgrounds(backgrounds.toString(), outDir);
        return count;
    }

    /// The colour the tiles are painted on, read back from the theme that was just
    /// installed rather than written down anywhere.
    ///
    /// The comparator needs this to tell widget pixels from backdrop, and it used to
    /// assume white for light and black for dark -- which is true of the mobile tiles
    /// and true of no desktop platform. Fluent's light surface is #F3F3F3 and its dark
    /// one #202020, Aqua's is #ECECEC, Adwaita's #FAFAFA. Against a hardcoded white
    /// the mask's tolerance (10 per channel) is exceeded by all but one of them, so
    /// roughly 80% of every tile -- the empty backdrop -- was classified as widget
    /// content. That does not fail; it inflates the score, because both tiles agree
    /// about the backdrop they are both mostly made of.
    ///
    /// Reading it from the theme rather than declaring it in the spec keeps one copy
    /// of the value. Restyle a theme's background and the measurement follows; a
    /// second copy in the YAML would go stale silently and in the direction that
    /// looks like success.
    /// Known limitation, recorded here because the number it produces looks fine:
    /// Fluent's light control fill (#FBFBFB) is 8 levels off its page surface (#F3F3F3),
    /// under the comparator's 10-per-channel content tolerance. So on Fluent light tiles
    /// the mask sees the button's BORDER and TEXT and not its fill, and the reported
    /// geometry bbox is the text's, not the control's. It does not bias the fidelity
    /// score -- the native Fluent button is the same two colours, so both sides mask
    /// identically -- but do not read a Fluent light width_ratio as a control width.
    /// Adwaita is tighter still at 5. Lowering the tolerance is not the fix; it would
    /// start counting anti-aliasing as content everywhere else.
    /// Kept in sync BY HAND with FULL_WIDTH_KINDS in each native reference app. The two
    /// sides must agree: if one stretches a control to the tile and the other does not, the
    /// comparison is between two different geometries and the score means nothing.
    private static final java.util.Set<String> FULL_WIDTH_IDS =
            new java.util.HashSet<String>(java.util.Arrays.asList(
                    "DesktopSlider", "DesktopProgressBar", "DesktopTextField"));

    private static int tileBackground() {
        return UIManager.getInstance().getComponentStyle("Form").getBgColor();
    }

    private static String toHex(int rgb) {
        String h = Integer.toHexString(rgb & 0xffffff);
        while (h.length() < 6) {
            h = "0" + h;
        }
        return "#" + h.toUpperCase();
    }

    private static void writeBackgrounds(String body, File outDir) throws Exception {
        OutputStream out = new FileOutputStream(new File(outDir, "tile-backgrounds.properties"));
        try {
            out.write(("# Written by DesktopTileRunner; read by ProcessScreenshots --mode fidelity.\n"
                    + "# The backdrop colour each appearance's tiles were painted on, taken from the\n"
                    + "# installed theme's Form style. See tileBackground().\n"
                    + body).getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }

    private static boolean renderTile(ComponentSpec c, String state, String appearance,
                                      int w, int h, File outDir) throws Exception {
        Form f = new Form(new BorderLayout());
        // A Form always builds a title area, and it is NOT free here. The desktop tile
        // contract is "the widget at its natural size, anchored top-left on the theme's
        // surface"; an unhidden title area both pushes the widget down and paints a strip
        // of its own across the top of every tile. On Fluent and Aqua the strip is the
        // same colour as the page so nothing looks wrong, and on Adwaita -- whose
        // headerbar is #ebebeb against a #fafafa page -- it is 15 levels off the
        // background, which is over the content mask's tolerance, so it was measured as
        // widget content on every single GNOME tile.
        //
        // setHidden(true) takes it out of layout as well as out of the paint, which
        // setVisible(false) alone does not.
        f.getTitleArea().setHidden(true);
        f.getTitleArea().setVisible(false);
        f.show();
        Component comp = Cn1WidgetRenderer.build(c, state, appearance);
        if (comp == null) {
            return false;
        }
        // Controls with no natural width: layout always assigns one, so the tile width is
        // the honest answer and it is the rule the native reference apps apply too. Left to
        // size itself a text field measures to its content, which is not a control anyone
        // would recognise -- AppKit gives 39px for the string "Text".
        if (FULL_WIDTH_IDS.contains(c.getId())) {
            comp.setPreferredW(w);
        }
        comp.getAllStyles().setMargin(0, 0, 0, 0);
        // getAllStyles() deliberately EXCLUDES the hover style, so it is zeroed here as well
        // -- and here rather than inside the renderer, because this clear runs AFTER build()
        // returns. A renderer that normalised hover during build had its work undone one line
        // later for every component whose own branch did not zero margins (DesktopSwitch and
        // DesktopSlider keep the theme's 0.5/0.8mm), leaving the hover tile at a different
        // offset and size from both the native control and the CN1 normal state -- scored as
        // a fidelity loss that has nothing to do with the hover colours being measured.
        Style hoverStyle = comp.getHoverStyle();
        if (hoverStyle != null) {
            hoverStyle.setMargin(0, 0, 0, 0);
        }

        // NORTH, not CENTER: BorderLayout's centre region would centre the widget
        // vertically in whatever space is left, and the contract is top-left.
        Container row = new Container(new FlowLayout());
        row.getAllStyles().setMargin(0, 0, 0, 0);
        row.getAllStyles().setPadding(0, 0, 0, 0);
        row.add(comp);
        f.add(BorderLayout.NORTH, row);
        f.setSize(new Dimension(w, h));
        f.layoutContainer();

        Image img = Image.createImage(w, h);
        f.paintComponent(img.getGraphics(), true);

        String name = c.getId() + "_" + state + "_" + appearance + "_cn1.png";
        writePng(img, new File(outDir, name));
        return true;
    }

    private static void installTheme(String themeRes) throws Exception {
        InputStream in = DesktopTileRunner.class.getResourceAsStream("/" + themeRes + ".res");
        if (in == null) {
            throw new IllegalStateException("theme resource not on the classpath: /" + themeRes + ".res");
        }
        try {
            Resources r = Resources.open(in);
            String[] names = r.getThemeResourceNames();
            if (names == null || names.length == 0) {
                throw new IllegalStateException("no themes inside " + themeRes);
            }
            UIManager.getInstance().setThemeProps(r.getTheme(names[0]));
        } finally {
            in.close();
        }
    }

    /// Writes one tile with javax.imageio, straight out of the JavaSE port's own
    /// BufferedImage peer.
    ///
    /// Host APIs are correct here and are the reason this class is not in `common`:
    /// that module is compiled as Codename One application code under a
    /// bytecode-compliance gate, and CN1's FileSystemStorage does not address a host
    /// directory the comparator can read anyway.
    private static void writePng(Image img, File dest) throws Exception {
        Object peer = img.getImage();
        if (peer instanceof java.awt.image.RenderedImage) {
            javax.imageio.ImageIO.write((java.awt.image.RenderedImage) peer, "png", dest);
            return;
        }
        // The JavaSE port backs a CN1 Image with a BufferedImage, so the branch above is the
        // one that runs. Falling through means the port changed underneath this, which must
        // fail rather than write nothing and report success.
        throw new IllegalStateException("cannot encode the tile: image peer is "
                + (peer == null ? "null" : peer.getClass().getName()));
    }

    private static String readSpec() throws Exception {
        InputStream in = DesktopTileRunner.class.getResourceAsStream("/fidelity-tests.yaml");
        if (in == null) {
            throw new IllegalStateException("fidelity-tests.yaml is not on the classpath");
        }
        try {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            return new String(bos.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }
}
