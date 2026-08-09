/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.hardening;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * Generates the obfuscation dictionaries ProGuard renames from. Every generated
 * name starts with a distinctive prefix, is at least six characters, is lower-case
 * and never contains an underscore.
 *
 * <p>This is not cosmetic -- it is the fix for a ParparVM build-killer. The
 * translator decides whether a class is reachable from native code by asking
 * whether the class name is a <em>substring</em> of any identifier in the native
 * sources ({@code BytecodeMethod.isMethodUsedByNative} /
 * {@code NativeSymbolIndex}). ProGuard's default names ({@code a}, {@code b},
 * {@code aa}) are substrings of almost every native identifier, so with default
 * names nothing is ever culled: the iOS/Windows/Linux/JS binaries balloon and the
 * translator runs out of heap. A {@code zq}-prefixed six-plus-character name is a
 * substring of nothing in the native sources, so culling works normally.
 *
 * <p>The dictionary is also sized so ProGuard never exhausts it and falls back to
 * its own short-name generator, which would reintroduce the pathology for the
 * overflow names.
 */
public final class Cn1NameFactory {

    /**
     * The name prefix. Chosen so it cannot occur inside a CN1 native identifier
     * (which are {@code package_Class_method}-mangled Java names and C runtime
     * symbols); ASCII, lower-case, underscore-free.
     */
    static final String PREFIX = "zq";

    private static final char[] ALPHABET = "abcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final int MIN_BODY_WIDTH = 4; // PREFIX(2) + 4 => 6-char minimum

    private Cn1NameFactory() {
    }

    /** The nth distinctive name: {@code zq} + a fixed-width base-26 body, e.g. {@code zqaaaa}. */
    public static String word(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index < 0");
        }
        StringBuilder body = new StringBuilder();
        int n = index;
        do {
            body.append(ALPHABET[n % 26]);
            n /= 26;
        } while (n > 0);
        while (body.length() < MIN_BODY_WIDTH) {
            body.append('a');
        }
        return PREFIX + body.reverse().toString();
    }

    /**
     * Writes a dictionary of {@code count} distinct names to {@code out}. A build feeds the same
     * file as the class, member and package obfuscation dictionary; sizing it above the number of
     * names any one scope needs guarantees ProGuard never falls back to short names. The
     * {@code seed} shifts the starting word so that different seeds (or build keys) yield different
     * name assignments -- hence different mappings -- while the same seed reproduces them exactly.
     */
    public static void writeDictionary(File out, int count, int seed) throws IOException {
        int safeCount = Math.max(count, 1);
        // A stable, non-negative offset from the seed; the word() indexing stays injective, so the
        // offset never introduces collisions.
        int offset = (seed & 0x7fffffff) % 1000000;
        FileOutputStream fo = new FileOutputStream(out);
        try {
            Writer w = new BufferedWriter(new OutputStreamWriter(fo, Charset.forName("UTF-8")));
            for (int i = 0; i < safeCount; i++) {
                w.write(word(offset + i));
                w.write('\n');
            }
            w.flush();
        } finally {
            fo.close();
        }
    }

    /**
     * The dictionary size to use for a jar with {@code classCount} classes and at most
     * {@code maxMembersInAnyClass} members (fields + methods) in any single class. The one dictionary
     * feeds ProGuard's class, member AND package obfuscation, so it must exceed the LARGEST naming scope:
     * the global class count, or the member count of the biggest class (a generated interface can have
     * tens of thousands of same-descriptor methods, all needing distinct names). Sizing to only the class
     * count would let a member-heavy class exhaust the dictionary and drop ProGuard back to its {@code a}/
     * {@code b} short names, reintroducing the ParparVM native-scan pathology this class exists to avoid.
     * Kept comfortably above that maximum with a floor for small apps.
     */
    public static int dictionarySizeFor(int classCount, int maxMembersInAnyClass) {
        int largestScope = Math.max(classCount, maxMembersInAnyClass);
        return Math.max(50000, largestScope * 4);
    }
}
