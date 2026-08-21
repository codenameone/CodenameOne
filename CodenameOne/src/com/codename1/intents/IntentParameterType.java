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

/// The parameter kinds an intent can accept.
///
/// The set is closed on purpose. Every entry has to survive being spoken aloud,
/// typed into the Shortcuts app, filled by a language model, and carried across
/// a platform boundary as text -- so it is the intersection of what iOS, Android
/// and a JSON payload can all express, not the union.
public enum IntentParameterType {
    /// Free text, or a closed vocabulary when the declaration lists options.
    STRING,

    /// A whole number. Maps to Java `int` and `long`.
    INTEGER,

    /// A fractional number. Maps to Java `float` and `double`.
    NUMBER,

    /// A yes/no value the platform can ask for directly.
    BOOLEAN,

    /// An instant. Crosses the boundary as epoch milliseconds so no locale or
    /// timezone parsing sits in the wire format.
    DATE,

    /// An app-defined noun declared with `com.codename1.annotations.IntentEntity`.
    /// Crosses the boundary as the entity's id string and is resolved back to an
    /// object by the generated `BY_ID` query. This is the type that lets the
    /// platform run its own picker before the handler is ever called.
    ENTITY
}
