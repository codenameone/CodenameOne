/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.python.runtime.PyFileSystem;
import com.codename1.python.runtime.PyHttp;
import com.codename1.python.runtime.PySql;
import com.codename1.python.transpiler.JvmFileSystem;
import com.codename1.python.transpiler.JvmHttp;
import com.codename1.python.transpiler.JvmSql;
import java.io.File;

/// Runs a transpiled workload on the JVM with the host's file system,
/// SQLite and HTTP behind the runtime's hooks, as a device port supplies
/// its own.
///
///     HeroMain <file system root> [program arguments]
public final class HeroMain {
    private HeroMain() {
    }

    public static void main(String[] args) {
        File root = new File(args[0]);
        File home = new File(root, "home");
        home.mkdirs();
        // A POSIX host's temporary directory, which MarkItDown's rlink test
        // writes its target into.
        new File(root, "tmp").mkdirs();
        PyFileSystem.install(new JvmFileSystem(root.getPath()));
        PySql.install(new JvmSql(home.getPath()));
        PyHttp.install(new JvmHttp());
        // An application decodes images through its Display; here that is
        // compat-testing's headless one over the JDK's image readers, as the
        // transpiler's client PIL conformance tests run it. The readers must
        // not start a toolkit.
        System.setProperty("java.awt.headless", "true");
        HeadlessImplementation.pixelImages = true;
        HeadlessImplementation.rasterImages = true;
        HeadlessImplementation.imageIO = new pilclienttest.JdkImageIO();
        HeadlessImplementation.install();
        String[] rest = new String[args.length - 1];
        System.arraycopy(args, 1, rest, 0, rest.length);
        com.codename1.python.runtime.Py.setArgv("main", rest);
        System.exit(com.codename1.generated.python.PythonModules.runMain());
    }
}
