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

/// Marks a `void` method whose callers never wait for it to finish, so the
/// JavaScript port may let it finish later.
///
/// On the web, a method that can wait -- through `Object.wait()`, `Thread.sleep()`,
/// `invokeAndBlock()`, or any call or interface method that leads to one -- becomes
/// a JavaScript generator, and so does every method that calls it. A method that
/// hands work off and returns, like `Display.callSerially(Runnable)`, carries that
/// cost to every caller only because one rarely taken path runs the work in place.
///
/// With this annotation the JavaScript port runs the method as an ordinary
/// function. Its body runs synchronously, exactly as written, until the first
/// point where it would really wait; from there the rest of it continues on a
/// new green thread and the caller returns. Nothing that calls the method becomes
/// a generator because of it. When the body never waits, nothing changes at all.
/// A caller that is a generator anyway may still run the body in place and wait
/// for it, as it does today for a private method. Every other platform ignores the
/// annotation.
///
/// Use it only on a method whose contract already says it returns without
/// waiting for the work it starts: anything its caller does next must not depend
/// on the body having finished. A method with a mode that waits does not qualify:
/// `EventDispatcher`'s off-EDT delivery waits for the listeners when the dispatcher
/// is blocking, and `NetworkManager` reads `isConsumed()` straight after it.
///
/// Before Codename One starts, `Display.callSerially` runs its runnable in place. If
/// that runnable waits, the rest of it now finishes on its own green thread, and
/// anything it throws from there reaches the uncaught-exception handler rather than
/// the caller.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface JavaScriptDetached {
}
