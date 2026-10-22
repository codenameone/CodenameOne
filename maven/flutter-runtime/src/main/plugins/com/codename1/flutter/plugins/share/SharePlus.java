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
import com.codename1.ui.Display;

import dart.async.Future;

/**
 * share_plus, over {@code Display.share}. The platform share sheet does not report
 * what the user picked, so a share that opened the sheet answers {@code unavailable}
 * -- share_plus's own answer on platforms that cannot tell -- and one that had no
 * display to open it on answers the same with an empty raw value.
 */
public class SharePlus {
    /** {@code SharePlus.instance}. */
    public static final SharePlus instance = new SharePlus();

    public Future<ShareResult> share(ShareParams p) {
        String text = p.text;
        if (text == null && p.uri != null) {
            text = String.valueOf(p.uri);
        }
        if (p.subject != null && text != null && p.firstFilePath() == null) {
            text = p.subject + "\n" + text;
        }
        return open(text, p.firstFilePath());
    }

    static Future<ShareResult> open(String text, String filePath) {
        if (!Display.isInitialized()) {
            return Future.value(new ShareResult("", ShareResultStatus.unavailable));
        }
        if (filePath != null) {
            Display.getInstance().share(text, filePath, mimeOf(filePath));
        } else {
            Display.getInstance().share(text == null ? "" : text);
        }
        return Future.value(new ShareResult("", ShareResultStatus.unavailable));
    }

    static String mimeOf(String path) {
        int dot = path.lastIndexOf('.');
        String ext = dot < 0 ? "" : path.substring(dot + 1);
        if (ext.equalsIgnoreCase("png")) {
            return "image/png";
        }
        if (ext.equalsIgnoreCase("jpg") || ext.equalsIgnoreCase("jpeg")) {
            return "image/jpeg";
        }
        if (ext.equalsIgnoreCase("gif")) {
            return "image/gif";
        }
        if (ext.equalsIgnoreCase("pdf")) {
            return "application/pdf";
        }
        if (ext.equalsIgnoreCase("txt")) {
            return "text/plain";
        }
        return "application/octet-stream";
    }

    // --- the static Share API share_plus deprecated but most apps still call -----

    /** {@code Share.share(text, {subject, sharePositionOrigin})}. */
    /** The pre-10 static {@code Share.share(text, subject: ...)}. */
    public static Future<ShareResult> share(String text, String subject, Object origin) {
        return shareText(text, subject, origin);
    }

    public static Future<ShareResult> shareText(String text, String subject, Object origin) {
        return open(subject != null && text != null ? subject + "\n" + text : text, null);
    }

    /** {@code Share.shareUri(uri, {sharePositionOrigin})}. */
    public static Future<ShareResult> shareUri(Object uri, Object origin) {
        return open(String.valueOf(uri), null);
    }

    /** {@code Share.shareXFiles(files, {subject, text, sharePositionOrigin})}. */
    public static Future<ShareResult> shareXFiles(Object files, String subject, String text, Object origin) {
        String path = null;
        if (files instanceof List) {
            for (Object f : (List<?>) files) {
                if (f instanceof XFile) {
                    path = ((XFile) f).materializedPath();
                    break;
                }
            }
        }
        return open(text, path);
    }
}
