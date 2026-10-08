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
package com.codename1.desktopcompat.java.beans;

import com.codename1.compat.jdk.LinkOnly;

import java.io.InputStream;

/// Reads the beans an [XMLEncoder] wrote.
///
/// Long-term persistence writes an object as the calls that rebuild it and
/// finds those calls by reflection, so nothing of it can run on a device. The
/// class is [LinkOnly]: it exists for the libraries that offer to save
/// themselves this way beside everything else they do, and an application's
/// own use of it is reported at build time.
///
/// [#readObject()] throws `UnsupportedOperationException`; closing it leaves
/// the stream alone, since nothing was read from it.
@LinkOnly
public class XMLDecoder implements AutoCloseable {

    private ExceptionListener listener;

    public XMLDecoder(InputStream in) {
        // The stream is never read.
    }

    public Object readObject() {
        throw new UnsupportedOperationException("java.beans.XMLDecoder needs reflection to read an object");
    }

    public void setExceptionListener(ExceptionListener exceptionListener) {
        this.listener = exceptionListener;
    }

    public ExceptionListener getExceptionListener() {
        return listener;
    }

    @Override
    public void close() {
        // Nothing was read.
    }
}
