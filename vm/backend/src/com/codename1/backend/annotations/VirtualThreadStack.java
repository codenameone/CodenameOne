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
package com.codename1.backend.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Gives every connection's virtual thread at least this much C stack.
///
/// A virtual thread's C stack holds native call depth, not Java data, and the
/// default of 64KB is far more than an ordinary handler reaches. A handler that
/// recurses deeply -- a recursive descent parser, a deep template, a regular
/// expression engine -- can still run out, and an overflow there kills the
/// connection's host thread with nothing but an empty reply to show for it.
/// Put this on any class in the backend module to raise the floor:
///
/// ```java
/// @VirtualThreadStack("1MB")
/// @RestController
/// public class ReportController { ... }
/// ```
///
/// The size is a number of bytes, optionally followed by `KB` or `MB` (case
/// does not matter; `1KB` is 1024 bytes). It is read at build time, so a value
/// that is not a size fails the build and names the class it sits on. When
/// several classes carry the annotation the largest size wins, the same way
/// `HttpServer.requireVirtualThreadStack` only ever raises the size.
///
/// The `CN1_HTTP_VT_STACK` environment variable still applies at start-up, and
/// the larger of the two is used. The stack is mapped lazily, so a generous
/// size costs address space rather than memory.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface VirtualThreadStack {
    /// The minimum C stack per virtual thread: bytes, or a number with a `KB`
    /// or `MB` suffix, such as `"512KB"` or `"1MB"`.
    String value();
}
