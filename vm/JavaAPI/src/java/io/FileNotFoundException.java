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

package java.io;

/**
 * Signals that an attempt to open the file denoted by a specified pathname
 * has failed. Thrown by FileInputStream and similar constructors and by
 * APIs that wrap them when the requested file does not exist, is a
 * directory rather than a regular file, or is otherwise inaccessible.
 *
 * See: java.io.FileInputStream, java.io.IOException
 */
public class FileNotFoundException extends java.io.IOException {
    /**
     * Constructs a FileNotFoundException with null as its error detail message.
     */
    public FileNotFoundException() {
    }

    /**
     * Constructs a FileNotFoundException with the specified detail message.
     * @param s the detail message
     */
    public FileNotFoundException(String s) {
        super(s);
    }
}
