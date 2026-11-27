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

import com.codename1.components.ExpandableText;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;

/// A collapsed paragraph with a More link, for the developer guide.
class ComponentsExpandableTextFigure implements GuideFigure {

    @Override
    public String id() {
        return "components-expandable-text";
    }

    @Override
    public Form build() {
        Form hi = new Form("Review", BoxLayout.y());
        hi.add(new ExpandableText("The battery lasts a full day of heavy use, the screen is "
                + "bright enough to read outdoors and the camera does well in low light. "
                + "The only real complaint is the weight: after an hour of reading it gets "
                + "tiring to hold, and the case adds even more.", 2));
        hi.show();
        return hi;
    }
}
