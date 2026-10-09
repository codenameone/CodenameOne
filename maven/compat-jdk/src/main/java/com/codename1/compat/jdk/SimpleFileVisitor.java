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
package com.codename1.compat.jdk;

import java.io.IOException;

/// `java.nio.file.SimpleFileVisitor` for the Codename One runtime: a
/// [FileVisitor] that visits everything and rethrows what failed.
public class SimpleFileVisitor<T> implements FileVisitor<T> {

    protected SimpleFileVisitor() {
    }

    @Override
    public FileVisitResult preVisitDirectory(T dir, BasicFileAttributes attrs) throws IOException {
        if (dir == null || attrs == null) {
            throw new NullPointerException();
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(T file, BasicFileAttributes attrs) throws IOException {
        if (file == null || attrs == null) {
            throw new NullPointerException();
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(T file, IOException exc) throws IOException {
        if (file == null) {
            throw new NullPointerException();
        }
        throw exc;
    }

    @Override
    public FileVisitResult postVisitDirectory(T dir, IOException exc) throws IOException {
        if (dir == null) {
            throw new NullPointerException();
        }
        if (exc != null) {
            throw exc;
        }
        return FileVisitResult.CONTINUE;
    }
}
