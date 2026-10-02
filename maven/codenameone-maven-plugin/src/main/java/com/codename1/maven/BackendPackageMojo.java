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
package com.codename1.maven;

import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Translates a backend module to C and compiles it to a native binary:
 * `mvn cn1:backend-package`.
 *
 * The counterpart to {@link BackendRunMojo}. That one runs the module on this JVM
 * in a couple of seconds and is what a developer uses; this one produces the
 * artifact that deploys -- a single executable with no runtime to install, which
 * is what makes a scratch container the size of the binary and a Lambda cold start
 * a process exec.
 *
 * What it does, in order:
 *
 * 1. Compiles the module's sources together with the backend runtime's SHARED and
 *    PARPARVM halves against the ParparVM JavaAPI as the BOOTCLASSPATH. That last
 *    part is the important one: the bootclasspath IS the server-safe surface, so a
 *    reference to something the translated runtime does not have fails here, in
 *    the IDE and in the build, rather than at link time or in production.
 * 2. Runs the translator over the result, with the runtime's C sources already in
 *    the source root -- they have to be there BEFORE it runs, because a native's
 *    Java method is kept alive by its C symbol being present.
 * 3. Compiles the generated C, either with the host compiler or, for a named
 *    Linux target, in a container.
 *
 * The compiler flags are not negotiable and are documented at the call site:
 * generated C relies on wrapping arithmetic, and clang -O3 miscompiles it without
 * them.
 */
// Forks the lifecycle up to compile first, so `mvn cn1:backend-package` on its own does
// the obvious thing on a clean checkout instead of failing on an empty
// target/classes.
@Execute(phase = LifecyclePhase.COMPILE)
@Mojo(name = "backend-package", requiresDependencyResolution = ResolutionScope.COMPILE)
public class BackendPackageMojo extends AbstractBackendNativeMojo {
}
