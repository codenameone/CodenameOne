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
package com.codename1.io.mqtt;

/// Topic names and topic filters: validation and the `+` / `#` matching rules of
/// MQTT 3.1.1 section 4.7.
public final class MqttTopic {
    private MqttTopic() {
    }

    /// Checks a topic name, the thing a message is published to.
    ///
    /// #### Parameters
    ///
    /// - `topic`: the name
    ///
    /// #### Throws
    ///
    /// `IllegalArgumentException` when it is empty, contains a wildcard, a NUL
    /// character, or is longer than 65535 UTF-8 bytes
    public static void validateName(String topic) {
        checkCommon(topic);
        if (topic.indexOf('+') >= 0 || topic.indexOf('#') >= 0) {
            throw new IllegalArgumentException("Topic name must not contain wildcards: " + topic);
        }
    }

    /// Checks a topic filter, the thing a subscription is made with.
    ///
    /// #### Parameters
    ///
    /// - `filter`: the filter
    ///
    /// #### Throws
    ///
    /// `IllegalArgumentException` when it is empty, has a `+` that is not a whole
    /// level, a `#` that is not the whole last level, or a NUL character
    public static void validateFilter(String filter) {
        checkCommon(filter);
        int n = filter.length();
        for (int i = 0; i < n; i++) {
            char c = filter.charAt(i);
            boolean levelStart = i == 0 || filter.charAt(i - 1) == '/';
            boolean levelEnd = i == n - 1 || filter.charAt(i + 1) == '/';
            if (c == '+' && !(levelStart && levelEnd)) {
                throw new IllegalArgumentException("'+' must be a whole level: " + filter);
            }
            if (c == '#' && !(levelStart && i == n - 1)) {
                throw new IllegalArgumentException("'#' must be the whole last level: " + filter);
            }
        }
    }

    private static void checkCommon(String s) {
        if (s == null || s.length() == 0) {
            throw new IllegalArgumentException("Topic must not be empty");
        }
        if (s.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Topic must not contain U+0000");
        }
        if (s.length() > 21845) {
            // 3 bytes per char is the worst case; the exact check is made by the codec.
            try {
                if (s.getBytes("UTF-8").length > 65535) {
                    throw new IllegalArgumentException("Topic is longer than 65535 bytes");
                }
            } catch (java.io.UnsupportedEncodingException e) {
                throw new IllegalArgumentException("UTF-8 is not available");
            }
        }
    }

    /// Tells whether a topic name matches a topic filter. A filter that starts
    /// with a wildcard does not match a name that starts with `$`.
    ///
    /// #### Parameters
    ///
    /// - `filter`: a valid topic filter
    ///
    /// - `topic`: a valid topic name
    ///
    /// #### Returns
    ///
    /// true when the name is covered by the filter
    public static boolean matches(String filter, String topic) {
        if (topic.length() > 0 && topic.charAt(0) == '$' && filter.length() > 0
                && (filter.charAt(0) == '+' || filter.charAt(0) == '#')) {
            return false;
        }
        int fi = 0;
        int ti = 0;
        int fn = filter.length();
        int tn = topic.length();
        while (true) {
            int fe = filter.indexOf('/', fi);
            if (fe < 0) {
                fe = fn;
            }
            boolean lastFilterLevel = fe == fn;
            if (fe - fi == 1 && filter.charAt(fi) == '#') {
                return true;
            }
            int te = topic.indexOf('/', ti);
            if (te < 0) {
                te = tn;
            }
            boolean lastTopicLevel = te == tn;
            boolean plus = fe - fi == 1 && filter.charAt(fi) == '+';
            if (!plus && !filter.regionMatches(fi, topic, ti, fe - fi)) {
                return false;
            }
            if (!plus && fe - fi != te - ti) {
                return false;
            }
            if (lastFilterLevel || lastTopicLevel) {
                if (lastFilterLevel && lastTopicLevel) {
                    return true;
                }
                // "a/#" also matches "a": the parent level.
                if (lastTopicLevel && fe + 2 == fn && filter.charAt(fe + 1) == '#') {
                    return true;
                }
                return false;
            }
            fi = fe + 1;
            ti = te + 1;
        }
    }
}
