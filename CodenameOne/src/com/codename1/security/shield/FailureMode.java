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
package com.codename1.security.shield;

/// What a protected host should do when a token cannot be obtained.
///
/// This is deliberately a per-host decision. An app typically wants [#CLOSED] on the handful of
/// endpoints that move money or read personal data, and [#OPEN] everywhere else, so that a shield
/// service outage degrades one feature rather than bricking the app.
public enum FailureMode {

    /// Send the request without a token. The customer's backend still decides what to do with an
    /// unattested request; this only means the client does not block it locally.
    ///
    /// This is the default, and it is the only behaviour available when the app was built without
    /// the enterprise engine.
    OPEN,

    /// Refuse to send the request, failing it with a [ShieldException] carrying the reason. Use
    /// this only where a false negative is more acceptable than an unattested call, and only after
    /// reading [ShieldStatus#isTransient()] -- most token failures are network problems, not
    /// compromised devices.
    CLOSED
}
