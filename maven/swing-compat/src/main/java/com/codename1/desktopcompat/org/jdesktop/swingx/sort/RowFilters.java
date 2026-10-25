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

import com.codename1.desktopcompat.javax.swing.RowFilter;

/// Row filter building blocks. The regular expression filters are
/// absent.
public class RowFilters {

    RowFilters() {
    }

    /// A filter that includes a row when one of a set of its values
    /// passes [#include(RowFilter.Entry, int)]: the values of the
    /// columns given, or every value when none is given.
    public abstract static class GeneralFilter extends RowFilter<Object, Object> {

        private final int[] columns;

        protected GeneralFilter(int... columns) {
            checkIndices(columns);
            int[] copy = new int[columns == null ? 0 : columns.length];
            for (int i = 0; i < copy.length; i++) {
                copy[i] = columns[i];
            }
            this.columns = copy;
        }

        @Override
        public boolean include(RowFilter.Entry<? extends Object, ? extends Object> value) {
            int count = value.getValueCount();
            if (columns.length > 0) {
                for (int i = columns.length - 1; i >= 0; i--) {
                    int index = columns[i];
                    if (index < count && include(value, index)) {
                        return true;
                    }
                }
                return false;
            }
            for (int i = count - 1; i >= 0; i--) {
                if (include(value, i)) {
                    return true;
                }
            }
            return false;
        }

        protected abstract boolean include(RowFilter.Entry<? extends Object, ? extends Object> value, int index);

        /// Rejects a negative column.
        protected void checkIndices(int[] columns) {
            if (columns == null) {
                return;
            }
            for (int i = columns.length - 1; i >= 0; i--) {
                if (columns[i] < 0) {
                    throw new IllegalArgumentException("Index must be >= 0");
                }
            }
        }
    }
}
