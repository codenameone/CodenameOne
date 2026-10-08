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
/// The bound-property subset of `java.beans`: property change events, their
/// listeners, the proxy that binds a listener to one property name, the
/// support class that fires them, and the veto exception.
///
/// The support class does not synchronize.
///
/// [Beans] answers the two environment questions, and [Introspector] answers
/// a [BeanInfo] that holds a class's [BeanDescriptor] and nothing discovered
/// about it: properties, events and methods are found by reflection on a
/// desktop, and a device has none.
///
/// Long-term persistence ([XMLEncoder], [XMLDecoder], [Encoder],
/// [PersistenceDelegate], [Statement], [Expression]) is reflection from end
/// to end and does not work. Those classes are here, marked
/// `com.codename1.compat.jdk.LinkOnly`, because libraries written against
/// Swing offer to save themselves that way beside everything else they do
/// and could not be bundled otherwise: the build accepts a reference from a
/// bundled library and reports one from an application's own code, as it
/// does for API that is absent.
package com.codename1.desktopcompat.java.beans;
