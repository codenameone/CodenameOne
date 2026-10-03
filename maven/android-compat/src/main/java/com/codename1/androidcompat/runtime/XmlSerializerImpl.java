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
package com.codename1.androidcompat.runtime;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

/// The runtime's XmlSerializer: `Xml.newSerializer()`. Writes well-formed
/// XML with escaped text and attributes. A namespace is declared on the
/// element that first needs it -- through `setPrefix` before `startTag`, or
/// with a generated `n0`, `n1`... prefix -- and goes out of scope with it.
/// The indent feature (`http://xmlpull.org/v1/doc/features.html#indent-output`)
/// puts each element on its own line unless it holds text.
public class XmlSerializerImpl implements XmlSerializer {

    private static final String FEATURE_INDENT = "http://xmlpull.org/v1/doc/features.html#indent-output";

    /// One open element.
    private static final class Element {
        final String namespace;
        final String prefix;
        final String name;
        /// Index in `scope` of this element's first declaration.
        final int scopeStart;
        boolean hasChildElements;
        boolean hasText;

        Element(String namespace, String prefix, String name, int scopeStart) {
            this.namespace = namespace;
            this.prefix = prefix;
            this.name = name;
            this.scopeStart = scopeStart;
        }
    }

    private Writer writer;
    private String encoding;
    private boolean asciiOnly;
    private boolean indent;
    private int generated;
    private boolean startTagOpen;
    private boolean wroteAnything;
    private final ArrayList<Element> open = new ArrayList<Element>();
    /// Declarations in scope: prefix, uri pairs, innermost last.
    private final ArrayList<String[]> scope = new ArrayList<String[]>();
    /// Declarations made by setPrefix for the next start tag.
    private final ArrayList<String[]> nextDeclarations = new ArrayList<String[]>();
    /// Declarations the open start tag still has to write.
    private final ArrayList<String[]> unwritten = new ArrayList<String[]>();

    @Override
    public void setFeature(String name, boolean value) {
        if (FEATURE_INDENT.equals(name)) {
            indent = value;
        } else {
            throw new IllegalArgumentException("unsupported feature: " + name);
        }
    }

    @Override
    public boolean getFeature(String name) {
        return FEATURE_INDENT.equals(name) && indent;
    }

    @Override
    public void setProperty(String name, Object value) {
        throw new IllegalArgumentException("unsupported property: " + name);
    }

    @Override
    public Object getProperty(String name) {
        return null;
    }

    @Override
    public void setOutput(OutputStream os, String enc) throws IOException {
        if (os == null) {
            throw new IllegalArgumentException("os == null");
        }
        setOutput(new OutputStreamWriter(os, enc == null ? "UTF-8" : enc));
        setEncoding(enc);
    }

    @Override
    public void setOutput(Writer w) {
        writer = w;
        open.clear();
        scope.clear();
        nextDeclarations.clear();
        unwritten.clear();
        startTagOpen = false;
        wroteAnything = false;
        generated = 0;
        setEncoding(null);
    }

    private void setEncoding(String enc) {
        encoding = enc;
        // Encoding names are ASCII; compare them without case folding.
        asciiOnly = enc != null && !enc.regionMatches(true, 0, "UTF", 0, 3);
    }

    @Override
    public void startDocument(String enc, Boolean standalone) throws IOException {
        if (enc != null) {
            setEncoding(enc);
        }
        writer.write("<?xml version='1.0' ");
        if (encoding != null) {
            writer.write("encoding='");
            writer.write(encoding);
            writer.write("' ");
        }
        if (standalone != null) {
            writer.write("standalone='");
            writer.write(standalone.booleanValue() ? "yes" : "no");
            writer.write("' ");
        }
        writer.write("?>");
        wroteAnything = true;
    }

    @Override
    public void endDocument() throws IOException {
        while (!open.isEmpty()) {
            Element e = open.get(open.size() - 1);
            endTag(e.namespace, e.name);
        }
        flush();
    }

    @Override
    public void setPrefix(String prefix, String namespace) throws IOException {
        closeStartTag(false);
        nextDeclarations.add(new String[] {prefix == null ? "" : prefix, namespace == null ? "" : namespace});
    }

    /// The URI `prefix` is bound to at this point, or null.
    private String uriOf(String prefix) {
        for (int i = scope.size() - 1; i >= 0; i--) {
            if (scope.get(i)[0].equals(prefix)) {
                return scope.get(i)[1];
            }
        }
        return null;
    }

    /// A prefix bound to `namespace` and not shadowed since, or null.
    private String boundPrefix(String namespace, boolean allowDefault) {
        for (int i = scope.size() - 1; i >= 0; i--) {
            String[] d = scope.get(i);
            if (d[1].equals(namespace) && (allowDefault || d[0].length() > 0) && namespace.equals(uriOf(d[0]))) {
                return d[0];
            }
        }
        return null;
    }

    private String declare(String namespace) {
        String prefix;
        do {
            prefix = "n" + (generated++);
        } while (uriOf(prefix) != null);
        String[] d = {prefix, namespace};
        scope.add(d);
        unwritten.add(d);
        return prefix;
    }

    @Override
    public String getPrefix(String namespace, boolean generatePrefix) {
        String p = boundPrefix(namespace, true);
        if (p == null && generatePrefix) {
            p = "n" + (generated++);
            nextDeclarations.add(new String[] {p, namespace});
        }
        return p;
    }

    @Override
    public int getDepth() {
        return open.size();
    }

    @Override
    public String getNamespace() {
        return open.isEmpty() ? null : open.get(open.size() - 1).namespace;
    }

    @Override
    public String getName() {
        return open.isEmpty() ? null : open.get(open.size() - 1).name;
    }

    private void closeStartTag(boolean empty) throws IOException {
        if (!startTagOpen) {
            return;
        }
        startTagOpen = false;
        for (String[] d : unwritten) {
            writer.write(" xmlns");
            if (d[0].length() > 0) {
                writer.write(':');
                writer.write(d[0]);
            }
            writer.write("=\"");
            writeEscaped(d[1], '"');
            writer.write('"');
        }
        unwritten.clear();
        writer.write(empty ? " />" : ">");
    }

    private void newLine(int depth) throws IOException {
        if (wroteAnything) {
            writer.write('\n');
        }
        for (int i = 0; i < depth; i++) {
            writer.write("  ");
        }
    }

    @Override
    public XmlSerializer startTag(String namespace, String name) throws IOException {
        closeStartTag(false);
        if (!open.isEmpty()) {
            open.get(open.size() - 1).hasChildElements = true;
        }
        if (indent && (open.isEmpty() || !open.get(open.size() - 1).hasText)) {
            newLine(open.size());
        }
        int scopeStart = scope.size();
        for (String[] d : nextDeclarations) {
            scope.add(d);
            unwritten.add(d);
        }
        nextDeclarations.clear();
        String prefix = null;
        if (namespace != null) {
            if (namespace.length() == 0) {
                String def = uriOf("");
                if (def != null && def.length() > 0) {
                    String[] d = {"", ""};
                    scope.add(d);
                    unwritten.add(d);
                }
                prefix = "";
            } else {
                prefix = boundPrefix(namespace, true);
                if (prefix == null) {
                    prefix = declare(namespace);
                }
            }
        }
        open.add(new Element(namespace, prefix, name, scopeStart));
        writer.write('<');
        if (prefix != null && prefix.length() > 0) {
            writer.write(prefix);
            writer.write(':');
        }
        writer.write(name);
        startTagOpen = true;
        wroteAnything = true;
        return this;
    }

    @Override
    public XmlSerializer attribute(String namespace, String name, String value) throws IOException {
        if (!startTagOpen) {
            throw new IllegalStateException("illegal position for attribute");
        }
        String prefix = "";
        if (namespace != null && namespace.length() > 0) {
            prefix = boundPrefix(namespace, false);
            if (prefix == null) {
                prefix = declare(namespace);
            }
        }
        writer.write(' ');
        if (prefix.length() > 0) {
            writer.write(prefix);
            writer.write(':');
        }
        writer.write(name);
        writer.write("=\"");
        writeEscaped(value, '"');
        writer.write('"');
        return this;
    }

    @Override
    public void flush() throws IOException {
        closeStartTag(false);
        writer.flush();
    }

    @Override
    public XmlSerializer endTag(String namespace, String name) throws IOException {
        if (open.isEmpty()) {
            throw new IllegalArgumentException("</" + name + "> without a start tag");
        }
        Element e = open.get(open.size() - 1);
        boolean nsMatch = namespace == null ? e.namespace == null : namespace.equals(e.namespace);
        if (!nsMatch || !e.name.equals(name)) {
            throw new IllegalArgumentException("</{" + namespace + "}" + name + "> does not match start tag <{"
                    + e.namespace + "}" + e.name + ">");
        }
        if (startTagOpen) {
            closeStartTag(true);
        } else {
            if (indent && e.hasChildElements && !e.hasText) {
                newLine(open.size() - 1);
            }
            writer.write("</");
            if (e.prefix != null && e.prefix.length() > 0) {
                writer.write(e.prefix);
                writer.write(':');
            }
            writer.write(name);
            writer.write('>');
        }
        while (scope.size() > e.scopeStart) {
            scope.remove(scope.size() - 1);
        }
        open.remove(open.size() - 1);
        return this;
    }

    private void markText() {
        if (!open.isEmpty()) {
            open.get(open.size() - 1).hasText = true;
        }
    }

    @Override
    public XmlSerializer text(String text) throws IOException {
        closeStartTag(false);
        markText();
        writeEscaped(text, (char) 0);
        wroteAnything = true;
        return this;
    }

    @Override
    public XmlSerializer text(char[] text, int start, int len) throws IOException {
        return text(new String(text, start, len));
    }

    @Override
    public void cdsect(String data) throws IOException {
        closeStartTag(false);
        markText();
        StringBuilder b = new StringBuilder();
        String rest = data;
        int i;
        while ((i = rest.indexOf("]]>")) >= 0) {
            b.append(rest.substring(0, i + 2)).append("]]><![CDATA[");
            rest = rest.substring(i + 2);
        }
        b.append(rest);
        writer.write("<![CDATA[");
        writer.write(b.toString());
        writer.write("]]>");
    }

    @Override
    public void comment(String comment) throws IOException {
        closeStartTag(false);
        writer.write("<!--");
        writer.write(comment);
        writer.write("-->");
        wroteAnything = true;
    }

    @Override
    public void docdecl(String dd) throws IOException {
        writer.write("<!DOCTYPE");
        writer.write(dd);
        writer.write(">");
        wroteAnything = true;
    }

    @Override
    public void entityRef(String name) throws IOException {
        closeStartTag(false);
        markText();
        writer.write('&');
        writer.write(name);
        writer.write(';');
    }

    @Override
    public void ignorableWhitespace(String s) throws IOException {
        closeStartTag(false);
        writer.write(s);
    }

    @Override
    public void processingInstruction(String pi) throws IOException {
        closeStartTag(false);
        writer.write("<?");
        writer.write(pi);
        writer.write("?>");
        wroteAnything = true;
    }

    private void writeEscaped(String s, char quot) throws IOException {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\n':
                case '\r':
                case '\t':
                    if (quot == 0) {
                        writer.write(c);
                    } else {
                        writer.write("&#" + ((int) c) + ';');
                    }
                    break;
                case '&':
                    writer.write("&amp;");
                    break;
                case '>':
                    writer.write("&gt;");
                    break;
                case '<':
                    writer.write("&lt;");
                    break;
                case '"':
                    writer.write(quot == '"' ? "&quot;" : "\"");
                    break;
                default:
                    if (c < 0x20 || (asciiOnly && c >= 0x7f)) {
                        writer.write("&#" + ((int) c) + ";");
                    } else {
                        writer.write(c);
                    }
                    break;
            }
        }
    }
}
