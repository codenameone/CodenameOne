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
package android.view;

import android.content.ComponentName;
import android.content.Intent;

/// An options, context or popup menu.
public interface Menu {
    int USER_MASK = 0x0000ffff;
    int USER_SHIFT = 0;
    int CATEGORY_MASK = 0xffff0000;
    int CATEGORY_SHIFT = 16;
    int NONE = 0;
    int FIRST = 1;
    int CATEGORY_CONTAINER = 0x00010000;
    int CATEGORY_SYSTEM = 0x00020000;
    int CATEGORY_SECONDARY = 0x00030000;
    int CATEGORY_ALTERNATIVE = 0x00040000;
    int FLAG_APPEND_TO_GROUP = 0x0001;
    int FLAG_PERFORM_NO_CLOSE = 0x0001;
    int FLAG_ALWAYS_PERFORM_CLOSE = 0x0002;
    int SUPPORTED_MODIFIERS_MASK = 0;

    MenuItem add(CharSequence title);

    MenuItem add(int titleRes);

    MenuItem add(int groupId, int itemId, int order, CharSequence title);

    MenuItem add(int groupId, int itemId, int order, int titleRes);

    SubMenu addSubMenu(CharSequence title);

    SubMenu addSubMenu(int titleRes);

    SubMenu addSubMenu(int groupId, int itemId, int order, CharSequence title);

    SubMenu addSubMenu(int groupId, int itemId, int order, int titleRes);

    int addIntentOptions(int groupId, int itemId, int order, ComponentName caller, Intent[] specifics,
                         Intent intent, int flags, MenuItem[] outSpecificItems);

    void removeItem(int id);

    void removeGroup(int groupId);

    void clear();

    void setGroupCheckable(int group, boolean checkable, boolean exclusive);

    void setGroupVisible(int group, boolean visible);

    void setGroupEnabled(int group, boolean enabled);

    boolean hasVisibleItems();

    MenuItem findItem(int id);

    int size();

    MenuItem getItem(int index);

    void close();

    boolean performShortcut(int keyCode, KeyEvent event, int flags);

    boolean isShortcutKey(int keyCode, KeyEvent event);

    boolean performIdentifierAction(int id, int flags);

    void setQwertyMode(boolean isQwerty);
}
