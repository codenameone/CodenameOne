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
public class JsReflectiveInstanceApp {
    public static int result;

    public interface Greeter {
        int greet();
    }

    // Created ONLY by reflection, by name, from a helper that is handed the name -- the shape of
    // issue #5774, where an application built common.Salt through Class.forName and then called
    // it through the interface it implements.
    public static class ReflectiveGreeter implements Greeter {
        public int greet() {
            return 42;
        }
    }

    static Object make(String name) throws Exception {
        return Class.forName(name).newInstance();
    }

    public static void main(String[] args) throws Exception {
        Greeter g = (Greeter) make("JsReflectiveInstanceApp$ReflectiveGreeter");
        result = g.greet();
        System.exit(result);
    }
}
