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
/** Rethrowing a catch parameter throws what the try block threw (JLS 11.2.2), not its declared type. */
import java.io.*;
public class PreciseRethrow {
    static void io(boolean fail) throws IOException {
        if (fail) {
            throw new FileNotFoundException("missing");
        }
    }
    static void multi(boolean fail) throws IOException {
        try {
            io(fail);
            Object o = null;
            o.hashCode();
        } catch (IOException | RuntimeException e) {
            System.out.println("multi caught " + e.getClass().getSimpleName());
            throw e;
        }
    }
    static void uni(boolean fail) throws IOException {
        try {
            io(fail);
        } catch (Exception e) {
            System.out.println("uni caught " + e.getClass().getSimpleName());
            throw e;
        }
    }
    static void narrowed() throws FileNotFoundException {
        try {
            if (System.nanoTime() != 0) {
                throw new FileNotFoundException("narrow");
            }
        } catch (IOException e) {
            throw e instanceof FileNotFoundException ? (FileNotFoundException) e : new FileNotFoundException();
        }
    }
    static void earlierClause() throws IOException {
        try {
            io(true);
        } catch (FileNotFoundException e) {
            System.out.println("first clause");
        } catch (IOException e) {
            throw e;
        }
    }
    public static void main(String[] args) {
        for (boolean fail : new boolean[] {true, false}) {
            try {
                multi(fail);
            } catch (Exception e) {
                System.out.println("out " + e.getClass().getSimpleName());
            }
            try {
                uni(fail);
                System.out.println("uni ok");
            } catch (IOException e) {
                System.out.println("out " + e.getMessage());
            }
        }
        try {
            narrowed();
        } catch (FileNotFoundException e) {
            System.out.println("narrowed " + e.getMessage());
        }
        try {
            earlierClause();
        } catch (IOException e) {
            System.out.println("unexpected");
        }
    }
}
