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
package com.codename1.ui.accessibility;

/// The semantic role of a lightweight component or virtual accessibility node.
///
/// Roles are intentionally portable. Platform ports map them to native control
/// types, traits, patterns, or ARIA roles and fall back to {@link #GENERIC} when
/// a platform has no exact equivalent.
public enum AccessibilityRole {
    NONE,
    GENERIC,
    BUTTON,
    TOGGLE_BUTTON,
    CHECKBOX,
    RADIO_BUTTON,
    SWITCH,
    HEADING,
    LINK,
    IMAGE,
    STATIC_TEXT,
    TEXT_FIELD,
    SEARCH_FIELD,
    SLIDER,
    PROGRESS_BAR,
    LIST,
    LIST_ITEM,
    GRID,
    ROW,
    CELL,
    COLUMN_HEADER,
    ROW_HEADER,
    TAB_LIST,
    TAB,
    TAB_PANEL,
    DIALOG,
    ALERT,
    MENU,
    MENU_ITEM,
    TOOLBAR,
    SCROLL_BAR,
    SPIN_BUTTON,
    COMBO_BOX,
    TREE,
    TREE_ITEM,
    SEPARATOR
}
