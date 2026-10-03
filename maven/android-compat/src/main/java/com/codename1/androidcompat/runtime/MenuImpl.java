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
package com.codename1.androidcompat.runtime;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/// The menu model behind options, context and popup menus, and the inflater
/// for `res/menu` XML.
public class MenuImpl implements Menu {

    /// Called when the menu changes so whoever shows it can refresh.
    public interface Listener {
        void menuChanged(MenuImpl menu);
    }

    final Context context;
    final List<Item> items = new ArrayList<Item>();
    private Listener listener;
    private CharSequence headerTitle;

    public MenuImpl(Context context) {
        this.context = context;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    void changed() {
        if (listener != null) {
            listener.menuChanged(this);
        }
    }

    public CharSequence getHeaderTitle() {
        return headerTitle;
    }

    /// Visible items in display order.
    private static final Comparator<Item> BY_ORDER = new Comparator<Item>() {
        @Override
        public int compare(Item a, Item b) {
            return a.order < b.order ? -1 : a.order == b.order ? 0 : 1;
        }
    };

    public List<Item> visibleItems() {
        List<Item> out = new ArrayList<Item>();
        for (Item i : items) {
            if (i.visible) {
                out.add(i);
            }
        }
        Collections.sort(out, BY_ORDER);
        return out;
    }

    private Item addInternal(int groupId, int itemId, int order, CharSequence title) {
        Item i = new Item(this, groupId, itemId, order, title);
        items.add(i);
        changed();
        return i;
    }

    @Override
    public MenuItem add(CharSequence title) {
        return addInternal(0, 0, 0, title);
    }

    @Override
    public MenuItem add(int titleRes) {
        return addInternal(0, 0, 0, context.getText(titleRes));
    }

    @Override
    public MenuItem add(int groupId, int itemId, int order, CharSequence title) {
        return addInternal(groupId, itemId, order, title);
    }

    @Override
    public MenuItem add(int groupId, int itemId, int order, int titleRes) {
        return addInternal(groupId, itemId, order, context.getText(titleRes));
    }

    @Override
    public SubMenu addSubMenu(CharSequence title) {
        return addSubMenu(0, 0, 0, title);
    }

    @Override
    public SubMenu addSubMenu(int titleRes) {
        return addSubMenu(0, 0, 0, context.getText(titleRes));
    }

    @Override
    public SubMenu addSubMenu(int groupId, int itemId, int order, CharSequence title) {
        Item i = addInternal(groupId, itemId, order, title);
        i.sub = new Sub(context, i);
        return i.sub;
    }

    @Override
    public SubMenu addSubMenu(int groupId, int itemId, int order, int titleRes) {
        return addSubMenu(groupId, itemId, order, context.getText(titleRes));
    }

    @Override
    public int addIntentOptions(int groupId, int itemId, int order, ComponentName caller, Intent[] specifics,
                                Intent intent, int flags, MenuItem[] outSpecificItems) {
        return 0;
    }

    @Override
    public void removeItem(int id) {
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).id == id) {
                items.remove(i);
            }
        }
        changed();
    }

    @Override
    public void removeGroup(int groupId) {
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).group == groupId) {
                items.remove(i);
            }
        }
        changed();
    }

    @Override
    public void clear() {
        items.clear();
        changed();
    }

    @Override
    public void setGroupCheckable(int group, boolean checkable, boolean exclusive) {
        for (Item i : items) {
            if (i.group == group) {
                i.checkable = checkable;
                i.exclusive = exclusive;
            }
        }
        changed();
    }

    @Override
    public void setGroupVisible(int group, boolean visible) {
        for (Item i : items) {
            if (i.group == group) {
                i.visible = visible;
            }
        }
        changed();
    }

    @Override
    public void setGroupEnabled(int group, boolean enabled) {
        for (Item i : items) {
            if (i.group == group) {
                i.enabled = enabled;
            }
        }
        changed();
    }

    @Override
    public boolean hasVisibleItems() {
        for (Item i : items) {
            if (i.visible) {
                return true;
            }
        }
        return false;
    }

    @Override
    public MenuItem findItem(int id) {
        for (Item i : items) {
            if (i.id == id) {
                return i;
            }
            if (i.sub != null) {
                MenuItem f = i.sub.findItem(id);
                if (f != null) {
                    return f;
                }
            }
        }
        return null;
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public MenuItem getItem(int index) {
        return items.get(index);
    }

    @Override
    public void close() {
    }

    @Override
    public boolean performShortcut(int keyCode, KeyEvent event, int flags) {
        return false;
    }

    @Override
    public boolean isShortcutKey(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean performIdentifierAction(int id, int flags) {
        MenuItem i = findItem(id);
        return i instanceof Item && ((Item) i).invoke();
    }

    @Override
    public void setQwertyMode(boolean isQwerty) {
    }

    void setHeaderTitleInternal(CharSequence title) {
        headerTitle = title;
    }

    /// Fallback click handling for items without a listener, set by the
    /// activity or popup that shows the menu.
    public interface ItemHandler {
        boolean onItemSelected(MenuItem item);
    }

    ItemHandler handler;

    public void setItemHandler(ItemHandler handler) {
        this.handler = handler;
    }

    /// The handler that receives this menu's selections: a submenu's
    /// selections go to the menu it belongs to, as on Android.
    ItemHandler rootHandler() {
        return handler;
    }

    // ------------------------------------------------------------ item

    public static class Item implements MenuItem {
        final MenuImpl menu;
        final int group;
        final int id;
        final int order;
        CharSequence title;
        CharSequence titleCondensed;
        Drawable icon;
        Intent intent;
        boolean checkable;
        boolean checked;
        boolean exclusive;
        boolean visible = true;
        boolean enabled = true;
        int showAsAction;
        Sub sub;
        View actionView;
        CharSequence contentDescription;
        OnMenuItemClickListener clickListener;
        char numeric;
        char alpha;

        Item(MenuImpl menu, int group, int id, int order, CharSequence title) {
            this.menu = menu;
            this.group = group;
            this.id = id;
            this.order = order;
            this.title = title;
        }

        /// Runs the item: its listener, then the menu's handler, then its intent.
        public boolean invoke() {
            if (!enabled) {
                return false;
            }
            if (clickListener != null && clickListener.onMenuItemClick(this)) {
                return true;
            }
            ItemHandler h = menu.rootHandler();
            if (h != null && h.onItemSelected(this)) {
                return true;
            }
            if (intent != null) {
                menu.context.startActivity(intent);
                return true;
            }
            return false;
        }

        public int getShowAsAction() {
            return showAsAction;
        }

        @Override
        public int getItemId() {
            return id;
        }

        @Override
        public int getGroupId() {
            return group;
        }

        @Override
        public int getOrder() {
            return order;
        }

        @Override
        public MenuItem setTitle(CharSequence title) {
            this.title = title;
            menu.changed();
            return this;
        }

        @Override
        public MenuItem setTitle(int title) {
            return setTitle(menu.context.getText(title));
        }

        @Override
        public CharSequence getTitle() {
            return title;
        }

        @Override
        public MenuItem setTitleCondensed(CharSequence title) {
            titleCondensed = title;
            return this;
        }

        @Override
        public CharSequence getTitleCondensed() {
            return titleCondensed != null ? titleCondensed : title;
        }

        @Override
        public MenuItem setIcon(Drawable icon) {
            this.icon = icon;
            menu.changed();
            return this;
        }

        @Override
        public MenuItem setIcon(int iconRes) {
            return setIcon(iconRes == 0 ? null : menu.context.getDrawable(iconRes));
        }

        @Override
        public Drawable getIcon() {
            return icon;
        }

        @Override
        public MenuItem setIntent(Intent intent) {
            this.intent = intent;
            return this;
        }

        @Override
        public Intent getIntent() {
            return intent;
        }

        @Override
        public MenuItem setShortcut(char numericChar, char alphaChar) {
            numeric = numericChar;
            alpha = alphaChar;
            return this;
        }

        @Override
        public MenuItem setNumericShortcut(char numericChar) {
            numeric = numericChar;
            return this;
        }

        @Override
        public char getNumericShortcut() {
            return numeric;
        }

        @Override
        public MenuItem setAlphabeticShortcut(char alphaChar) {
            alpha = alphaChar;
            return this;
        }

        @Override
        public char getAlphabeticShortcut() {
            return alpha;
        }

        @Override
        public MenuItem setCheckable(boolean checkable) {
            this.checkable = checkable;
            menu.changed();
            return this;
        }

        @Override
        public boolean isCheckable() {
            return checkable;
        }

        @Override
        public MenuItem setChecked(boolean checked) {
            boolean changed = this.checked != checked;
            if (checked && exclusive) {
                for (Item o : menu.items) {
                    if (o != this && o.group == group && o.exclusive && o.checked) {
                        o.checked = false;
                        changed = true;
                    }
                }
            }
            this.checked = checked;
            // As MenuItemImpl: observers hear about a change, not a repeat.
            if (changed) {
                menu.changed();
            }
            return this;
        }

        @Override
        public boolean isChecked() {
            return checked;
        }

        @Override
        public MenuItem setVisible(boolean visible) {
            this.visible = visible;
            menu.changed();
            return this;
        }

        @Override
        public boolean isVisible() {
            return visible;
        }

        @Override
        public MenuItem setEnabled(boolean enabled) {
            this.enabled = enabled;
            menu.changed();
            return this;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public boolean hasSubMenu() {
            return sub != null;
        }

        @Override
        public SubMenu getSubMenu() {
            return sub;
        }

        @Override
        public MenuItem setOnMenuItemClickListener(OnMenuItemClickListener l) {
            clickListener = l;
            return this;
        }

        @Override
        public ContextMenu.ContextMenuInfo getMenuInfo() {
            return null;
        }

        @Override
        public void setShowAsAction(int actionEnum) {
            showAsAction = actionEnum;
            menu.changed();
        }

        @Override
        public MenuItem setShowAsActionFlags(int actionEnum) {
            setShowAsAction(actionEnum);
            return this;
        }

        @Override
        public MenuItem setActionView(View view) {
            actionView = view;
            return this;
        }

        @Override
        public MenuItem setActionView(int resId) {
            actionView = LayoutInflater.from(menu.context).inflate(resId, null, false);
            return this;
        }

        @Override
        public View getActionView() {
            return actionView;
        }

        @Override
        public MenuItem setOnActionExpandListener(OnActionExpandListener listener) {
            return this;
        }

        @Override
        public boolean expandActionView() {
            return false;
        }

        @Override
        public boolean collapseActionView() {
            return false;
        }

        @Override
        public boolean isActionViewExpanded() {
            return false;
        }

        @Override
        public MenuItem setContentDescription(CharSequence contentDescription) {
            this.contentDescription = contentDescription;
            return this;
        }

        @Override
        public CharSequence getContentDescription() {
            return contentDescription;
        }

        @Override
        public MenuItem setIconTintList(ColorStateList tint) {
            if (icon != null) {
                icon.setTintList(tint);
            }
            return this;
        }

        @Override
        public String toString() {
            return title == null ? "" : title.toString();
        }
    }

    // ------------------------------------------------------------ submenu

    static final class Sub extends MenuImpl implements SubMenu {
        final Item item;

        Sub(Context context, Item item) {
            super(context);
            this.item = item;
        }

        @Override
        void changed() {
            item.menu.changed();
        }

        @Override
        ItemHandler rootHandler() {
            return handler != null ? handler : item.menu.rootHandler();
        }

        @Override
        public SubMenu setHeaderTitle(int titleRes) {
            setHeaderTitleInternal(context.getText(titleRes));
            return this;
        }

        @Override
        public SubMenu setHeaderTitle(CharSequence title) {
            setHeaderTitleInternal(title);
            return this;
        }

        @Override
        public void clearHeader() {
            setHeaderTitleInternal(null);
        }

        @Override
        public SubMenu setHeaderIcon(int iconRes) {
            return this;
        }

        @Override
        public SubMenu setHeaderIcon(Drawable icon) {
            return this;
        }

        @Override
        public SubMenu setHeaderView(View view) {
            return this;
        }

        @Override
        public SubMenu setIcon(int iconRes) {
            item.setIcon(iconRes);
            return this;
        }

        @Override
        public SubMenu setIcon(Drawable icon) {
            item.setIcon(icon);
            return this;
        }

        @Override
        public MenuItem getItem() {
            return item;
        }
    }

    // ------------------------------------------------------------ inflation

    public static void inflate(Context context, XmlNode node, Menu menu) {
        inflateChildren(context, node, menu, 0, 0, true, true, 0);
    }

    private static void inflateChildren(Context context, XmlNode node, Menu menu, int groupId, int groupCategory,
                                        boolean groupVisible, boolean groupEnabled, int groupCheckable) {
        for (XmlNode c : node.children) {
            if (c.tag.equals("group")) {
                TypedArray a = context.obtainStyledAttributes(new CompiledAttributeSet(c), android.R.styleable.MenuGroup);
                int gid = a.getResourceId(android.R.styleable.MenuGroup_id, 0);
                int cat = a.getInt(android.R.styleable.MenuGroup_menuCategory, 0);
                boolean vis = a.getBoolean(android.R.styleable.MenuGroup_visible, true);
                boolean en = a.getBoolean(android.R.styleable.MenuGroup_enabled, true);
                int checkable = a.getInt(android.R.styleable.MenuGroup_checkableBehavior, 0);
                a.recycle();
                inflateChildren(context, c, menu, gid, cat, vis, en, checkable);
            } else if (c.tag.equals("item")) {
                TypedArray a = context.obtainStyledAttributes(new CompiledAttributeSet(c), android.R.styleable.MenuItem);
                int id = a.getResourceId(android.R.styleable.MenuItem_id, 0);
                int order = a.getInt(android.R.styleable.MenuItem_orderInCategory, 0)
                        | a.getInt(android.R.styleable.MenuItem_menuCategory, groupCategory);
                CharSequence title = a.getText(android.R.styleable.MenuItem_title);
                XmlNode subNode = null;
                for (XmlNode s : c.children) {
                    if (s.tag.equals("menu")) {
                        subNode = s;
                    }
                }
                MenuItem item;
                if (subNode != null) {
                    SubMenu sub = menu.addSubMenu(groupId, id, order, title);
                    inflateChildren(context, subNode, sub, 0, 0, true, true, 0);
                    item = sub.getItem();
                } else {
                    item = menu.add(groupId, id, order, title);
                }
                int icon = a.getResourceId(android.R.styleable.MenuItem_icon, 0);
                if (icon != 0) {
                    item.setIcon(icon);
                }
                CharSequence condensed = a.getText(android.R.styleable.MenuItem_titleCondensed);
                if (condensed != null) {
                    item.setTitleCondensed(condensed);
                }
                boolean checkable = groupCheckable != 0 || a.getBoolean(android.R.styleable.MenuItem_checkable, false);
                item.setCheckable(checkable);
                if (item instanceof Item) {
                    ((Item) item).exclusive = groupCheckable == 2;
                }
                item.setChecked(a.getBoolean(android.R.styleable.MenuItem_checked, false));
                item.setVisible(groupVisible && a.getBoolean(android.R.styleable.MenuItem_visible, true));
                item.setEnabled(groupEnabled && a.getBoolean(android.R.styleable.MenuItem_enabled, true));
                int show = a.getInt(android.R.styleable.MenuItem_showAsAction, -1);
                if (show < 0) {
                    // AppCompat menus put showAsAction in the app namespace.
                    ResValue v = c.value(XmlNode.NS_APP, "showAsAction");
                    show = v == null ? 0 : parseShowAsAction(v);
                }
                item.setShowAsAction(show);
                int actionLayout = a.getResourceId(android.R.styleable.MenuItem_actionLayout, 0);
                if (actionLayout != 0) {
                    item.setActionView(actionLayout);
                }
                CharSequence cd = a.getText(android.R.styleable.MenuItem_contentDescription);
                if (cd != null) {
                    item.setContentDescription(cd);
                }
                a.recycle();
            }
        }
    }

    static int parseShowAsAction(ResValue v) {
        if (v.type >= android.util.TypedValue.TYPE_FIRST_INT && v.type <= android.util.TypedValue.TYPE_LAST_INT) {
            return v.data;
        }
        String s = v.string == null ? "" : v.string;
        int r = 0;
        int start = 0;
        for (int i = 0; i <= s.length(); i++) {
            if (i == s.length() || s.charAt(i) == '|') {
                String part = s.substring(start, i).trim();
                if (part.equals("ifRoom")) {
                    r |= MenuItem.SHOW_AS_ACTION_IF_ROOM;
                } else if (part.equals("always")) {
                    r |= MenuItem.SHOW_AS_ACTION_ALWAYS;
                } else if (part.equals("withText")) {
                    r |= MenuItem.SHOW_AS_ACTION_WITH_TEXT;
                } else if (part.equals("collapseActionView")) {
                    r |= MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW;
                }
                start = i + 1;
            }
        }
        return r;
    }
}
