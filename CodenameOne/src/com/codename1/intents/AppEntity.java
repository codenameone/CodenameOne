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
package com.codename1.intents;

import com.codename1.ui.EncodedImage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// The platform-facing view of one of your app's nouns -- an order, a playlist,
/// a saved route.
///
/// This is what an entity looks like *after* it has left your code. Your own
/// class stays your own class; the build reads its `EntityId` / `EntityTitle` /
/// `EntitySubtitle` / `EntityImage` members and generates the adapter that
/// produces one of these. You construct `AppEntity` directly only when indexing
/// content that has no annotated class behind it.
///
/// The field set is small because it is the intersection of what every platform
/// entity display model actually has: something to identify it by, something to
/// show, something to show underneath, a picture, and words to match a search
/// against.
///
/// #### The id is a promise
///
/// Ids outlive the process. Spotlight keeps indexed items and the system keeps
/// donated shortcuts, both of them holding your id, so an id has to mean the
/// same thing after an app update as it did before. An id derived from a list
/// position or a content hash will silently start resolving to the wrong object
/// and nothing will report an error.
public final class AppEntity {

    private final String type;
    private final String id;
    private String title;
    private String subtitle;
    private EncodedImage image;
    private final List<String> keywords = new ArrayList<String>();

    /// Creates an entity of a declared type.
    ///
    /// #### Parameters
    ///
    /// - `type`: the entity type id, matching an `IntentEntity` declaration. It may not contain
    ///   `:`, which separates the type from the id in the identifier the platforms store.
    /// - `id`: the stable identifier of this instance. Colons are fine here: the uid splits at
    ///   the first one, so everything after it is the id.
    public AppEntity(String type, String id) {
        checkType(type);
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("id is required");
        }
        this.type = type;
        this.id = id;
    }

    /// Enforces the one rule an entity type has to obey, in one place.
    ///
    /// Shared with the string-keyed paths on [Intents] -- removal and clearing take a type
    /// without ever constructing an entity, so a check that lived only in this constructor
    /// guarded half the surface. Package-private because those callers are its only users.
    static void checkType(String type) {
        if (type == null || type.length() == 0) {
            throw new IllegalArgumentException("type is required");
        }
        if (type.indexOf(':') >= 0) {
            // A platform index stores one opaque identifier per entry and hands that same
            // string back on a tap, so the type travels inside it as "type:id" and is split at
            // the first colon. A colon in the type moves that boundary: new AppEntity(
            // "shop:order", "42") comes back as type "shop", id "order:42", and the application
            // cannot recognise the entity it published. The build enforces the same shape on a
            // declared @IntentEntity; this is the path that skips the build.
            throw new IllegalArgumentException("type may not contain ':': \"" + type
                    + "\". The character separates the type from the id in the identifier the "
                    + "platforms store, so an entity declaring one cannot be resolved when the "
                    + "user taps it.");
        }
    }

    /// The entity type id.
    public String getType() {
        return type;
    }

    /// The stable identifier of this instance.
    public String getId() {
        return id;
    }

    /// The primary line shown by a picker or a search result.
    public String getTitle() {
        return title;
    }

    /// Sets the primary line. Required for anything indexed -- an entity with no
    /// title has nothing to show in a search result.
    ///
    /// #### Parameters
    ///
    /// - `title`: the display title
    ///
    /// #### Returns
    ///
    /// this entity, for chaining
    public AppEntity setTitle(String title) {
        this.title = title;
        return this;
    }

    /// The secondary line, or null.
    public String getSubtitle() {
        return subtitle;
    }

    /// Sets the secondary line.
    ///
    /// #### Parameters
    ///
    /// - `subtitle`: the display subtitle
    ///
    /// #### Returns
    ///
    /// this entity, for chaining
    public AppEntity setSubtitle(String subtitle) {
        this.subtitle = subtitle;
        return this;
    }

    /// The thumbnail, or null.
    public EncodedImage getImage() {
        return image;
    }

    /// Sets the thumbnail.
    ///
    /// An `EncodedImage` rather than an `Image` because the bytes cross to the
    /// platform as-is. Handing over an image that still has to be rasterized
    /// would mean encoding it here, which on a device is exactly the kind of
    /// work that looks instantaneous in the simulator and stalls on hardware.
    ///
    /// #### Parameters
    ///
    /// - `image`: the thumbnail
    ///
    /// #### Returns
    ///
    /// this entity, for chaining
    public AppEntity setImage(EncodedImage image) {
        this.image = image;
        return this;
    }

    /// Extra words a search should match this entity on, beyond its title and
    /// subtitle.
    ///
    /// #### Parameters
    ///
    /// - `words`: the keywords to add; null is ignored
    ///
    /// #### Returns
    ///
    /// this entity, for chaining
    public AppEntity addKeywords(String... words) {
        if (words != null) {
            for (String w : words) {
                if (w != null && w.length() > 0) {
                    keywords.add(w);
                }
            }
        }
        return this;
    }

    /// The keywords added so far.
    public List<String> getKeywords() {
        return Collections.unmodifiableList(keywords);
    }

    @Override
    public String toString() {
        return type + ":" + id;
    }
}
