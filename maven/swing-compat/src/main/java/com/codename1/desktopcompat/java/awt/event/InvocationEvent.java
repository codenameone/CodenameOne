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
package com.codename1.desktopcompat.java.awt.event;

import com.codename1.desktopcompat.java.awt.ActiveEvent;
import com.codename1.desktopcompat.java.awt.AWTEvent;

/// An event that runs a `Runnable` when it is dispatched.
///
/// The user interface is single threaded here, so the notifier of the JDK
/// constructors is accepted but nobody waits on it.
public class InvocationEvent extends AWTEvent implements ActiveEvent {

    public static final int INVOCATION_FIRST = 1200;

    public static final int INVOCATION_DEFAULT = 1200;

    public static final int INVOCATION_LAST = 1200;

    private final Runnable runnable;

    private final boolean catchExceptions;

    private final long when;

    private Throwable throwable;

    public InvocationEvent(Object source, Runnable runnable) {
        this(source, INVOCATION_DEFAULT, runnable, null, false);
    }

    public InvocationEvent(Object source, Runnable runnable, Object notifier, boolean catchThrowables) {
        this(source, INVOCATION_DEFAULT, runnable, notifier, catchThrowables);
    }

    protected InvocationEvent(Object source, int id, Runnable runnable, Object notifier,
            boolean catchThrowables) {
        super(source, id);
        this.runnable = runnable;
        this.catchExceptions = catchThrowables;
        this.when = System.currentTimeMillis();
    }

    public void dispatch() {
        if (catchExceptions) {
            try {
                runnable.run();
            } catch (Throwable t) {
                throwable = t;
            }
        } else {
            runnable.run();
        }
    }

    public Exception getException() {
        return catchExceptions && throwable instanceof Exception ? (Exception) throwable : null;
    }

    public Throwable getThrowable() {
        return catchExceptions ? throwable : null;
    }

    public long getWhen() {
        return when;
    }

    @Override
    public String paramString() {
        String typeStr;
        switch (id) {
            case INVOCATION_DEFAULT:
                typeStr = "INVOCATION_DEFAULT";
                break;
            default:
                typeStr = "unknown type";
                break;
        }
        return typeStr + ",runnable=" + runnable + ",when=" + when;
    }
}
