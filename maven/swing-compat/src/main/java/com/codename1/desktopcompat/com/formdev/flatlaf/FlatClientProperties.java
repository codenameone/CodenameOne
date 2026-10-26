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
package com.codename1.desktopcompat.com.formdev.flatlaf;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.javax.swing.JComponent;

/// The names and values of FlatLaf's client properties, so that
/// `component.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, ...)`
/// compiles and means what it says.
///
/// What this layer does with each property is in the description of
/// [ClientProps][com.codename1.desktopcompat.rt.ClientProps]: the ones a
/// Codename One widget can honour change the widget, the others are
/// kept on the component and change nothing.
public interface FlatClientProperties {

    String BUTTON_TYPE = "JButton.buttonType";
    String BUTTON_TYPE_SQUARE = "square";
    String BUTTON_TYPE_ROUND_RECT = "roundRect";
    String BUTTON_TYPE_TAB = "tab";
    String BUTTON_TYPE_HELP = "help";
    String BUTTON_TYPE_TOOLBAR_BUTTON = "toolBarButton";
    String BUTTON_TYPE_BORDERLESS = "borderless";
    String SQUARE_SIZE = "JButton.squareSize";
    String SELECTED_STATE = "JButton.selectedState";
    String SELECTED_STATE_INDETERMINATE = "indeterminate";
    String STYLE = "FlatLaf.style";
    String STYLE_CLASS = "FlatLaf.styleClass";
    String MINIMUM_WIDTH = "JComponent.minimumWidth";
    String MINIMUM_HEIGHT = "JComponent.minimumHeight";
    String COMPONENT_ROUND_RECT = "JComponent.roundRect";
    String OUTLINE = "JComponent.outline";
    String OUTLINE_ERROR = "error";
    String OUTLINE_WARNING = "warning";
    String OUTLINE_SUCCESS = "success";
    String COMPONENT_FOCUS_OWNER = "JComponent.focusOwner";
    String COMPONENT_TITLE_BAR_CAPTION = "JComponent.titleBarCaption";
    String FULL_WINDOW_CONTENT_BUTTONS_PLACEHOLDER = "FlatLaf.fullWindowContent.buttonsPlaceholder";
    String POPUP_BORDER_CORNER_RADIUS = "Popup.borderCornerRadius";
    String POPUP_ROUNDED_BORDER_WIDTH = "Popup.roundedBorderWidth";
    String POPUP_DROP_SHADOW_PAINTED = "Popup.dropShadowPainted";
    String POPUP_FORCE_HEAVY_WEIGHT = "Popup.forceHeavyWeight";
    String PROGRESS_BAR_LARGE_HEIGHT = "JProgressBar.largeHeight";
    String PROGRESS_BAR_SQUARE = "JProgressBar.square";
    String USE_WINDOW_DECORATIONS = "JRootPane.useWindowDecorations";
    String MENU_BAR_EMBEDDED = "JRootPane.menuBarEmbedded";
    String FULL_WINDOW_CONTENT = "FlatLaf.fullWindowContent";
    String FULL_WINDOW_CONTENT_BUTTONS_BOUNDS = "FlatLaf.fullWindowContent.buttonsBounds";
    String TITLE_BAR_SHOW_ICON = "JRootPane.titleBarShowIcon";
    String TITLE_BAR_SHOW_TITLE = "JRootPane.titleBarShowTitle";
    String TITLE_BAR_SHOW_ICONIFFY = "JRootPane.titleBarShowIconify";
    String TITLE_BAR_SHOW_MAXIMIZE = "JRootPane.titleBarShowMaximize";
    String TITLE_BAR_SHOW_CLOSE = "JRootPane.titleBarShowClose";
    String TITLE_BAR_BACKGROUND = "JRootPane.titleBarBackground";
    String TITLE_BAR_FOREGROUND = "JRootPane.titleBarForeground";
    String TITLE_BAR_HEIGHT = "JRootPane.titleBarHeight";
    String GLASS_PANE_FULL_HEIGHT = "JRootPane.glassPaneFullHeight";
    String WINDOW_STYLE = "Window.style";
    String WINDOW_STYLE_SMALL = "small";
    String SCROLL_BAR_SHOW_BUTTONS = "JScrollBar.showButtons";
    String SCROLL_PANE_SMOOTH_SCROLLING = "JScrollPane.smoothScrolling";
    String SPLIT_PANE_EXPANDABLE_SIDE = "JSplitPane.expandableSide";
    String SPLIT_PANE_EXPANDABLE_SIDE_LEFT = "left";
    String SPLIT_PANE_EXPANDABLE_SIDE_RIGHT = "right";
    String TABBED_PANE_TAB_TYPE = "JTabbedPane.tabType";
    String TABBED_PANE_TAB_TYPE_UNDERLINED = "underlined";
    String TABBED_PANE_TAB_TYPE_CARD = "card";
    String TABBED_PANE_SHOW_TAB_SEPARATORS = "JTabbedPane.showTabSeparators";
    String TABBED_PANE_SHOW_CONTENT_SEPARATOR = "JTabbedPane.showContentSeparator";
    String TABBED_PANE_HAS_FULL_BORDER = "JTabbedPane.hasFullBorder";
    String TABBED_PANE_HIDE_TAB_AREA_WITH_ONE_TAB = "JTabbedPane.hideTabAreaWithOneTab";
    String TABBED_PANE_MINIMUM_TAB_WIDTH = "JTabbedPane.minimumTabWidth";
    String TABBED_PANE_MAXIMUM_TAB_WIDTH = "JTabbedPane.maximumTabWidth";
    String TABBED_PANE_TAB_HEIGHT = "JTabbedPane.tabHeight";
    String TABBED_PANE_TAB_INSETS = "JTabbedPane.tabInsets";
    String TABBED_PANE_TAB_AREA_INSETS = "JTabbedPane.tabAreaInsets";
    String TABBED_PANE_TAB_CLOSABLE = "JTabbedPane.tabClosable";
    String TABBED_PANE_TAB_CLOSE_TOOLTIPTEXT = "JTabbedPane.tabCloseToolTipText";
    String TABBED_PANE_TAB_CLOSE_CALLBACK = "JTabbedPane.tabCloseCallback";
    String TABBED_PANE_TABS_POPUP_POLICY = "JTabbedPane.tabsPopupPolicy";
    String TABBED_PANE_SCROLL_BUTTONS_POLICY = "JTabbedPane.scrollButtonsPolicy";
    String TABBED_PANE_POLICY_NEVER = "never";
    String TABBED_PANE_POLICY_AS_NEEDED = "asNeeded";
    String TABBED_PANE_POLICY_AS_NEEDED_SINGLE = "asNeededSingle";
    String TABBED_PANE_SCROLL_BUTTONS_PLACEMENT = "JTabbedPane.scrollButtonsPlacement";
    String TABBED_PANE_PLACEMENT_BOTH = "both";
    String TABBED_PANE_PLACEMENT_TRAILING = "trailing";
    String TABBED_PANE_TAB_AREA_ALIGNMENT = "JTabbedPane.tabAreaAlignment";
    String TABBED_PANE_TAB_ALIGNMENT = "JTabbedPane.tabAlignment";
    String TABBED_PANE_ALIGN_LEADING = "leading";
    String TABBED_PANE_ALIGN_TRAILING = "trailing";
    String TABBED_PANE_ALIGN_CENTER = "center";
    String TABBED_PANE_ALIGN_FILL = "fill";
    String TABBED_PANE_TAB_WIDTH_MODE = "JTabbedPane.tabWidthMode";
    String TABBED_PANE_TAB_WIDTH_MODE_PREFERRED = "preferred";
    String TABBED_PANE_TAB_WIDTH_MODE_EQUAL = "equal";
    String TABBED_PANE_TAB_WIDTH_MODE_COMPACT = "compact";
    String TABBED_PANE_TAB_WIDTH_MODE_ICON_ONLY = "iconOnly";
    String TABBED_PANE_TAB_ICON_PLACEMENT = "JTabbedPane.tabIconPlacement";
    String TABBED_PANE_TAB_ROTATION = "JTabbedPane.tabRotation";
    String TABBED_PANE_TAB_ROTATION_NONE = "none";
    String TABBED_PANE_TAB_ROTATION_AUTO = "auto";
    String TABBED_PANE_TAB_ROTATION_LEFT = "left";
    String TABBED_PANE_TAB_ROTATION_RIGHT = "right";
    String TABBED_PANE_LEADING_COMPONENT = "JTabbedPane.leadingComponent";
    String TABBED_PANE_TRAILING_COMPONENT = "JTabbedPane.trailingComponent";
    String SELECT_ALL_ON_FOCUS_POLICY = "JTextField.selectAllOnFocusPolicy";
    String SELECT_ALL_ON_FOCUS_POLICY_NEVER = "never";
    String SELECT_ALL_ON_FOCUS_POLICY_ONCE = "once";
    String SELECT_ALL_ON_FOCUS_POLICY_ALWAYS = "always";
    String SELECT_ALL_ON_MOUSE_CLICK = "JTextField.selectAllOnMouseClick";
    String PLACEHOLDER_TEXT = "JTextField.placeholderText";
    String TEXT_FIELD_PADDING = "JTextField.padding";
    String TEXT_FIELD_LEADING_ICON = "JTextField.leadingIcon";
    String TEXT_FIELD_TRAILING_ICON = "JTextField.trailingIcon";
    String TEXT_FIELD_LEADING_COMPONENT = "JTextField.leadingComponent";
    String TEXT_FIELD_TRAILING_COMPONENT = "JTextField.trailingComponent";
    String TEXT_FIELD_SHOW_CLEAR_BUTTON = "JTextField.showClearButton";
    String TEXT_FIELD_CLEAR_CALLBACK = "JTextField.clearCallback";
    String TAB_BUTTON_UNDERLINE_PLACEMENT = "JToggleButton.tab.underlinePlacement";
    String TAB_BUTTON_UNDERLINE_HEIGHT = "JToggleButton.tab.underlineHeight";
    String TAB_BUTTON_UNDERLINE_COLOR = "JToggleButton.tab.underlineColor";
    String TAB_BUTTON_SELECTED_BACKGROUND = "JToggleButton.tab.selectedBackground";
    String TREE_WIDE_SELECTION = "JTree.wideSelection";
    String TREE_WIDE_CELL_RENDERER = "JTree.wideCellRenderer";
    String TREE_PAINT_SELECTION = "JTree.paintSelection";
    String MACOS_WINDOW_BUTTONS_SPACING = "FlatLaf.macOS.windowButtonsSpacing";
    String MACOS_WINDOW_BUTTONS_SPACING_MEDIUM = "medium";
    String MACOS_WINDOW_BUTTONS_SPACING_LARGE = "large";

    /// The client property `key` of `c` when it is a `type`, else `def`.
    @SuppressWarnings("unchecked")
    static <T> T clientProperty(JComponent c, String key, T def, Class<T> type) {
        Object value = c.getClientProperty(key);
        return type.isInstance(value) ? (T) value : def;
    }

    static boolean clientPropertyEquals(JComponent c, String key, Object value) {
        Object v = c.getClientProperty(key);
        return v == null ? value == null : v.equals(value);
    }

    static boolean clientPropertyBoolean(JComponent c, String key, boolean defaultValue) {
        Object value = c.getClientProperty(key);
        return value instanceof Boolean ? ((Boolean) value).booleanValue() : defaultValue;
    }

    static Boolean clientPropertyBooleanStrict(JComponent c, String key, Boolean defaultValue) {
        Object value = c.getClientProperty(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    static int clientPropertyInt(JComponent c, String key, int defaultValue) {
        Object value = c.getClientProperty(key);
        return value instanceof Integer ? ((Integer) value).intValue() : defaultValue;
    }

    static Color clientPropertyColor(JComponent c, String key, Color defaultValue) {
        Object value = c.getClientProperty(key);
        return value instanceof Color ? (Color) value : defaultValue;
    }
}
