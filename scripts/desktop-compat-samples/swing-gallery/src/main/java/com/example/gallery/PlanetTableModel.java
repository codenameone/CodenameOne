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
package com.example.gallery;

import javax.swing.table.AbstractTableModel;

/**
 * The planets as a table: three read-only columns and an editable check box.
 */
public class PlanetTableModel extends AbstractTableModel {

    public static final int NAME = 0;
    public static final int MASS = 1;
    public static final int MOONS = 2;
    public static final int VISITED = 3;

    private static final String[] COLUMNS = {"Name", "Mass (Earths)", "Moons", "Visited"};

    private final Planet[] planets;

    public PlanetTableModel(Planet[] planets) {
        this.planets = planets;
    }

    @Override
    public int getRowCount() {
        return planets.length;
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        switch (column) {
            case MASS:
                return Double.class;
            case MOONS:
                return Integer.class;
            case VISITED:
                return Boolean.class;
            default:
                return String.class;
        }
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return column == VISITED;
    }

    @Override
    public Object getValueAt(int row, int column) {
        Planet p = planets[row];
        switch (column) {
            case MASS:
                return p.getMass();
            case MOONS:
                return p.getMoons();
            case VISITED:
                return p.isVisited();
            default:
                return p.getName();
        }
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        if (column == VISITED) {
            planets[row].setVisited(Boolean.TRUE.equals(value));
            fireTableCellUpdated(row, column);
        }
    }
}
