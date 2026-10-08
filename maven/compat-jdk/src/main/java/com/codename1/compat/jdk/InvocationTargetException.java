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
package com.codename1.compat.jdk;

/// `java.lang.reflect.InvocationTargetException` for the Codename One
/// runtime. A device has no reflective invocation, so nothing here throws
/// it on its own account: it exists for the code that declares, catches or
/// wraps it, `EventQueue.invokeAndWait` and `SwingUtilities.invokeAndWait`
/// first among them.
public class InvocationTargetException extends Exception {

    private static final long serialVersionUID = 1L;

    private final Throwable target;

    protected InvocationTargetException() {
        super((Throwable) null);
        this.target = null;
    }

    public InvocationTargetException(Throwable target) {
        super((Throwable) null);
        this.target = target;
    }

    public InvocationTargetException(Throwable target, String s) {
        super(s, null);
        this.target = target;
    }

    public Throwable getTargetException() {
        return target;
    }

    @Override
    public Throwable getCause() {
        return target;
    }
}
