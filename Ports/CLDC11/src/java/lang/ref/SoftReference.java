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
package java.lang.ref;
/// A reference the collector keeps while the referent is being used and memory allows, and
/// clears before the process runs out.
///
/// This directory is the SUPPORTED API SURFACE, not an implementation: `maven/java-runtime`
/// compiles it and `BytecodeComplianceMojo` indexes the result as the set of types an
/// application is allowed to touch. A class implemented in `vm/JavaAPI` but missing here is
/// therefore invisible to user code -- javac resolves it against the host JDK and the
/// compliance check then rejects it as forbidden API, which is a confusing way to learn a
/// feature was never exported. The bodies are deliberately inert for the same reason the
/// neighbouring stubs are.
public class SoftReference extends java.lang.ref.Reference{
    /// Creates a new soft reference that refers to the given object.
    public SoftReference(java.lang.Object ref){
    }

}
