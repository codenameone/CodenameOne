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
package com.codename1.desktopcompat.javax.swing;

import java.util.Arrays;
import java.util.List;

/// A spinner model over the elements of a list, which the model keeps and
/// does not copy. The sequence ends at both ends of the list.
public class SpinnerListModel extends AbstractSpinnerModel {

    private List<?> list;
    private int index;

    public SpinnerListModel(List<?> values) {
        if (values == null || values.size() == 0) {
            throw new IllegalArgumentException("SpinnerListModel(List) expects non-null non-empty List");
        }
        list = values;
    }

    public SpinnerListModel(Object[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("SpinnerListModel(Object[]) expects non-null non-empty Object[]");
        }
        list = Arrays.asList(values);
    }

    public SpinnerListModel() {
        this(new Object[]{"empty"});
    }

    public List<?> getList() {
        return list;
    }

    public void setList(List<?> list) {
        if (list == null || list.size() == 0) {
            throw new IllegalArgumentException("invalid list");
        }
        if (!list.equals(this.list)) {
            this.list = list;
            index = 0;
            fireStateChanged();
        }
    }

    @Override
    public Object getValue() {
        return list.get(index);
    }

    /// Makes `elt` the current value; an object that is not in the list is
    /// refused.
    @Override
    public void setValue(Object elt) {
        int at = list.indexOf(elt);
        if (at == -1) {
            throw new IllegalArgumentException("invalid sequence element");
        }
        if (at != index) {
            index = at;
            fireStateChanged();
        }
    }

    @Override
    public Object getNextValue() {
        return index >= list.size() - 1 ? null : list.get(index + 1);
    }

    @Override
    public Object getPreviousValue() {
        return index <= 0 ? null : list.get(index - 1);
    }
}
