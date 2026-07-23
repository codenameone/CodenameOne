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
package com.codename1.settings.project;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Util;

import java.io.IOException;
import java.io.InputStream;

public final class ProjectIO {
    public static final String INPUT_PROPERTY = "settings.input";

    private ProjectIO() {
    }

    public static ProjectBinding loadBinding() {
        String path = System.getProperty(INPUT_PROPERTY);
        if (path == null || path.trim().length() == 0) {
            return null;
        }
        InputStream in = null;
        try {
            String url = fsUrl(path);
            FileSystemStorage fs = FileSystemStorage.getInstance();
            if (!fs.exists(url)) {
                return null;
            }
            in = fs.openInputStream(url);
            ProjectBinding b = ProjectBinding.parse(Util.readToString(in, "UTF-8"));
            return b.isValid() ? b : null;
        } catch (IOException ex) {
            return null;
        } finally {
            Util.cleanup(in);
        }
    }

    public static String fsUrl(String path) {
        if (path == null) {
            return null;
        }
        if (path.startsWith("file://") || path.indexOf("://") > 0) {
            return path;
        }
        String normalized = path.replace('\\', '/');
        if (normalized.length() > 1 && normalized.charAt(1) == ':') {
            // Windows drive-letter path (C:\Users\...): a file URL needs a slash
            // before the drive letter, otherwise stripping the file:// prefix
            // yields a drive-relative path like /C:\... that resolves to
            // C:\C:\... and silently breaks every project-file read.
            return "file:///" + normalized;
        }
        return "file://" + normalized;
    }
}
