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
package com.codename1.backend;

/// The module's class path resources, compiled into a packaged server.
///
/// A placeholder with the shape the packager's generated class has: the
/// backend packager compiles a replacement over it holding every file of
/// `src/main/resources` (except the migrations, which are compiled into code
/// of their own) under its path from the resource root, Base64 encoded in
/// chunks that fit a class file's string constants. [ClassLibraryHost]
/// answers `getResourceAsStream` from it. On the JVM the JDK reads the class
/// path and nothing calls this class.
final class EmbeddedResources {
    private EmbeddedResources() {
    }

    /// The Base64 chunks of the resource at `path`, or null.
    static String[] chunks(String path) {
        return null;
    }
}
