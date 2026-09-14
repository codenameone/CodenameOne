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
package com.demo;

import java.io.IOException;

/**
 * An exception that nothing catches, thrown from inside a catch block.
 *
 * This is the shape that used to be silently discarded on the clean target:
 * throwException walked the try-block stack, found no handler, and RETURNED, so
 * the generated code carried on with the statement after the throw. A server then
 * kept running with whatever half-built state the failed operation left behind --
 * in the case that found this, a null database handle that segfaulted two
 * statements later.
 *
 * Driven by BackendUncaughtExceptionTest, which requires the message, a stack
 * trace and a non-zero exit; the marker below must NOT be printed.
 */
public class Uncaught {
    public static void main(String[] args) throws Exception {
        System.out.println("before the throw");
        try {
            open();
        } catch (IOException err) {
            // Rethrowing from a catch, out of a main that has no other handler.
            throw err;
        }
    }

    private static void open() throws IOException {
        try {
            throw new IOException("deliberate failure with a message");
        } catch (IOException err) {
            throw err;
        }
    }
}
