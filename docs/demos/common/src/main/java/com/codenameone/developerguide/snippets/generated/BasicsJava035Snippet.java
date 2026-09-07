/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

package com.codenameone.developerguide.snippets.generated;

import com.codename1.ui.*;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.layouts.mig.MigLayout;

public class BasicsJava035Snippet {
    // tag::basics-java-035[]
    public static Form createForm() {
        Form hi = new Form("MigLayout",
                new MigLayout("wrap 2, insets 4mm", "[right]3mm[32mm]", "[]12[]12[]12[]"));
        hi.add(new Label("First name"));
        hi.add("growx, w 32mm", new TextField("", "First name"));
        hi.add(new Label("Last name"));
        hi.add("growx, w 32mm", new TextField("", "Last name"));
        hi.add(new Label("Phone"));
        hi.add("growx, w 32mm", new TextField("", "Phone"));
        Button ok = new Button("OK");
        ok.setCapsText(false);
        Style okStyle = ok.getAllStyles();
        okStyle.setBgColor(0xf4f8ff);
        okStyle.setBgTransparency(255);
        okStyle.setFgColor(0x0d47a1);
        okStyle.setBorder(Border.createLineBorder(1, 0x2b5c9e));
        okStyle.setPaddingUnit(Style.UNIT_TYPE_DIPS);
        okStyle.setPadding(2, 2, 2, 2);
        hi.add("span 2, growx", FlowLayout.encloseCenter(ok));
        return hi;
    }
    // end::basics-java-035[]

    void snippet() {
        createForm().show();
    }
}
