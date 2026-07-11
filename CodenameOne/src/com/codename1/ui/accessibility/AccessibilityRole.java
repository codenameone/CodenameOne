/*
 * Copyright (c) 2026 Codename One and contributors. All rights reserved.
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
