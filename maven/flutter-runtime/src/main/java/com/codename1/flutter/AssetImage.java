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
package com.codename1.flutter;

/**
 * An {@link ImageProvider} that loads a bundled asset — Flutter's
 * {@code AssetImage}. The optional {@code package} qualifies the asset's
 * owning package ({@code packages/<package>/<name>}), matching Flutter's
 * asset resolution.
 */
public class AssetImage extends ImageProvider {

    private final String assetName;
    private String packageName;
    private Object bundle;

    public AssetImage(String assetName) {
        this.assetName = assetName;
    }

    /**
     * Named parameter setter for the Dart {@code package:} parameter. The
     * transpiler escapes the reserved word {@code package} to {@code package_}.
     */
    public void package_(String v) {
        this.packageName = v;
    }

    /** Named parameter setter for the Dart {@code bundle:} parameter. */
    public void bundle(Object v) {
        this.bundle = v;
    }

    public String getAssetName() {
        return assetName;
    }

    public String getPackage() {
        return packageName;
    }

    /**
     * The classpath-relative asset path, honoring the optional package
     * qualifier ({@code packages/<package>/<name>}).
     */
    public String resolvedName() {
        return qualify(assetName, packageName);
    }

    /**
     * Prefixes an asset name with its owning package, Flutter's
     * {@code packages/<package>/<name>}. Shared so every way of naming an asset —
     * {@code AssetImage}, {@code Image.asset} — resolves to the same file.
     */
    public static String qualify(String assetName, String packageName) {
        if (assetName != null && packageName != null && !assetName.startsWith("packages/")) {
            return "packages/" + packageName + "/" + assetName;
        }
        return assetName;
    }

    @Override
    public String sourceKey() {
        return "asset:" + resolvedName();
    }
}
