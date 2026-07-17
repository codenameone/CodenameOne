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
package com.codename1.bluetooth.helper;

import java.util.ArrayList;
import java.util.List;

/// Builds [NativeSubprocessTransport]s for a native port. Resolving the
/// helper binary on the native ports is deliberately simpler than the
/// JavaSE classpath-extraction path: there is no runtime jar to extract
/// from, so the helper is located by
///
/// 1. the `cn1.bluetooth.helperPath` system property (an explicit path), or
/// 2. the bare executable name, spawned through the OS `PATH` search
///    (`posix_spawnp` on Linux, `CreateProcess` `PATH` lookup on Windows),
///    so a helper bundled next to the app or installed on the `PATH` is
///    found without extra plumbing.
///
/// The subclass supplies the actual transport (which carries the port's
/// native `proc*` bridge) via [#createTransport(List)].
public abstract class NativeSubprocessTransportFactory
        implements HelperTransportFactory {

    /// The system property that overrides the helper location. Kept in sync
    /// with the JavaSE resolver's property name.
    public static final String HELPER_PATH_PROPERTY = "cn1.bluetooth.helperPath";

    private static final String HELPER_BASENAME = "cn1-ble-helper";

    @Override
    public HelperTransport create() {
        return createTransport(resolveCommand());
    }

    /// Creates the port-specific transport bound to the given launch
    /// command (helper path + args). Implemented per port over its native
    /// `proc*` bridge.
    protected abstract NativeSubprocessTransport createTransport(
            List<String> command);

    /// The executable name on this OS -- Windows appends `.exe`.
    protected abstract String executableName(String basename);

    private List<String> resolveCommand() {
        List<String> command = new ArrayList<String>();
        String override = getProperty(HELPER_PATH_PROPERTY);
        if (override != null && override.length() > 0) {
            command.add(override);
        } else {
            command.add(executableName(HELPER_BASENAME));
        }
        return command;
    }

    private static String getProperty(String key) {
        try {
            return System.getProperty(key);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
