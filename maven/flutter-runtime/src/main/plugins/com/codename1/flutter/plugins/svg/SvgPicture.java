/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.svg;

import java.io.InputStream;

import com.codename1.flutter.BoxFit;
import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Element;
import com.codename1.flutter.FlutterAssets;
import com.codename1.flutter.Key;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.Size;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.Log;
import com.codename1.io.NetworkManager;
import com.codename1.io.Util;
import com.codename1.ui.Display;
import com.codename1.ui.Image;

import dart.runtime.Funcs;
import dart.typed_data.Uint8List;

/**
 * flutter_svg's SvgPicture over Codename One's {@link Image#createSVG}, which renders
 * where the port supports SVG ({@link Image#isSVGSupported}). Where it does not, and
 * headless, the picture is an empty box of the requested size: a pure-Java SVG
 * rasterizer is not part of the runtime. colorFilter, placeholderBuilder, fit and
 * alignment are accepted and not applied; the image is scaled to the box.
 */
public class SvgPicture extends Widget {
    private final int kind;
    private final Object source;
    Double width;
    Double height;
    String semanticsLabel;
    private byte[] data;

    static final int ASSET = 0;
    static final int NETWORK = 1;
    static final int STRING = 2;
    static final int FILE = 3;
    static final int MEMORY = 4;

    SvgPicture(int kind, Object source, Key key, Double width, Double height, String semanticsLabel) {
        this.kind = kind;
        this.source = source;
        this.width = width;
        this.height = height;
        this.semanticsLabel = semanticsLabel;
        if (key != null) {
            key(key);
        }
    }

    public static SvgPicture asset(String assetName, Key key, Double width, Double height, BoxFit fit, Object alignment, String pkg, ColorFilter colorFilter, String semanticsLabel, Funcs.Func1<BuildContext, Widget> placeholderBuilder, Boolean matchTextDirection, Boolean excludeFromSemantics, Object clipBehavior, Object theme) {
        return new SvgPicture(ASSET, assetName, key, width, height, semanticsLabel);
    }

    public static SvgPicture network(String url, Key key, Double width, Double height, BoxFit fit, Object alignment, Object headers, ColorFilter colorFilter, String semanticsLabel, Funcs.Func1<BuildContext, Widget> placeholderBuilder, Boolean matchTextDirection, Boolean excludeFromSemantics, Object clipBehavior, Object theme) {
        return new SvgPicture(NETWORK, url, key, width, height, semanticsLabel);
    }

    public static SvgPicture string(String string, Key key, Double width, Double height, BoxFit fit, Object alignment, ColorFilter colorFilter, String semanticsLabel, Funcs.Func1<BuildContext, Widget> placeholderBuilder, Boolean matchTextDirection, Boolean excludeFromSemantics, Object clipBehavior, Object theme) {
        return new SvgPicture(STRING, string, key, width, height, semanticsLabel);
    }

    public static SvgPicture file(dart.io.File file, Key key, Double width, Double height, BoxFit fit, Object alignment, ColorFilter colorFilter, String semanticsLabel, Funcs.Func1<BuildContext, Widget> placeholderBuilder, Boolean matchTextDirection, Boolean excludeFromSemantics, Object clipBehavior, Object theme) {
        return new SvgPicture(FILE, file == null ? null : file.path(), key, width, height, semanticsLabel);
    }

    public static SvgPicture memory(Uint8List bytes, Key key, Double width, Double height, BoxFit fit, Object alignment, ColorFilter colorFilter, String semanticsLabel, Funcs.Func1<BuildContext, Widget> placeholderBuilder, Boolean matchTextDirection, Boolean excludeFromSemantics, Object clipBehavior, Object theme) {
        return new SvgPicture(MEMORY, bytes == null ? null : bytes.toBytes(), key, width, height, semanticsLabel);
    }

    public Double width() {
        return width;
    }

    public Double height() {
        return height;
    }

    Size preferredLayoutSize() {
        double w = width == null ? (height == null ? 24 : height.doubleValue()) : width.doubleValue();
        double h = height == null ? w : height.doubleValue();
        return new Size(w, h);
    }

    /** The SVG document's bytes, or null when they cannot be read (logged). */
    byte[] bytes() {
        if (data != null || source == null) {
            return data;
        }
        try {
            if (kind == STRING) {
                data = ((String) source).getBytes("UTF-8");
            } else if (kind == MEMORY) {
                data = (byte[]) source;
            } else if (kind == ASSET) {
                FlutterAssets.Resolved r = FlutterAssets.open(SvgPicture.class, (String) source);
                InputStream in = r == null ? null : r.stream();
                if (in != null) {
                    try {
                        data = Util.readInputStream(in);
                    } finally {
                        Util.cleanup(in);
                    }
                }
            } else if (kind == FILE) {
                InputStream in = com.codename1.io.FileSystemStorage.getInstance().openInputStream((String) source);
                try {
                    data = Util.readInputStream(in);
                } finally {
                    Util.cleanup(in);
                }
            } else {
                ConnectionRequest req = new ConnectionRequest((String) source, false);
                NetworkManager.getInstance().addToQueueAndWait(req);
                data = req.getResponseData();
            }
        } catch (Exception e) {
            Log.e(e);
        }
        return data;
    }

    /** The rendered picture, or null where SVG is unsupported or headless. */
    Image image() {
        if (!Display.isInitialized() || !Image.isSVGSupported()) {
            return null;
        }
        byte[] b = bytes();
        if (b == null) {
            return null;
        }
        try {
            String base = kind == NETWORK || kind == FILE ? (String) source : "";
            return Image.createSVG(base, false, b);
        } catch (Exception e) {
            Log.e(e);
            return null;
        }
    }

    @Override
    public Element createElement() {
        return new SvgRenderElement(this);
    }
}
