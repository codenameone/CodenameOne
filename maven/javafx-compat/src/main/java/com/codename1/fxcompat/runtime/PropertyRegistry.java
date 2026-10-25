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
package com.codename1.fxcompat.runtime;

/// The application's property accessor. This is a placeholder: the build
/// replaces it, in every application that uses the JavaFX layer, with a
/// class of the same name that overrides the methods of [PropertyAccess]
/// for the bean classes of that application.
///
/// The placeholder is what an application runs with where no build step
/// generated anything -- a unit test of this module, a desktop run straight
/// from an IDE -- and there it knows no bean, which
/// `javafx.scene.control.cell.PropertyValueFactory` reports by name.
///
/// The build does not copy this class out of the runtime jar; it writes its
/// own. Anything added here would therefore be missing on a device: keep
/// the class empty.
public final class PropertyRegistry extends PropertyAccess {

    /// Creates the accessor.
    public PropertyRegistry() {
        super();
    }
}
