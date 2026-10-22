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
package com.codename1.impl;

import com.codename1.util.regex.StringRegex;

/// Bridge methods used by bytecode rewrite rules for JDK APIs that are risky/unsupported on some targets.
public final class JdkApiRewriteHelper {
    private JdkApiRewriteHelper() {
    }

    // The static hash and compare methods Java 8 added to the primitive
    // wrappers, which the device runtime does not define. Kotlin emits them for
    // every data class (hashCode of each property) and comparison; the values
    // are the JDK's.

    public static int hashCode(int value) {
        return value;
    }

    public static int hashCode(long value) {
        return (int) (value ^ (value >>> 32));
    }

    public static int hashCode(double value) {
        long bits = Double.doubleToLongBits(value);
        return (int) (bits ^ (bits >>> 32));
    }

    public static int hashCode(float value) {
        return Float.floatToIntBits(value);
    }

    public static int hashCode(boolean value) {
        return value ? 1231 : 1237;
    }

    public static int hashCode(char value) {
        return value;
    }

    public static int hashCode(short value) {
        return value;
    }

    public static int hashCode(byte value) {
        return value;
    }

    public static int compare(boolean x, boolean y) {
        return x == y ? 0 : (x ? 1 : -1);
    }

    public static int compare(char x, char y) {
        return x - y;
    }

    public static int compare(byte x, byte y) {
        return x - y;
    }

    // The String regex members delegate to StringRegex, the one engine every
    // build rewrites String's regex members to (BytecodeCompliance's
    // INVOCATION_REWRITE_RULES). They stay because a class rewritten by an
    // older build names them; they must never grow an engine of their own
    // again -- two engines made the same call answer differently by target.

    public static String replaceAll(String source, String regex, String replacement) {
        return StringRegex.replaceAll(source, regex, replacement);
    }

    public static String replaceFirst(String source, String regex, String replacement) {
        return StringRegex.replaceFirst(source, regex, replacement);
    }

    public static String[] split(String source, String regex) {
        return StringRegex.split(source, regex);
    }

    public static String[] split(String source, String regex, int limit) {
        return StringRegex.split(source, regex, limit);
    }
}
