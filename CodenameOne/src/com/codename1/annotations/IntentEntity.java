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
package com.codename1.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks one of your application's own nouns -- an order, a playlist, a saved
/// route -- as something the system can name, list, search and hand back to an
/// [AppIntent].
///
/// ```java
/// @IntentEntity(value = "playlist", title = "Playlist")
/// public class Playlist {
///     @EntityId    public String getId()   { return id; }
///     @EntityTitle public String getName() { return name; }
///
///     @EntityQuery(EntityQuery.Kind.BY_ID)
///     public static Playlist byId(String id) { return Library.playlist(id); }
///
///     @EntityQuery(EntityQuery.Kind.SUGGESTED)
///     public static List&lt;Playlist&gt; recent() { return Library.recentPlaylists(); }
/// }
/// ```
///
/// Your class stays your class. There is no interface to implement and no base
/// class to extend, because a domain model outlives the framework it is used
/// with; the build reads the annotated members and generates the adapter.
///
/// #### Why this is what makes intents feel intelligent
///
/// An entity-typed parameter is what lets the platform run its **own** picker
/// before your code is ever called. "Play a playlist" with nothing specified
/// makes the system ask "Which one?", fill the list from your `SUGGESTED` query,
/// and hand your handler the chosen object. A plain `String` parameter can never
/// do that -- it can only be typed or spoken verbatim.
///
/// A `BY_ID` query is mandatory. Entities cross the platform boundary as their
/// id and nothing else, so resolving that id back to an object is the one
/// operation the framework cannot do without.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface IntentEntity {

    /// The entity type id, matching `[a-z][a-z0-9_]{2,63}`. Required, and stable
    /// for the same reason an intent id is: indexed items persist it.
    String value();

    /// The human-readable type name shown by pickers. Defaults to the id.
    String title() default "";

    /// True to make instances of this type eligible for device search through
    /// `com.codename1.intents.Intents#index`. Requires an [EntityTitle], since
    /// an entry with nothing to display is not a search result.
    boolean indexed() default false;
}
