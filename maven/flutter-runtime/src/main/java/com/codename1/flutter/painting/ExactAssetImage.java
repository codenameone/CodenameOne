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
package com.codename1.flutter.painting;

import com.codename1.flutter.ImageProvider;

/**
 * An {@link ImageProvider} that loads a bundled asset at an exact device-pixel
 * scale — Flutter's {@code ExactAssetImage}. Unlike {@code AssetImage} it does
 * not pick a resolution-appropriate variant; the named {@code scale} identifies
 * the density the asset was authored for. The product thumbnails use it to load
 * the pre-scaled catalog images.
 */
public class ExactAssetImage extends ImageProvider {

    private final String assetName;
    private double scale = 1.0;
    private String packageName;
    private Object bundle;

    public ExactAssetImage(String assetName) {
        this.assetName = assetName;
    }

    public void scale(double v) {
        this.scale = v;
    }

    /**
     * Named parameter setter for the Dart {@code package:} parameter. The
     * transpiler escapes the reserved word {@code package} to {@code package_}.
     */
    public void package_(String v) {
        this.packageName = v;
    }

    public void bundle(Object v) {
        this.bundle = v;
    }

    public String getAssetName() {
        return assetName;
    }

    public double getScale() {
        return scale;
    }

    public String getPackage() {
        return packageName;
    }

    /**
     * The classpath-relative asset path, honoring the optional package
     * qualifier ({@code packages/<package>/<name>}).
     */
    public String resolvedName() {
        if (packageName != null && !assetName.startsWith("packages/")) {
            return "packages/" + packageName + "/" + assetName;
        }
        return assetName;
    }

    @Override
    public String sourceKey() {
        return "asset:" + resolvedName() + "@" + scale;
    }
}
