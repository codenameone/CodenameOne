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

import com.codename1.impl.JdkHost;
import com.codename1.impl.PosixJdkHost;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
public class PosixHostApp {
    static void check(boolean ok, String what) {
        if (!ok) { System.out.println("FAIL " + what); System.exit(1); }
    }
    static void write(JdkHost h, String path, boolean append, byte[] data) throws Exception {
        OutputStream o = h.fileWrite(path, append);
        o.write(data);
        o.close();
    }
    static void run(String[] args) throws Exception {
        // Nothing installed: the plain-Java defaults, which touch no native.
        JdkHost d = JdkHost.get();
        check(d != null && !(d instanceof PosixJdkHost), "defaults");
        check(d.fileStat("anything") == null, "default stat");
        check(!d.fileMkdir("anything"), "default mkdir");
        boolean threw = false;
        try { d.fileRead("anything"); } catch (java.io.IOException e) { threw = true; }
        check(threw, "default read throws");
        d.setPreference("k", "v");
        check("v".equals(d.preference("k")), "default preference");
        d.deletePreference("k");
        check(d.preference("k") == null, "default preference deleted");
        check(d.providers("svc").length == 0, "no registry");

        JdkHost.install(new PosixJdkHost());
        JdkHost h = JdkHost.get();
        check(h instanceof PosixJdkHost, "installed");
        String home = h.appHome();
        check(home != null && home.length() > 0 && !home.endsWith("."), "home " + home);
        check(h.fileStat(home) != null && h.fileStat(home).isDirectory(), "home is a directory");
        String dir = home + "/posix-host-dir";
        String a = dir + "/a.txt";
        String b = dir + "/b.txt";
        h.fileDelete(a); h.fileDelete(b); h.fileDelete(dir);
        check(h.fileStat(dir) == null, "clean start");
        check(h.fileMkdir(dir), "mkdir");
        check(!h.fileMkdir(dir), "mkdir of an existing directory");
        write(h, a, false, new byte[] {1, 2, 3});
        write(h, a, true, new byte[] {4});
        JdkHost.FileStat st = h.fileStat(a);
        check(st != null && !st.isDirectory() && st.length() == 4, "stat after append");
        check(st.lastModified() > 0, "modified time");
        InputStream in = h.fileRead(a);
        byte[] buf = new byte[8];
        int n = in.read(buf);
        in.close();
        check(n == 4 && buf[0] == 1 && buf[3] == 4, "read back");
        write(h, a, false, new byte[] {9});
        check(h.fileStat(a).length() == 1, "overwrite truncates");
        String[] names = h.fileList(dir);
        check(names != null && names.length == 1 && "a.txt".equals(names[0]), "list");
        check(h.fileList(a) == null, "list of a file");
        check(h.fileRename(a, b), "rename");
        check(h.fileStat(a) == null && h.fileStat(b) != null, "renamed");
        write(h, a, false, new byte[] {5});
        check(!h.fileRename(a, b), "rename onto an existing file");
        check(h.fileStat(b).length() == 1, "destination untouched");
        check(!h.fileDelete(dir), "delete of a non-empty directory");
        check(h.fileDelete(a) && h.fileDelete(b) && h.fileDelete(dir), "delete");
        check(!h.fileDelete(dir), "delete of nothing");
        threw = false;
        try { h.fileRead(a); } catch (java.io.IOException e) { threw = true; }
        check(threw, "read of a missing file throws");
        // 0 is "unknown": the volume natives answer it unless the build
        // defines CN1_ENABLE_FILE_SYSTEM_STATS.
        check(h.fileTotalSpace(home) >= 0 && h.fileUsableSpace(home) >= 0, "space");
        check(h.cachesDir() != null, "caches dir");
        // Only where the source exists; the Windows hosts have none.
        if (new File("/dev/urandom").exists()) {
            byte[] r = new byte[64];
            h.randomBytes(r);
            boolean nonZero = false;
            for (int i = 0; i < r.length; i++) { if (r[i] != 0) { nonZero = true; } }
            check(nonZero, "random bytes");
        }
        JdkHost.install(null);
        check(!(JdkHost.get() instanceof PosixJdkHost), "uninstalled");
        System.out.println("POSIX-HOST-OK");
    }
}
