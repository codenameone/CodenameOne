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
package com.codename1.desktopcompat.org.jdesktop.swingx.decorator;

import com.codename1.desktopcompat.java.awt.Component;

/// Enables or disables the cell's component, which grays out a cell.
public class EnabledHighlighter extends AbstractHighlighter {

    private boolean enabled;

    public EnabledHighlighter() {
        this(null);
    }

    public EnabledHighlighter(boolean enabled) {
        this(null, enabled);
    }

    public EnabledHighlighter(HighlightPredicate predicate) {
        this(predicate, false);
    }

    public EnabledHighlighter(HighlightPredicate predicate, boolean enabled) {
        super(predicate);
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        fireStateChanged();
    }

    @Override
    protected Component doHighlight(Component renderer, ComponentAdapter adapter) {
        renderer.setEnabled(enabled);
        return renderer;
    }
}
