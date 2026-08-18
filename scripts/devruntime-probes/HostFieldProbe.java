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

/**
 * A field a pushed class inherits from its host superclass.
 *
 * javac records the *pushed* class as the owner -- `HostFieldProbe.focusScrolling`,
 * not `Form.focusScrolling` -- and the installed app has never heard of that
 * name, so the read reached the linker with an owner it could not resolve.
 */
public class HostFieldProbe extends Form {
    String readAndWrite() {
        boolean before = focusScrolling;
        focusScrolling = !before;
        boolean after = focusScrolling;
        return "read=" + before + " wrote=" + after;
    }

    public static void main(String[] a) {
        String result;
        try {
            result = new HostFieldProbe().readAndWrite();
        } catch (Throwable t) {
            result = "threw " + t.getClass().getName() + ": " + t.getMessage();
        }
        System.out.println("PROBE HostFieldProbe: " + result);
        new Form("HostFields").show();
    }
}
