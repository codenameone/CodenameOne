/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.share;

import java.util.ArrayList;
import java.util.List;

/// Content shared into the app from another application via the platform share sheet
/// (Android) or share extension / open-in flow (iOS). Delivered to the app through
/// `com.codename1.system.Lifecycle#onReceivedSharedContent(SharedContent)`.
///
/// A shared payload may contain multiple items of mixed type (for example several images
/// shared at once). File and image items are exposed as paths in the Codename One
/// `com.codename1.io.FileSystemStorage`, having been copied out of the originating app's
/// sandbox by the platform port, so application code is fully platform neutral.
///
/// Usage
/// ```java
/// public class MyApp extends Lifecycle {
///     public void onReceivedSharedContent(SharedContent content) {
///         if (content.hasText()) {
///             handleText(content.getFirstItem().getText());
///         }
///         for (SharedContent.Item item : content.getItems()) {
///             if (item.getType() == SharedContent.TYPE_IMAGE) {
///                 importImage(item.getFilePath());
///             }
///         }
///     }
/// }
/// ```
public final class SharedContent {

    /// Item type: plain text.
    public static final int TYPE_TEXT = 1;

    /// Item type: a URL (delivered as text).
    public static final int TYPE_URL = 2;

    /// Item type: a file, exposed as a `FileSystemStorage` path.
    public static final int TYPE_FILE = 3;

    /// Item type: an image, exposed as a `FileSystemStorage` path.
    public static final int TYPE_IMAGE = 4;

    private final String subject;
    private final Item[] items;

    private SharedContent(String subject, Item[] items) {
        this.subject = subject;
        this.items = items;
    }

    /// Returns the optional subject of the shared content (for example an email subject),
    /// or null if none was supplied.
    ///
    /// #### Returns
    ///
    /// the subject, or null
    public String getSubject() {
        return subject;
    }

    /// Returns the items contained in this shared payload.
    ///
    /// #### Returns
    ///
    /// the items, never null
    public Item[] getItems() {
        return items;
    }

    /// Returns the first item, or null if the payload is empty.
    ///
    /// #### Returns
    ///
    /// the first item, or null
    public Item getFirstItem() {
        return items.length == 0 ? null : items[0];
    }

    /// Returns true if any item is text or a URL.
    ///
    /// #### Returns
    ///
    /// true if textual content is present
    public boolean hasText() {
        for (Item i : items) {
            if (i.type == TYPE_TEXT || i.type == TYPE_URL) {
                return true;
            }
        }
        return false;
    }

    /// Returns true if any item is a file or an image.
    ///
    /// #### Returns
    ///
    /// true if file content is present
    public boolean hasFiles() {
        for (Item i : items) {
            if (i.type == TYPE_FILE || i.type == TYPE_IMAGE) {
                return true;
            }
        }
        return false;
    }

    /// Creates a new builder. Intended for use by the platform ports that construct the
    /// shared content before delivering it to the app.
    ///
    /// #### Returns
    ///
    /// a new builder
    public static Builder builder() {
        return new Builder();
    }

    /// A single item within a shared payload.
    public static final class Item {
        private final int type;
        private final String mimeType;
        private final String text;
        private final String filePath;
        private final String title;

        Item(int type, String mimeType, String text, String filePath, String title) {
            this.type = type;
            this.mimeType = mimeType;
            this.text = text;
            this.filePath = filePath;
            this.title = title;
        }

        /// Returns the item type, one of the `TYPE_` constants.
        ///
        /// #### Returns
        ///
        /// the item type
        public int getType() {
            return type;
        }

        /// Returns the MIME type of the item, or null if unknown.
        ///
        /// #### Returns
        ///
        /// the MIME type, or null
        public String getMimeType() {
            return mimeType;
        }

        /// Returns the textual value for text and URL items, or null for file items.
        ///
        /// #### Returns
        ///
        /// the text, or null
        public String getText() {
            return text;
        }

        /// Returns the `FileSystemStorage` path for file and image items, or null for
        /// textual items.
        ///
        /// #### Returns
        ///
        /// the file path, or null
        public String getFilePath() {
            return filePath;
        }

        /// Returns an optional title for the item, or null.
        ///
        /// #### Returns
        ///
        /// the title, or null
        public String getTitle() {
            return title;
        }
    }

    /// Builder for `SharedContent`. Used by the platform ports.
    public static final class Builder {
        private String subject;
        private final List<Item> items = new ArrayList<Item>();

        /// Sets the subject.
        ///
        /// #### Parameters
        ///
        /// - `s`: the subject
        ///
        /// #### Returns
        ///
        /// this builder for chaining
        public Builder subject(String s) {
            this.subject = s;
            return this;
        }

        /// Adds a text item.
        ///
        /// #### Parameters
        ///
        /// - `text`: the text
        ///
        /// #### Returns
        ///
        /// this builder for chaining
        public Builder addText(String text) {
            items.add(new Item(TYPE_TEXT, "text/plain", text, null, null));
            return this;
        }

        /// Adds a URL item.
        ///
        /// #### Parameters
        ///
        /// - `url`: the URL
        ///
        /// #### Returns
        ///
        /// this builder for chaining
        public Builder addUrl(String url) {
            items.add(new Item(TYPE_URL, "text/uri-list", url, null, null));
            return this;
        }

        /// Adds a file item.
        ///
        /// #### Parameters
        ///
        /// - `mime`: the MIME type, or null
        ///
        /// - `cn1Path`: the `FileSystemStorage` path of the copied file
        ///
        /// - `title`: an optional title, or null
        ///
        /// #### Returns
        ///
        /// this builder for chaining
        public Builder addFile(String mime, String cn1Path, String title) {
            items.add(new Item(TYPE_FILE, mime, null, cn1Path, title));
            return this;
        }

        /// Adds an image item.
        ///
        /// #### Parameters
        ///
        /// - `mime`: the MIME type, or null
        ///
        /// - `cn1Path`: the `FileSystemStorage` path of the copied image
        ///
        /// - `title`: an optional title, or null
        ///
        /// #### Returns
        ///
        /// this builder for chaining
        public Builder addImage(String mime, String cn1Path, String title) {
            items.add(new Item(TYPE_IMAGE, mime, null, cn1Path, title));
            return this;
        }

        /// Builds the immutable shared content.
        ///
        /// #### Returns
        ///
        /// the shared content
        public SharedContent build() {
            return new SharedContent(subject, items.toArray(new Item[items.size()]));
        }
    }
}
