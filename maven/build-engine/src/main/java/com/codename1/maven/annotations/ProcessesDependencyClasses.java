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
package com.codename1.maven.annotations;

/// A processor that also wants the annotated classes of the module's
/// DEPENDENCIES, not only the module's own.
///
/// A module is normally processed alone: the build scans its output directory
/// and nothing else. That leaves no place for a class two modules share. A REST
/// contract and its transfer objects belong in a library both the application
/// and its server depend on, and scanned that way neither side ever sees them --
/// the application gets no client and the server no routes, with nothing in
/// either build to say so.
///
/// A processor implementing this is offered the classes [DependencyClasses]
/// finds as well, through the same `processClass`. Whatever it generates still
/// goes into the module being built; a dependency is somebody else's output and
/// is never written to.
public interface ProcessesDependencyClasses {

    /// Whether this build is one where dependency classes should be offered.
    ///
    /// Asked once per run, after `start`. A processor answers from the compile
    /// classpath: the client half of a contract belongs in a module that has the
    /// Codename One core, and offering it to a server module would generate code
    /// that cannot link there.
    boolean acceptsDependencyClasses(ProcessorContext ctx);
}
