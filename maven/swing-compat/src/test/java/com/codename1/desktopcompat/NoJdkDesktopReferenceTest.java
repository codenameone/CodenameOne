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
package com.codename1.desktopcompat;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// No class the module ships may name the JDK's own desktop packages.
///
/// A device has no `java.awt`: a main source that imports the real
/// `java.awt.Color` where the layer's was meant compiles on the build
/// machine, where the JDK supplies it, and fails only when the application
/// is translated. Every UTF-8 constant of every class is read -- class
/// names, descriptors and generic signatures all live there -- and none may
/// contain one of the relocated package paths except as part of the layer's
/// own `com/codename1/desktopcompat/` name.
public class NoJdkDesktopReferenceTest {

    private static final String OWN = "com/codename1/desktopcompat/";
    private static final String[] FORBIDDEN = {
        "java/awt/", "javax/swing/", "java/beans/", "javax/accessibility/", "org/jdesktop/"
    };

    @Test
    public void noClassNamesTheJdkDesktopPackages() throws IOException {
        File root = new File(System.getProperty("swingcompat.classes", "target/classes"));
        List<String> problems = new ArrayList<String>();
        int count = walk(root, problems);
        if (!problems.isEmpty()) {
            StringBuilder sb = new StringBuilder("references to the JDK's desktop packages:\n");
            for (String p : problems) {
                sb.append("  ").append(p).append('\n');
            }
            fail(sb.toString());
        }
        assertTrue("no classes under " + root, count > 0);
    }

    private static int walk(File dir, List<String> problems) throws IOException {
        File[] files = dir.listFiles();
        int count = 0;
        if (files == null) {
            return 0;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                count += walk(f, problems);
            } else if (f.getName().endsWith(".class")) {
                count++;
                scan(f, problems);
            }
        }
        return count;
    }

    static boolean offends(String constant) {
        for (String pkg : FORBIDDEN) {
            int at = constant.indexOf(pkg);
            while (at >= 0) {
                boolean own = at >= OWN.length() && constant.startsWith(OWN, at - OWN.length());
                // An 'L' in front is the type marker of a descriptor unless
                // it ends a lower case word ("xmljava/awt/").
                boolean descriptor = at > 0 && constant.charAt(at - 1) == 'L'
                        && (at < 2 || !Character.isLowerCase(constant.charAt(at - 2)));
                boolean partOfLongerName = at > 0 && !descriptor
                        && (Character.isJavaIdentifierPart(constant.charAt(at - 1)) || constant.charAt(at - 1) == '/');
                if (!own && !partOfLongerName) {
                    return true;
                }
                at = constant.indexOf(pkg, at + 1);
            }
        }
        return false;
    }

    private static void scan(File f, List<String> problems) throws IOException {
        DataInputStream in = new DataInputStream(new FileInputStream(f));
        try {
            in.readInt();
            in.readUnsignedShort();
            in.readUnsignedShort();
            int n = in.readUnsignedShort();
            for (int i = 1; i < n; i++) {
                int tag = in.readUnsignedByte();
                switch (tag) {
                    case 1:
                        String s = in.readUTF();
                        if (offends(s)) {
                            problems.add(f.getPath() + ": " + s);
                        }
                        break;
                    case 3: case 4: case 9: case 10: case 11: case 12: case 17: case 18:
                        in.readInt();
                        break;
                    case 5: case 6:
                        in.readLong();
                        i++;
                        break;
                    case 7: case 8: case 16: case 19: case 20:
                        in.readUnsignedShort();
                        break;
                    case 15:
                        in.readUnsignedByte();
                        in.readUnsignedShort();
                        break;
                    default:
                        throw new IOException(f + ": constant pool tag " + tag);
                }
            }
        } finally {
            in.close();
        }
    }

    @Test
    public void theScanTellsTheLayerFromTheJdk() {
        assertTrue(offends("Ljava/awt/Color;"));
        assertTrue(offends("javax/swing/JButton"));
        assertFalse(offends("Lcom/codename1/desktopcompat/java/awt/Color;"));
        assertTrue(offends("(Lcom/codename1/desktopcompat/java/awt/Color;Ljava/awt/Font;)V"));
        assertFalse(offends("java/lang/String"));
    }
}
