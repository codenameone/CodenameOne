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
package com.codename1.settings.extensions;

/// Edits the dependency declarations in a project's build file, as text.
///
/// The add-on page installs and removes library dependencies without knowing
/// which build tool the project uses: it asks [forBuildSystem(String)] for the
/// editor that matches the launch binding's `buildSystem=` and hands it the
/// file named by the binding. The editors do string surgery rather than
/// building a model of the file, so everything the developer wrote around the
/// declaration -- formatting, comments, other blocks -- survives an edit.
///
/// Every method is pure: it takes the file's text and returns the new text,
/// returning the input unchanged when there is nothing to do, so the caller
/// can tell "already there" from "changed" with `equals`.
public interface DependencyEditor {
    /// Whether `text` already declares `dependency`, matched by group and
    /// artifact only: a different version is still the same library.
    boolean contains(String text, MavenDependency dependency);

    /// `text` with `dependency` declared, or `text` itself when it already is.
    String add(String text, MavenDependency dependency);

    /// `text` without the declaration of `dependency`, or `text` itself when
    /// it declares none.
    String remove(String text, MavenDependency dependency);

    /// The name the UI uses for the file, e.g. `common/pom.xml`.
    String fileLabel();

    /// The editor for a binding's `buildSystem=` value: [GradleBuildEditor]
    /// for `GRADLE`, [PomEditor] for `MAVEN`, and null otherwise. An Ant
    /// project declares no Maven coordinates at all -- it installs legacy
    /// `.cn1lib` files -- so it has no dependency file to edit.
    static DependencyEditor forBuildSystem(String buildSystem) {
        if ("GRADLE".equals(buildSystem)) {
            return GradleBuildEditor.INSTANCE;
        }
        if ("MAVEN".equals(buildSystem)) {
            return PomEditor.INSTANCE;
        }
        return null;
    }
}
