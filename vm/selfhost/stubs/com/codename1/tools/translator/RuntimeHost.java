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
package com.codename1.tools.translator;

import com.codename1.impl.JdkHost;
import com.codename1.impl.PosixJdkHost;

/**
 * The self-hosted translator's start-up hook. See {@code vm/selfhost/README.md}.
 *
 * The real class is a no-op for the JVM. Here the translator runs on
 * {@code vm/JavaAPI}, which asks {@code JdkHost} for the platform's file system
 * and properties; this is a program with no Codename One in it, so it gets the
 * POSIX host over the file natives it already links.
 */
final class RuntimeHost {
    private RuntimeHost() {
    }

    static void install() {
        JdkHost.install(new PosixJdkHost());
    }
}
