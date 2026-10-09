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

import android.content.res.XmlResourceParser;

import org.xmlpull.v1.XmlPullParserException;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;

/// The XmlResourceParser `Resources.getXml`, `getLayout` and
/// `getAnimation` return: the compiled element tree replayed as pull events.
/// Each element's text, when it has any, comes right after its start tag.
/// Attribute values keep the types the build gave them, so the typed
/// AttributeSet accessors need no parsing.
public final class CompiledXmlParser implements XmlResourceParser {

    private final XmlNode root;
    private final ArrayList<XmlNode> nodes = new ArrayList<XmlNode>();
    private final ArrayList<Integer> types = new ArrayList<Integer>();
    private final ArrayList<Integer> depths = new ArrayList<Integer>();
    private int index;
    private CompiledAttributeSet attrs;
    private XmlNode current;

    public CompiledXmlParser(XmlNode root) {
        this.root = root;
        add(START_DOCUMENT, null, 0);
        flatten(root, 1);
        add(END_DOCUMENT, null, 0);
    }

    private void add(int type, XmlNode node, int depth) {
        types.add(Integer.valueOf(type));
        nodes.add(node);
        depths.add(Integer.valueOf(depth));
    }

    private void flatten(XmlNode n, int depth) {
        add(START_TAG, n, depth);
        if (n.text != null) {
            add(TEXT, n, depth);
        }
        for (XmlNode c : n.children) {
            flatten(c, depth + 1);
        }
        add(END_TAG, n, depth);
    }

    /// The compiled root element, for code that consumes the tree directly
    /// (layout inflation from a parser).
    public XmlNode getRootNode() {
        return root;
    }

    /// The element of the current event, or null at the document's ends.
    public XmlNode getCurrentNode() {
        return nodes.get(index);
    }

    private int type() {
        return types.get(index).intValue();
    }

    // ------------------------------------------------------------ XmlPullParser

    @Override
    public void setFeature(String name, boolean state) throws XmlPullParserException {
        if (FEATURE_PROCESS_NAMESPACES.equals(name) && state) {
            return;
        }
        if (FEATURE_REPORT_NAMESPACE_ATTRIBUTES.equals(name) && !state) {
            return;
        }
        throw new XmlPullParserException("Unsupported feature: " + name);
    }

    @Override
    public boolean getFeature(String name) {
        return FEATURE_PROCESS_NAMESPACES.equals(name);
    }

    @Override
    public void setProperty(String name, Object value) throws XmlPullParserException {
        throw new XmlPullParserException("setProperty() not supported");
    }

    @Override
    public Object getProperty(String name) {
        return null;
    }

    @Override
    public void setInput(Reader in) throws XmlPullParserException {
        throw new XmlPullParserException("setInput() not supported");
    }

    @Override
    public void setInput(InputStream inputStream, String inputEncoding) throws XmlPullParserException {
        throw new XmlPullParserException("setInput() not supported");
    }

    @Override
    public String getInputEncoding() {
        return null;
    }

    @Override
    public void defineEntityReplacementText(String entityName, String replacementText)
            throws XmlPullParserException {
        throw new XmlPullParserException("defineEntityReplacementText() not supported");
    }

    @Override
    public int getNamespaceCount(int depth) throws XmlPullParserException {
        return 0;
    }

    @Override
    public String getNamespacePrefix(int pos) throws XmlPullParserException {
        throw new XmlPullParserException("no namespace declarations");
    }

    @Override
    public String getNamespaceUri(int pos) throws XmlPullParserException {
        throw new XmlPullParserException("no namespace declarations");
    }

    @Override
    public String getNamespace(String prefix) {
        if ("android".equals(prefix)) {
            return XmlNode.ANDROID_NS_URI;
        }
        if ("app".equals(prefix)) {
            return XmlNode.APP_NS_URI;
        }
        return null;
    }

    @Override
    public int getDepth() {
        return depths.get(index).intValue();
    }

    @Override
    public String getPositionDescription() {
        XmlNode n = nodes.get(index);
        String source = n == null ? root.getSource() : n.getSource();
        return "Binary XML file " + (source == null ? "" : source + " ") + "line #" + getLineNumber();
    }

    @Override
    public int getLineNumber() {
        XmlNode n = nodes.get(index);
        return n == null ? -1 : n.line;
    }

    @Override
    public int getColumnNumber() {
        return -1;
    }

    @Override
    public boolean isWhitespace() throws XmlPullParserException {
        if (type() != TEXT) {
            throw new XmlPullParserException("isWhitespace() applies to text only");
        }
        return nodes.get(index).text.trim().length() == 0;
    }

    @Override
    public String getText() {
        return type() == TEXT ? nodes.get(index).text : null;
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
        int t = type();
        return t == START_TAG || t == END_TAG ? NO_NAMESPACE : null;
    }

    @Override
    public String getName() {
        int t = type();
        return t == START_TAG || t == END_TAG ? nodes.get(index).tag : null;
    }

    @Override
    public String getPrefix() {
        return null;
    }

    @Override
    public boolean isEmptyElementTag() throws XmlPullParserException {
        if (type() != START_TAG) {
            throw new XmlPullParserException("isEmptyElementTag() applies to START_TAG only");
        }
        return false;
    }

    private CompiledAttributeSet attrs() {
        return type() == START_TAG ? attrs : null;
    }

    @Override
    public int getAttributeCount() {
        CompiledAttributeSet a = attrs();
        return a == null ? -1 : a.getAttributeCount();
    }

    private CompiledAttributeSet checked(int i) {
        checkIndex(i);
        return attrs();
    }

    private void checkIndex(int i) {
        CompiledAttributeSet a = attrs();
        if (a == null || i < 0 || i >= a.getAttributeCount()) {
            throw new IndexOutOfBoundsException(String.valueOf(i));
        }
    }

    @Override
    public String getAttributeNamespace(int index) {
        return checked(index).getAttributeNamespace(index);
    }

    @Override
    public String getAttributeName(int index) {
        return checked(index).getAttributeName(index);
    }

    @Override
    public String getAttributePrefix(int index) {
        checkIndex(index);
        return null;
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
        return checked(index).getAttributeValue(index);
    }

    @Override
    public String getAttributeValue(String namespace, String name) {
        CompiledAttributeSet a = attrs();
        return a == null ? null : a.getAttributeValue(namespace, name);
    }

    @Override
    public int getEventType() throws XmlPullParserException {
        return type();
    }

    @Override
    public int next() throws XmlPullParserException {
        if (index < types.size() - 1) {
            index++;
        }
        int t = type();
        if (t == START_TAG) {
            current = nodes.get(index);
            attrs = new CompiledAttributeSet(current);
        }
        return t;
    }

    @Override
    public int nextToken() throws XmlPullParserException {
        return next();
    }

    @Override
    public void require(int type, String namespace, String name) throws XmlPullParserException {
        if (type != type() || (namespace != null && !namespace.equals(getNamespace()))
                || (name != null && !name.equals(getName()))) {
            throw new XmlPullParserException("expected " + TYPES[type] + " " + getPositionDescription());
        }
    }

    @Override
    public String nextText() throws XmlPullParserException {
        if (type() != START_TAG) {
            throw new XmlPullParserException(getPositionDescription() + ": parser must be on START_TAG to read next text");
        }
        int t = next();
        if (t == TEXT) {
            String result = getText();
            t = next();
            if (t != END_TAG) {
                throw new XmlPullParserException(getPositionDescription() + ": event TEXT it must be immediately followed by END_TAG");
            }
            return result;
        } else if (t == END_TAG) {
            return "";
        }
        throw new XmlPullParserException(getPositionDescription() + ": parser must be on START_TAG or TEXT to read text");
    }

    @Override
    public int nextTag() throws XmlPullParserException {
        int t = next();
        if (t == TEXT && isWhitespace()) {
            t = next();
        }
        if (t != START_TAG && t != END_TAG) {
            throw new XmlPullParserException(getPositionDescription() + ": expected start or end tag");
        }
        return t;
    }

    // ------------------------------------------------------------ AttributeSet

    private CompiledAttributeSet set() {
        return attrs != null ? attrs : new CompiledAttributeSet(root);
    }

    @Override
    public int getAttributeNameResource(int index) {
        return set().getAttributeNameResource(index);
    }

    @Override
    public int getAttributeListValue(String namespace, String attribute, String[] options, int defaultValue) {
        return set().getAttributeListValue(namespace, attribute, options, defaultValue);
    }

    @Override
    public boolean getAttributeBooleanValue(String namespace, String attribute, boolean defaultValue) {
        return set().getAttributeBooleanValue(namespace, attribute, defaultValue);
    }

    @Override
    public int getAttributeResourceValue(String namespace, String attribute, int defaultValue) {
        return set().getAttributeResourceValue(namespace, attribute, defaultValue);
    }

    @Override
    public int getAttributeIntValue(String namespace, String attribute, int defaultValue) {
        return set().getAttributeIntValue(namespace, attribute, defaultValue);
    }

    @Override
    public int getAttributeUnsignedIntValue(String namespace, String attribute, int defaultValue) {
        return set().getAttributeUnsignedIntValue(namespace, attribute, defaultValue);
    }

    @Override
    public float getAttributeFloatValue(String namespace, String attribute, float defaultValue) {
        return set().getAttributeFloatValue(namespace, attribute, defaultValue);
    }

    @Override
    public int getAttributeListValue(int index, String[] options, int defaultValue) {
        return set().getAttributeListValue(index, options, defaultValue);
    }

    @Override
    public boolean getAttributeBooleanValue(int index, boolean defaultValue) {
        return set().getAttributeBooleanValue(index, defaultValue);
    }

    @Override
    public int getAttributeResourceValue(int index, int defaultValue) {
        return set().getAttributeResourceValue(index, defaultValue);
    }

    @Override
    public int getAttributeIntValue(int index, int defaultValue) {
        return set().getAttributeIntValue(index, defaultValue);
    }

    @Override
    public int getAttributeUnsignedIntValue(int index, int defaultValue) {
        return set().getAttributeUnsignedIntValue(index, defaultValue);
    }

    @Override
    public float getAttributeFloatValue(int index, float defaultValue) {
        return set().getAttributeFloatValue(index, defaultValue);
    }

    @Override
    public String getIdAttribute() {
        return set().getIdAttribute();
    }

    @Override
    public String getClassAttribute() {
        return set().getClassAttribute();
    }

    @Override
    public int getIdAttributeResourceValue(int defaultValue) {
        return set().getIdAttributeResourceValue(defaultValue);
    }

    @Override
    public int getStyleAttribute() {
        return set().getStyleAttribute();
    }

    @Override
    public void close() {
    }
}
