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
/// JDK classes that code written for another platform expects and the
/// Codename One device runtime does not have, implemented over what the
/// device does have.
///
/// Every class here has the simple name and the public API of the JDK class
/// it stands in for. Nothing refers to these classes by this package name: the
/// build's remap step rewrites an application's references to the JDK names
/// (`java.io.File`, `java.util.Optional`, `java.text.DecimalFormat`,
/// `java.net.URL`, ...) so that they point here, and copies the classes into
/// the application.
///
/// Three groups live in the package: the file and buffered stream classes of
/// `java.io`, the `java.util` and `java.util.concurrent` classes the device
/// library leaves out (`Optional`, `ResourceBundle`, `ConcurrentHashMap`,
/// `CopyOnWriteArrayList`, ...), and the number and message formats of
/// `java.text` together with `java.net.URL`.
///
/// Codename One runs user interface code on one thread. The classes named
/// after `java.util.concurrent` keep the API and the iteration guarantees of
/// the originals and do not synchronize; see each class.
package com.codename1.compat.jdk;
