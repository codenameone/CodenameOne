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
package com.codename1.home;

/// Raised when the app itself is missing something the backend needs -- an
/// entitlement, a build hint, a Google Cloud project id -- rather than the
/// user or the device being at fault.
///
/// This is a **developer** error, and it is separated from the rest of
/// [HomeException] because the recovery is different in kind: nothing the user
/// can do will fix it, so an app must not offer them a retry or a settings
/// link. The message repeats the text from
/// [SmartHome#getConfigurationProblems()], which names each missing piece and
/// the build hint that supplies it.
///
/// It is thrown in development and reported through the `AsyncResource` in
/// production for the same reason every other failure here is: an operation
/// that fails must fail through one channel, so a caller has one place to
/// handle it.
public class HomeConfigurationException extends HomeException {

    /// Creates an exception naming what the build is missing.
    ///
    /// #### Parameters
    ///
    /// - `message`: what is missing and which build hint supplies it
    public HomeConfigurationException(String message) {
        super(HomeError.NOT_CONFIGURED, message);
    }
}
