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
package com.codename1.wordprocessing;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Test-only writer for the Compound File Binary format (MS-CFB, version 3): 512-byte sectors, FAT,
/// DIFAT, directory and a mini stream for streams under 4096 bytes. Used to synthesize `.doc` files.
final class CompoundFileWriter {
    private static final int FREE = -1;
    private static final int END = -2;
    private static final int FATSECT = -3;
    private static final int DIFSECT = -4;
    private static final int NOSTREAM = -1;
    private static final int MINI_CUTOFF = 4096;
    private static final byte[] MAGIC = {
        (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
    };

    private CompoundFileWriter() {
    }

    private static final class Node {
        String name;
        boolean stream;
        byte[] data;
        List<Node> kids = new ArrayList<Node>();
        int id, left = NOSTREAM, right = NOSTREAM, child = NOSTREAM, color = 1, start = END;
        long size;
    }

    private static int compareNames(String a, String b) {
        if (a.length() != b.length()) return a.length() < b.length() ? -1 : 1;
        return a.toUpperCase().compareTo(b.toUpperCase());
    }

    private static Node find(Node dir, String name) {
        for (Node k : dir.kids) if (k.name.equals(name)) return k;
        return null;
    }

    /// Writes streams (path to bytes, in the given map) as a CFB file. Intermediate storages are created.
    static byte[] write(Map<String, byte[]> streams) {
        Node root = new Node();
        root.name = "Root Entry";
        for (Map.Entry<String, byte[]> e : streams.entrySet()) {
            String path = e.getKey();
            Node dir = root;
            int from = 0;
            while (true) {
                int slash = path.indexOf('/', from);
                String part = slash < 0 ? path.substring(from) : path.substring(from, slash);
                Node n = find(dir, part);
                if (n == null) {
                    n = new Node();
                    n.name = part;
                    dir.kids.add(n);
                }
                if (slash < 0) {
                    n.stream = true;
                    n.data = e.getValue();
                    break;
                }
                dir = n;
                from = slash + 1;
            }
        }
        List<Node> all = new ArrayList<Node>();
        all.add(root);
        number(root, all);

        // Mini stream and mini FAT.
        ByteArrayOutputStream mini = new ByteArrayOutputStream();
        List<Integer> miniFat = new ArrayList<Integer>();
        for (Node n : all) {
            if (!n.stream) continue;
            n.size = n.data.length;
            if (n.data.length == 0 || n.data.length >= MINI_CUTOFF) continue;
            int count = (n.data.length + 63) / 64;
            n.start = miniFat.size();
            for (int i = 0; i < count; i++) miniFat.add(i == count - 1 ? END : miniFat.size() + 1);
            mini.write(n.data, 0, n.data.length);
            for (int i = n.data.length; i < count * 64; i++) mini.write(0);
        }
        byte[] miniBytes = mini.toByteArray();
        root.size = miniBytes.length;

        // Regular sectors, in order: big streams, mini stream, mini FAT, directory, FAT, DIFAT.
                int next = 0;
        for (Node n : all) {
            if (n.stream && n.data.length >= MINI_CUTOFF) {
                int c = (n.data.length + 511) / 512;
                n.start = next;
                next += c;
            }
        }
        int miniStart = END;
        int miniSectors = (miniBytes.length + 511) / 512;
        if (miniSectors > 0) {
            miniStart = next;
            next += miniSectors;
        }
        root.start = miniStart;
        int mfSectors = (miniFat.size() * 4 + 511) / 512;
        int mfStart = mfSectors > 0 ? next : END;
        next += mfSectors;
        int dirSectors = (all.size() + 3) / 4;
        int dirStart = next;
        next += dirSectors;
        int fats = 1, difs = 0;
        while (true) {
            difs = fats > 109 ? (fats - 109 + 126) / 127 : 0;
            int need = (next + fats + difs + 127) / 128;
            if (need <= fats) break;
            fats = need;
        }
        int fatStart = next;
        int difStart = difs > 0 ? fatStart + fats : END;
        int total = next + fats + difs;

        int[] fat = new int[fats * 128];
        for (int i = 0; i < fat.length; i++) fat[i] = FREE;
        for (Node n : all) {
            if (n.stream && n.data.length >= MINI_CUTOFF) chain(fat, n.start, (n.data.length + 511) / 512);
        }
        if (miniSectors > 0) chain(fat, miniStart, miniSectors);
        if (mfSectors > 0) chain(fat, mfStart, mfSectors);
        chain(fat, dirStart, dirSectors);
        for (int i = 0; i < fats; i++) fat[fatStart + i] = FATSECT;
        for (int i = 0; i < difs; i++) fat[difStart + i] = DIFSECT;

        byte[] out = new byte[512 + total * 512];
        System.arraycopy(MAGIC, 0, out, 0, 8);
        put16(out, 24, 0x3E);
        put16(out, 26, 3);
        put16(out, 28, 0xFFFE);
        put16(out, 30, 9);
        put16(out, 32, 6);
        put32(out, 44, fats);
        put32(out, 48, dirStart);
        put32(out, 56, MINI_CUTOFF);
        put32(out, 60, mfStart);
        put32(out, 64, mfSectors);
        put32(out, 68, difStart);
        put32(out, 72, difs);
        for (int i = 0; i < 109; i++) put32(out, 76 + i * 4, i < fats ? fatStart + i : FREE);
        for (Node n : all)
            if (n.stream && n.data.length >= MINI_CUTOFF)
                System.arraycopy(n.data, 0, out, 512 + n.start * 512, n.data.length);
        if (miniSectors > 0) System.arraycopy(miniBytes, 0, out, 512 + miniStart * 512, miniBytes.length);
        for (int i = 0; i < miniFat.size(); i++) put32(out, 512 + mfStart * 512 + i * 4, miniFat.get(i));
        for (int i = miniFat.size(); i < mfSectors * 128; i++) put32(out, 512 + mfStart * 512 + i * 4, FREE);
        // Directory.
        for (int i = 0; i < dirSectors * 4; i++) {
            int o = 512 + dirStart * 512 + i * 128;
            if (i < all.size()) {
                Node n = all.get(i);
                for (int k = 0; k < n.name.length(); k++) put16(out, o + k * 2, n.name.charAt(k));
                put16(out, o + 64, n.name.length() * 2 + 2);
                out[o + 66] = (byte) (n == root ? 5 : n.stream ? 2 : 1);
                out[o + 67] = (byte) n.color;
                put32(out, o + 68, n.left);
                put32(out, o + 72, n.right);
                put32(out, o + 76, n.child);
                put32(out, o + 116, n.start);
                put32(out, o + 120, (int) n.size);
            } else {
                put32(out, o + 68, NOSTREAM);
                put32(out, o + 72, NOSTREAM);
                put32(out, o + 76, NOSTREAM);
            }
        }
        for (int i = 0; i < fat.length; i++) put32(out, 512 + fatStart * 512 + i * 4, fat[i]);
        // DIFAT sectors hold FAT sector numbers 109.. then the next DIFAT sector.
        for (int d = 0; d < difs; d++) {
            int o = 512 + (difStart + d) * 512;
            for (int i = 0; i < 127; i++) {
                int idx = 109 + d * 127 + i;
                put32(out, o + i * 4, idx < fats ? fatStart + idx : FREE);
            }
            put32(out, o + 508, d + 1 < difs ? difStart + d + 1 : END);
        }
        return out;
    }

    private static void chain(int[] fat, int start, int count) {
        for (int i = 0; i < count; i++) fat[start + i] = i == count - 1 ? END : start + i + 1;
    }

    /// Assigns ids breadth-first and builds a red-black tree of each storage's children.
    private static void number(Node dir, List<Node> all) {
        List<Node> kids = dir.kids;
        for (int i = 1; i < kids.size(); i++) {
            Node k = kids.get(i);
            int j = i - 1;
            while (j >= 0 && compareNames(kids.get(j).name, k.name) > 0) {
                kids.set(j + 1, kids.get(j));
                j--;
            }
            kids.set(j + 1, k);
        }
        for (Node k : kids) {
            k.id = all.size();
            all.add(k);
        }
        if (!kids.isEmpty()) {
            int levels = 0;
            while ((1 << levels) - 1 < kids.size()) levels++;
            boolean full = (1 << levels) - 1 == kids.size();
            dir.child = tree(kids, 0, kids.size() - 1, 0, levels, full);
        }
        for (Node k : kids) if (!k.stream) number(k, all);
    }

    private static int tree(List<Node> kids, int lo, int hi, int depth, int levels, boolean full) {
        if (lo > hi) return NOSTREAM;
        int mid = (lo + hi + 1) / 2;
        Node n = kids.get(mid);
        n.color = (!full && depth == levels - 1) ? 0 : 1;
        n.left = tree(kids, lo, mid - 1, depth + 1, levels, full);
        n.right = tree(kids, mid + 1, hi, depth + 1, levels, full);
        return n.id;
    }

    private static void put16(byte[] b, int o, int v) {
        b[o] = (byte) v;
        b[o + 1] = (byte) (v >> 8);
    }

    private static void put32(byte[] b, int o, int v) {
        b[o] = (byte) v;
        b[o + 1] = (byte) (v >> 8);
        b[o + 2] = (byte) (v >> 16);
        b[o + 3] = (byte) (v >> 24);
    }
}
