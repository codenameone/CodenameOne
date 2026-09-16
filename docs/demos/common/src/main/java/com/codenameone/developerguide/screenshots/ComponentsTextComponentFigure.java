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

import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;

/// Text fields with the constraints the chapter describes.
class ComponentsTextComponentFigure implements GuideFigure {

    @Override
    public String id() {
        return "components-text-component";
    }

    /// The tagged region is what the chapter includes. The section had no
    /// listing at all -- the sentence promising one was followed straight by the
    /// picture -- so this is the sample it was describing.
    @Override
    public Form build() {
        // tag::the-components-of-codename-one-text-component[]
        Form hi = new Form("Text", BoxLayout.y());
        TextField plain = new TextField("", "Any text");
        TextField email = new TextField("", "E-Mail", 20, TextField.EMAILADDR);
        TextField url = new TextField("", "URL", 20, TextField.URL);
        TextField phone = new TextField("", "Phone", 20, TextField.PHONENUMBER);
        TextField number = new TextField("", "Number", 20, TextField.NUMERIC);
        TextArea notes = new TextArea(4, 20);
        notes.setHint("Notes");
        hi.addAll(plain, email, url, phone, number, notes);
        hi.show();
        // end::the-components-of-codename-one-text-component[]
        return hi;
    }
}
