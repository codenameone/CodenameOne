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
/// The four OSGi types a library asks, to find out whether it is running in
/// an OSGi framework: [FrameworkUtil], [Bundle], [BundleContext] and
/// [ServiceReference], with only the members that question takes.
///
/// A library that supports OSGi and the plain class path names these types
/// in the one method that probes for a framework, and its jar declares the
/// OSGi API an optional dependency. The build's remap step points
/// `org.osgi.framework` at this package, so that such a library links, and
/// [FrameworkUtil#getBundle(Class)] answers what the real one answers for a
/// class no framework loaded: null. The library then takes its class path
/// route.
///
/// This is not an OSGi implementation and will not grow into one. No bundle,
/// context or service reference is ever created; the three interfaces exist
/// so that the probe's descriptors resolve.
package com.codename1.compat.jdk.osgi;
