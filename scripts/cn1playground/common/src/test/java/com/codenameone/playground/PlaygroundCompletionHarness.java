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
package com.codenameone.playground;

import java.util.List;

/// The editor's member completion: which type it offers members of after a dot.
/// Headless; exits non-zero on the first wrong answer.
public final class PlaygroundCompletionHarness {

    private static int failures;

    public static void main(String[] args) {
        String doc = "Form hi = new Form(\"Button\");\n"
                + "Button b = new Button(\"Link Button\");\n"
                + "b.getAllStyles().setBorder(Border.createEmpty());\n";
        // The report that started this: a chained call's return type.
        expect(doc + "b.getAllStyles().", "com.codename1.ui.plaf.Style");
        expect(doc + "b.getAllStyles().setFg", "com.codename1.ui.plaf.Style");
        expect(doc + "b.", "com.codename1.ui.Button");
        expect(doc + "hi.getContentPane().", "com.codename1.ui.Container");
        expect(doc + "new Button(\"x\").", "com.codename1.ui.Button");
        expect(doc + "\"text\".", "java.lang.String");
        expect(doc + "b.getText().", "java.lang.String");
        expect(doc + "Style.", "com.codename1.ui.plaf.Style");
        expect(doc + "b.getUnselectedStyle().getFont().", "com.codename1.ui.Font");
        // Argument lists with commas, nested calls and literals are skipped whole.
        expect(doc + "hi.getComponentAt(Math.max(1, 2)).", "com.codename1.ui.Component");
        expect(doc + "b.getText().substring(1, \"a,b)\".length()).", "java.lang.String");
        // A member position whose receiver cannot be typed offers nothing at all.
        expect(doc + "unknownThing.", "");
        expect(doc + "b.noSuchMethod().", "");
        expect(doc + "((Button) b).", "");
        expect(doc + "b.getPreferredW().", ""); // int: no members
        // Not a member position.
        expect(doc + "b", null);
        expect(doc + "double d = 1.", null);
        expect(doc + "hi.add(", null);

        List<String> members = PlaygroundCompletionModel.get().memberSignatures("com.codename1.ui.plaf.Style");
        check("Style members include setBorder", members.contains("setBorder(Border)"));
        check("Style members include TEXT_DECORATION_UNDERLINE", members.contains("TEXT_DECORATION_UNDERLINE"));

        if (failures > 0) {
            System.out.println("Playground completion: " + failures + " failure(s)");
            System.exit(1);
        }
        System.out.println("Playground completion: all checks passed");
        System.exit(0);
    }

    private static void expect(String code, String type) {
        String got = PlaygroundCompletionModel.get().receiverType(code, code.length());
        boolean ok = type == null ? got == null : type.equals(got);
        check("after '" + code.substring(code.lastIndexOf('\n') + 1) + "' -> " + type + " (got " + got + ")", ok);
    }

    private static void check(String what, boolean ok) {
        if (!ok) {
            failures++;
            System.out.println("FAIL " + what);
        }
    }
}
