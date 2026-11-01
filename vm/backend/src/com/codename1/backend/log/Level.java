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
package com.codename1.backend.log;

/// The severity of a log line, lowest first. A logger set to a level writes
/// that level and every one above it.
public enum Level {
    TRACE, DEBUG, INFO, WARN, ERROR, OFF;

    /// The level a configuration value names, ignoring case, or null for one this
    /// does not know. Compared without `toUpperCase`, which is locale sensitive:
    /// on a Turkish host "info" would not fold to "INFO".
    public static Level parse(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        for (Level level : values()) {
            if (level.name().equalsIgnoreCase(v)) {
                return level;
            }
        }
        // Spring Boot also accepts these two names for the extremes.
        if ("FATAL".equalsIgnoreCase(v)) {
            return ERROR;
        }
        if ("ALL".equalsIgnoreCase(v)) {
            return TRACE;
        }
        return null;
    }
}
