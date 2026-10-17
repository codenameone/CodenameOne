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
/// Loading a user interface from an FXML document.
///
/// #### How it works here
///
/// A desktop `FXMLLoader` parses the document while the application runs
/// and builds the scene graph by reflection. A device has neither an XML
/// parser to spare nor reflection, so the build does that work: every
/// `.fxml` file among the application's desktop resources is compiled into
/// a Java class that builds the same tree with plain constructors and
/// setters, and [javafx.fxml.FXMLLoader] only finds the class for the
/// location it is given and runs it. What a controller's
/// [javafx.fxml.FXML] members need -- setting a private field, calling a
/// private handler -- is generated too, as direct code.
///
/// The consequence to know: a document is fixed when the application is
/// built. One that is fetched or written at run time cannot be loaded, and
/// loading it fails with a [javafx.fxml.LoadException] that says so.
package javafx.fxml;
