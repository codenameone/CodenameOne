/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui.html;

    /**
     * CellConstraint is very similar to com.codename1.ui.table.TableLayout.Constraint in the sense it holds about the same data.
     * It is used to store the constraints of each cell, and allows modifying and readind them - then, when the actual table is drawn
     * They are converted to TableLayout.Constraint in HTMLTable.createCellConstraint.
     *
     * This is needed because:
     * 1. Once a TableLayout.Constraint is used to draw a table it is assigned a parent and can't be reused again (See TableLayout.addLayoutComponent)
     * 2. TableLayout.Constraint does not allow reading its values, so cloning the constraint (without the parent) is impossible
     *
     * @author Ofir Leitner
     */
    class CellConstraint {
        int width = -1;//defaultColumnWidth;
        int height = -1;//defaultRowHeight;
        int spanHorizontal = 1;
        int spanVertical = 1;
        int align = -1;
        int valign = -1;

        /**
         * Sets the cells to span vertically, this number must never be smaller than 1
         *
         * @param span a number larger than 1
         */
        public void setVerticalSpan(int span) {
            if(span < 1) {
                throw new IllegalArgumentException("Illegal span");
            }
            spanVertical = span;
        }

        /**
         * Sets the cells to span horizontally, this number must never be smaller than 1
         *
         * @param span a number larger than 1
         */
        public void setHorizontalSpan(int span) {
            if(span < 1) {
                throw new IllegalArgumentException("Illegal span");
            }
            spanHorizontal = span;
        }

        /**
         * Sets the column width based on percentage of the parent
         *
         * @param width negative number indicates ignoring this member
         */
        public void setWidthPercentage(int width) {
            this.width = width;
        }

        /**
         * Sets the row height based on percentage of the parent
         *
         * @param height negative number indicates ignoring this member
         */
        public void setHeightPercentage(int height) {
            this.height = height;
        }

        /**
         * Sets the horizontal alignment of the table cell
         *
         * @param align Component.LEFT/RIGHT/CENTER
         */
        public void setHorizontalAlign(int align) {
            this.align = align;
        }

        /**
         * Sets the vertical alignment of the table cell
         *
         * @param valign Component.TOP/BOTTOM/CENTER
         */
        public void setVerticalAlign(int valign) {
            this.valign = valign;
        }
    }

