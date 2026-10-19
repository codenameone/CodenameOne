/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.share;

import java.util.List;

import com.codename1.flutter.plugins.crossfile.XFile;

/** share_plus's {@code ShareParams}: the text, link or files to share. */
public class ShareParams {
    String text;
    String subject;
    String title;
    Object uri;
    List<?> files;

    public ShareParams() {
    }

    public void text(String v) {
        this.text = v;
    }

    public void subject(String v) {
        this.subject = v;
    }

    public void title(String v) {
        this.title = v;
    }

    public void uri(Object v) {
        this.uri = v;
    }

    public void files(Object v) {
        this.files = v instanceof List ? (List<?>) v : null;
    }

    /** Accepted for source compatibility; the platform sheet places itself. */
    public void sharePositionOrigin(Object v) {
    }

    String firstFilePath() {
        if (files == null) {
            return null;
        }
        for (Object f : files) {
            if (f instanceof XFile) {
                return ((XFile) f).path();
            }
        }
        return null;
    }
}
