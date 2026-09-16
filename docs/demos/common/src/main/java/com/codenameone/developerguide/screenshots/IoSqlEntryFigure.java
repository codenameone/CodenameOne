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

import com.codename1.ui.Button;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

/// The query dialog the SQL explorer opens.
class IoSqlEntryFigure implements GuideFigure {

    @Override
    public String id() {
        return "sql-entry";
    }

    /// The explorer form the chapter builds; its listing is included once, a few
    /// lines above the picture.
    @Override
    public Form build() {
        Toolbar.setGlobalToolbar(true);
        Style s = UIManager.getInstance().getComponentStyle("TitleCommand");
        FontImage icon = FontImage.createMaterial(FontImage.MATERIAL_QUERY_BUILDER, s);
        Form hi = new Form("SQL Explorer", new BorderLayout());
        hi.getToolbar().addCommandToRightBar("", icon, e -> {
        });
        hi.show();
        return hi;
    }

    /// The listing reaches the dialog through Dialog.show, which is modal and
    /// never returns while nothing is there to dismiss it, so the figure builds
    /// the same dialog and shows it modelessly. The query is typed in, because
    /// an empty box is not what "issuing a query" looks like.
    @Override
    public void afterShow(Form form) {
        TextArea query = new TextArea(3, 80);
        query.setText("select * from Person");
        Dialog d = new Dialog("Query");
        d.setLayout(new BorderLayout());
        d.add(BorderLayout.CENTER, query);
        d.add(BorderLayout.SOUTH, com.codename1.ui.layouts.FlowLayout.encloseCenter(
                new Button("Execute"), new Button("Cancel")));
        d.showModeless();
    }
}
