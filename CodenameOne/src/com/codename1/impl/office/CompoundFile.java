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
package com.codename1.impl.office;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/// Reader for the Compound File Binary format (MS-CFB, versions 3 and 4) that holds a `.doc`:
/// the FAT, DIFAT, directory and mini stream are decoded and every stream returned by its
/// `/`-separated path. Hostile input only ever yields an `IOException`.
public final class CompoundFile {
    private static final int NOSTREAM = -1;
    private static final byte[] MAGIC = {
            (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1};

    private CompoundFile() {
    }

    public static boolean hasSignature(byte[] d) {
        if (d == null || d.length < 8) {
            return false;
        }
        for (int i = 0; i < 8; i++) {
            if (d[i] != MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    private static int get16(byte[] b, int o) {
        return (b[o] & 0xff) | ((b[o + 1] & 0xff) << 8);
    }

    private static int get32(byte[] b, int o) {
        return (b[o] & 0xff) | ((b[o + 1] & 0xff) << 8) | ((b[o + 2] & 0xff) << 16) | ((b[o + 3] & 0xff) << 24);
    }

    /// Reads every stream of a CFB file into a map keyed by `/`-separated path.
    public static Map<String, byte[]> read(byte[] d) throws IOException {
        if (!hasSignature(d) || d.length < 512) {
            throw new IOException("Not a compound file");
        }
        int ssz = 1 << get16(d, 30);
        int mssz = 1 << get16(d, 32);
        if (ssz != 512 && ssz != 4096) {
            throw new IOException("Bad compound file sector size");
        }
        if (mssz < 8 || mssz > 4096) {
            throw new IOException("Bad compound file mini sector size");
        }
        int nfat = get32(d, 44);
        int dirStart = get32(d, 48);
        int cutoff = get32(d, 56);
        int mfStart = get32(d, 60);
        int difStart = get32(d, 68);
        int ndif = get32(d, 72);
        int sectors = (d.length - ssz) / ssz;
        if (nfat < 0 || nfat > sectors) {
            throw new IOException("Bad compound file FAT");
        }
        int per = ssz / 4;
        int[] fatSecs = new int[nfat];
        int got = 0;
        for (int i = 0; i < 109 && got < nfat; i++) {
            fatSecs[got++] = get32(d, 76 + i * 4);
        }
        int dif = difStart;
        for (int k = 0; k < ndif && got < nfat; k++) {
            if (dif < 0 || dif >= sectors) {
                throw new IOException("Bad compound file DIFAT");
            }
            for (int i = 0; i < per - 1 && got < nfat; i++) {
                fatSecs[got++] = get32(d, ssz + dif * ssz + i * 4);
            }
            dif = get32(d, ssz + dif * ssz + (per - 1) * 4);
        }
        int[] fat = new int[nfat * per];
        for (int i = 0; i < nfat; i++) {
            int s = fatSecs[i];
            if (s < 0 || s >= sectors) {
                throw new IOException("Bad compound file FAT sector");
            }
            for (int j = 0; j < per; j++) {
                fat[i * per + j] = get32(d, ssz + s * ssz + j * 4);
            }
        }
        byte[] dir = chain(d, fat, dirStart, ssz, -1);
        byte[] mfBytes = chain(d, fat, mfStart, ssz, -1);
        int[] miniFat = new int[mfBytes.length / 4];
        for (int i = 0; i < miniFat.length; i++) {
            miniFat[i] = get32(mfBytes, i * 4);
        }
        int count = dir.length / 128;
        if (count == 0 || (dir[66] & 0xff) != 5) {
            throw new IOException("Missing compound file root");
        }
        int rootStart = get32(dir, 116);
        long rootSize = get32(dir, 120) & 0xffffffffL;
        byte[] miniStream = rootSize == 0 ? new byte[0] : chain(d, fat, rootStart, ssz, (int) rootSize);
        Map<String, byte[]> out = new LinkedHashMap<String, byte[]>();
        boolean[] seen = new boolean[count];
        // every stream materialized counts against one budget: entries may share a chain, so a
        // per-stream size check alone lets a small file expand into gigabytes
        long[] budget = {2L * d.length + 4096};
        walk(d, dir, count, get32(dir, 76), "", fat, miniFat, miniStream, ssz, mssz, cutoff, out, seen,
                0, budget);
        return out;
    }

    private static final int MAX_DEPTH = 256;

    private static void walk(byte[] d, byte[] dir, int count, int id, String prefix, int[] fat, int[] miniFat,
            byte[] miniStream, int ssz, int mssz, int cutoff, Map<String, byte[]> out, boolean[] seen,
            int depth, long[] budget) throws IOException {
        if (id == NOSTREAM) {
            return;
        }
        // the directory is a red-black tree, so a real file is shallow; a degenerate one
        // must not overflow the (fixed size, on device) thread stack
        if (depth > MAX_DEPTH) {
            throw new IOException("Compound file directory too deep");
        }
        if (id < 0 || id >= count || seen[id]) {
            throw new IOException("Bad compound file directory");
        }
        seen[id] = true;
        int o = id * 128;
        int len = get16(dir, o + 64);
        if (len < 2 || len > 64) {
            throw new IOException("Bad compound file entry name");
        }
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < len / 2 - 1; i++) {
            name.append((char) get16(dir, o + i * 2));
        }
        int type = dir[o + 66] & 0xff;
        walk(d, dir, count, get32(dir, o + 68), prefix, fat, miniFat, miniStream, ssz, mssz, cutoff, out, seen, depth + 1, budget);
        String path = prefix + name;
        if (type == 2) {
            long size = get32(dir, o + 120) & 0xffffffffL;
            int start = get32(dir, o + 116);
            if (size > d.length || size > budget[0]) {
                throw new IOException("Bad compound file stream size");
            }
            budget[0] -= size;
            byte[] data;
            if (size == 0) {
                data = new byte[0];
            } else if (size < (cutoff & 0xffffffffL)) {
                data = miniChain(miniStream, miniFat, start, mssz, (int) size);
            } else {
                data = chain(d, fat, start, ssz, (int) size);
            }
            out.put(path, data);
        } else if (type == 1) {
            walk(d, dir, count, get32(dir, o + 76), path + "/", fat, miniFat, miniStream, ssz, mssz, cutoff, out, seen, depth + 1, budget);
        }
        walk(d, dir, count, get32(dir, o + 72), prefix, fat, miniFat, miniStream, ssz, mssz, cutoff, out, seen, depth + 1, budget);
    }

    /// Follows a regular chain; size -1 means whole sectors.
    private static byte[] chain(byte[] d, int[] fat, int start, int ssz, int size) throws IOException {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        int s = start;
        int steps = 0;
        int sectors = (d.length - ssz) / ssz;
        while (s >= 0) {
            if (s >= sectors || s >= fat.length || ++steps > fat.length + 1) { //NOPMD AssignmentInOperand - the step count bounds a cyclic chain
                throw new IOException("Bad compound file chain");
            }
            o.write(d, ssz + s * ssz, ssz);
            s = fat[s];
        }
        byte[] b = o.toByteArray();
        if (size < 0) {
            return b;
        }
        if (b.length < size) {
            throw new IOException("Truncated compound file stream");
        }
        byte[] r = new byte[size];
        System.arraycopy(b, 0, r, 0, size);
        return r;
    }

    private static byte[] miniChain(byte[] mini, int[] fat, int start, int mssz, int size) throws IOException {
        byte[] r = new byte[size];
        int s = start;
        int pos = 0;
        int steps = 0;
        while (pos < size) {
            if (s < 0 || s >= fat.length || (s + 1) * mssz > mini.length || ++steps > fat.length + 1) { //NOPMD AssignmentInOperand - the step count bounds a cyclic chain
                throw new IOException("Bad compound file mini chain");
            }
            int n = Math.min(mssz, size - pos);
            System.arraycopy(mini, s * mssz, r, pos, n);
            pos += n;
            s = fat[s];
        }
        return r;
    }
}
