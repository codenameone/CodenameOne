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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListPopupWindow;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.List;

/// Shows a menu as a list popup: what `PopupMenu` and the action bar's
/// overflow button both open.
public final class MenuPopup {

    private MenuPopup() {
    }

    /// Shows the visible items of `menu` anchored to `anchor`. Selecting an
    /// item runs it and closes the popup; an item with a submenu opens the
    /// submenu in its place.
    public static ListPopupWindow show(Context context, MenuImpl menu, View anchor, int gravity,
                                       int defStyleAttr, int defStyleRes, PopupWindow.OnDismissListener onDismiss) {
        return show(context, menu.visibleItems(), anchor, gravity, defStyleAttr, defStyleRes, onDismiss);
    }

    /// Shows `items`, a part of a menu such as the action bar's overflow.
    public static ListPopupWindow show(final Context context, final List<MenuImpl.Item> items, final View anchor,
                                       final int gravity, final int defStyleAttr, final int defStyleRes,
                                       final PopupWindow.OnDismissListener onDismiss) {
        final ListPopupWindow popup = new ListPopupWindow(context, null, defStyleAttr, defStyleRes);
        ItemAdapter adapter = new ItemAdapter(context, items);
        popup.setAdapter(adapter);
        popup.setAnchorView(anchor);
        popup.setModal(true);
        popup.setDropDownGravity(gravity);
        popup.setContentWidth(measureWidth(context, adapter));
        popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MenuImpl.Item item = items.get(position);
                if (item.hasSubMenu()) {
                    popup.setOnDismissListener(null);
                    popup.dismiss();
                    show(context, (MenuImpl) item.getSubMenu(), anchor, gravity, defStyleAttr, defStyleRes, onDismiss);
                    return;
                }
                item.invoke();
                popup.dismiss();
            }
        });
        popup.setOnDismissListener(onDismiss);
        popup.show();
        return popup;
    }

    /// The widest item, capped like Android at half the screen or 320dp,
    /// whichever is wider.
    private static int measureWidth(Context context, ItemAdapter adapter) {
        float dp = context.getResources().getDisplayMetrics().density;
        int max = Math.max(context.getResources().getDisplayMetrics().widthPixels / 2, Math.round(320 * dp));
        int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int width = 0;
        View convert = null;
        android.widget.FrameLayout parent = new android.widget.FrameLayout(context);
        for (int i = 0; i < adapter.getCount(); i++) {
            convert = adapter.getView(i, convert, parent);
            convert.measure(spec, spec);
            width = Math.max(width, convert.getMeasuredWidth());
            if (width >= max) {
                return max;
            }
        }
        return width;
    }

    private static final class ItemAdapter extends BaseAdapter {
        private final Context context;
        private final List<MenuImpl.Item> items;

        ItemAdapter(Context context, List<MenuImpl.Item> items) {
            this.context = context;
            this.items = items;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return items.get(position).getItemId();
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return items.get(position).isEnabled();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = LayoutInflater.from(context).inflate(android.R.layout.popup_menu_item_layout, parent, false);
            }
            MenuImpl.Item item = items.get(position);
            TextView title = row.findViewById(android.R.id.title);
            title.setText(item.getTitle());
            row.setEnabled(item.isEnabled());
            return row;
        }
    }
}
