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

import com.codename1.components.*;
import com.codename1.ui.*;
import com.codename1.ui.table.TableLayout;

class BasicsJava022Snippet {
    void snippet() {
        // tag::basics-java-022[]
        TableLayout layout = new TableLayout(4, 3);
        layout.setGrowHorizontally(true);
        Form hi = new Form("Table Layout", layout);

        TableLayout.Constraint title = layout.createConstraint();
        title.setHorizontalSpan(3);
        title.setHorizontalAlign(Component.CENTER);
        hi.add(title, new Label("Invoice"));

        hi.add(new Label("Item"));
        hi.add(new Label("Qty"));
        hi.add(new Label("Total"));
        hi.add(new Label("Design"));
        hi.add(new Label("2"));
        hi.add(new Label("$120"));

        TableLayout.Constraint notes = layout.createConstraint();
        notes.setHorizontalSpan(2);
        notes.setHeightPercentage(40);
        hi.add(notes, new SpanLabel("Notes span two columns"));
        hi.add(new Button("Pay"));

        hi.show();
        // end::basics-java-022[]
    }
}
