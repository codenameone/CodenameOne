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
package com.codename1.realtime;

/// Thrown out of a [RealtimeTask]'s `cycle()` when the cycle allocates beyond its
/// budget ([RealtimeParameters#setAllocationBudget(int)], zero unless set).
///
/// It ends the steady state: the thread delivers it to [RealtimeTask#failed(Throwable)]
/// and stops. The message names the class whose allocation was refused. The build
/// rejects the allocations it can see, so this one came from a path the check cannot
/// follow -- a call through an interface or a method overridden in several classes.
@com.codename1.impl.SharedWithBackend
public class RealtimeAllocationError extends Error { //NOPMD DoNotExtendJavaLangError - an Error so catch (Exception) in a cycle cannot swallow it

    /// Creates an error with the given message.
    public RealtimeAllocationError(String message) {
        super(message);
    }
}
