/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Licensed under the GNU General Public License version 2 with the Classpath
 * exception, as the rest of the Codename One Python runtime.
 */
package com.pyhero;

/// The entry point of a native hero workload: SQLite behind the runtime's
/// hook, then the program. The file system is the host's, through
/// FileSystemStorage (scripts/python-bench/hostfs).
public final class NativeMain {
    private NativeMain() {
    }

    public static void main(String[] args) {
        com.codename1.impl.JdkHost.install(new HostEntropy());
        com.codename1.python.runtime.PySql.install(new HostSql());
        com.codename1.python.runtime.Py.setArgv("main", args);
        System.exit(com.codename1.generated.python.PythonModules.runMain());
    }
}
