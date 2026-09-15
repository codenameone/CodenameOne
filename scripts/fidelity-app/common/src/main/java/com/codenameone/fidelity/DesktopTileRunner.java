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
 * wrong here: the same host has to be able to render whichever theme it is asked for, and the
 * golden set is named for a design generation rather than for a machine.</p>
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

        Display.init(new java.awt.Container());
        final int[] written = new int[1];
        final Throwable[] failure = new Throwable[1];
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                try {
                    written[0] = renderAll(platform, themeRes, outDir);
                } catch (Throwable t) {
                    failure[0] = t;
                }
            }
        });

        // The EDT does the work; this thread waits for it. Bounded rather than open-ended so
        // a hang fails the run instead of holding a CI job until the job timeout.
        long deadline = System.currentTimeMillis() + 120000L;
        while (written[0] == 0 && failure[0] == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(200);
        }
        if (failure[0] != null) {
            failure[0].printStackTrace();
            System.exit(3);
        }
        if (written[0] == 0) {
            System.err.println("DesktopTileRunner: no tiles were rendered before the deadline");
            System.exit(4);
        }
        System.out.println("CN1SS:INFO desktop tiles written: " + written[0]);
        System.exit(0);
    }

    private static int renderAll(String platform, String themeRes, File outDir) throws Exception {
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
        f.show();
        Component comp = Cn1WidgetRenderer.build(c, state, appearance);
        if (comp == null) {
            return false;
        }
        if ("DesktopSlider".equals(c.getId()) || "DesktopProgressBar".equals(c.getId())) {
            comp.setPreferredW(w);
        }
        comp.getAllStyles().setMargin(0, 0, 0, 0);

        Container row = new Container(new FlowLayout());
        row.add(comp);
        f.add(BorderLayout.CENTER, row);
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
