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

package com.codename1.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks a class whose constructor-created primitive-array fields are fully
/// ENCAPSULATED, letting ParparVM allocate each instance TOGETHER with those
/// arrays as a single heap block ("fused object"): one allocation instead of
/// several, one object for the garbage collector instead of several, and the
/// arrays share the owner's cache lines. A fused array keeps a completely
/// ordinary header -- all reading code, `System.arraycopy`, iteration and
/// bounds checks work unchanged -- but it has no independent GC identity: it
/// is never separately tracked, marked for reclamation, or swept; it simply
/// lives and dies with its owner.
///
/// The translator fuses a constructor-assigned field when the constructor
/// contains an unconditional `this.f = new T[n]` where `T` is a primitive type
/// and `n` is a constructor parameter or constant (bounds-check guards that
/// throw are fine before it). Constructors keep chaining normally, and every
/// other instantiation path -- reflection, deserialization, oversized arrays
/// that do not fit a fused block -- transparently falls back to ordinary
/// separate allocations with identical semantics.
///
/// THE CONTRACT: the marked class must never let a fused array reference
/// escape into another object's field, an array element, or a static that can
/// outlive the instance. Holding it in locals, passing it as a call argument,
/// or copying its contents out is always safe (the VM keeps the whole block
/// alive through any reference into it held on a thread stack). Typical
/// candidates: string/buffer classes owning their `char[]`, image types owning
/// their pixel `int[]`/`byte[]` data.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@com.codename1.impl.SharedWithBackend
public @interface Fused {
}
