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
/// The build-time half of FXML and style sheet support in the JavaFX
/// compatibility layer.
///
/// A device has no XML parser to spare, no reflection and no CSS parser, so
/// everything a desktop `FXMLLoader` and style manager work out while the
/// application runs is worked out here, while it is built:
///
/// - [com.codename1.fxml.DesktopResourceCompiler] is what a build plugin
///   calls before javac: it turns every FXML document into Java source and
///   every style sheet into a binary table.
/// - [com.codename1.fxml.FxmlDispatchGenerator] is what the remap step
///   calls after javac: it generates the code that reaches a controller's
///   `@FXML` members.
///
/// The module depends on the JDK, on ASM and on the resource naming rule,
/// and on nothing of the layer itself: the layer's classes are read as
/// class files from the application's class path.
package com.codename1.fxml;
