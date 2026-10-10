/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.mvc;

import com.codename1.backend.ByteSink;
import com.codename1.backend.HttpServer;
import com.codename1.backend.security.CsrfToken;

/// Primitives called by generated views; no template evaluation occurs here.
public final class Html {
    private Html() {}

    public static byte[] utf8(String text) {
        try {
            return text.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static byte[] bytes(ByteSink out) {
        byte[] result = new byte[out.length()];
        System.arraycopy(out.bytes(), 0, result, 0, result.length);
        return result;
    }

    public static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public static boolean truth(Object value) {
        if (value == null || Boolean.FALSE.equals(value)) {
            return false;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue() != 0.0;
        }
        if (value instanceof Character) {
            return ((Character) value).charValue() != 0;
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            return !"false".equalsIgnoreCase(text)
                    && !"no".equalsIgnoreCase(text)
                    && !"off".equalsIgnoreCase(text);
        }
        return true;
    }

    public static boolean equal(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    public static void text(ByteSink out, Object value) {
        String s = string(value);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '&') {
                out.putAscii("&amp;");
            } else if (c == '<') {
                out.putAscii("&lt;");
            } else if (c == '>') {
                out.putAscii("&gt;");
            } else if (c == '"') {
                out.putAscii("&quot;");
            } else if (c == '\'') {
                out.putAscii("&#39;");
            } else if (Character.isHighSurrogate(c)
                    && i + 1 < s.length()
                    && Character.isLowSurrogate(s.charAt(i + 1))) {
                out.putCodePoint(Character.toCodePoint(c, s.charAt(++i)));
            } else {
                out.putCodePoint(Character.isSurrogate(c) ? 0xfffd : c);
            }
        }
    }

    public static void attribute(ByteSink out, String name, Object value) {
        if (value == null) {
            return;
        }
        name = asciiLower(name);
        if (name.startsWith("data-hx-")) {
            name = name.substring(5);
        }
        if ("href".equals(name)
                || "xlink:href".equals(name)
                || "src".equals(name)
                || "data".equals(name)
                || "action".equals(name)
                || "formaction".equals(name)
                || "hx-get".equals(name)
                || "hx-post".equals(name)
                || "hx-put".equals(name)
                || "hx-patch".equals(name)
                || "hx-delete".equals(name)) {
            safeUrl(string(value));
        }
        out.put(' ');
        out.putAscii(name);
        out.putAscii("=\"");
        text(out, value);
        out.put('"');
    }

    /// Snapshot an attribute's textual value before it is checked and rendered.
    public static String attributeValue(Object value) {
        return value == null ? null : string(value);
    }

    /// A submitter can belong to a form outside this template or fragment.
    public static String submitMethod(Model model, String value) {
        if (value != null
                && model.getAttribute("_csrf") != null
                && !"post".equalsIgnoreCase(value)
                && !"dialog".equalsIgnoreCase(value)) {
            throw new IllegalArgumentException(
                    "GET submit-method overrides are not supported with CSRF tokens; use a separate"
                        + " GET form");
        }
        return value;
    }

    /// Form binding supports the two structured HTML form encodings.
    public static String formEncoding(String value) {
        if (value != null
                && value.length() > 0
                && !"application/x-www-form-urlencoded".equalsIgnoreCase(value)
                && !"multipart/form-data".equalsIgnoreCase(value)) {
            throw new IllegalArgumentException("Unsupported form encoding: " + value);
        }
        return value;
    }

    /// Preserve parameter filtering while keeping CSRF tokens out of htmx GET URLs.
    public static String htmxParameters(Model model, boolean get, String parameters) {
        String filter =
                parameters == null || parameters.length() == 0 || "unset".equals(parameters)
                        ? "*"
                        : parameters;
        CsrfToken token = (CsrfToken) model.getAttribute("_csrf");
        if (!get || token == null || "none".equals(filter)) {
            return filter;
        }
        String name = token.getParameterName();
        // htmx's comma-separated syntax cannot represent these names safely.
        if (name.indexOf(',') >= 0 || !name.equals(name.trim())) {
            return "none";
        }
        if ("*".equals(filter)) {
            return "not " + name;
        }
        boolean exclude = filter.startsWith("not ");
        StringBuilder result = new StringBuilder();
        int start = exclude ? 4 : 0;
        while (start <= filter.length()) {
            int end = filter.indexOf(',', start);
            if (end < 0) {
                end = filter.length();
            }
            String candidate = filter.substring(start, end).trim();
            start = end + 1;
            if (candidate.equals(name)) {
                if (exclude) {
                    return filter;
                }
                continue;
            }
            if (result.length() > 0) {
                result.append(',');
            }
            result.append(candidate);
        }
        if (exclude) {
            return "not " + result + (result.length() == 0 ? "" : ",") + name;
        }
        return result.length() == 0 ? "none" : result.toString();
    }

    public static void booleanAttribute(ByteSink out, String name, Object value) {
        if (truth(value)) {
            attribute(out, name, name);
        }
    }

    public static String urlPart(Object value) {
        byte[] bytes = utf8(string(value));
        StringBuilder out = new StringBuilder();
        String hex = "0123456789ABCDEF";
        for (byte b : bytes) {
            int c = b & 255;
            if ((c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == '-'
                    || c == '_'
                    || c == '.'
                    || c == '~') {
                out.append((char) c);
            } else {
                out.append('%').append(hex.charAt(c >> 4)).append(hex.charAt(c & 15));
            }
        }
        return out.toString();
    }

    public static Object badModel(String message) {
        throw new IllegalStateException(message);
    }

    public static Object require(Model model, String name, String view) {
        if (!model.containsAttribute(name)) {
            throw new IllegalStateException("Missing model '" + name + "' for " + view);
        }
        return model.getAttribute(name);
    }

    public static Object field(Model model, String form, String name, Object fallback) {
        BindingResult result = (BindingResult) model.getAttribute("BindingResult." + form);
        return result == null ? fallback : result.fieldValue(name, fallback);
    }

    public static String errors(Model model, String form, String name) {
        BindingResult result = (BindingResult) model.getAttribute("BindingResult." + form);
        return result == null ? "" : result.messages(name);
    }

    /// The browser derives an option's implicit value by collapsing ASCII whitespace.
    public static String optionValue(Object value) {
        String text = string(value);
        StringBuilder out = new StringBuilder();
        boolean space = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f') {
                space = out.length() > 0;
            } else {
                if (space) {
                    out.append(' ');
                    space = false;
                }
                out.append(c);
            }
        }
        return out.toString();
    }

    public static boolean checked(Object value, Object candidate) {
        if ("true".equalsIgnoreCase(string(candidate))
                && ("on".equalsIgnoreCase(string(value)) || "1".equals(string(value)))) {
            return true;
        }
        return string(value).equals(string(candidate));
    }

    public static Model model(HttpServer.Request request) {
        return new Model(request);
    }

    public static void csrf(ByteSink out, Model model) {
        csrf(out, model, null);
    }

    public static void csrf(ByteSink out, Model model, Object formId) {
        CsrfToken token = (CsrfToken) model.getAttribute("_csrf");
        if (token == null) {
            return;
        }
        out.putAscii("<input type=\"hidden\"");
        attribute(out, "name", token.getParameterName());
        attribute(out, "value", token.getToken());
        attribute(out, "form", formId);
        out.put('>');
    }

    private static String asciiLower(String value) {
        // HTML names and URI schemes are ASCII; don't depend on the default locale.
        char[] chars = null;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                if (chars == null) {
                    chars = value.toCharArray();
                }
                chars[i] = (char) (c + ('a' - 'A'));
            }
        }
        return chars == null ? value : new String(chars);
    }

    public static void safeUrl(String value) {
        String lower = asciiLower(value.trim());
        for (int i = 0; i < lower.length(); i++) {
            if (lower.charAt(i) < 32 || lower.charAt(i) == 127 || lower.charAt(i) == '\\') {
                throw new IllegalArgumentException("Invalid URL");
            }
        }
        int colon = lower.indexOf(':');
        for (int i = 0; i < colon; i++) {
            char c = lower.charAt(i);
            if (c == '/' || c == '?' || c == '#') {
                return;
            }
        }
        if (colon >= 0
                && !lower.startsWith("https:")
                && !lower.startsWith("http:")
                && !lower.startsWith("mailto:")
                && !lower.startsWith("tel:")) {
            throw new IllegalArgumentException("Unsafe URL scheme");
        }
    }

    public static void localLocation(String location) {
        if (location == null
                || !location.startsWith("/")
                || location.startsWith("//")
                || location.indexOf('\\') >= 0) {
            throw new IllegalArgumentException("Redirect must be a local absolute path");
        }
        for (int i = 0; i < location.length(); i++) {
            if (location.charAt(i) <= 32 || location.charAt(i) == 127) {
                throw new IllegalArgumentException("Invalid redirect");
            }
        }
    }

    public static HttpServer.Response redirect(HttpServer.Request request, String location) {
        localLocation(location);
        HttpServer.Response response =
                Htmx.isRequest(request)
                        ? Htmx.redirect(location)
                        : HttpServer.Response.text(303, "").header("Location", location);
        return response.header("Vary", "HX-Request, HX-History-Restore-Request");
    }
}
