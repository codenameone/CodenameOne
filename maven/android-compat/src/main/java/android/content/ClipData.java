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
package android.content;

import android.net.Uri;

import java.util.ArrayList;

/// The items of a multiple selection, as a document picker returns them in
/// `Intent#getClipData()`. Only what that needs: text and URI items, no
/// clipboard.
public class ClipData {
    private final CharSequence label;
    private final ArrayList<Item> items = new ArrayList<Item>();

    public ClipData(CharSequence label, String[] mimeTypes, Item item) {
        this.label = label;
        items.add(item);
    }

    public static ClipData newRawUri(CharSequence label, Uri uri) {
        return new ClipData(label, new String[] {"text/uri-list"}, new Item(uri));
    }

    public static ClipData newUri(ContentResolver resolver, CharSequence label, Uri uri) {
        return newRawUri(label, uri);
    }

    public static ClipData newPlainText(CharSequence label, CharSequence text) {
        return new ClipData(label, new String[] {"text/plain"}, new Item(text));
    }

    public void addItem(Item item) {
        items.add(item);
    }

    public int getItemCount() {
        return items.size();
    }

    public Item getItemAt(int index) {
        return items.get(index);
    }

    public CharSequence getLabel() {
        return label;
    }

    public static class Item {
        private final CharSequence text;
        private final Uri uri;

        public Item(CharSequence text) {
            this.text = text;
            this.uri = null;
        }

        public Item(Uri uri) {
            this.text = null;
            this.uri = uri;
        }

        public CharSequence getText() {
            return text;
        }

        public Uri getUri() {
            return uri;
        }
    }
}
