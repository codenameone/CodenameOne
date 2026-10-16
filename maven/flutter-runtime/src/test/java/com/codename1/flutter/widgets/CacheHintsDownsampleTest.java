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

import com.codename1.flutter.testsupport.RasterDisplay;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@code Image.asset}'s own {@code cacheWidth}/{@code cacheHeight} (stored directly on
 * the widget, distinct from a wrapping {@code ResizeImage}'s width/height) must bound the
 * RETAINED decoded bitmap, exactly as a {@code ResizeImage} does -- Flutter's own
 * {@code Image.asset} constructor wraps its provider in
 * {@code ResizeImage.resizeIfNeeded(cacheWidth, cacheHeight, provider)}, so the two are
 * the same hint under two different call shapes.
 *
 * <p>{@link ImageRenderElement#downsample} used to consult only
 * {@code Image.getResizeWidthPx/Height} (the {@code ResizeImage} path) and ignore
 * {@code cacheWidth}/{@code cacheHeight} entirely, so {@code Image.asset(..., cacheWidth:
 * w, cacheHeight: h)} decoded and KEPT the full-resolution bitmap even though layout
 * already shrank the box to the hint -- the memory blow-up {@code ResizeImage} exists to
 * prevent. Separately, {@code Image.sourceKey()} never folded either hint into the
 * identity {@link ImageRenderElement#loadImage} uses to decide whether to reload, so a
 * rebuild that only changed the hint on the SAME asset was treated as no change at all.</p>
 */
class CacheHintsDownsampleTest {

    @BeforeEach
    void install() {
        RasterDisplay.install();
    }

    @AfterEach
    void uninstall() {
        RasterDisplay.uninstall();
    }

    private static Image assetWithCacheHints(Long cacheWidth, Long cacheHeight) {
        return Image.asset("pic.png", null, null, null, null, null, null, null,
                cacheWidth, cacheHeight, null, null, null, null);
    }

    @Test
    void assetCacheHintsShrinkTheRetainedBitmapJustLikeResizeImage() {
        ImageRenderElement e = new ImageRenderElement(assetWithCacheHints(100L, 50L));
        com.codename1.ui.Image full = com.codename1.ui.Image.createImage(400, 400, 0xffffffff);

        com.codename1.ui.Image down = e.downsampleForTest(full);

        assertEquals(100, down.getWidth(),
                "cacheWidth must bound the DECODED bitmap, not just the layout box");
        assertEquals(50, down.getHeight(), "cacheHeight likewise");
    }

    @Test
    void noCacheHintsLeavesTheFullBitmapRetained() {
        ImageRenderElement e = new ImageRenderElement(assetWithCacheHints(null, null));
        com.codename1.ui.Image full = com.codename1.ui.Image.createImage(400, 400, 0xffffffff);

        assertSame(full, e.downsampleForTest(full),
                "with neither hint set, the full-resolution decode is simply returned");
    }

    @Test
    void changingOnlyTheCacheHintsIsADifferentSourceKey() {
        Image small = assetWithCacheHints(80L, 80L);
        Image large = assetWithCacheHints(400L, 400L);
        Image none = assetWithCacheHints(null, null);

        assertNotEquals(small.sourceKey(), large.sourceKey(),
                "a rebuild that only changes the decode-size hint on the SAME asset must "
                        + "be treated as a new source, or loadImage's cache never re-decodes it");
        assertNotEquals(small.sourceKey(), none.sourceKey());
    }
}
