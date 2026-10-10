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
package com.codename1.desktopcompat.javax.swing.filechooser;

import java.io.File;

/// `javax.swing.filechooser.FileNameExtensionFilter`: accepts every
/// directory, and the files whose name ends in one of a set of extensions,
/// whatever their case.
public final class FileNameExtensionFilter extends FileFilter {

    private final String description;
    private final String[] extensions;

    public FileNameExtensionFilter(String description, String... extensions) {
        if (extensions == null || extensions.length == 0) {
            throw new IllegalArgumentException("Extensions must be non-null and not empty");
        }
        this.description = description;
        this.extensions = new String[extensions.length];
        for (int i = 0; i < extensions.length; i++) {
            if (extensions[i] == null || extensions[i].length() == 0) {
                throw new IllegalArgumentException("Each extension must be non-null and not empty");
            }
            this.extensions[i] = extensions[i];
        }
    }

    @Override
    public boolean accept(File f) {
        if (f == null) {
            return false;
        }
        if (f.isDirectory()) {
            return true;
        }
        String name = f.getName();
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1) {
            return false;
        }
        String ext = name.substring(dot + 1);
        for (int i = 0; i < extensions.length; i++) {
            // An extension is ASCII; this comparison does not depend on
            // the device's locale the way folding the case would.
            if (ext.equalsIgnoreCase(extensions[i])) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public String[] getExtensions() {
        String[] r = new String[extensions.length];
        System.arraycopy(extensions, 0, r, 0, r.length);
        return r;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("FileNameExtensionFilter[description=");
        sb.append(description).append(" extensions=[");
        for (int i = 0; i < extensions.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(extensions[i]);
        }
        return sb.append("]]").toString();
    }
}
