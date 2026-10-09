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
package com.codename1.desktopcompat.java.awt.datatransfer;

/// The kind of data a [Transferable] can hand over: a MIME type and the
/// class of the object that carries it.
///
/// The system clipboard of the layer carries text, so [#stringFlavor] is
/// the flavor that matters; others can be made and compared, and are
/// carried only by a `Transferable` and a [Clipboard] of the application's
/// own.
public class DataFlavor {

    /// A Java string.
    public static final DataFlavor stringFlavor = new DataFlavor(String.class, "Unicode String");

    /// A list of files, each a `java.io.File`: what a drag of files from
    /// another application carries.
    public static final DataFlavor javaFileListFlavor = new DataFlavor("application/x-java-file-list",
            java.util.List.class, "application/x-java-file-list");

    public static final String javaSerializedObjectMimeType = "application/x-java-serialized-object";

    private final String primary;
    private final Class<?> representationClass;
    private String humanPresentableName;

    public DataFlavor(Class<?> representationClass, String humanPresentableName) {
        if (representationClass == null) {
            throw new NullPointerException("representationClass");
        }
        this.primary = javaSerializedObjectMimeType;
        this.representationClass = representationClass;
        this.humanPresentableName = humanPresentableName == null ? primary : humanPresentableName;
    }

    private DataFlavor(String primary, Class<?> representationClass, String humanPresentableName) {
        this.primary = primary;
        this.representationClass = representationClass;
        this.humanPresentableName = humanPresentableName;
    }

    /// A flavor for a MIME type whose data is read from a stream. The
    /// parameters of the type, after the first `;`, are not kept.
    public DataFlavor(String mimeType, String humanPresentableName) {
        if (mimeType == null) {
            throw new NullPointerException("mimeType");
        }
        int cut = mimeType.indexOf(';');
        this.primary = fold(cut < 0 ? mimeType : mimeType.substring(0, cut));
        this.representationClass = java.io.InputStream.class;
        this.humanPresentableName = humanPresentableName == null ? primary : humanPresentableName;
    }

    /// A MIME type is ASCII and compared without regard to case; it is
    /// folded here by hand because the platform's case folding follows the
    /// device's language.
    private static String fold(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                c = (char) (c + ('a' - 'A'));
            }
            if (c != ' ' && c != '\t') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public String getMimeType() {
        return primary + "; class=" + representationClass.getName();
    }

    public String getPrimaryType() {
        int cut = primary.indexOf('/');
        return cut < 0 ? primary : primary.substring(0, cut);
    }

    public String getSubType() {
        int cut = primary.indexOf('/');
        return cut < 0 ? "" : primary.substring(cut + 1);
    }

    public Class<?> getRepresentationClass() {
        return representationClass;
    }

    public String getHumanPresentableName() {
        return humanPresentableName;
    }

    public void setHumanPresentableName(String humanPresentableName) {
        this.humanPresentableName = humanPresentableName;
    }

    /// Whether the type and subtype of `mimeType` are this flavor's.
    public boolean isMimeTypeEqual(String mimeType) {
        if (mimeType == null) {
            throw new NullPointerException("mimeType");
        }
        int cut = mimeType.indexOf(';');
        return primary.equals(fold(cut < 0 ? mimeType : mimeType.substring(0, cut)));
    }

    public final boolean isMimeTypeEqual(DataFlavor dataFlavor) {
        return dataFlavor != null && primary.equals(dataFlavor.primary);
    }

    /// Whether this is the flavor of a list of files.
    public boolean isFlavorJavaFileListType() {
        return primary.equals(javaFileListFlavor.primary)
                && java.util.List.class.equals(representationClass);
    }

    public boolean isFlavorTextType() {
        return String.class.equals(representationClass) || "text".equals(getPrimaryType());
    }

    public boolean match(DataFlavor that) {
        return equals(that);
    }

    /// Two flavors are equal when their MIME types and their
    /// representation classes are.
    public boolean equals(DataFlavor that) {
        return that != null && primary.equals(that.primary)
                && representationClass.equals(that.representationClass);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DataFlavor && equals((DataFlavor) o);
    }

    @Override
    public int hashCode() {
        return primary.hashCode() * 31 + representationClass.hashCode();
    }

    @Override
    public String toString() {
        return "java.awt.datatransfer.DataFlavor[mimetype=" + primary + ";representationclass="
                + representationClass.getName() + "]";
    }
}
