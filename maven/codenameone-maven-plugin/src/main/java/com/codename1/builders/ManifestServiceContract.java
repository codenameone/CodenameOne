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
package com.codename1.builders;

/**
 * What a {@code <service>} the platform binds has to carry, and whether a
 * hand-written one does.
 *
 * <p>Every generated service in this package is suppressed when the project
 * declares one of the same class in {@code android.xapplication}, which is
 * right -- the project may have reasons -- but only if what it declared can
 * be bound. Suppressing on the class NAME alone produced the worst kind of
 * build: green, complete-looking, and inert on the device. Telecom refuses to
 * bind a ConnectionService without
 * {@code BIND_TELECOM_CONNECTION_SERVICE}; a VpnService without
 * {@code BIND_VPN_SERVICE} is never bound either; a component with an intent
 * filter and no {@code android:exported} fails the build outright from API
 * 31; and from API 34 {@code startForeground} is refused for a
 * {@code foregroundServiceType} the manifest never declared. None of those
 * is visible from the class name.</p>
 *
 * <p>Shared rather than written per feature. The call fragments and the VPN
 * fragments arrived at the same suppression rule separately and had
 * different amounts of it, which is how one of them ended up checking a
 * permission the other did not.</p>
 */
final class ManifestServiceContract {

    private ManifestServiceContract() {
    }

    /**
     * Returns {@code xml} with every {@code <!-- ... -->} span removed.
     *
     * <p>An unterminated comment swallows the rest of the fragment, which is
     * what an XML parser would do with it too. Everything here asks whether
     * the project SUPPLIES something, and a plain text search answers yes for
     * a declaration it has commented out.</p>
     *
     * @param xml the fragment
     * @return the fragment with its comments removed
     */
    static String withoutComments(String xml) {
        String text = xml == null ? "" : xml;
        int open = text.indexOf("<!--");
        if (open < 0) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        int from = 0;
        while (open >= 0) {
            sb.append(text, from, open);
            int close = text.indexOf("-->", open + 4);
            if (close < 0) {
                return sb.toString();
            }
            from = close + 3;
            open = text.indexOf("<!--", from);
        }
        sb.append(text, from, text.length());
        return sb.toString();
    }

    /**
     * Returns the index of the live {@code <service>} tag for {@code name},
     * or -1.
     *
     * <p>The ELEMENT, not the string. The class name appearing on a
     * {@code <meta-data android:name="...">} as a value is not a
     * declaration, and neither is one inside a comment.</p>
     *
     * @param live the fragment with comments already removed
     * @param name the service class name
     * @return the index of the opening angle bracket, or -1
     */
    private static int liveServiceStart(String live, String name) {
        int at = elementStart(live, "<service", 0);
        while (at >= 0) {
            int close = live.indexOf('>', at);
            if (close < 0) {
                return -1;
            }
            String tag = live.substring(at, close);
            if (name.equals(attributeValue(tag, "android:name"))) {
                return at;
            }
            at = elementStart(live, "<service", close);
        }
        return -1;
    }

    /**
     * Returns the index of the next {@code element} start tag at or after
     * {@code from}, or -1.
     *
     * <p>The ELEMENT, not the prefix: a bare {@code indexOf("<service")}
     * also matches {@code <serviceGroup}, and the tag it then read belongs
     * to something else entirely.</p>
     */
    private static int elementStart(String live, String element, int from) {
        int at = live.indexOf(element, from);
        while (at >= 0) {
            int after = at + element.length();
            if (after >= live.length()) {
                return -1;
            }
            char c = live.charAt(after);
            if (isSpace(c) || c == '>' || c == '/') {
                return at;
            }
            at = live.indexOf(element, at + 1);
        }
        return -1;
    }

    /**
     * Returns whether {@code existing} already has a live {@code <service>}
     * for {@code name}.
     *
     * @param existing the accumulated fragment
     * @param name     the service class name
     * @return true when a live declaration is already there
     */
    static boolean declares(String existing, String name) {
        return liveServiceStart(withoutComments(existing), name) >= 0;
    }

    /**
     * Returns whether a LIVE {@code <uses-permission>} declares {@code name}
     * with no bound on it.
     *
     * <p>The name alone is not the question. A declaration carrying
     * {@code android:maxSdkVersion} asks for the permission only up to that
     * level, and the generated ones are unbounded because the platform needs
     * them at the level the app actually runs -- so suppressing an
     * unbounded FOREGROUND_SERVICE for a declaration capped at 33 left a
     * VoIP call on Android 14 unable to promote its service, with call
     * integration detected and the manifest looking complete.</p>
     *
     * <p>The name appearing as a VALUE is not a declaration either: an
     * {@code android:permission} attribute names what a component REQUIRES
     * of its binder, which says nothing about what this app holds.</p>
     *
     * @param existing the accumulated fragment
     * @param name     the permission name
     * @return true when an unbounded live declaration is already there
     */
    static boolean declaresPermission(String existing, String name) {
        String live = withoutComments(existing);
        int at = elementStart(live, "<uses-permission", 0);
        while (at >= 0) {
            int close = live.indexOf('>', at);
            if (close < 0) {
                return false;
            }
            String tag = live.substring(at, close);
            if (name.equals(attributeValue(tag, "android:name"))
                    && attributeValue(tag, "android:maxSdkVersion") == null) {
                return true;
            }
            at = elementStart(live, "<uses-permission", close);
        }
        return false;
    }

    /**
     * Returns what a project-declared service is missing, or null when it
     * carries everything the generated one would have.
     *
     * @param existing              the accumulated fragment
     * @param name                  the service class name
     * @param permission            the {@code android:permission} the system
     *                              binder must hold
     * @param action                the intent-filter action the system finds
     *                              it by
     * @param foregroundServiceType the {@code android:foregroundServiceType}
     *                              token the service promotes with, or null
     *                              when it never promotes
     * @return a phrase naming what is absent, or null when nothing is
     */
    static String whatIsMissing(String existing, String name,
            String permission, String action, String foregroundServiceType) {
        String live = withoutComments(existing);
        int at = liveServiceStart(live, name);
        if (at < 0) {
            return null;
        }
        int close = live.indexOf('>', at);
        String tag = live.substring(at, close);
        StringBuilder missing = new StringBuilder();
        if (!hasAttribute(tag, "android:permission", permission)) {
            append(missing, "android:permission=\"" + permission + "\"");
        }
        if (!hasAttribute(tag, "android:exported", "true")) {
            // Not pedantry: a component with an intent filter and no
            // android:exported fails the build from API 31, and an
            // unexported one cannot be bound by the system process at all.
            append(missing, "android:exported=\"true\"");
        }
        if (foregroundServiceType != null
                && !hasToken(tag, "android:foregroundServiceType",
                        foregroundServiceType)) {
            append(missing, "android:foregroundServiceType=\""
                    + foregroundServiceType + "\"");
        }
        // A self-closing element has no body, so it carries no filter.
        String body = "";
        if (live.charAt(close - 1) != '/') {
            int end = live.indexOf("</service>", close);
            body = end < 0 ? live.substring(close) : live.substring(close, end);
        }
        if (!filtersOn(body, action)) {
            append(missing, "an <intent-filter> with the " + action
                    + " action");
        }
        return missing.length() == 0 ? null : missing.toString();
    }

    /** Adds one phrase to a comma-and-"and" list. */
    private static void append(StringBuilder list, String item) {
        if (list.length() > 0) {
            list.append(", and ");
        }
        list.append(item);
    }

    /** Whether {@code tag} sets {@code attribute} to exactly {@code value}. */
    private static boolean hasAttribute(String tag, String attribute,
            String value) {
        return value.equals(attributeValue(tag, attribute));
    }

    /**
     * Whether {@code tag} sets {@code attribute} to a {@code |}-separated
     * list containing {@code token}.
     *
     * <p>android:foregroundServiceType is a flag attribute: a service that
     * promotes for two reasons names both, separated by a pipe. Testing for
     * the whole value would reject a declaration that is not merely valid but
     * more complete than the generated one.</p>
     */
    private static boolean hasToken(String tag, String attribute,
            String token) {
        String value = attributeValue(tag, attribute);
        if (value == null) {
            return false;
        }
        int at = 0;
        while (at <= value.length()) {
            int bar = value.indexOf('|', at);
            String part = bar < 0 ? value.substring(at)
                    : value.substring(at, bar);
            if (token.equals(part.trim())) {
                return true;
            }
            if (bar < 0) {
                return false;
            }
            at = bar + 1;
        }
        return false;
    }

    /**
     * The value of {@code attribute} in {@code tag}, or null.
     *
     * <p>Whitespace around the {@code =} is legal XML, and either quoting
     * is, so {@code android:name = 'x'} is the same declaration as
     * {@code android:name="x"}. Read as a literal substring instead, a
     * project that formatted its manifest that way had its service reported
     * ABSENT -- so the builder appended a second declaration of the same
     * class -- and its attributes reported MISSING, refusing a configuration
     * that was correct.</p>
     *
     * <p>The name is matched as a whole attribute: {@code android:name} must
     * not be found inside {@code android:nameGroup}, and must not be the
     * tail of a longer prefixed name.</p>
     */
    private static String attributeValue(String tag, String attribute) {
        int at = tag.indexOf(attribute);
        while (at >= 0) {
            int i = at + attribute.length();
            boolean whole = at == 0 || !isNameChar(tag.charAt(at - 1));
            while (i < tag.length() && isSpace(tag.charAt(i))) {
                i++;
            }
            if (whole && i < tag.length() && tag.charAt(i) == '=') {
                i++;
                while (i < tag.length() && isSpace(tag.charAt(i))) {
                    i++;
                }
                if (i < tag.length()
                        && (tag.charAt(i) == '"' || tag.charAt(i) == '\'')) {
                    char quote = tag.charAt(i);
                    int end = tag.indexOf(quote, i + 1);
                    return end < 0 ? null : tag.substring(i + 1, end);
                }
                return null;
            }
            at = tag.indexOf(attribute, at + 1);
        }
        return null;
    }

    /** Whether this character can appear inside an XML attribute name. */
    private static boolean isNameChar(char c) {
        return c == ':' || c == '_' || c == '-' || c == '.'
                || (c >= '0' && c <= '9')
                || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    /** Whether this character is XML whitespace. */
    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    /**
     * Whether the service body declares {@code action} inside an intent
     * filter.
     *
     * <p>The filter, not the body. A search for the action name anywhere
     * matched a {@code <meta-data android:name="android.net.VpnService">},
     * which tells the system nothing -- the component is still undiscoverable
     * -- and the generated element was suppressed for it.</p>
     */
    private static boolean filtersOn(String body, String action) {
        int at = elementStart(body, "<intent-filter", 0);
        while (at >= 0) {
            int end = body.indexOf("</intent-filter>", at);
            String filter = end < 0 ? body.substring(at)
                    : body.substring(at, end);
            int actionAt = elementStart(filter, "<action", 0);
            while (actionAt >= 0) {
                int close = filter.indexOf('>', actionAt);
                if (close < 0) {
                    break;
                }
                String tag = filter.substring(actionAt, close);
                if (hasAttribute(tag, "android:name", action)) {
                    return true;
                }
                actionAt = elementStart(filter, "<action", close);
            }
            if (end < 0) {
                return false;
            }
            at = elementStart(body, "<intent-filter", end);
        }
        return false;
    }
}
