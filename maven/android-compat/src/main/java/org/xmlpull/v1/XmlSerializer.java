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
package org.xmlpull.v1;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/// Writes XML through a sequence of calls mirroring the pull parser's
/// events.
public interface XmlSerializer {

    void setFeature(String name, boolean state) throws IllegalArgumentException, IllegalStateException;

    boolean getFeature(String name);

    void setProperty(String name, Object value) throws IllegalArgumentException, IllegalStateException;

    Object getProperty(String name);

    void setOutput(OutputStream os, String encoding) throws IOException, IllegalArgumentException,
            IllegalStateException;

    void setOutput(Writer writer) throws IOException, IllegalArgumentException, IllegalStateException;

    void startDocument(String encoding, Boolean standalone) throws IOException, IllegalArgumentException,
            IllegalStateException;

    void endDocument() throws IOException, IllegalArgumentException, IllegalStateException;

    void setPrefix(String prefix, String namespace) throws IOException, IllegalArgumentException,
            IllegalStateException;

    String getPrefix(String namespace, boolean generatePrefix) throws IllegalArgumentException;

    int getDepth();

    String getNamespace();

    String getName();

    XmlSerializer startTag(String namespace, String name) throws IOException, IllegalArgumentException,
            IllegalStateException;

    XmlSerializer attribute(String namespace, String name, String value) throws IOException,
            IllegalArgumentException, IllegalStateException;

    XmlSerializer endTag(String namespace, String name) throws IOException, IllegalArgumentException,
            IllegalStateException;

    XmlSerializer text(String text) throws IOException, IllegalArgumentException, IllegalStateException;

    XmlSerializer text(char[] buf, int start, int len) throws IOException, IllegalArgumentException,
            IllegalStateException;

    void cdsect(String text) throws IOException, IllegalArgumentException, IllegalStateException;

    void entityRef(String text) throws IOException, IllegalArgumentException, IllegalStateException;

    void processingInstruction(String text) throws IOException, IllegalArgumentException, IllegalStateException;

    void comment(String text) throws IOException, IllegalArgumentException, IllegalStateException;

    void docdecl(String text) throws IOException, IllegalArgumentException, IllegalStateException;

    void ignorableWhitespace(String text) throws IOException, IllegalArgumentException, IllegalStateException;

    void flush() throws IOException;
}
