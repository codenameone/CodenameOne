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
package com.codename1.desktopcompat.org.jdesktop.swingx.sort;

import com.codename1.desktopcompat.javax.swing.ListModel;

/// The sort controller of a list: a model of one column, column 0.
public class ListSortController<M extends ListModel> extends DefaultSortController<M> {

    private M listModel;

    public ListSortController(M model) {
        super();
        setModel(model);
    }

    public void setModel(M model) {
        listModel = model;
        setModelWrapper(new Wrapper());
    }

    private final class Wrapper extends ModelWrapper<M, Integer> {

        @Override
        public M getModel() {
            return listModel;
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public int getRowCount() {
            return listModel == null ? 0 : listModel.getSize();
        }

        @Override
        public Object getValueAt(int row, int column) {
            return listModel.getElementAt(row);
        }

        @Override
        public String getStringValueAt(int row, int column) {
            String s = getStringValueProvider().getStringValue(row, column).getString(getValueAt(row, column));
            return s == null ? "" : s;
        }

        @Override
        public Integer getIdentifier(int row) {
            return Integer.valueOf(row);
        }
    }
}
