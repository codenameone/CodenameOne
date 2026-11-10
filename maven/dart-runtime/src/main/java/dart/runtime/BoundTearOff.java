/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package dart.runtime;

/// An instance-method tear-off (`list.add` as a value). Dart makes two tear-offs of the
/// same method from the same receiver `==` (and of equal hashCode) but not identical. The
/// transpiler emits each one as a TearOff value carrying its receiver and a key naming the
/// method, so Java's own equals and hashCode answer Dart's `==`: a List.remove, HashSet or
/// HashMap needs no help, and nothing is recorded anywhere else.
public interface BoundTearOff {
    /// The object the method was torn off from.
    Object receiver();

    /// The method torn off: its name, or `super:Class.name` for a super tear-off.
    String key();
}
