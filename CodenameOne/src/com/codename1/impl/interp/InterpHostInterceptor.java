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
package com.codename1.impl.interp;

/// Answers a host static call in place of the linker.
///
/// The device runtime uses this to stand in for subsystems it cannot honestly
/// provide but a developer still needs to exercise: a purchase flow, a social
/// login. Those are reached through static factories -- `Purchase
/// .getInAppPurchase()`, `FacebookConnect.getInstance()` -- so intercepting the
/// factory is enough to hand pushed code a mock, and every later call lands on
/// the mock by ordinary dispatch.
///
/// It is deliberately narrow. Only static calls are offered, only the
/// interpreter consults it, and the host application is untouched: a mock
/// installed here changes what a *pushed program* sees and nothing else.
///
/// @author Shai Almog
public interface InterpHostInterceptor {
    /// Returned when the call should go to the linker as usual.
    Object NOT_INTERCEPTED = new Object();

    /// Answers a static call, or [#NOT_INTERCEPTED] to decline it.
    ///
    /// #### Parameters
    ///
    /// - `owner`: JVM internal name of the class the call site named
    /// - `name`: method name
    /// - `descriptor`: JVM method descriptor
    /// - `args`: the arguments, already converted for host code
    Object interceptStatic(String owner, String name, String descriptor, Object[] args)
            throws Throwable;
}
