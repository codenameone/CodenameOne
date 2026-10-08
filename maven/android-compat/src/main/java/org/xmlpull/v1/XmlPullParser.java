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
import java.io.InputStream;
import java.io.Reader;

/// The XmlPull API: a pull parser reporting a document as a sequence of
/// events. Android's resource parsers and `Xml.newPullParser()` implement
/// it.
public interface XmlPullParser {

    String NO_NAMESPACE = "";
    int START_DOCUMENT = 0;
    int END_DOCUMENT = 1;
    int START_TAG = 2;
    int END_TAG = 3;
    int TEXT = 4;
    int CDSECT = 5;
    int ENTITY_REF = 6;
    int IGNORABLE_WHITESPACE = 7;
    int PROCESSING_INSTRUCTION = 8;
    int COMMENT = 9;
    int DOCDECL = 10;

    String[] TYPES = {
        "START_DOCUMENT",
        "END_DOCUMENT",
        "START_TAG",
        "END_TAG",
        "TEXT",
        "CDSECT",
        "ENTITY_REF",
        "IGNORABLE_WHITESPACE",
        "PROCESSING_INSTRUCTION",
        "COMMENT",
        "DOCDECL"
    };

    String FEATURE_PROCESS_NAMESPACES = "http://xmlpull.org/v1/doc/features.html#process-namespaces";
    String FEATURE_REPORT_NAMESPACE_ATTRIBUTES = "http://xmlpull.org/v1/doc/features.html#report-namespace-prefixes";
    String FEATURE_PROCESS_DOCDECL = "http://xmlpull.org/v1/doc/features.html#process-docdecl";
    String FEATURE_VALIDATION = "http://xmlpull.org/v1/doc/features.html#validation";

    void setFeature(String name, boolean state) throws XmlPullParserException;

    boolean getFeature(String name);

    void setProperty(String name, Object value) throws XmlPullParserException;

    Object getProperty(String name);

    void setInput(Reader in) throws XmlPullParserException;

    void setInput(InputStream inputStream, String inputEncoding) throws XmlPullParserException;

    String getInputEncoding();

    void defineEntityReplacementText(String entityName, String replacementText) throws XmlPullParserException;

    int getNamespaceCount(int depth) throws XmlPullParserException;

    String getNamespacePrefix(int pos) throws XmlPullParserException;

    String getNamespaceUri(int pos) throws XmlPullParserException;

    String getNamespace(String prefix);

    int getDepth();

    String getPositionDescription();

    int getLineNumber();

    int getColumnNumber();

    boolean isWhitespace() throws XmlPullParserException;

    String getText();

    char[] getTextCharacters(int[] holderForStartAndLength);

    String getNamespace();

    String getName();

    String getPrefix();

    boolean isEmptyElementTag() throws XmlPullParserException;

    int getAttributeCount();

    String getAttributeNamespace(int index);

    String getAttributeName(int index);

    String getAttributePrefix(int index);

    String getAttributeType(int index);

    boolean isAttributeDefault(int index);

    String getAttributeValue(int index);

    String getAttributeValue(String namespace, String name);

    int getEventType() throws XmlPullParserException;

    int next() throws XmlPullParserException, IOException;

    int nextToken() throws XmlPullParserException, IOException;

    void require(int type, String namespace, String name) throws XmlPullParserException, IOException;

    String nextText() throws XmlPullParserException, IOException;

    int nextTag() throws XmlPullParserException, IOException;
}
