/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.play;

import com.codename1.ui.Font;
import com.codename1.ui.Image;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/// The images and the two fonts, loaded once from the jar's root, where
/// `tools/make-art.py` writes them. ART.md says where each comes from.
public final class Art {
    private static final Map<String, Image> CACHE = new HashMap<String, Image>();

    private Art() {
    }

    /// The image `/name.png` (or `.jpg` for the `tex_` paper photos).
    public static Image get(String name) {
        Image i = CACHE.get(name);
        if (i == null) {
            String path = "/" + name + (name.startsWith("tex_") ? ".jpg" : ".png");
            try {
                InputStream in = Art.class.getResourceAsStream(path);
                if (in == null) {
                    throw new IOException(path);
                }
                try {
                    i = Image.createImage(in);
                } finally {
                    in.close();
                }
            } catch (IOException missing) {
                // A missing image is a packaging mistake; draw a dot rather than die.
                i = Image.createImage(4, 4, 0xffff00ff);
            }
            CACHE.put(name, i);
        }
        return i;
    }

    /// The creature image for a kind.
    public static String enemyImage(Enemy e) {
        switch (e.kind) {
            case Enemy.DART:
                return "e_dart";
            case Enemy.PINWHEEL:
                return "e_pinwheel";
            case Enemy.CRANE:
                return "e_crane";
            case Enemy.CRANELET:
                return "e_cranelet";
            case Enemy.SNIPPER:
                return e.u != 0 || e.t > 0.9f ? "e_snipper_shut" : "e_snipper_open";
            case Enemy.INKWELL:
                return "e_inkwell";
            default:
                return "fx_px";
        }
    }

    /// Gochi Hand, for numbers and titles, at `mm` millimetres.
    public static Font title(float mm) {
        return font("GochiHand-Regular.ttf", "Gochi Hand", mm);
    }

    /// Patrick Hand, for everything else.
    public static Font body(float mm) {
        return font("PatrickHand-Regular.ttf", "Patrick Hand", mm);
    }

    private static Font font(String file, String name, float mm) {
        if (Font.isTrueTypeFileSupported()) {
            Font f = Font.createTrueTypeFont(name, file);
            if (f != null) {
                return f.derive(com.codename1.ui.CN.convertToPixels(mm), Font.STYLE_PLAIN);
            }
        }
        return Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE);
    }
}
