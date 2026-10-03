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
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class JsFileIoProgram {
    public static int result;

    public static void main(String[] args) throws Exception {
        int mask = 0;
        File root = new File(args[0]);
        File dir = new File(root, "a/b");
        if (dir.mkdirs() && dir.isDirectory()) mask |= 1;
        File file = new File(dir, "data.bin");
        FileOutputStream out = new FileOutputStream(file);
        out.write(new byte[] {1, -2, 3, (byte) 0xC3});
        out.close();
        FileOutputStream more = new FileOutputStream(file, true);
        more.write(new byte[] {9});
        more.close();
        if (file.isFile() && file.length() == 5) mask |= 2;
        FileInputStream in = new FileInputStream(file);
        byte[] back = new byte[8];
        int n = in.read(back, 0, 8);
        int eof = in.read(back, 0, 8);
        in.close();
        if (n == 5 && back[1] == -2 && back[3] == (byte) 0xC3 && back[4] == 9 && eof == -1) mask |= 4;
        String[] names = dir.list();
        if (names != null && names.length == 1 && names[0].equals("data.bin")) mask |= 8;
        if (file.delete() && !file.exists() && dir.delete()) mask |= 16;
        if ("from-host".equals(System.getenv("CN1_JS_PROGRAM_PROBE"))) mask |= 32;
        // The console fast path printed the class name of anything but a String.
        System.out.println(new StringBuilder("sb:").append(42));
        System.out.println("mask:" + mask);
        result = mask;
        System.exit(mask);
    }
}
