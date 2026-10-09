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

/// Whether a chain uses the HTTP session, and what happens to it at sign-in.
///
/// ```java
/// http.sessionManagement(session ->
///         session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
/// ```
public final class SessionManagementConfigurer extends SecurityConfigurer {
    private SessionCreationPolicy policy = SessionCreationPolicy.IF_REQUIRED;
    private int fixation = SessionAuthentication.CHANGE_SESSION_ID;

    SessionManagementConfigurer() {
    }

    /// See [SessionCreationPolicy]; `IF_REQUIRED` unless set.
    public SessionManagementConfigurer sessionCreationPolicy(SessionCreationPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("sessionCreationPolicy cannot be null");
        }
        this.policy = policy;
        return this;
    }

    /// What protects a sign-in from a session id planted on the client
    /// beforehand; the id is changed unless set otherwise.
    public SessionManagementConfigurer sessionFixation(
            Customizer<SessionFixationConfigurer> sessionFixationCustomizer) {
        sessionFixationCustomizer.customize(new SessionFixationConfigurer());
        return this;
    }

    SessionCreationPolicy getPolicy() {
        return policy;
    }

    SessionAuthentication strategy() {
        return new SessionAuthentication(fixation, policy == SessionCreationPolicy.STATELESS);
    }

    /// The choices for [SessionManagementConfigurer#sessionFixation].
    public final class SessionFixationConfigurer {
        private SessionFixationConfigurer() {
        }

        /// Gives the session a new id and keeps its attributes. The default.
        public SessionManagementConfigurer changeSessionId() {
            fixation = SessionAuthentication.CHANGE_SESSION_ID;
            return SessionManagementConfigurer.this;
        }

        /// The same as [#changeSessionId] here: the attributes move to a new id.
        public SessionManagementConfigurer migrateSession() {
            fixation = SessionAuthentication.CHANGE_SESSION_ID;
            return SessionManagementConfigurer.this;
        }

        /// Ends the session and starts an empty one.
        public SessionManagementConfigurer newSession() {
            fixation = SessionAuthentication.NEW_SESSION;
            return SessionManagementConfigurer.this;
        }

        /// Leaves the session as it is. A client that was handed its session id
        /// by somebody else stays on it after signing in.
        public SessionManagementConfigurer none() {
            fixation = SessionAuthentication.NONE;
            return SessionManagementConfigurer.this;
        }
    }
}
