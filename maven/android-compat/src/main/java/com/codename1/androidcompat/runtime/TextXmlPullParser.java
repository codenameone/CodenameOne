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

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

/// The runtime's XmlPull parser for XML text: what `Xml.newPullParser()`
/// and `XmlPullParserFactory` return. It streams from its reader, resolves
/// namespaces when asked to, expands the predefined, numeric and defined
/// entities, coalesces text, CDATA and references in `next()`, and reports
/// every construct separately in `nextToken()`. A DOCTYPE is skipped, not
/// processed, as Android's parser does by default.
public class TextXmlPullParser implements XmlPullParser {

    public static final String FEATURE_RELAXED = "http://xmlpull.org/v1/doc/features.html#relaxed";
    private static final String XML_URI = "http://www.w3.org/XML/1998/namespace";
    private static final String XMLNS_URI = "http://www.w3.org/2000/xmlns/";

    private Reader reader;
    private final char[] buf = new char[8192];
    private int bufPos;
    private int bufLen;
    private int line = 1;
    private int column;
    private int peeked = -2;

    private boolean processNsp;
    private boolean reportNsAttrs;
    private boolean relaxed;
    private String encoding;
    private HashMap<String, String> entityMap;

    private int depth;
    private String[] elementStack = new String[64];
    private String[] nspStack = new String[16];
    private int[] nspCounts = new int[8];

    private int type = START_DOCUMENT;
    private String name;
    private String prefix;
    private String namespace;
    private String text;
    private boolean degenerated;
    private boolean pendingEnd;
    private int attributeCount = -1;
    private String[] attributes = new String[32];
    private final StringBuilder sb = new StringBuilder();

    // ------------------------------------------------------------ setup

    @Override
    public void setFeature(String feature, boolean value) throws XmlPullParserException {
        if (FEATURE_PROCESS_NAMESPACES.equals(feature)) {
            processNsp = value;
        } else if (FEATURE_REPORT_NAMESPACE_ATTRIBUTES.equals(feature)) {
            reportNsAttrs = value;
        } else if (FEATURE_RELAXED.equals(feature)) {
            relaxed = value;
        } else if (FEATURE_PROCESS_DOCDECL.equals(feature) || FEATURE_VALIDATION.equals(feature)) {
            if (value) {
                throw new XmlPullParserException("unsupported feature: " + feature);
            }
        } else {
            throw new XmlPullParserException("unsupported feature: " + feature);
        }
    }

    @Override
    public boolean getFeature(String feature) {
        if (FEATURE_PROCESS_NAMESPACES.equals(feature)) {
            return processNsp;
        }
        if (FEATURE_REPORT_NAMESPACE_ATTRIBUTES.equals(feature)) {
            return reportNsAttrs;
        }
        if (FEATURE_RELAXED.equals(feature)) {
            return relaxed;
        }
        return false;
    }

    @Override
    public void setProperty(String property, Object value) throws XmlPullParserException {
        throw new XmlPullParserException("unsupported property: " + property);
    }

    @Override
    public Object getProperty(String property) {
        return null;
    }

    @Override
    public void setInput(Reader in) throws XmlPullParserException {
        reader = in;
        bufPos = 0;
        bufLen = 0;
        line = 1;
        column = 0;
        peeked = -2;
        depth = 0;
        nspCounts[0] = 0;
        type = START_DOCUMENT;
        name = null;
        prefix = null;
        namespace = null;
        text = null;
        degenerated = false;
        pendingEnd = false;
        attributeCount = -1;
        entityMap = null;
    }

    @Override
    public void setInput(InputStream is, String inputEncoding) throws XmlPullParserException {
        if (is == null) {
            throw new IllegalArgumentException("inputStream must not be null");
        }
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int n;
            while ((n = is.read(chunk)) > 0) {
                bos.write(chunk, 0, n);
            }
            byte[] data = bos.toByteArray();
            int skip = 0;
            String enc = inputEncoding;
            if (data.length >= 3 && (data[0] & 0xff) == 0xef && (data[1] & 0xff) == 0xbb && (data[2] & 0xff) == 0xbf) {
                skip = 3;
                if (enc == null) {
                    enc = "UTF-8";
                }
            } else if (data.length >= 2 && (data[0] & 0xff) == 0xfe && (data[1] & 0xff) == 0xff) {
                skip = 2;
                if (enc == null) {
                    enc = "UTF-16BE";
                }
            } else if (data.length >= 2 && (data[0] & 0xff) == 0xff && (data[1] & 0xff) == 0xfe) {
                skip = 2;
                if (enc == null) {
                    enc = "UTF-16LE";
                }
            }
            if (enc == null) {
                enc = declaredEncoding(data);
            }
            String s = new String(data, skip, data.length - skip, enc);
            setInput(new StringReader(s));
            encoding = enc;
        } catch (UnsupportedEncodingException e) {
            throw new XmlPullParserException("unsupported encoding " + inputEncoding, this, e);
        } catch (IOException e) {
            throw new XmlPullParserException("cannot read the input", this, e);
        }
    }

    /// The `encoding` of an XML declaration, read as ASCII; UTF-8 when absent.
    private static String declaredEncoding(byte[] data) {
        int end = Math.min(data.length, 200);
        StringBuilder head = new StringBuilder();
        for (int i = 0; i < end; i++) {
            char c = (char) (data[i] & 0xff);
            head.append(c);
            if (c == '>') {
                break;
            }
        }
        String h = head.toString();
        if (!h.startsWith("<?xml")) {
            return "UTF-8";
        }
        int i = h.indexOf("encoding");
        if (i < 0) {
            return "UTF-8";
        }
        int q = i + 8;
        while (q < h.length() && h.charAt(q) != '"' && h.charAt(q) != '\'') {
            q++;
        }
        if (q >= h.length()) {
            return "UTF-8";
        }
        char quote = h.charAt(q);
        int close = h.indexOf(quote, q + 1);
        return close < 0 ? "UTF-8" : h.substring(q + 1, close);
    }

    @Override
    public String getInputEncoding() {
        return encoding;
    }

    @Override
    public void defineEntityReplacementText(String entityName, String replacementText) throws XmlPullParserException {
        if (entityMap == null) {
            entityMap = new HashMap<String, String>();
        }
        entityMap.put(entityName, replacementText);
    }

    // ------------------------------------------------------------ characters

    private int read() throws IOException {
        int c;
        if (peeked != -2) {
            c = peeked;
            peeked = -2;
        } else {
            c = rawRead();
        }
        if (c == '\r') {
            // CR LF and a lone CR both read as LF.
            if (peekRaw() == '\n') {
                peeked = -2;
            }
            c = '\n';
        }
        if (c == '\n') {
            line++;
            column = 0;
        } else if (c != -1) {
            column++;
        }
        return c;
    }

    private int peekRaw() throws IOException {
        if (peeked == -2) {
            peeked = rawRead();
        }
        return peeked;
    }

    private int peek() throws IOException {
        int c = peekRaw();
        return c == '\r' ? '\n' : c;
    }

    private int rawRead() throws IOException {
        if (bufPos >= bufLen) {
            if (reader == null) {
                return -1;
            }
            bufLen = reader.read(buf, 0, buf.length);
            bufPos = 0;
            if (bufLen <= 0) {
                bufLen = 0;
                return -1;
            }
        }
        return buf[bufPos++];
    }

    /// Whether the input continues with `s`; consumes it when it does. Only
    /// used right after a '<', so the look-ahead stays inside the buffer.
    private boolean consume(String s) throws IOException {
        int c = peek();
        if (c != s.charAt(0)) {
            return false;
        }
        int needed = s.length() - 1;
        if (bufLen - bufPos < needed) {
            compact(needed);
        }
        if (bufLen - bufPos < needed) {
            return false;
        }
        for (int i = 1; i < s.length(); i++) {
            if (buf[bufPos + i - 1] != s.charAt(i)) {
                return false;
            }
        }
        read();
        for (int i = 1; i < s.length(); i++) {
            read();
        }
        return true;
    }

    /// Makes at least `needed` characters available after the peeked one.
    private void compact(int needed) throws IOException {
        System.arraycopy(buf, bufPos, buf, 0, bufLen - bufPos);
        bufLen -= bufPos;
        bufPos = 0;
        while (bufLen < needed && reader != null) {
            int n = reader.read(buf, bufLen, buf.length - bufLen);
            if (n <= 0) {
                break;
            }
            bufLen += n;
        }
    }

    private static boolean isWs(int c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    private void skipWs() throws IOException {
        while (isWs(peek())) {
            read();
        }
    }

    private XmlPullParserException error(String message) {
        return new XmlPullParserException(message, this, null);
    }

    private void expect(char c) throws IOException, XmlPullParserException {
        int r = read();
        if (r != c) {
            throw error("expected '" + c + "' but found " + (r == -1 ? "end of input" : "'" + (char) r + "'"));
        }
    }

    private String readName() throws IOException, XmlPullParserException {
        sb.setLength(0);
        int c = peek();
        if (c == -1 || isWs(c) || c == '>' || c == '/' || c == '=' || c == '<') {
            throw error("name expected");
        }
        while (true) {
            c = peek();
            if (c == -1 || isWs(c) || c == '>' || c == '/' || c == '=' || c == '<' || c == '"' || c == '\'') {
                break;
            }
            sb.append((char) read());
        }
        return sb.toString();
    }

    /// Appends the expansion of the reference after '&' (already read) to
    /// `out`; answers the entity name for nextToken, or null.
    private String readReference(StringBuilder out) throws IOException, XmlPullParserException {
        StringBuilder ref = new StringBuilder();
        while (true) {
            int c = read();
            if (c == ';') {
                break;
            }
            if (c == -1 || isWs(c) || c == '<' || c == '&' || ref.length() > 64) {
                if (relaxed) {
                    out.append('&').append(ref);
                    if (c != -1) {
                        out.append((char) c);
                    }
                    return null;
                }
                throw error("unterminated entity reference");
            }
            ref.append((char) c);
        }
        String r = ref.toString();
        if (r.startsWith("#")) {
            try {
                int code = r.startsWith("#x") ? Integer.parseInt(r.substring(2), 16) : Integer.parseInt(r.substring(1));
                if (code >= 0x10000) {
                    code -= 0x10000;
                    out.append((char) (0xd800 + (code >> 10)));
                    out.append((char) (0xdc00 + (code & 0x3ff)));
                } else {
                    out.append((char) code);
                }
                return r;
            } catch (NumberFormatException e) {
                throw error("invalid character reference &" + r + ";");
            }
        }
        if (r.equals("lt")) {
            out.append('<');
        } else if (r.equals("gt")) {
            out.append('>');
        } else if (r.equals("amp")) {
            out.append('&');
        } else if (r.equals("apos")) {
            out.append('\'');
        } else if (r.equals("quot")) {
            out.append('"');
        } else {
            String v = entityMap == null ? null : entityMap.get(r);
            if (v != null) {
                out.append(v);
            } else if (relaxed) {
                out.append('&').append(r).append(';');
            } else {
                throw error("unresolved entity &" + r + ";");
            }
        }
        return r;
    }

    // ------------------------------------------------------------ events

    @Override
    public int next() throws XmlPullParserException, IOException {
        return advance(false);
    }

    @Override
    public int nextToken() throws XmlPullParserException, IOException {
        return advance(true);
    }

    /// The character after the peeked one, without consuming either.
    private int peek2() throws IOException {
        peek();
        if (bufPos >= bufLen) {
            compact(1);
        }
        return bufPos < bufLen ? buf[bufPos] : -1;
    }

    private int advance(boolean tokens) throws XmlPullParserException, IOException {
        if (type == END_DOCUMENT) {
            return type;
        }
        if (pendingEnd) {
            pendingEnd = false;
            type = END_TAG;
            attributeCount = -1;
            return type;
        }
        if (type == END_TAG) {
            popElement();
        }
        attributeCount = -1;
        degenerated = false;
        text = null;
        name = null;
        prefix = null;
        namespace = null;
        StringBuilder txt = null;
        while (true) {
            int c = peek();
            if (c == -1) {
                if (txt != null) {
                    int r = finishText(txt, tokens);
                    if (r >= 0) {
                        return r;
                    }
                    txt = null;
                }
                if (depth > 0 && !relaxed) {
                    throw error("unexpected end of document");
                }
                type = END_DOCUMENT;
                return type;
            }
            if (c == '<') {
                int d = peek2();
                if (txt != null && (tokens || (d != '!' && d != '?'))) {
                    // A tag ends the text; so does any markup in nextToken().
                    int r = finishText(txt, tokens);
                    if (r >= 0) {
                        return r;
                    }
                    txt = null;
                    continue;
                }
                read();
                if (d == '/') {
                    read();
                    parseEndTag();
                    return type;
                }
                if (d == '!' && consume("![CDATA[")) {
                    String cdata = readUntil("]]>");
                    if (tokens) {
                        text = cdata;
                        type = CDSECT;
                        return type;
                    }
                    if (txt == null) {
                        txt = new StringBuilder();
                    }
                    txt.append(cdata);
                    continue;
                }
                if (d == '!' && consume("!--")) {
                    String body = readUntil("-->");
                    if (tokens) {
                        text = body;
                        type = COMMENT;
                        return type;
                    }
                    continue;
                }
                if (d == '!' && consume("!DOCTYPE")) {
                    String body = readDoctype();
                    if (tokens) {
                        text = body;
                        type = DOCDECL;
                        return type;
                    }
                    continue;
                }
                if (d == '?') {
                    read();
                    String body = readUntil("?>");
                    if (body.equals("xml") || (body.startsWith("xml") && body.length() > 3 && isWs(body.charAt(3)))) {
                        // The XML declaration is not a processing instruction.
                        continue;
                    }
                    if (tokens) {
                        text = body;
                        type = PROCESSING_INSTRUCTION;
                        return type;
                    }
                    continue;
                }
                if (d == '!') {
                    throw error("unexpected markup <!");
                }
                parseStartTag();
                return type;
            }
            if (c == '&') {
                if (tokens) {
                    if (txt != null) {
                        int r = finishText(txt, tokens);
                        if (r >= 0) {
                            return r;
                        }
                        txt = null;
                        continue;
                    }
                    read();
                    StringBuilder rep = new StringBuilder();
                    String ent = readReference(rep);
                    name = ent;
                    text = rep.toString();
                    type = ENTITY_REF;
                    return type;
                }
                read();
                if (txt == null) {
                    txt = new StringBuilder();
                }
                readReference(txt);
                continue;
            }
            if (txt == null) {
                txt = new StringBuilder();
            }
            txt.append((char) read());
        }
    }

    /// Ends a run of text: a TEXT event inside the root element; outside it,
    /// whitespace is ignorable (reported only by nextToken) and anything
    /// else is an error. Answers -1 when the run produced no event.
    private int finishText(StringBuilder txt, boolean tokens) throws XmlPullParserException {
        String t = txt.toString();
        if (depth == 0) {
            if (isAllWs(t)) {
                if (tokens) {
                    text = t;
                    type = IGNORABLE_WHITESPACE;
                    return type;
                }
                return -1;
            }
            if (!relaxed) {
                throw error("text is not allowed outside the root element");
            }
        }
        text = t;
        type = TEXT;
        return type;
    }

    private static boolean isAllWs(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!isWs(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private String readUntil(String end) throws IOException, XmlPullParserException {
        StringBuilder out = new StringBuilder();
        int matched = 0;
        while (true) {
            int c = read();
            if (c == -1) {
                throw error("unterminated " + (end.equals("-->") ? "comment" : end.equals("?>")
                        ? "processing instruction" : "CDATA section"));
            }
            out.append((char) c);
            if (c == end.charAt(matched)) {
                matched++;
                if (matched == end.length()) {
                    out.setLength(out.length() - end.length());
                    return out.toString();
                }
            } else {
                matched = c == end.charAt(0) ? 1 : 0;
            }
        }
    }

    private String readDoctype() throws IOException, XmlPullParserException {
        StringBuilder out = new StringBuilder();
        int nesting = 0;
        char quote = 0;
        while (true) {
            int c = read();
            if (c == -1) {
                throw error("unterminated DOCTYPE");
            }
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                }
            } else if (c == '"' || c == '\'') {
                quote = (char) c;
            } else if (c == '[') {
                nesting++;
            } else if (c == ']') {
                nesting--;
            } else if (c == '>' && nesting <= 0) {
                return out.toString().trim();
            }
            out.append((char) c);
        }
    }

    private void ensureElementCapacity() {
        int needed = (depth + 1) * 4;
        if (elementStack.length < needed) {
            String[] n = new String[needed + 32];
            System.arraycopy(elementStack, 0, n, 0, elementStack.length);
            elementStack = n;
        }
        if (nspCounts.length <= depth + 1) {
            int[] n = new int[depth + 8];
            System.arraycopy(nspCounts, 0, n, 0, nspCounts.length);
            nspCounts = n;
        }
    }

    private void parseStartTag() throws IOException, XmlPullParserException {
        String rawName = readName();
        attributeCount = 0;
        while (true) {
            skipWs();
            int c = peek();
            if (c == -1) {
                throw error("unexpected end of input in a start tag");
            }
            if (c == '/') {
                read();
                expect('>');
                degenerated = true;
                break;
            }
            if (c == '>') {
                read();
                break;
            }
            String attrName = readName();
            skipWs();
            if (peek() != '=') {
                if (!relaxed) {
                    throw error("attribute " + attrName + " has no value");
                }
                addAttribute(attrName, attrName);
                continue;
            }
            read();
            skipWs();
            int q = read();
            if (q != '"' && q != '\'') {
                throw error("attribute value must be quoted");
            }
            StringBuilder value = new StringBuilder();
            while (true) {
                int v = read();
                if (v == -1) {
                    throw error("unterminated attribute value");
                }
                if (v == q) {
                    break;
                }
                if (v == '&') {
                    readReference(value);
                } else if (v == '<' && !relaxed) {
                    throw error("'<' is not allowed in an attribute value");
                } else if (isWs(v)) {
                    value.append(' ');
                } else {
                    value.append((char) v);
                }
            }
            addAttribute(attrName, value.toString());
        }
        depth++;
        ensureElementCapacity();
        nspCounts[depth] = nspCounts[depth - 1];
        int sp = (depth - 1) * 4;
        elementStack[sp + 3] = rawName;
        if (processNsp) {
            resolveNamespaces(rawName);
        } else {
            name = rawName;
            prefix = null;
            namespace = NO_NAMESPACE;
            for (int i = 0; i < attributeCount; i++) {
                attributes[i * 4] = NO_NAMESPACE;
                attributes[i * 4 + 1] = null;
            }
        }
        elementStack[sp] = namespace;
        elementStack[sp + 1] = prefix;
        elementStack[sp + 2] = name;
        type = START_TAG;
        if (degenerated) {
            pendingEnd = true;
        }
    }

    private void addAttribute(String attrName, String value) {
        int i = attributeCount * 4;
        if (attributes.length < i + 4) {
            String[] n = new String[attributes.length * 2];
            System.arraycopy(attributes, 0, n, 0, attributes.length);
            attributes = n;
        }
        attributes[i] = NO_NAMESPACE;
        attributes[i + 1] = null;
        attributes[i + 2] = attrName;
        attributes[i + 3] = value;
        attributeCount++;
    }

    private void resolveNamespaces(String rawName) throws XmlPullParserException {
        // Declarations first: they apply to this element and its attributes.
        for (int i = 0; i < attributeCount; ) {
            String attrName = attributes[i * 4 + 2];
            int colon = attrName.indexOf(':');
            String declared = null;
            if (attrName.equals("xmlns")) {
                declared = "";
            } else if (colon > 0 && attrName.substring(0, colon).equals("xmlns")) {
                declared = attrName.substring(colon + 1);
            }
            if (declared == null) {
                i++;
                continue;
            }
            int j = nspCounts[depth] * 2;
            if (nspStack.length < j + 2) {
                String[] n = new String[nspStack.length * 2 + 2];
                System.arraycopy(nspStack, 0, n, 0, nspStack.length);
                nspStack = n;
            }
            nspStack[j] = declared;
            nspStack[j + 1] = attributes[i * 4 + 3];
            nspCounts[depth]++;
            if (reportNsAttrs) {
                attributes[i * 4] = XMLNS_URI;
                attributes[i * 4 + 1] = declared.length() == 0 ? null : "xmlns";
                attributes[i * 4 + 2] = declared.length() == 0 ? "xmlns" : declared;
                i++;
            } else {
                System.arraycopy(attributes, (i + 1) * 4, attributes, i * 4, (attributeCount - i - 1) * 4);
                attributeCount--;
            }
        }
        for (int i = 0; i < attributeCount; i++) {
            if (XMLNS_URI.equals(attributes[i * 4])) {
                continue;
            }
            String attrName = attributes[i * 4 + 2];
            int colon = attrName.indexOf(':');
            if (colon > 0) {
                String p = attrName.substring(0, colon);
                String uri = getNamespace(p);
                if (uri == null) {
                    if (!relaxed) {
                        throw error("undefined prefix " + p);
                    }
                    uri = NO_NAMESPACE;
                }
                attributes[i * 4] = uri;
                attributes[i * 4 + 1] = p;
                attributes[i * 4 + 2] = attrName.substring(colon + 1);
            } else {
                attributes[i * 4] = NO_NAMESPACE;
                attributes[i * 4 + 1] = null;
            }
        }
        int colon = rawName.indexOf(':');
        if (colon > 0) {
            prefix = rawName.substring(0, colon);
            name = rawName.substring(colon + 1);
        } else {
            prefix = null;
            name = rawName;
        }
        String uri = getNamespace(prefix == null ? "" : prefix);
        if (uri == null) {
            if (prefix != null && !relaxed) {
                throw error("undefined prefix " + prefix);
            }
            uri = NO_NAMESPACE;
        }
        namespace = uri;
    }

    private void parseEndTag() throws IOException, XmlPullParserException {
        String rawName = readName();
        skipWs();
        expect('>');
        if (depth == 0) {
            throw error("end tag </" + rawName + "> without a start tag");
        }
        int sp = (depth - 1) * 4;
        if (!rawName.equals(elementStack[sp + 3]) && !relaxed) {
            throw error("expected </" + elementStack[sp + 3] + "> but found </" + rawName + ">");
        }
        namespace = elementStack[sp];
        prefix = elementStack[sp + 1];
        name = elementStack[sp + 2];
        type = END_TAG;
    }

    private void popElement() {
        if (depth > 0) {
            depth--;
        }
    }

    // ------------------------------------------------------------ accessors

    @Override
    public int getNamespaceCount(int d) throws XmlPullParserException {
        if (d > depth) {
            throw new IndexOutOfBoundsException();
        }
        return nspCounts[d];
    }

    @Override
    public String getNamespacePrefix(int pos) throws XmlPullParserException {
        String p = nspStack[pos * 2];
        return p == null || p.length() == 0 ? null : p;
    }

    @Override
    public String getNamespaceUri(int pos) throws XmlPullParserException {
        return nspStack[pos * 2 + 1];
    }

    @Override
    public String getNamespace(String p) {
        if ("xml".equals(p)) {
            return XML_URI;
        }
        if ("xmlns".equals(p)) {
            return XMLNS_URI;
        }
        String key = p == null ? "" : p;
        for (int i = nspCounts[depth] * 2 - 2; i >= 0; i -= 2) {
            if (key.equals(nspStack[i])) {
                return nspStack[i + 1];
            }
        }
        return null;
    }

    @Override
    public int getDepth() {
        return depth;
    }

    @Override
    public String getPositionDescription() {
        StringBuilder b = new StringBuilder(type < TYPES.length ? TYPES[type] : "unknown");
        b.append(' ');
        if (type == START_TAG || type == END_TAG) {
            if (degenerated) {
                b.append("(empty) ");
            }
            b.append('<');
            if (type == END_TAG) {
                b.append('/');
            }
            if (prefix != null) {
                b.append('{').append(namespace).append('}').append(prefix).append(':');
            }
            b.append(name).append('>');
        } else if (type == TEXT && text != null) {
            String t = text.length() > 16 ? text.substring(0, 16) + "..." : text;
            b.append(t);
        }
        b.append('@').append(line).append(':').append(column);
        return b.toString();
    }

    @Override
    public int getLineNumber() {
        return line;
    }

    @Override
    public int getColumnNumber() {
        return column;
    }

    @Override
    public boolean isWhitespace() throws XmlPullParserException {
        if (type != TEXT && type != IGNORABLE_WHITESPACE && type != CDSECT) {
            throw error("isWhitespace() applies to text only");
        }
        return text == null || isAllWs(text);
    }

    @Override
    public String getText() {
        if (type < TEXT) {
            return null;
        }
        return text;
    }

    @Override
    public char[] getTextCharacters(int[] holderForStartAndLength) {
        String t = getText();
        if (t == null) {
            holderForStartAndLength[0] = -1;
            holderForStartAndLength[1] = -1;
            return null;
        }
        holderForStartAndLength[0] = 0;
        holderForStartAndLength[1] = t.length();
        return t.toCharArray();
    }

    @Override
    public String getNamespace() {
        return type == START_TAG || type == END_TAG ? namespace : null;
    }

    @Override
    public String getName() {
        return type == START_TAG || type == END_TAG || type == ENTITY_REF ? name : null;
    }

    @Override
    public String getPrefix() {
        return type == START_TAG || type == END_TAG ? prefix : null;
    }

    @Override
    public boolean isEmptyElementTag() throws XmlPullParserException {
        if (type != START_TAG) {
            throw error("isEmptyElementTag() applies to START_TAG only");
        }
        return degenerated;
    }

    @Override
    public int getAttributeCount() {
        return attributeCount;
    }

    private void checkAttr(int index) {
        if (index < 0 || index >= attributeCount) {
            throw new IndexOutOfBoundsException("attribute " + index + " of " + attributeCount);
        }
    }

    @Override
    public String getAttributeNamespace(int index) {
        checkAttr(index);
        return attributes[index * 4];
    }

    @Override
    public String getAttributeName(int index) {
        checkAttr(index);
        return attributes[index * 4 + 2];
    }

    @Override
    public String getAttributePrefix(int index) {
        checkAttr(index);
        return attributes[index * 4 + 1];
    }

    @Override
    public String getAttributeType(int index) {
        return "CDATA";
    }

    @Override
    public boolean isAttributeDefault(int index) {
        return false;
    }

    @Override
    public String getAttributeValue(int index) {
        checkAttr(index);
        return attributes[index * 4 + 3];
    }

    @Override
    public String getAttributeValue(String ns, String attrName) {
        for (int i = 0; i < attributeCount; i++) {
            if (attrName.equals(attributes[i * 4 + 2])
                    && (ns == null || !processNsp || ns.equals(attributes[i * 4]))) {
                return attributes[i * 4 + 3];
            }
        }
        return null;
    }

    @Override
    public int getEventType() throws XmlPullParserException {
        return type;
    }

    @Override
    public void require(int t, String ns, String n) throws XmlPullParserException, IOException {
        if (t != type || (ns != null && !ns.equals(getNamespace())) || (n != null && !n.equals(getName()))) {
            throw error("expected " + TYPES[t] + (n == null ? "" : " " + n));
        }
    }

    @Override
    public String nextText() throws XmlPullParserException, IOException {
        if (type != START_TAG) {
            throw error("precondition: START_TAG");
        }
        next();
        String result;
        if (type == TEXT) {
            result = getText();
            next();
        } else {
            result = "";
        }
        if (type != END_TAG) {
            throw error("END_TAG expected");
        }
        return result;
    }

    @Override
    public int nextTag() throws XmlPullParserException, IOException {
        next();
        if (type == TEXT && isWhitespace()) {
            next();
        }
        if (type != END_TAG && type != START_TAG) {
            throw error("unexpected type");
        }
        return type;
    }
}
