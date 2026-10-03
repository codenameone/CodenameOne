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
package android.util;

import com.codename1.androidcompat.runtime.TextXmlPullParser;
import com.codename1.androidcompat.runtime.XmlPullAttributeSet;
import com.codename1.androidcompat.runtime.XmlSerializerImpl;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.UnsupportedEncodingException;

/// XML utilities: pull parsers, serializers and attribute sets.
public class Xml {

    public static final String FEATURE_RELAXED = TextXmlPullParser.FEATURE_RELAXED;

    /// The encodings XML text is read and written in.
    public enum Encoding {
        US_ASCII("US-ASCII"),
        UTF_8("UTF-8"),
        UTF_16("UTF-16"),
        ISO_8859_1("ISO-8859-1");

        final String expatName;

        Encoding(String expatName) {
            this.expatName = expatName;
        }
    }

    private Xml() {
    }

    /// A namespace-aware parser for XML text.
    public static XmlPullParser newPullParser() {
        TextXmlPullParser parser = new TextXmlPullParser();
        try {
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        } catch (XmlPullParserException e) {
            throw new AssertionError(e.toString());
        }
        return parser;
    }

    public static XmlPullParser newFastPullParser() {
        return newPullParser();
    }

    public static XmlSerializer newSerializer() {
        return new XmlSerializerImpl();
    }

    public static Encoding findEncodingByName(String encodingName) throws UnsupportedEncodingException {
        if (encodingName == null) {
            return Encoding.UTF_8;
        }
        for (Encoding encoding : Encoding.values()) {
            if (encoding.expatName.equalsIgnoreCase(encodingName)) {
                return encoding;
            }
        }
        throw new UnsupportedEncodingException(encodingName);
    }

    /// The parser itself when it already is an AttributeSet (a compiled
    /// resource parser), else a view of its current element's attributes.
    public static AttributeSet asAttributeSet(XmlPullParser parser) {
        return parser instanceof AttributeSet ? (AttributeSet) parser : new XmlPullAttributeSet(parser);
    }
}
