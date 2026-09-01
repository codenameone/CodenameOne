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
package com.example.notes.ui;

import com.codename1.ui.Button;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.example.notes.model.Note;
import com.example.notes.model.NoteStore;

/** A Form subclass, which is the case that needs a generated shim. */
public class NotesForm extends Form {
    private final NoteStore store = new NoteStore().seed();
    private final Label status = new Label("ready");

    public NotesForm() {
        super("Notes", BoxLayout.y());
        for (Note n : store.sorted()) {
            add(new Label(n.toString()));
        }
        Button b = new Button("count");
        b.addActionListener(e -> status.setText("notes=" + store.count()));
        add(b).add(status);
        b.pressed();
        b.released();
        System.out.println("REALAPP: sorted=" + store.sorted()
            + " status=" + status.getText());
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        System.out.println("REALAPP: initComponent reached interpreted override");
    }
}
