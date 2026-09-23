/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.flutter.widgets;

import com.codename1.flutter.BoxFit;
import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Element;
import com.codename1.flutter.ImageProvider;
import com.codename1.flutter.Key;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * An image, created via Dart's {@code Image.asset} (bundled under the app's
 * {@code /assets} resources) or {@code Image.network} named constructors.
 * Backed by a CN1 Label (UIID "FlutterImage") carrying an EncodedImage or a
 * URLImage.
 */
public class Image extends Widget {

    private String assetName;
    private String url;
    /// Request headers from a NetworkImage provider; null when there are none.
    private java.util.Map<?, ?> headers;
    /// The encoded picture a MemoryImage provider carries; null otherwise.
    private byte[] memoryBytes;
    /// The provider's own key for a memory image, so a new buffer reloads.
    private String memoryKey;
    /// Encoded pixels per logical pixel for a memory image: MemoryImage's scale.
    private double memoryScale = 1;
    private Double width;
    /// Flutter's decode-size hints. cacheWidth/cacheHeight decode the asset at
    /// that many DEVICE pixels, so they bound the picture's intrinsic size --
    /// a 200px decode is 200 device pixels wide however large the file is.
    ///
    /// They were accepted and dropped, so an image asking to be decoded small
    /// took its full intrinsic size instead and laid out several times too
    /// large wherever the box did not pin it.
    Long cacheWidth;
    Long cacheHeight;

    private Double height;
    private BoxFit fit;
    private ImageProvider imageProvider;
    private Funcs.Func4<BuildContext, Widget, Long, Boolean, Widget> frameBuilder;

    private Image(String assetName, String url) {
        this.assetName = assetName;
        this.url = url;
    }

    /** Dart's unnamed {@code Image(image: ...)} constructor, in allocate-then-setters form. */
    public Image() {
    }

    /**
     * {@code Image(image: provider)}: resolves the provider's source into this widget's
     * asset/url identity so the render path is unchanged.
     */
    public void image(ImageProvider v) {
        this.imageProvider = v;
        // Providers WRAP: ResizeImage(AssetImage(...)) is the ordinary way to
        // ask for a thumbnail, and its own source key reads
        // "resize:80x80:asset:...". Matching the prefix on the outermost key
        // therefore found no asset at all, so Crane's destination photographs
        // never loaded -- and, because nothing had failed, nothing was
        // reported either.
        ImageProvider inner = v;
        for (int depth = 0; inner instanceof com.codename1.flutter.ResizeImage && depth < 8; depth++) {
            com.codename1.flutter.ResizeImage rz = (com.codename1.flutter.ResizeImage) inner;
            // A ResizeImage is a DECODE instruction, in raw pixels: "give me
            // this picture at 80x80 and never hold it larger". Discarding it
            // meant a list of thumbnails each kept a full-resolution decode --
            // for the gallery's 3.0x artwork that is roughly a thousand times
            // the pixels the screen ever shows. The innermost wrapper wins,
            // as it does in Flutter.
            if (rz.getWidth() != null) {
                resizeWidthPx = rz.getWidth();
            }
            if (rz.getHeight() != null) {
                resizeHeightPx = rz.getHeight();
            }
            inner = rz.getImageProvider();
        }
        if (inner != null) {
            String key = inner.sourceKey();
            if (key != null && key.startsWith("asset:")) {
                this.assetName = key.substring("asset:".length());
            } else if (key != null && key.startsWith("url:")) {
                this.url = key.substring("url:".length());
                // The headers go with the URL. Keeping only the URL sent every
                // request bare, so an image behind an Authorization header was
                // refused on every load and never drawn.
                if (inner instanceof com.codename1.flutter.NetworkImage) {
                    Object h = ((com.codename1.flutter.NetworkImage) inner).getHeaders();
                    this.headers = h instanceof java.util.Map && !((java.util.Map<?, ?>) h).isEmpty()
                            ? (java.util.Map<?, ?>) h : null;
                }
            } else if (inner instanceof com.codename1.flutter.MemoryImage) {
                // The bytes are already here, so there is nothing to resolve: the
                // render element decodes them directly. This branch used to report
                // the provider as unimplemented and draw nothing.
                dart.typed_data.Uint8List data = ((com.codename1.flutter.MemoryImage) inner).getBytes();
                this.memoryBytes = data == null ? null : data.toBytes();
                this.memoryKey = key;
                // Kept, not dropped: MemoryImage(bytes, scale: 2.0) says two encoded
                // pixels make one logical one. Without it the render element sized
                // the picture as if each were one logical pixel -- twice as large
                // as asked, with every fit computed against the wrong size.
                double scale = ((com.codename1.flutter.MemoryImage) inner).getScale();
                this.memoryScale = scale > 0 ? scale : 1;
            } else {
                com.codename1.flutter.FlutterErrorReport.unimplemented("ImageProvider",
                        inner.getClass().getName() + " resolves to no asset or URL ("
                        + key + "), so nothing will be drawn");
            }
        }
    }

    public void width(Double v) {
        this.width = v;
    }

    public void height(Double v) {
        this.height = v;
    }

    public void fit(Object v) {
        this.fit = (v instanceof BoxFit) ? (BoxFit) v : null;
    }

    public void excludeFromSemantics(boolean v) {
    }

    public void frameBuilder(Funcs.Func4<BuildContext, Widget, Long, Boolean, Widget> v) {
        this.frameBuilder = v;
    }

    /**
     * Dart's {@code Image.asset} named constructor in canonical positional
     * form.
     */
    public static Image asset(String name, Key key, Double width, Double height, BoxFit fit,
                              String packageName, Boolean excludeFromSemantics,
                              Boolean gaplessPlayback, Long cacheWidth, Long cacheHeight,
                              com.codename1.flutter.Color color, Object colorBlendMode,
                              Object alignment, String semanticLabel) {
        // The package qualifier is part of the PATH, exactly as in AssetImage: an asset
        // shipped by a package lives at packages/<package>/<name>. Dropping it - which is
        // what happened while this factory had no such parameter - leaves the image looking
        // for a file that is not there, and the widget renders nothing with no error.
        Image i = new Image(com.codename1.flutter.AssetImage.qualify(name, packageName), null);
        i.key(key);
        i.width = width;
        i.height = height;
        i.fit = fit;
        i.cacheWidth = cacheWidth;
        i.cacheHeight = cacheHeight;
        return i;
    }

    /**
     * Dart's {@code Image.network} named constructor in canonical positional
     * form.
     */
    public static Image network(String src, Key key, Double width, Double height, BoxFit fit) {
        Image i = new Image(null, src);
        i.key(key);
        i.width = width;
        i.height = height;
        i.fit = fit;
        return i;
    }

    /// The decode size a ResizeImage asked for, in raw pixels, or null.
    private Long resizeWidthPx;
    private Long resizeHeightPx;

    /** The width a {@code ResizeImage} asked this picture to be decoded at, or null. */
    public Long getResizeWidthPx() {
        return resizeWidthPx;
    }

    /** The height a {@code ResizeImage} asked this picture to be decoded at, or null. */
    public Long getResizeHeightPx() {
        return resizeHeightPx;
    }

    public String getAssetName() {
        return assetName;
    }

    public String getUrl() {
        return url;
    }

    public Double getWidth() {
        return width;
    }

    public Double getHeight() {
        return height;
    }

    public BoxFit getFit() {
        return fit;
    }

    /**
     * A stable identity for the image source, used to detect source changes
     * across in-place widget updates.
     */
    public String sourceKey() {
        if (assetName != null) {
            return "asset:" + assetName;
        }
        if (memoryBytes != null) {
            return memoryKey;
        }
        // The headers are part of what was asked for: a refreshed token must
        // fetch again rather than be answered by the load made with the old one.
        return headers == null ? "url:" + url : "url:" + url + "#h" + headers.hashCode();
    }

    /// Request headers for a network image, or null.
    public java.util.Map<?, ?> getHeaders() {
        return headers;
    }

    /// The encoded bytes of a memory image, or null.
    public byte[] getMemoryBytes() {
        return memoryBytes;
    }

    /// Encoded pixels per logical pixel for a memory image (1 unless given).
    public double getMemoryScale() {
        return memoryScale;
    }

    @Override
    public Element createElement() {
        return new ImageRenderElement(this);
    }
}
