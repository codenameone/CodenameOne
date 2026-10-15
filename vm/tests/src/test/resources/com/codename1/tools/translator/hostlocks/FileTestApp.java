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

import java.io.File;
public class FileTestApp {
    static void run(String[] args) {
        // Translated without Codename One: the program brings its file system.
        com.codename1.impl.JdkHost.install(new com.codename1.impl.PosixJdkHost());
        try {
            // Construct string manually to avoid constant pool issues in test env
            char[] pathChars = new char[]{'t','e','s','t','f','i','l','e','.','t','x','t'};
            String path = new String(pathChars);
            File f = new File(path);
            if (f.exists()) {
                f.delete();
            }
            boolean created = f.createNewFile();
            if (!created) throw new RuntimeException("Create failed");
            if (!f.exists()) throw new RuntimeException("Exists failed");
            if (f.isDirectory()) throw new RuntimeException("IsDirectory failed");
            if (!f.delete()) throw new RuntimeException("Delete failed");
            if (f.exists()) throw new RuntimeException("Delete verification failed");
            // File.list(): nothing exercised it before, so the native listing --
            // rewritten from a racy count-then-refill pair into one enumeration --
            // was compiled but never RUN by this suite.
            char[] dirChars = new char[]{'l','s','d','i','r'};
            File dir = new File(new String(dirChars));
            dir.mkdir();
            char[] aChars = new char[]{'l','s','d','i','r','/','a'};
            char[] bChars = new char[]{'l','s','d','i','r','/','b'};
            File fa = new File(new String(aChars));
            File fb = new File(new String(bChars));
            fa.createNewFile();
            fb.createNewFile();
            String[] names = dir.list();
            if (names == null) throw new RuntimeException("list returned null");
            if (names.length != 2) throw new RuntimeException("list length");
            // A trailing null is what the old two-pass version produced when the
            // second walk saw fewer entries than the first.
            if (names[0] == null || names[1] == null) throw new RuntimeException("null entry");
            // The result must be a String[], not a String: allocArray installs the
            // class it is handed as the ARRAY's own class.
            Object asObject = names;
            if (!(asObject instanceof String[])) throw new RuntimeException("not a String[]");
            boolean sawA = false; boolean sawB = false;
            for (int i = 0; i < names.length; i++) {
                if (names[i].equals(new String(new char[]{'a'}))) sawA = true;
                if (names[i].equals(new String(new char[]{'b'}))) sawB = true;
            }
            if (!sawA || !sawB) throw new RuntimeException("missing entry");
            fa.delete(); fb.delete(); dir.delete();
            // A REGRESSION GUARD, not a proof of atomicity -- stated plainly
            // because the difference is easy to misread. This checks the
            // uncontended path: createNewFile on an existing file returns false
            // and leaves it intact. The check-then-act version it replaced passes
            // this too, since access() succeeds and it returns before ever
            // reaching the fopen that would truncate. Verified by running it
            // against the old code: 5/5 green.
            //
            // The actual defect needs a file to appear BETWEEN the check and the
            // open, which one thread cannot produce, so no single-threaded test
            // can demonstrate it. The correctness argument is structural instead:
            // one O_EXCL syscall where there were two operations, with the kernel
            // deciding who wins. What this guards is that the rewrite did not
            // break the ordinary path.
            char[] exChars = new char[]{'e','x','c','l','.','t','x','t'};
            File ex = new File(new String(exChars));
            if (ex.exists()) ex.delete();
            if (!ex.createNewFile()) throw new RuntimeException("first create");
            java.io.FileOutputStream os = new java.io.FileOutputStream(ex);
            os.write(new byte[]{1,2,3,4});
            os.close();
            if (ex.createNewFile()) throw new RuntimeException("second create returned true");
            if (ex.length() != 4) throw new RuntimeException("existing file was truncated");
            ex.delete();
            // renameTo was never exercised, which is how an implementation that
            // renamed the destination onto itself survived. Content is checked
            // too: the aliased version reported success while moving nothing.
            char[] rnChars = new char[]{'r','n','-','s','r','c'};
            char[] rdChars = new char[]{'r','n','-','d','s','t','-','l','o','n','g','e','r'};
            File rsrc = new File(new String(rnChars));
            File rdst = new File(new String(rdChars));
            if (rsrc.exists()) rsrc.delete();
            if (rdst.exists()) rdst.delete();
            rsrc.createNewFile();
            java.io.FileOutputStream ros = new java.io.FileOutputStream(rsrc);
            ros.write(new byte[]{7,7,7});
            ros.close();
            if (!rsrc.renameTo(rdst)) throw new RuntimeException("rename returned false");
            if (rsrc.exists()) throw new RuntimeException("source still present");
            if (!rdst.exists()) throw new RuntimeException("destination missing");
            if (rdst.length() != 3) throw new RuntimeException("content not moved");
            rdst.delete();
        } catch (Exception e) {
            // e.printStackTrace(); // Can't print stack trace without constants
            System.exit(1);
        }
    }
}
