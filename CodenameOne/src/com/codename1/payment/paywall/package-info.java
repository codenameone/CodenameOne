/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
/// Paywalls for the Codename One Commerce service: the screen that presents
/// the packages of an [com.codename1.payment.Offering] and sells one.
///
/// A paywall is a JSON document designed in the console and delivered with
/// the offering, so its content can change without a new build.
/// [com.codename1.payment.paywall.PaywallView] builds it from standard
/// components with documented UIIDs, which a theme can restyle, and
/// [com.codename1.payment.paywall.Paywall] shows it as a form of its own. An
/// offering with no document gets a built-in one.
package com.codename1.payment.paywall;
