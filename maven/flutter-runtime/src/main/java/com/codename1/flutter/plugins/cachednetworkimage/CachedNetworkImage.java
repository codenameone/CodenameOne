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
package com.codename1.flutter.plugins.cachednetworkimage;

import com.codename1.flutter.BoxFit;
import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Image;

import dart.runtime.Funcs;

/**
 * package:cached_network_image's {@code CachedNetworkImage}. {@code Image.network}
 * already caches what it downloads, so this is that widget under the plugin's
 * parameter names. The placeholder and error builders are accepted; the network
 * image shows its own placeholder while loading.
 */
public class CachedNetworkImage extends StatelessWidget {
    private String imageUrl;
    private Double width;
    private Double height;
    private BoxFit fit;

    public CachedNetworkImage() {
    }

    public void imageUrl(String v) {
        this.imageUrl = v;
    }

    public void width(Double v) {
        this.width = v;
    }

    public void height(Double v) {
        this.height = v;
    }

    public void fit(BoxFit v) {
        this.fit = v;
    }

    /** Accepted for source compatibility; see the class comment. */
    public void placeholder(Funcs.Func2<BuildContext, String, Widget> v) {
    }

    /** Accepted for source compatibility; see the class comment. */
    public void errorWidget(Funcs.Func3<BuildContext, String, Object, Widget> v) {
    }

    /** Accepted for source compatibility. */
    public void httpHeaders(Object v) {
    }

    /** Accepted for source compatibility. */
    public void memCacheWidth(Object v) {
    }

    /** Accepted for source compatibility. */
    public void memCacheHeight(Object v) {
    }

    /** Accepted for source compatibility. */
    public void fadeInDuration(Object v) {
    }

    /** Accepted for source compatibility. */
    public void cacheKey(String v) {
    }

    @Override
    public Widget build(BuildContext context) {
        return Image.network(imageUrl == null ? "" : imageUrl, null, width, height, fit);
    }
}
