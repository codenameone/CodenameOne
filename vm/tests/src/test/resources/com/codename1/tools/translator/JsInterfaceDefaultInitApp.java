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

/// A default method reading its own interface's static field. Nothing names
/// the interface in a GETSTATIC before the call, so only class initialization
/// of the implementing class (JVMS 5.5: superinterfaces that declare default
/// methods are initialized with it) runs the interface's static initializer.
/// This is Kotlin's `override val key get() = Key` on a companion, as in
/// Compose's MonotonicFrameClock.
public class JsInterfaceDefaultInitApp {
    static int result;

    interface Keyed {
        Object KEY = new StringBuilder("key").toString();

        default Object key() {
            return KEY;
        }
    }

    interface Inherited extends Keyed {
    }

    static final class Direct implements Keyed {
    }

    static final class Indirect implements Inherited {
    }

    static class Base implements Keyed {
    }

    static final class Sub extends Base {
    }

    public static void main(String[] args) {
        if ("key".equals(new Direct().key())) result |= 1;
        if ("key".equals(new Indirect().key())) result |= 2;
        if ("key".equals(new Sub().key())) result |= 4;
    }
}
