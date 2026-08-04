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

/// Service-provider interface between the public shield API and the attestation engine.
///
/// Application code does not use this package. It exists so the engine that performs attestation,
/// pin enforcement and tamper detection can be supplied separately from the framework, while
/// [com.codename1.security.shield.AppShield] keeps a single stable surface that compiles and runs
/// whether or not an engine is present.
///
/// See [com.codename1.security.shield.spi.ShieldEngine] for the contract, and in particular for
/// the list of responsibilities an engine must not delegate back into open framework code.
package com.codename1.security.shield.spi;
