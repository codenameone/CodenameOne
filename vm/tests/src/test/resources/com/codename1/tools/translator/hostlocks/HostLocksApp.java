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

/// The program HostLocksIntegrationTest builds once per compiler configuration.
///
/// PosixHostApp runs first because it checks the JdkHost DEFAULTS, which only
/// hold while nothing has been installed; it uninstalls its host when it is
/// done, and FileTestApp installs its own. Either exits 1 on a failure. Each
/// part's entry point is `run`, not `main`: the translator takes the one
/// `main(String[])` in the program as its entry point and refuses a second.
///
/// The three lock programs are built and never run, as they were when each had
/// a test of its own: their run was switched off because the executable crashed
/// on Linux runners. They must still be REACHABLE, or the dead-code pass drops
/// them and the build covers nothing -- hence a branch no command line takes.
public class HostLocksApp {
    public static void main(String[] args) throws Exception {
        PosixHostApp.run(args);
        FileTestApp.run(args);
        if (args != null && args.length > 99) {
            LockTestApp.run(args);
            ReadWriteLockTestApp.run(args);
            StampedLockTestApp.run(args);
        }
    }
}
