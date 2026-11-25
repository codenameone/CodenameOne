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
package com.codename1.components;

import com.codename1.io.Preferences;
import com.codename1.ui.Button;
import com.codename1.ui.ButtonGroup;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.RadioButton;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.EventDispatcher;

import java.util.ArrayList;
import java.util.List;

/// An emoji keyboard: a grid of emoji by category, with a search field and a
/// row of recently used ones.
///
/// ```java
/// TextField message = new TextField();
/// EmojiPicker emoji = new EmojiPicker();
/// emoji.bindTo(message);                 // inserts at the cursor
/// form.add(BorderLayout.SOUTH, emoji);
///
/// // or handle the choice yourself
/// emoji.addActionListener(e -> send(emoji.getSelectedEmoji()));
/// ```
///
/// Tabs along the top switch category; the search field matches emoji names.
/// The recent row remembers the last emoji picked across launches, in
/// `Preferences`; turn it off with `setRecentsEnabled(false)`.
///
/// The emoji themselves are drawn by the platform's emoji font, so they look
/// like the emoji the user's keyboard shows.
///
/// UIIDs: `EmojiPicker` for the container, `EmojiPickerItem` for an emoji,
/// `EmojiPickerTab` for a category tab and `EmojiPickerSearch` for the search
/// field, falling back to `Label`, `Button` and `TextField`.
public class EmojiPicker extends Container {
    private static final String RECENTS_KEY = "cn1.emojiPicker.recent";
    private static final int MAX_RECENTS = 24;
    private static final String[][] CATEGORIES = {
        EmojiData.SMILEYS, EmojiData.ANIMALS, EmojiData.FOODS, EmojiData.ACTIVITIES,
        EmojiData.TRAVEL, EmojiData.OBJECTS, EmojiData.SYMBOLS, EmojiData.FLAGS
    };
    private static final char[] CATEGORY_ICONS = {
        FontImage.MATERIAL_EMOJI_EMOTIONS, FontImage.MATERIAL_PETS, FontImage.MATERIAL_RESTAURANT,
        FontImage.MATERIAL_SPORTS_SOCCER, FontImage.MATERIAL_DIRECTIONS_CAR,
        FontImage.MATERIAL_LIGHTBULB, FontImage.MATERIAL_FAVORITE, FontImage.MATERIAL_FLAG
    };
    private static final String[] CATEGORY_NAMES = {
        "Smileys", "Animals", "Food", "Activities", "Travel", "Objects", "Symbols", "Flags"
    };

    private final TextField search = new TextField();
    private final Container tabs = new Container(new GridLayout(1, CATEGORIES.length + 1));
    private final Container grid = new Container(new GridLayout(8));
    private final ButtonGroup tabGroup = new ButtonGroup();
    private final RadioButton recentTab = new RadioButton();
    private final RadioButton[] categoryTabs = new RadioButton[CATEGORIES.length];
    private final EventDispatcher listeners = new EventDispatcher();
    private int columns = 8;
    private int category;
    private boolean recentsEnabled = true;
    private String selected;
    private Font emojiFont;

    /// A picker showing the first category.
    public EmojiPicker() {
        super(new BorderLayout());
        setUIID("EmojiPicker");
        search.setUIID(themed("EmojiPickerSearch", "TextField"));
        search.setHint("Search emoji");
        search.addDataChangedListener(new DataChangedListener() {
            @Override
            public void dataChanged(int type, int index) {
                refresh();
            }
        });
        tabs.setUIID("Container");
        addTab(recentTab, FontImage.MATERIAL_HISTORY, "Recent", -1);
        for (int i = 0; i < CATEGORIES.length; i++) {
            categoryTabs[i] = new RadioButton();
            addTab(categoryTabs[i], CATEGORY_ICONS[i], CATEGORY_NAMES[i], i);
        }
        grid.setUIID("Container");
        grid.setScrollableY(true);
        Container north = BorderLayout.center(search).add(BorderLayout.SOUTH, tabs);
        add(BorderLayout.NORTH, north).add(BorderLayout.CENTER, grid);
        category = 0;
        categoryTabs[0].setSelected(true);
        refresh();
    }

    @Override
    protected void initUnselectedStyle(Style unselectedStyle) {
        super.initUnselectedStyle(unselectedStyle);
        if (unselectedStyle != null && !getUIManager().isThemeStyleDefined(getUIID())) {
            unselectedStyle.setBgTransparency(0);
        }
    }

    private String themed(String uiid, String fallback) {
        return getUIManager().isThemeStyleDefined(uiid) ? uiid : fallback;
    }

    private void addTab(RadioButton tab, char icon, String name, final int index) {
        tab.setToggle(true);
        tab.setUIID(themed("EmojiPickerTab", "Label"));
        FontImage.setMaterialIcon(tab, icon, 3.5f);
        tab.setAccessibilityText(name);
        tabGroup.add(tab);
        tab.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                showCategory(index);
            }
        });
        tabs.add(tab);
    }

    /// The number of category tabs, not counting the recent one.
    ///
    /// #### Returns
    ///
    /// the category count
    public static int getCategoryCount() {
        return CATEGORIES.length;
    }

    /// The emoji of a category, in display order.
    ///
    /// #### Parameters
    ///
    /// - `index`: the category, 0 to `getCategoryCount() - 1`
    ///
    /// #### Returns
    ///
    /// the emoji
    public static String[] getEmoji(int index) {
        String[] pairs = CATEGORIES[index];
        String[] out = new String[pairs.length / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = pairs[i * 2];
        }
        return out;
    }

    /// The emoji whose name contains `query`, ignoring case.
    ///
    /// #### Parameters
    ///
    /// - `query`: part of a name, such as "heart"
    ///
    /// #### Returns
    ///
    /// the matching emoji
    public static List<String> search(String query) {
        List<String> out = new ArrayList<String>();
        if (query == null || query.trim().length() == 0) {
            return out;
        }
        String q = query.trim();
        for (String[] pairs : CATEGORIES) {
            for (int i = 0; i + 1 < pairs.length; i += 2) {
                if (containsIgnoreCase(pairs[i + 1], q)) {
                    out.add(pairs[i]);
                }
            }
        }
        return out;
    }

    private static boolean containsIgnoreCase(String text, String part) {
        int max = text.length() - part.length();
        for (int i = 0; i <= max; i++) {
            if (text.regionMatches(true, i, part, 0, part.length())) {
                return true;
            }
        }
        return false;
    }

    /// Shows a category, clearing the search.
    ///
    /// #### Parameters
    ///
    /// - `index`: the category, or -1 for the recent emoji
    public void showCategory(int index) {
        category = index;
        if (index < 0) {
            recentTab.setSelected(true);
        } else {
            categoryTabs[index].setSelected(true);
        }
        if (search.getText().length() > 0) {
            search.setText("");
        }
        refresh();
    }

    /// The category showing, -1 for the recent emoji.
    ///
    /// #### Returns
    ///
    /// the category index
    public int getCategory() {
        return category;
    }

    /// The search field.
    ///
    /// #### Returns
    ///
    /// the field
    public TextField getSearchField() {
        return search;
    }

    /// Whether the picker keeps a row of recently used emoji.
    ///
    /// #### Returns
    ///
    /// true by default
    public boolean isRecentsEnabled() {
        return recentsEnabled;
    }

    /// Turns the recently used tab on or off.
    ///
    /// #### Parameters
    ///
    /// - `enabled`: false to hide it and stop recording picks
    public void setRecentsEnabled(boolean enabled) {
        recentsEnabled = enabled;
        recentTab.setHidden(!enabled);
        tabs.setLayout(new GridLayout(1, CATEGORIES.length + (enabled ? 1 : 0)));
        if (!enabled && category < 0) {
            showCategory(0);
        }
        revalidateLater();
    }

    /// Sets how many emoji a row holds.
    ///
    /// #### Parameters
    ///
    /// - `columns`: the column count, 8 by default
    public void setColumns(int columns) {
        this.columns = Math.max(1, columns);
        refresh();
    }

    /// The emoji the user picked last.
    ///
    /// #### Returns
    ///
    /// the emoji, or null before the first pick
    public String getSelectedEmoji() {
        return selected;
    }

    /// The emoji most recently picked, newest first.
    ///
    /// #### Returns
    ///
    /// the recent emoji
    public static List<String> getRecents() {
        List<String> out = new ArrayList<String>();
        String stored = Preferences.get(RECENTS_KEY, "");
        int start = 0;
        for (int i = 0; i <= stored.length(); i++) {
            if (i == stored.length() || stored.charAt(i) == '\n') {
                if (i > start) {
                    out.add(stored.substring(start, i));
                }
                start = i + 1;
            }
        }
        return out;
    }

    /// Adds a listener called when the user picks an emoji; read it with
    /// `#getSelectedEmoji()`.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void addActionListener(ActionListener l) {
        listeners.addListener(l);
    }

    /// Removes a listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void removeActionListener(ActionListener l) {
        listeners.removeListener(l);
    }

    /// Inserts every picked emoji into a text field at its cursor.
    ///
    /// #### Parameters
    ///
    /// - `field`: the field to type into
    public void bindTo(final TextArea field) {
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                insert(field, selected);
            }
        });
    }

    static void insert(TextArea field, String emoji) {
        String text = field.getText() == null ? "" : field.getText();
        int pos = field.getCursorPosition();
        if (pos < 0 || pos > text.length()) {
            pos = text.length();
        }
        field.setText(text.substring(0, pos) + emoji + text.substring(pos));
        if (field instanceof TextField) {
            ((TextField) field).setCursorPosition(pos + emoji.length());
        }
    }

    /// Picks an emoji as a tap would: records it as recent and fires the listeners.
    ///
    /// #### Parameters
    ///
    /// - `emoji`: the emoji
    public void pick(String emoji) {
        selected = emoji;
        if (recentsEnabled) {
            remember(emoji);
        }
        listeners.fireActionEvent(new ActionEvent(this, ActionEvent.Type.Other));
    }

    private static void remember(String emoji) {
        List<String> recent = getRecents();
        recent.remove(emoji);
        recent.add(0, emoji);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < recent.size() && i < MAX_RECENTS; i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(recent.get(i));
        }
        Preferences.set(RECENTS_KEY, sb.toString());
    }

    private Font emojiFont() {
        if (emojiFont == null) {
            Font base = getUIManager().getComponentStyle("Label").getFont();
            int size = CN.convertToPixels(5.5f);
            if (Font.isNativeFontSchemeSupported()) {
                emojiFont = Font.createTrueTypeFont("native:MainRegular").derive(size, Font.STYLE_PLAIN);
            } else {
                emojiFont = base;
            }
        }
        return emojiFont;
    }

    private void refresh() {
        List<String> items;
        String q = search.getText();
        if (q != null && q.trim().length() > 0) {
            items = search(q);
        } else if (category < 0) {
            items = getRecents();
        } else {
            items = new ArrayList<String>();
            String[] pairs = CATEGORIES[category];
            for (int i = 0; i < pairs.length; i += 2) {
                items.add(pairs[i]);
            }
        }
        grid.removeAll();
        grid.setLayout(new GridLayout(Math.max(1, (items.size() + columns - 1) / columns), columns));
        String uiid = themed("EmojiPickerItem", "Label");
        boolean themedItem = getUIManager().isThemeStyleDefined("EmojiPickerItem");
        for (final String e : items) {
            Button b = new Button(e);
            b.setUIID(uiid);
            if (!themedItem) {
                Style s = b.getAllStyles();
                s.setFont(emojiFont());
                s.setAlignment(CENTER);
                s.setPaddingUnit(Style.UNIT_TYPE_DIPS);
                s.setPadding(1, 1, 0, 0);
            }
            b.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    pick(e);
                }
            });
            grid.add(b);
        }
        if (items.isEmpty()) {
            grid.setLayout(new BorderLayout());
            grid.add(BorderLayout.CENTER, new Label(category < 0 && (q == null || q.length() == 0)
                    ? "No recent emoji" : "No emoji found", "Label"));
        }
        revalidateLater();
    }
}
