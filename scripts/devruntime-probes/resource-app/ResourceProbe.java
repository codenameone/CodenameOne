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
import com.codename1.ui.*;
import com.codename1.io.Util;
import com.codename1.ui.util.Resources;
import java.io.InputStream;

/**
 * A program that brings its own resources.
 *
 * The plain stream proves the implementation layer serves them. Resources.open
 * is the one that matters: it resolves inside the framework, which asks the
 * implementation directly and never passes through anything the interpreter
 * sees -- so if the hook were on Display instead, this line would still load
 * the host app's file.
 */
public class ResourceProbe {
    public static void main(String[] a) {
        String text = "?";
        String res = "?";
        try {
            InputStream in = Display.getInstance().getResourceAsStream(null, "/pushed.txt");
            text = in == null ? "missing" : Util.readToString(in).trim();
        } catch (Exception e) {
            text = "threw " + e;
        }
        try {
            Resources r = Resources.open("/pushed.res");
            res = "opened themes=" + r.getThemeResourceNames().length;
        } catch (Exception e) {
            res = "threw " + e;
        }
        System.out.println("PROBE ResourceProbe: text=" + text + " res=" + res);
        new Form("Resource").show();
    }
}
