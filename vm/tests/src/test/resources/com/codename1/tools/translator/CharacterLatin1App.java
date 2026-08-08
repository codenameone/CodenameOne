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
/**
 * Prints Character's answers for every ISO Latin-1 code point.
 *
 * Run twice by CharacterLatin1TypeTest -- once on the host JDK, once through
 * ParparVM against vm/JavaAPI's Character -- and the two RESULT lines must be
 * identical. Latin-1 is the range this class documents as supported, and the
 * category table backing getType was generated from a JDK, so the JDK is the
 * right oracle for it.
 */
public class CharacterLatin1App {

    public static void main(String[] args) {
        StringBuilder sb = new StringBuilder("RESULT=");
        for (int c = 0; c < 256; c++) {
            sb.append(Character.getType(c));
            sb.append(flag(Character.isLetter(c)));
            sb.append(flag(Character.isLowerCase(c)));
            sb.append(flag(Character.isUpperCase(c)));
            sb.append(flag(Character.isDigit(c)));
            sb.append(flag(Character.isLetterOrDigit(c)));
            sb.append(flag(Character.isJavaIdentifierStart(c)));
            sb.append(flag(Character.isJavaIdentifierPart(c)));
            sb.append(';');
        }
        System.out.println(sb.toString());
    }

    private static char flag(boolean value) {
        return value ? 'T' : 'F';
    }
}
