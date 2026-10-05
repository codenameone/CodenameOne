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
/** An instance initializer may throw what every constructor that does not delegate with this(...) declares. */
import java.io.IOException;
public class InitializerThrows {
    static int calls;
    static int read(boolean fail) throws IOException {
        calls++;
        if (fail) throw new IOException("init " + calls);
        return calls;
    }
    final boolean fail;
    int x = read(failNext);
    static boolean failNext;
    { x += 100; }
    InitializerThrows() throws IOException { fail = false; }
    InitializerThrows(int y) throws IOException { this(); x += y; }
    InitializerThrows(String s) throws Exception { fail = true; }
    public static void main(String[] args) throws Exception {
        System.out.println(new InitializerThrows().x + " " + new InitializerThrows(5).x);
        failNext = true;
        try {
            new InitializerThrows("s");
        } catch (IOException e) {
            System.out.println("caught " + e.getMessage());
        }
    }
}
