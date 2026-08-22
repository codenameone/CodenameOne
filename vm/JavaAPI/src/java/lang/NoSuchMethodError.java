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
package java.lang;

/**
 * Thrown when the virtual machine notices that a program tries to reference,
 * on a class or object, a method that does not exist.
 * <p>
 * An ahead-of-time compiled program cannot raise this by itself -- a missing
 * method is a compile error there. The device runtime can: a pushed program is
 * linked against the app that happens to be installed, and naming a method that
 * app does not carry is exactly this condition. The sibling
 * {@link NoSuchFieldError} was already present; this completes the pair.
 */
public class NoSuchMethodError extends IncompatibleClassChangeError {

    private static final long serialVersionUID = -3765521442372831335L;

    /**
     * Constructs a new {@code NoSuchMethodError} that includes the current
     * stack trace.
     */
    public NoSuchMethodError() {
        super();
    }

    /**
     * Constructs a new {@code NoSuchMethodError} with the current stack trace
     * and the specified detail message.
     *
     * @param detailMessage
     *            the detail message for this error.
     */
    public NoSuchMethodError(String detailMessage) {
        super(detailMessage);
    }
}
