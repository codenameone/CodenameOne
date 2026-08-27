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
package com.codename1.build.shared;

/**
 * The kind of value a build hint carries.
 *
 * <p>This is the single source of truth for hint typing. Three other
 * vocabularies used to describe the same thing and drifted apart from each
 * other; they are now derived from this one via
 * {@link BuildHints#settingsType(HintType)} and
 * {@link BuildHints#editorWidget(HintType)}.</p>
 */
public enum HintType {
    /** {@code "true"} or {@code "false"}. Maps to a Java {@code boolean}. */
    BOOLEAN,
    /** A decimal integer. Maps to a Java {@code int}. */
    INT,
    /** Free text on a single line. */
    STRING,
    /** Free text that is expected to span lines. Same Java type as STRING. */
    TEXT_BLOCK,
    /** A delimited list. Maps to {@code String[]}; requires a separator. */
    STRING_LIST,
    /** A closed set of values. Maps to a generated Java enum. */
    ENUM,
    /** An XML fragment spliced into a manifest or plist. */
    XML,
    /** A filesystem path. */
    PATH,
    /** An absolute URL. */
    URL,
    /** A dotted version number. */
    VERSION,
    /** A credential. Never echoed in logs or diagnostics. */
    SECRET
}
