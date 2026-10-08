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
package com.codename1.backend.security;

import com.codename1.backend.HttpServer;
import java.util.Collections;
import java.util.Map;

/// Decides whether a rule applies to a request: which chain guards it, which
/// authorization rule covers it, which requests CSRF protection leaves alone.
public interface RequestMatcher {
    boolean matches(HttpServer.Request request);

    /// The match with the variables it bound: `{id}` in a path pattern.
    default MatchResult matcher(HttpServer.Request request) {
        return matches(request) ? MatchResult.match() : MatchResult.notMatch();
    }

    /// Whether a request matched, and the variables its pattern bound.
    final class MatchResult {
        private static final MatchResult NO = new MatchResult(false,
                Collections.<String, String>emptyMap());
        private static final MatchResult YES = new MatchResult(true,
                Collections.<String, String>emptyMap());
        private final boolean match;
        private final Map<String, String> variables;

        private MatchResult(boolean match, Map<String, String> variables) {
            this.match = match;
            this.variables = variables;
        }

        public boolean isMatch() {
            return match;
        }

        /// The variables by name; empty when the pattern has none.
        public Map<String, String> getVariables() {
            return variables;
        }

        public static MatchResult match() {
            return YES;
        }

        public static MatchResult match(Map<String, String> variables) {
            return new MatchResult(true, Collections.unmodifiableMap(variables));
        }

        public static MatchResult notMatch() {
            return NO;
        }
    }
}
