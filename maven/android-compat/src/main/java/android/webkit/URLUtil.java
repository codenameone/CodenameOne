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
package android.webkit;

/// Classifies URLs by scheme. Schemes are compared case-insensitively
/// character by character, never through a locale-sensitive case fold.
public final class URLUtil {

    private URLUtil() {
    }

    private static boolean starts(String url, String prefix) {
        return url != null && url.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    public static boolean isAssetUrl(String url) {
        return url != null && url.startsWith("file:///android_asset/");
    }

    public static boolean isResourceUrl(String url) {
        return url != null && url.startsWith("file:///android_res/");
    }

    public static boolean isFileUrl(String url) {
        return starts(url, "file://") && !isAssetUrl(url) && !isResourceUrl(url);
    }

    public static boolean isAboutUrl(String url) {
        return starts(url, "about:");
    }

    public static boolean isDataUrl(String url) {
        return starts(url, "data:");
    }

    public static boolean isJavaScriptUrl(String url) {
        return starts(url, "javascript:");
    }

    public static boolean isHttpUrl(String url) {
        return starts(url, "http://");
    }

    public static boolean isHttpsUrl(String url) {
        return starts(url, "https://");
    }

    public static boolean isNetworkUrl(String url) {
        return url != null && url.length() > 0 && (isHttpUrl(url) || isHttpsUrl(url));
    }

    public static boolean isContentUrl(String url) {
        return starts(url, "content:");
    }

    public static boolean isValidUrl(String url) {
        if (url == null || url.length() == 0) {
            return false;
        }
        return isAssetUrl(url) || isResourceUrl(url) || isFileUrl(url) || isAboutUrl(url) || isHttpUrl(url)
                || isHttpsUrl(url) || isJavaScriptUrl(url) || isContentUrl(url);
    }

    /// Prefixes `http://` to a URL without a scheme.
    public static String guessUrl(String inUrl) {
        if (inUrl == null) {
            return null;
        }
        String url = inUrl.trim();
        // A scheme needs at least one character before "://".
        boolean hasScheme = url.indexOf("://") >= 0 && !url.startsWith("://");
        if (url.length() == 0 || hasScheme || isJavaScriptUrl(url) || isAboutUrl(url)
                || isDataUrl(url)) {
            return url;
        }
        return "http://" + url;
    }

    public static String stripAnchor(String url) {
        int anchor = url.indexOf('#');
        return anchor < 0 ? url : url.substring(0, anchor);
    }

    /// A file name for a download: the `filename` of a content disposition,
    /// else the last path segment of the URL, else "downloadfile".
    public static String guessFileName(String url, String contentDisposition, String mimeType) {
        String name = null;
        if (contentDisposition != null) {
            int i = indexOfIgnoreCase(contentDisposition, "filename=");
            if (i >= 0) {
                name = contentDisposition.substring(i + "filename=".length()).trim();
                int semi = name.indexOf(';');
                if (semi >= 0) {
                    name = name.substring(0, semi).trim();
                }
                if (name.length() >= 2 && name.charAt(0) == '"' && name.charAt(name.length() - 1) == '"') {
                    name = name.substring(1, name.length() - 1);
                }
                int slash = name.lastIndexOf('/');
                if (slash >= 0) {
                    name = name.substring(slash + 1);
                }
            }
        }
        if ((name == null || name.length() == 0) && url != null) {
            String path = url;
            int q = path.indexOf('?');
            if (q >= 0) {
                path = path.substring(0, q);
            }
            path = stripAnchor(path);
            if (!path.endsWith("/")) {
                int slash = path.lastIndexOf('/');
                name = slash >= 0 ? path.substring(slash + 1) : path;
            }
        }
        if (name == null || name.length() == 0) {
            name = "downloadfile";
        }
        if (name.indexOf('.') < 0) {
            String ext = extensionFor(mimeType);
            if (ext != null) {
                name = name + "." + ext;
            }
        }
        return name;
    }

    private static String extensionFor(String mime) {
        if (mime == null) {
            return null;
        }
        if (mime.equalsIgnoreCase("text/html")) {
            return "html";
        }
        if (mime.equalsIgnoreCase("text/plain")) {
            return "txt";
        }
        if (mime.equalsIgnoreCase("image/png")) {
            return "png";
        }
        if (mime.equalsIgnoreCase("image/jpeg")) {
            return "jpg";
        }
        if (mime.equalsIgnoreCase("application/pdf")) {
            return "pdf";
        }
        return "bin";
    }

    private static int indexOfIgnoreCase(String s, String what) {
        for (int i = 0; i + what.length() <= s.length(); i++) {
            if (s.regionMatches(true, i, what, 0, what.length())) {
                return i;
            }
        }
        return -1;
    }
}
