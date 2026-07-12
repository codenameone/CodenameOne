/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codenameone.fidelity.spec;

import java.util.ArrayList;
import java.util.List;

/**
 * Parsed fidelity-tests.yaml: the global defaults plus the component list.
 */
public class FidelitySpec {
    private int defaultTileWidthMm = 60;
    private int defaultTileHeightMm = 14;
    private String backgroundHex = "ffffff";
    private List appearances = new ArrayList();
    private List components = new ArrayList();

    public int getDefaultTileWidthMm() {
        return defaultTileWidthMm;
    }

    public void setDefaultTileWidthMm(int defaultTileWidthMm) {
        this.defaultTileWidthMm = defaultTileWidthMm;
    }

    public int getDefaultTileHeightMm() {
        return defaultTileHeightMm;
    }

    public void setDefaultTileHeightMm(int defaultTileHeightMm) {
        this.defaultTileHeightMm = defaultTileHeightMm;
    }

    public String getBackgroundHex() {
        return backgroundHex;
    }

    public void setBackgroundHex(String backgroundHex) {
        this.backgroundHex = backgroundHex;
    }

    public List getAppearances() {
        return appearances;
    }

    public void setAppearances(List appearances) {
        this.appearances = appearances;
    }

    public List getComponents() {
        return components;
    }

    public void setComponents(List components) {
        this.components = components;
    }

    /** Effective tile width for a component, honouring its per-component override. */
    public int tileWidthMm(ComponentSpec component) {
        return component.getTileWidthMm() > 0 ? component.getTileWidthMm() : defaultTileWidthMm;
    }

    /** Effective tile height for a component, honouring its per-component override. */
    public int tileHeightMm(ComponentSpec component) {
        return component.getTileHeightMm() > 0 ? component.getTileHeightMm() : defaultTileHeightMm;
    }
}
