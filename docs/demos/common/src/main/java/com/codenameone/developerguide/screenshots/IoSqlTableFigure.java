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

import com.codename1.db.Cursor;
import com.codename1.db.Database;
import com.codename1.db.Row;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.table.DefaultTableModel;
import com.codename1.ui.table.Table;

import java.io.IOException;
import java.util.ArrayList;

/// The SQL explorer showing the rows a query returned.
class IoSqlTableFigure implements GuideFigure {

    private static final String DB = "GuideFigureDemo.db";

    private Form form;

    @Override
    public String id() {
        return "sql-table";
    }

    /// The explorer form the chapter builds; its listing is included once, a few
    /// lines above the picture.
    @Override
    public Form build() {
        seed();
        Toolbar.setGlobalToolbar(true);
        Style s = UIManager.getInstance().getComponentStyle("TitleCommand");
        FontImage icon = FontImage.createMaterial(FontImage.MATERIAL_QUERY_BUILDER, s);
        Form hi = new Form("SQL Explorer", new BorderLayout());
        hi.getToolbar().addCommandToRightBar("", icon, e -> {
        });
        hi.show();
        form = hi;
        return hi;
    }

    /// A figure has to bring its own database. The chapter's sample queries
    /// whatever the SQLDemo application left behind, which is nothing at all on
    /// a machine that has never run it -- so the table would be empty here and
    /// on the runner both.
    private static void seed() {
        Database db = null;
        try {
            db = Display.getInstance().openOrCreate(DB);
            db.execute("drop table if exists Person");
            db.execute("create table Person (name text, house text, seat text)");
            db.execute("insert into Person values ('Eddard', 'Stark', 'Winterfell')");
            db.execute("insert into Person values ('Tyrion', 'Lannister', 'Casterly Rock')");
            db.execute("insert into Person values ('Daenerys', 'Targaryen', 'Dragonstone')");
            db.execute("insert into Person values ('Jon', 'Snow', 'Castle Black')");
        } catch (IOException err) {
            Log.e(err);
        } finally {
            Util.cleanup(db);
        }
    }

    /// The chapter reaches this through Dialog.show, which is modal and never
    /// returns while nothing is there to dismiss it, so the figure runs the
    /// query the dialog would have carried and builds the same table from the
    /// same cursor calls.
    @Override
    public void afterShow(Form f) {
        Database db = null;
        Cursor cur = null;
        try {
            db = Display.getInstance().openOrCreate(DB);
            cur = db.executeQuery("select * from Person");
            int columns = cur.getColumnCount();
            String[] columnNames = new String[columns];
            for (int iter = 0; iter < columns; iter++) {
                columnNames[iter] = cur.getColumnName(iter);
            }
            ArrayList<String[]> data = new ArrayList<>();
            boolean next = cur.next();
            while (next) {
                Row currentRow = cur.getRow();
                String[] currentRowArray = new String[columns];
                for (int iter = 0; iter < columns; iter++) {
                    currentRowArray[iter] = currentRow.getString(iter);
                }
                data.add(currentRowArray);
                next = cur.next();
            }
            Object[][] arr = new Object[data.size()][];
            data.toArray(arr);
            form.removeAll();
            form.add(BorderLayout.CENTER, new Table(new DefaultTableModel(columnNames, arr)));
            form.revalidate();
        } catch (IOException err) {
            Log.e(err);
        } finally {
            Util.cleanup(cur);
            Util.cleanup(db);
        }
    }
}
