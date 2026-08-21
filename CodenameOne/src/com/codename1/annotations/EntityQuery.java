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

/// Marks a `public static` method as one of an [IntentEntity]'s lookups.
///
/// These are the methods the platform calls **on its own**, before your handler
/// runs, to resolve or disambiguate an entity-typed parameter. They can be
/// invoked while your app has no UI, so treat them as data lookups: no `Form`,
/// no `Dialog`, and quick enough that a user waiting on a picker does not
/// notice.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface EntityQuery {

    /// Which lookup this method provides. Required.
    Kind value();

    /// The three lookups an entity type can offer.
    enum Kind {
        /// `static T byId(String id)` -- resolve one instance from its
        /// [EntityId]. Mandatory for every entity type, because an entity
        /// crosses the platform boundary as its id and nothing else.
        BY_ID,

        /// `static List&lt;T&gt; suggested()` -- the instances offered when the user
        /// is asked to choose. This is what fills the platform's picker, so
        /// return the handful a person would plausibly want, not the whole
        /// table.
        SUGGESTED,

        /// `static List&lt;T&gt; matching(String query)` -- free-text lookup as the
        /// user types or speaks. Without it, a user can only pick from
        /// [#SUGGESTED].
        SEARCH
    }
}
