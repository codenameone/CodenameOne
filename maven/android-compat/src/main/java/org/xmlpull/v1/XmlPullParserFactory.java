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

import com.codename1.androidcompat.runtime.TextXmlPullParser;
import com.codename1.androidcompat.runtime.XmlSerializerImpl;

import java.util.HashMap;

/// Creates the runtime's XmlPull parser and serializer. There is one
/// implementation, so the class-name lookup of the original is not used.
public class XmlPullParserFactory {

    public static final String PROPERTY_NAME = "org.xmlpull.v1.XmlPullParserFactory";

    protected HashMap<String, Boolean> features = new HashMap<String, Boolean>();

    protected XmlPullParserFactory() {
    }

    public static XmlPullParserFactory newInstance() throws XmlPullParserException {
        return new XmlPullParserFactory();
    }

    public static XmlPullParserFactory newInstance(String classNames, Class context) throws XmlPullParserException {
        return new XmlPullParserFactory();
    }

    public void setFeature(String name, boolean state) throws XmlPullParserException {
        features.put(name, Boolean.valueOf(state));
    }

    public boolean getFeature(String name) {
        Boolean value = features.get(name);
        return value != null && value.booleanValue();
    }

    public void setNamespaceAware(boolean awareness) {
        features.put(XmlPullParser.FEATURE_PROCESS_NAMESPACES, Boolean.valueOf(awareness));
    }

    public boolean isNamespaceAware() {
        return getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES);
    }

    public void setValidating(boolean validating) {
        features.put(XmlPullParser.FEATURE_VALIDATION, Boolean.valueOf(validating));
    }

    public boolean isValidating() {
        return getFeature(XmlPullParser.FEATURE_VALIDATION);
    }

    public XmlPullParser newPullParser() throws XmlPullParserException {
        XmlPullParser pp = new TextXmlPullParser();
        for (java.util.Map.Entry<String, Boolean> e : features.entrySet()) {
            Boolean value = e.getValue();
            if (value != null && value.booleanValue()) {
                pp.setFeature(e.getKey(), true);
            }
        }
        return pp;
    }

    public XmlSerializer newSerializer() throws XmlPullParserException {
        return new XmlSerializerImpl();
    }
}
