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
package com.codenameone.developerguide.screenshots;

import com.codename1.components.Chip;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;

/// Selectable, removable and plain chips, for the developer guide.
class ComponentsChipFigure implements GuideFigure {

    @Override
    public String id() {
        return "components-chip";
    }

    @Override
    public Form build() {
        Form hi = new Form("Chips", BoxLayout.y());
        Container row = new Container(new FlowLayout());
        row.add(new Chip("All").setSelectable(true).setSelected(true));
        row.add(new Chip("Music").setSelectable(true));
        row.add(new Chip("Podcasts").setSelectable(true));
        row.add(new Chip("Kotlin").setRemovable(true));
        row.add(new Chip("Java").setRemovable(true));
        hi.add(row);
        hi.show();
        return hi;
    }
}
