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
package com.codename1.health;

/// Creates [HealthBackgroundListener] instances after the app's process has
/// been killed and relaunched.
///
/// #### Why this exists rather than reflection
///
/// When the operating system relaunches an app in the background to deliver
/// health data, the only durable record of which listener to invoke is
/// whatever was persisted -- a name, not a `Class`. Resolving that name
/// reflectively is the obvious approach and the wrong one on this
/// framework's targets:
///
/// - A class referenced only by a string is invisible to the iOS and
///   JavaScript translators' dead-code elimination, so it can be stripped
///   out of the very build that needs it.
/// - Obfuscation renames the class, so a name persisted by an earlier
///   version of the app no longer resolves.
///
/// So the binding is produced at **build time** instead. The build server
/// scans for implementations of [HealthBackgroundListener], generates a
/// factory that constructs each one with a direct `new` expression -- a
/// real reference, which dead-code elimination and obfuscation both follow
/// correctly -- and registers it through
/// [HealthStore#setBackgroundListenerFactory(HealthBackgroundListenerFactory)]
/// during app startup.
///
/// Application code never implements or calls this.
public interface HealthBackgroundListenerFactory {

    /// Creates the listener registered under `className`, or returns null
    /// when this build has no such listener.
    ///
    /// `className` is the source-level class name recorded when the
    /// subscription was registered. It stays valid across obfuscation
    /// because the build server emits a keep rule for every class it
    /// generates a binding for.
    HealthBackgroundListener create(String className);
}
