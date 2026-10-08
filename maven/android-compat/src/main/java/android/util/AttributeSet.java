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

/// The attributes of one XML element of a compiled resource, as a view
/// constructor receives them.
public interface AttributeSet {
    int getAttributeCount();

    String getAttributeNamespace(int index);

    String getAttributeName(int index);

    String getAttributeValue(int index);

    String getAttributeValue(String namespace, String name);

    String getPositionDescription();

    int getAttributeNameResource(int index);

    int getAttributeListValue(String namespace, String attribute, String[] options, int defaultValue);

    boolean getAttributeBooleanValue(String namespace, String attribute, boolean defaultValue);

    int getAttributeResourceValue(String namespace, String attribute, int defaultValue);

    int getAttributeIntValue(String namespace, String attribute, int defaultValue);

    int getAttributeUnsignedIntValue(String namespace, String attribute, int defaultValue);

    float getAttributeFloatValue(String namespace, String attribute, float defaultValue);

    int getAttributeListValue(int index, String[] options, int defaultValue);

    boolean getAttributeBooleanValue(int index, boolean defaultValue);

    int getAttributeResourceValue(int index, int defaultValue);

    int getAttributeIntValue(int index, int defaultValue);

    int getAttributeUnsignedIntValue(int index, int defaultValue);

    float getAttributeFloatValue(int index, float defaultValue);

    String getIdAttribute();

    String getClassAttribute();

    int getIdAttributeResourceValue(int defaultValue);

    int getStyleAttribute();
}
