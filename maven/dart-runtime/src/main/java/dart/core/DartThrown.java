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
package dart.core;

/**
 * Carries a thrown value that is not a Java throwable -- Dart can throw any object:
 * {@code throw 'boom'}, {@code throw token}. The value itself is kept, and a catch
 * clause binds it back ({@link dart.runtime.DartRuntime#caught}), so {@code catch (e)}
 * sees the identical object with its type and fields. It used to be replaced by a
 * DartException holding only its text: identity, {@code is} tests and fields were lost,
 * and a thrown string printed as "Exception: boom".
 */
public final class DartThrown extends RuntimeException {

    private final Object value;

    public DartThrown(Object value) {
        // The no-argument constructor: the (message, cause, suppression, stack) one is not
        // in ParparVM's JavaAPI or CLDC11, and using it would break every native build.
        super();
        this.value = value;
    }

    /** The object the Dart code threw. */
    public Object value() {
        return value;
    }

    @Override
    public String getMessage() {
        return dart.runtime.DartRuntime.str(value);
    }

    @Override
    public String toString() {
        return dart.runtime.DartRuntime.str(value);
    }
}
