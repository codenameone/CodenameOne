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
package com.codename1.desktopcompat.org.jdesktop.swingx.decorator;

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/// Decides whether a cell is to be highlighted.
///
/// The rollover predicates read the client property `swingx.rollover` of
/// the cell's component, a point of view column and view row that the
/// extended table, list and tree keep while a pointer hovers over them. A
/// touch screen has no hover, so they match nothing there.
///
/// `BIG_DECIMAL_NEGATIVE` is absent because a device has no `BigDecimal`;
/// the pattern and search predicates are absent because it has no
/// `java.util.regex`.
public interface HighlightPredicate {

    HighlightPredicate ALWAYS = new HighlightPredicate() {
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            return true;
        }
    };

    HighlightPredicate NEVER = new HighlightPredicate() {
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            return false;
        }
    };

    /// The row the pointer is over.
    HighlightPredicate ROLLOVER_ROW = new HighlightPredicate() {
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            Object p = adapter.getComponent().getClientProperty("swingx.rollover");
            return p instanceof Point && ((Point) p).y == adapter.row;
        }
    };

    /// The column the pointer is over.
    HighlightPredicate ROLLOVER_COLUMN = new HighlightPredicate() {
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            Object p = adapter.getComponent().getClientProperty("swingx.rollover");
            return p instanceof Point && ((Point) p).x == adapter.column;
        }
    };

    /// The cell the pointer is over.
    HighlightPredicate ROLLOVER_CELL = new HighlightPredicate() {
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            Object p = adapter.getComponent().getClientProperty("swingx.rollover");
            return p instanceof Point && ((Point) p).y == adapter.row && ((Point) p).x == adapter.column;
        }
    };

    HighlightPredicate EDITABLE = new HighlightPredicate() {
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            return adapter.isEditable();
        }
    };

    HighlightPredicate READ_ONLY = new HighlightPredicate() {
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            return !adapter.isEditable();
        }
    };

    HighlightPredicate IS_LEAF = new HighlightPredicate() {
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            return adapter.isLeaf();
        }
    };

    HighlightPredicate IS_FOLDER = new HighlightPredicate() {
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            return !adapter.isLeaf();
        }
    };

    HighlightPredicate IS_SELECTED = new HighlightPredicate() {
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            return adapter.isSelected();
        }
    };

    /// The cells whose renderer component wants more width than the cell
    /// has.
    HighlightPredicate IS_TEXT_TRUNCATED = new HighlightPredicate() {
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            if (adapter.getComponent() == null || renderer == null) {
                return false;
            }
            return renderer.getPreferredSize().width > adapter.getCellBounds().width;
        }
    };

    HighlightPredicate HAS_FOCUS = new HighlightPredicate() {
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            return adapter.hasFocus();
        }
    };

    /// The rows with an even view index: the first, the third, ...
    HighlightPredicate EVEN = new HighlightPredicate() {
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            return adapter.row % 2 == 0;
        }
    };

    /// The rows with an odd view index: the second, the fourth, ...
    HighlightPredicate ODD = new HighlightPredicate() {
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            return adapter.row % 2 != 0;
        }
    };

    /// The cells whose value is a number below zero.
    HighlightPredicate INTEGER_NEGATIVE = new HighlightPredicate() {
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            Object v = adapter.getValue();
            return v instanceof Number && ((Number) v).intValue() < 0;
        }
    };

    HighlightPredicate[] EMPTY_PREDICATE_ARRAY = new HighlightPredicate[0];

    Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    Integer[] EMPTY_INTEGER_ARRAY = new Integer[0];

    /// Whether the cell `adapter` describes, about to be drawn by
    /// `renderer`, is to be highlighted.
    boolean isHighlighted(Component renderer, ComponentAdapter adapter);

    /// Negates a predicate.
    class NotHighlightPredicate implements HighlightPredicate {

        private final HighlightPredicate predicate;

        public NotHighlightPredicate(HighlightPredicate predicate) {
            if (predicate == null) {
                throw new NullPointerException("predicate must not be null");
            }
            this.predicate = predicate;
        }

        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            return !predicate.isHighlighted(renderer, adapter);
        }

        public HighlightPredicate getHighlightPredicate() {
            return predicate;
        }
    }

    /// Matches when every one of its predicates does; never with none.
    class AndHighlightPredicate implements HighlightPredicate {

        private final List<HighlightPredicate> predicates;

        public AndHighlightPredicate(HighlightPredicate... predicate) {
            predicates = new ArrayList<HighlightPredicate>();
            for (int i = 0; i < predicate.length; i++) {
                if (predicate[i] == null) {
                    throw new NullPointerException("predicate must not be null");
                }
                predicates.add(predicate[i]);
            }
        }

        public AndHighlightPredicate(Collection<HighlightPredicate> list) {
            predicates = new ArrayList<HighlightPredicate>(list);
            if (predicates.contains(null)) {
                throw new NullPointerException("predicate must not be null");
            }
        }

        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            for (int i = 0; i < predicates.size(); i++) {
                if (!predicates.get(i).isHighlighted(renderer, adapter)) {
                    return false;
                }
            }
            return !predicates.isEmpty();
        }

        public HighlightPredicate[] getHighlightPredicates() {
            return predicates.toArray(new HighlightPredicate[predicates.size()]);
        }
    }

    /// Matches when at least one of its predicates does.
    class OrHighlightPredicate implements HighlightPredicate {

        private final List<HighlightPredicate> predicates;

        public OrHighlightPredicate(HighlightPredicate... predicate) {
            predicates = new ArrayList<HighlightPredicate>();
            for (int i = 0; i < predicate.length; i++) {
                if (predicate[i] == null) {
                    throw new NullPointerException("predicate must not be null");
                }
                predicates.add(predicate[i]);
            }
        }

        public OrHighlightPredicate(Collection<HighlightPredicate> list) {
            predicates = new ArrayList<HighlightPredicate>(list);
            if (predicates.contains(null)) {
                throw new NullPointerException("predicate must not be null");
            }
        }

        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            for (int i = 0; i < predicates.size(); i++) {
                if (predicates.get(i).isHighlighted(renderer, adapter)) {
                    return true;
                }
            }
            return false;
        }

        public HighlightPredicate[] getHighlightPredicates() {
            return predicates.toArray(new HighlightPredicate[predicates.size()]);
        }
    }

    /// Stripes: matches every second group of `linesPerGroup` rows,
    /// starting with the second group.
    class RowGroupHighlightPredicate implements HighlightPredicate {

        private final int linesPerGroup;

        public RowGroupHighlightPredicate(int linesPerGroup) {
            if (linesPerGroup < 1) {
                throw new IllegalArgumentException("a group contain at least 1 row, was: " + linesPerGroup);
            }
            this.linesPerGroup = linesPerGroup;
        }

        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            return adapter.row >= 0 && (adapter.row / linesPerGroup) % 2 != 0;
        }

        public int getLinesPerGroup() {
            return linesPerGroup;
        }
    }

    /// Matches the cells of the given model columns.
    class ColumnHighlightPredicate implements HighlightPredicate {

        private final List<Integer> columnList;

        public ColumnHighlightPredicate(int... columns) {
            columnList = new ArrayList<Integer>();
            for (int i = 0; i < columns.length; i++) {
                columnList.add(Integer.valueOf(columns[i]));
            }
        }

        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            return columnList.contains(Integer.valueOf(adapter.convertColumnIndexToModel(adapter.column)));
        }

        public Integer[] getColumns() {
            return columnList.toArray(new Integer[columnList.size()]);
        }
    }

    /// Matches the cells of the columns with one of the given identifiers.
    class IdentifierHighlightPredicate implements HighlightPredicate {

        private final List<Object> identifierList;

        public IdentifierHighlightPredicate(Object... identifiers) {
            identifierList = new ArrayList<Object>();
            for (int i = 0; i < identifiers.length; i++) {
                identifierList.add(identifiers[i]);
            }
        }

        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            int modelIndex = adapter.convertColumnIndexToModel(adapter.column);
            return identifierList.contains(adapter.getColumnIdentifierAt(modelIndex));
        }

        public Object[] getIdentifiers() {
            return identifierList.toArray();
        }
    }

    /// Matches the cells of nodes at one of the given depths.
    class DepthHighlightPredicate implements HighlightPredicate {

        private final List<Integer> depthList;

        public DepthHighlightPredicate(int... depths) {
            depthList = new ArrayList<Integer>();
            for (int i = 0; i < depths.length; i++) {
                depthList.add(Integer.valueOf(depths[i]));
            }
        }

        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            return depthList.contains(Integer.valueOf(adapter.getDepth()));
        }

        public Integer[] getDepths() {
            return depthList.toArray(new Integer[depthList.size()]);
        }
    }

    /// Matches the cells whose value equals a given one.
    class EqualsHighlightPredicate implements HighlightPredicate {

        private final Object compareValue;

        public EqualsHighlightPredicate() {
            this(null);
        }

        public EqualsHighlightPredicate(Object compareValue) {
            this.compareValue = compareValue;
        }

        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            Object value = adapter.getValue();
            return compareValue == null ? value == null : compareValue.equals(value);
        }

        public Object getCompareValue() {
            return compareValue;
        }
    }

    /// Matches the cells whose value is an instance of a class.
    class TypeHighlightPredicate implements HighlightPredicate {

        private final Class<?> clazz;

        public TypeHighlightPredicate() {
            this(Object.class);
        }

        public TypeHighlightPredicate(Class<?> clazz) {
            if (clazz == null) {
                throw new NullPointerException("class must not be null");
            }
            this.clazz = clazz;
        }

        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            Object value = adapter.getValue();
            return value != null && clazz.isInstance(value);
        }

        public Class<?> getType() {
            return clazz;
        }
    }

    /// Matches the cells whose column class is assignable to a class.
    class ColumnTypeHighlightPredicate implements HighlightPredicate {

        private final Class<?> clazz;

        public ColumnTypeHighlightPredicate() {
            this(Object.class);
        }

        public ColumnTypeHighlightPredicate(Class<?> clazz) {
            if (clazz == null) {
                throw new NullPointerException("class must not be null");
            }
            this.clazz = clazz;
        }

        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            Class<?> c = adapter.getColumnClass();
            return c != null && clazz.isAssignableFrom(c);
        }

        public Class<?> getType() {
            return clazz;
        }
    }
}
