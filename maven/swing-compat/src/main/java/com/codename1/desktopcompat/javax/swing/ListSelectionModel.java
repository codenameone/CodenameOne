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

import com.codename1.desktopcompat.javax.swing.event.ListSelectionListener;

/// Which rows of a list are selected.
public interface ListSelectionModel {

    int SINGLE_SELECTION = 0;

    int SINGLE_INTERVAL_SELECTION = 1;

    int MULTIPLE_INTERVAL_SELECTION = 2;

    void setSelectionInterval(int index0, int index1);

    void addSelectionInterval(int index0, int index1);

    void removeSelectionInterval(int index0, int index1);

    int getMinSelectionIndex();

    int getMaxSelectionIndex();

    boolean isSelectedIndex(int index);

    int getAnchorSelectionIndex();

    void setAnchorSelectionIndex(int index);

    int getLeadSelectionIndex();

    void setLeadSelectionIndex(int index);

    void clearSelection();

    boolean isSelectionEmpty();

    void insertIndexInterval(int index, int length, boolean before);

    void removeIndexInterval(int index0, int index1);

    void setValueIsAdjusting(boolean valueIsAdjusting);

    boolean getValueIsAdjusting();

    void setSelectionMode(int selectionMode);

    int getSelectionMode();

    void addListSelectionListener(ListSelectionListener x);

    void removeListSelectionListener(ListSelectionListener x);
}
