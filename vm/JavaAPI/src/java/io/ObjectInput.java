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

/// Reads objects as well as primitive values. Codename One has no object
/// serialization of its own; this type exists so code written against it
/// compiles, and an implementation supplies the reading.
public interface ObjectInput extends DataInput, AutoCloseable {

    /// Reads the next object.
    Object readObject() throws ClassNotFoundException, IOException;

    /// Reads one byte, or -1 at the end of the input.
    int read() throws IOException;

    /// Reads into `b`, returning how many bytes were read or -1 at the end.
    int read(byte[] b) throws IOException;

    /// Reads into `b` from `off`, returning how many bytes were read or -1
    /// at the end.
    int read(byte[] b, int off, int len) throws IOException;

    /// Skips up to `n` bytes, returning how many were skipped.
    long skip(long n) throws IOException;

    /// How many bytes can be read without blocking.
    int available() throws IOException;

    /// Closes the input.
    @Override
    void close() throws IOException;
}
