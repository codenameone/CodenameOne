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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.maps.LatLng;
import com.codename1.maps.MapView;
import com.codename1.maps.vector.BundledTileSource;
import com.codename1.maps.vector.MapStyle;
import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

/// Renders the bundled real San Francisco OSM tiles with the built-in dark
/// style, exercising the style engine (background + per-layer colors) on real
/// data so it is distinct from the light basemap.
public class VectorMapDarkStyleScreenshotTest extends VectorMapScreenshotBaseTest {
    // cn1ss-skip: watchos -- captures nothing on these ports (the guard below);
    // scripts/ci/cn1ss-golden-plan.py expects a golden everywhere else.

    @Override
    public boolean runTest() {
        if (CN.isWatch()) {
            // No committed watch golden; phone/tablet form factors cover this.
            System.out.println(
                    "CN1SS:INFO:test=VectorMapDarkStyle status=SKIPPED reason=watch-form-factor");
            done();
            return true;
        }
        Form form = createForm("Vector Map Dark", new BorderLayout(), "VectorMapDarkStyle");
        MapView map = new MapView(
                new BundledTileSource("/maptiles/mt_{z}_{x}_{y}.mvt", true, 13, 13).setAttribution("(c) OSM"),
                MapStyle.dark());
        map.moveCamera(new LatLng(37.808, -122.412), 13);
        form.add(BorderLayout.CENTER, map);
        mapUnderTest = map;
        form.show();
        return true;
    }
}
