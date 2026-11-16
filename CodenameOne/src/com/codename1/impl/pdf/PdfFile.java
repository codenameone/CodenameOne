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
package com.codename1.impl.pdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A parsed PDF file: the cross-reference data, lazily loaded objects and
/// the flattened page list.
///
/// Damaged files are the norm, so every structural step has a fallback:
/// a broken or missing cross-reference is rebuilt by scanning the file for
/// `n g obj`, a wrong stream `/Length` is replaced by the distance to
/// `endstream`, and an unreadable object reads as null.
@com.codename1.impl.SharedWithBackend
public final class PdfFile {
    private static final int MAX_DEPTH = 64;

    private final byte[] data;
    /// Object number to `{offset}` (type 1) or `{-1, objstm, index}` (type 2).
    private final Map<Integer, int[]> xref = new HashMap<Integer, int[]>();
    private final Map<Integer, Object> cache = new HashMap<Integer, Object>();
    private final Map<Integer, Object[]> objectStreams = new HashMap<Integer, Object[]>();
    private PdfDict trailer;
    private PdfSecurity security;
    private int encryptObject = -1;
    private final List<PdfDict> pages = new ArrayList<PdfDict>();
    private int resolveDepth;

    /// Parses `data`, decrypting with `password` (null for none).
    public PdfFile(byte[] data, String password) throws IOException {
        this.data = data;
        if (indexOf(data, "%PDF-", 0, Math.min(data.length, 1024)) < 0) {
            throw new IOException("Not a PDF file");
        }
        boolean ok;
        try {
            ok = readXrefChain();
        } catch (RuntimeException e) {
            ok = false;
        }
        if (!ok || trailer == null || !(trailer.get("Root") instanceof PdfDict)) {
            rebuild();
        }
        if (trailer == null) {
            throw new IOException("PDF has no trailer");
        }
        Object enc = trailer.getRaw("Encrypt");
        if (enc != null) {
            if (enc instanceof PdfRef) {
                encryptObject = ((PdfRef) enc).number;
            }
            Object e = resolve(enc);
            if (e instanceof PdfDict) {
                byte[] id0 = new byte[0];
                List<Object> ids = trailer.getArray("ID");
                if (ids != null && !ids.isEmpty() && ids.get(0) instanceof PdfString) {
                    id0 = ((PdfString) ids.get(0)).bytes;
                }
                security = new PdfSecurity((PdfDict) e, id0, password);
                cache.clear();
                objectStreams.clear();
            }
        }
        PdfDict root = getCatalog();
        if (root == null) {
            throw new IOException("PDF has no document catalog");
        }
        collectPages(root.getDict("Pages"), null, new HashMap<Object, Object>(), 0);
    }

    /// True when the file was encrypted.
    public boolean isEncrypted() {
        return security != null;
    }

    /// The trailer dictionary.
    public PdfDict getTrailer() {
        return trailer;
    }

    /// The document catalog.
    public PdfDict getCatalog() {
        return trailer.getDict("Root");
    }

    /// The information dictionary, or null.
    public PdfDict getInfo() {
        return trailer.getDict("Info");
    }

    /// The pages, in order. Each is the page dictionary with inherited
    /// attributes (`Resources`, `MediaBox`, `CropBox`, `Rotate`) copied in.
    public List<PdfDict> getPages() {
        return pages;
    }

    private void collectPages(PdfDict node, PdfDict inherited, Map<Object, Object> seen, int depth) {
        if (node == null || depth > MAX_DEPTH || seen.containsKey(node)) {
            return;
        }
        seen.put(node, node);
        PdfDict inh = new PdfDict();
        inh.file = this;
        String[] keys = {"Resources", "MediaBox", "CropBox", "Rotate"};
        for (int i = 0; i < keys.length; i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            Object v = node.getRaw(keys[i]);
            if (v == null && inherited != null) {
                v = inherited.getRaw(keys[i]);
            }
            if (v != null) {
                inh.put(keys[i], v);
            }
        }
        List<Object> kids = node.getArray("Kids");
        String type = node.getName("Type");
        if (kids == null || "Page".equals(type)) {
            for (int i = 0; i < keys.length; i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
                if (!node.has(keys[i]) && inh.has(keys[i])) {
                    node.put(keys[i], inh.getRaw(keys[i]));
                }
            }
            pages.add(node);
            return;
        }
        for (int i = 0; i < kids.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            Object k = resolve(kids.get(i));
            if (k instanceof PdfDict) {
                collectPages((PdfDict) k, inh, seen, depth + 1);
            }
        }
    }

    /// Follows `o` if it is a reference; anything else is returned as is.
    public Object resolve(Object o) {
        if (!(o instanceof PdfRef)) {
            return o;
        }
        PdfRef r = (PdfRef) o;
        Integer key = Integer.valueOf(r.number);
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        if (resolveDepth > MAX_DEPTH) {
            return null;
        }
        resolveDepth++;
        Object v;
        try {
            v = load(r.number);
        } catch (RuntimeException e) {
            v = null;
        } catch (IOException e) {
            v = null;
        } finally {
            resolveDepth--;
        }
        cache.put(key, v);
        return v;
    }

    private Object load(int num) throws IOException {
        int[] e = xref.get(Integer.valueOf(num));
        if (e == null) {
            return null;
        }
        if (e[0] == -1) {
            return fromObjectStream(e[1], e[2], num);
        }
        return parseAt(e[0], num);
    }

    private Object parseAt(int offset, int expected) throws IOException {
        if (offset < 0 || offset >= data.length) {
            return null;
        }
        PdfLexer lx = new PdfLexer(data, offset, data.length, true);
        Object n = lx.next();
        Object g = lx.next();
        Object kw = lx.next();
        if (!(n instanceof Integer) || !(g instanceof Integer) || !(kw instanceof PdfLexer.Op)
                || !"obj".equals(((PdfLexer.Op) kw).name)) {
            return null;
        }
        int num = ((Integer) n).intValue();
        int gen = ((Integer) g).intValue();
        if (expected >= 0 && num != expected) {
            return null;
        }
        Object v = lx.next();
        if (v instanceof PdfDict) {
            int save = lx.pos;
            Object next = lx.next();
            if (next instanceof PdfLexer.Op && "stream".equals(((PdfLexer.Op) next).name)) {
                return stream((PdfDict) v, lx.pos, num, gen);
            }
            lx.pos = save;
        }
        return finish(v, num, gen);
    }

    private PdfStream stream(PdfDict dict, int afterKeyword, int num, int gen) {
        int start = afterKeyword;
        if (start < data.length && data[start] == '\r') {
            start++;
        }
        if (start < data.length && data[start] == '\n') {
            start++;
        }
        PdfStream s = new PdfStream();
        s.file = this;
        for (java.util.Iterator<String> it = dict.keys(); it.hasNext();) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            String k = it.next();
            s.put(k, dict.getRaw(k));
        }
        int len = -1;
        Object l = s.getRaw("Length");
        if (l instanceof Integer) {
            len = ((Integer) l).intValue();
        } else if (l instanceof PdfRef && ((PdfRef) l).number != num) {
            Object r = resolve(l);
            if (r instanceof Integer) {
                len = ((Integer) r).intValue();
            }
        }
        boolean valid = len >= 0 && (long) start + len <= data.length
                && indexOf(data, "endstream", start + len, Math.min(data.length, start + len + 32)) >= 0;
        if (!valid) {
            int end = indexOf(data, "endstream", start, data.length);
            if (end < 0) {
                end = data.length;
            }
            int e = end;
            if (e > start && data[e - 1] == '\n') {
                e--;
            }
            if (e > start && data[e - 1] == '\r') {
                e--;
            }
            len = e - start;
        }
        byte[] raw = new byte[len];
        System.arraycopy(data, start, raw, 0, len);
        if (security != null && num != encryptObject && !"XRef".equals(s.getName("Type"))) {
            raw = security.decryptStream(s, raw, num, gen);
        }
        s.raw = raw;
        decryptStrings(s, num, gen, 0);
        return s;
    }

    private Object finish(Object v, int num, int gen) {
        if (v instanceof PdfDict) {
            ((PdfDict) v).file = this;
        }
        if (security != null && num != encryptObject) {
            return decryptStrings(v, num, gen, 0);
        }
        attach(v, 0);
        return v;
    }

    private void attach(Object v, int depth) {
        if (depth > MAX_DEPTH) {
            return;
        }
        if (v instanceof PdfDict) {
            PdfDict d = (PdfDict) v;
            d.file = this;
            for (java.util.Iterator<String> it = d.keys(); it.hasNext();) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
                attach(d.getRaw(it.next()), depth + 1);
            }
        } else if (v instanceof List) {
            List<?> l = (List<?>) v;
            for (int i = 0; i < l.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
                attach(l.get(i), depth + 1);
            }
        }
    }

    private Object decryptStrings(Object v, int num, int gen, int depth) {
        if (depth > MAX_DEPTH) {
            return v;
        }
        if (v instanceof PdfString) {
            if (security != null && num != encryptObject) {
                PdfString s = (PdfString) v;
                s.bytes = security.decryptString(s.bytes, num, gen);
            }
        } else if (v instanceof PdfDict) {
            PdfDict d = (PdfDict) v;
            d.file = this;
            for (java.util.Iterator<String> it = d.keys(); it.hasNext();) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
                decryptStrings(d.getRaw(it.next()), num, gen, depth + 1);
            }
        } else if (v instanceof List) {
            List<?> l = (List<?>) v;
            for (int i = 0; i < l.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
                decryptStrings(l.get(i), num, gen, depth + 1);
            }
        }
        return v;
    }

    private Object fromObjectStream(int streamNum, int index, int num) throws IOException {
        Integer key = Integer.valueOf(streamNum);
        Object[] objs = objectStreams.get(key);
        if (objs == null) {
            Object s = resolve(new PdfRef(streamNum, 0));
            if (!(s instanceof PdfStream)) {
                return null;
            }
            PdfStream st = (PdfStream) s;
            byte[] d = st.getData();
            // Every entry takes at least two bytes of header, which bounds a
            // hostile /N before anything is allocated for it.
            int n = Math.max(0, Math.min(st.getInt("N", 0), d.length / 2));
            int first = st.getInt("First", 0);
            if (first < 0 || first > d.length) {
                n = 0;
            }
            PdfLexer header = new PdfLexer(d, 0, d.length, false);
            int[] nums = new int[n];
            int[] offs = new int[n];
            for (int i = 0; i < n; i++) {
                Object a = header.next();
                Object b = header.next();
                nums[i] = a instanceof Integer ? ((Integer) a).intValue() : -1;
                offs[i] = b instanceof Integer ? ((Integer) b).intValue() : -1;
            }
            objs = new Object[n * 2];
            for (int i = 0; i < n; i++) {
                objs[i * 2] = Integer.valueOf(nums[i]);
                Object o = null;
                if (offs[i] >= 0 && (long) first + offs[i] < d.length) {
                    o = new PdfLexer(d, first + offs[i], d.length, true).next();
                    // Objects inside an object stream are never encrypted
                    // themselves: the stream was.
                    attach(o, 0);
                }
                objs[i * 2 + 1] = o == PdfLexer.EOF ? null : o; // NOPMD CompareObjectsWithEquals - identity of a sentinel object is intended
            }
            objectStreams.put(key, objs);
        }
        if (index >= 0 && index * 2 + 1 < objs.length && objs[index * 2] instanceof Integer
                && ((Integer) objs[index * 2]).intValue() == num) {
            return objs[index * 2 + 1];
        }
        for (int i = 0; i + 1 < objs.length; i += 2) {
            if (objs[i] instanceof Integer && ((Integer) objs[i]).intValue() == num) {
                return objs[i + 1];
            }
        }
        return null;
    }

    private boolean readXrefChain() throws IOException {
        int sx = lastIndexOf(data, "startxref", Math.max(0, data.length - 2048));
        if (sx < 0) {
            return false;
        }
        PdfLexer lx = new PdfLexer(data, sx + 9, data.length, false);
        Object off = lx.next();
        if (!(off instanceof Integer)) {
            return false;
        }
        int offset = ((Integer) off).intValue();
        Map<Integer, Object> visited = new HashMap<Integer, Object>();
        while (offset > 0 && offset < data.length && !visited.containsKey(Integer.valueOf(offset))) {
            visited.put(Integer.valueOf(offset), Boolean.TRUE);
            PdfDict t = readXref(offset);
            if (t == null) {
                return false;
            }
            if (trailer == null) {
                trailer = t;
            }
            Object stm = t.getRaw("XRefStm");
            if (stm instanceof Integer) {
                readXref(((Integer) stm).intValue());
            }
            Object prev = t.getRaw("Prev");
            offset = prev instanceof Integer ? ((Integer) prev).intValue() : -1;
        }
        return trailer != null;
    }

    /// Reads one cross-reference section at `offset`, keeping entries
    /// already known (newer sections are read first), and returns its
    /// trailer.
    private PdfDict readXref(int offset) throws IOException {
        int p = offset;
        while (p < data.length && PdfFormat.isWhitespace(data[p] & 0xff)) {
            p++;
        }
        if (startsWith(data, p, "xref")) {
            PdfLexer lx = new PdfLexer(data, p + 4, data.length, false);
            while (true) {
                int save = lx.pos;
                Object a = lx.next();
                if (a instanceof PdfLexer.Op && "trailer".equals(((PdfLexer.Op) a).name)) {
                    Object t = new PdfLexer(data, lx.pos, data.length, true).next();
                    if (t instanceof PdfDict) {
                        attach(t, 0);
                        return (PdfDict) t;
                    }
                    return null;
                }
                Object b = lx.next();
                if (!(a instanceof Integer) || !(b instanceof Integer)) {
                    lx.pos = save;
                    return null;
                }
                int first = ((Integer) a).intValue();
                int count = ((Integer) b).intValue();
                lx.skipWhitespace();
                for (int i = 0; i < count; i++) {
                    lx.skipWhitespace();
                    int q = lx.pos;
                    if (q + 18 > data.length) {
                        return null;
                    }
                    int entryOff = (int) digits(q, 10);
                    char type = (char) data[q + 17];
                    lx.pos = q + 18;
                    Integer key = Integer.valueOf(first + i);
                    if (type == 'n' && !xref.containsKey(key) && entryOff > 0) {
                        xref.put(key, new int[] {entryOff});
                    } else if (type == 'f' && !xref.containsKey(key)) {
                        xref.put(key, null);
                    }
                }
            }
        }
        Object o = parseAt(p, -1);
        if (!(o instanceof PdfStream)) {
            return null;
        }
        PdfStream s = (PdfStream) o;
        List<Object> w = s.getArray("W");
        if (w == null || w.size() < 3) {
            return null;
        }
        int[] widths = new int[3];
        for (int i = 0; i < 3; i++) {
            widths[i] = w.get(i) instanceof Integer ? ((Integer) w.get(i)).intValue() : 0;
            if (widths[i] < 0 || widths[i] > 8) {
                return s;
            }
        }
        int entry = widths[0] + widths[1] + widths[2];
        if (entry == 0) {
            return s;
        }
        List<Object> index = s.getArray("Index");
        int size = s.getInt("Size", 0);
        byte[] d = s.getData();
        int pos = 0;
        int sections = index == null ? 1 : index.size() / 2;
        for (int sec = 0; sec < sections; sec++) {
            Object fo = index == null ? null : index.get(sec * 2);
            Object co = index == null ? null : index.get(sec * 2 + 1);
            if (index != null && !(fo instanceof Number && co instanceof Number)) {
                continue;
            }
            int first = index == null ? 0 : ((Number) fo).intValue();
            int count = index == null ? size : ((Number) co).intValue();
            // The data holds at most this many entries, whatever /Size says.
            count = Math.min(count, (d.length - pos) / entry);
            for (int i = 0; i < count; i++) {
                if (pos + entry > d.length) {
                    break;
                }
                int type = widths[0] == 0 ? 1 : (int) field(d, pos, widths[0]);
                pos += widths[0];
                long f2 = field(d, pos, widths[1]);
                pos += widths[1];
                long f3 = field(d, pos, widths[2]);
                pos += widths[2];
                Integer key = Integer.valueOf(first + i);
                if (xref.containsKey(key)) {
                    continue;
                }
                if (type == 1) {
                    xref.put(key, new int[] {(int) f2});
                } else if (type == 2) {
                    xref.put(key, new int[] {-1, (int) f2, (int) f3});
                } else {
                    xref.put(key, null);
                }
            }
        }
        return s;
    }

    private static long field(byte[] d, int pos, int width) {
        long v = 0;
        for (int i = 0; i < width; i++) {
            v = (v << 8) | (d[pos + i] & 0xff);
        }
        return v;
    }

    private long digits(int p, int n) {
        long v = 0;
        for (int i = 0; i < n; i++) {
            int c = data[p + i];
            if (c >= '0' && c <= '9') {
                v = v * 10 + (c - '0');
            }
        }
        return v;
    }

    /// Rebuilds the cross-reference by scanning for `n g obj`; later
    /// definitions win, as in an incrementally updated file.
    private void rebuild() {
        xref.clear();
        cache.clear();
        objectStreams.clear();
        PdfDict lastTrailer = null;
        for (int i = 0; i + 3 < data.length; i++) {
            if (data[i] == 'o' && data[i + 1] == 'b' && data[i + 2] == 'j'
                    && (i + 3 >= data.length || !isRegular(data[i + 3]))) {
                int p = i - 1;
                while (p >= 0 && PdfFormat.isWhitespace(data[p] & 0xff)) {
                    p--;
                }
                int genEnd = p + 1;
                while (p >= 0 && data[p] >= '0' && data[p] <= '9') {
                    p--;
                }
                if (p + 1 == genEnd) {
                    continue;
                }
                while (p >= 0 && PdfFormat.isWhitespace(data[p] & 0xff)) {
                    p--;
                }
                int numEnd = p + 1;
                while (p >= 0 && data[p] >= '0' && data[p] <= '9') {
                    p--;
                }
                if (p + 1 == numEnd) {
                    continue;
                }
                int num = (int) digits(p + 1, numEnd - p - 1);
                xref.put(Integer.valueOf(num), new int[] {p + 1});
            } else if (data[i] == 't' && startsWith(data, i, "trailer")) {
                Object t = new PdfLexer(data, i + 7, data.length, true).next();
                if (t instanceof PdfDict) {
                    lastTrailer = (PdfDict) t;
                }
            }
        }
        if (lastTrailer != null) {
            attach(lastTrailer, 0);
            if (lastTrailer.get("Root") instanceof PdfDict) {
                trailer = lastTrailer;
                return;
            }
        }
        // No usable trailer: find the catalog, and any object streams'
        // contents, by looking at every object.
        PdfDict t = new PdfDict();
        t.file = this;
        List<Integer> nums = new ArrayList<Integer>(xref.keySet());
        for (int i = 0; i < nums.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            Object o = resolve(new PdfRef(nums.get(i).intValue(), 0));
            if (o instanceof PdfStream && "ObjStm".equals(((PdfStream) o).getName("Type"))) {
                indexObjectStream(nums.get(i).intValue(), (PdfStream) o);
            }
        }
        nums = new ArrayList<Integer>(xref.keySet());
        for (int i = 0; i < nums.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            Object o = resolve(new PdfRef(nums.get(i).intValue(), 0));
            if (o instanceof PdfDict && "Catalog".equals(((PdfDict) o).getName("Type"))) {
                t.put("Root", new PdfRef(nums.get(i).intValue(), 0));
            } else if (o instanceof PdfStream && "XRef".equals(((PdfStream) o).getName("Type"))) {
                PdfStream xs = (PdfStream) o;
                if (xs.has("Encrypt")) {
                    t.put("Encrypt", xs.getRaw("Encrypt"));
                }
                if (xs.has("ID")) {
                    t.put("ID", xs.getRaw("ID"));
                }
                if (xs.has("Info")) {
                    t.put("Info", xs.getRaw("Info"));
                }
            }
        }
        trailer = t;
    }

    private void indexObjectStream(int streamNum, PdfStream s) {
        try {
            byte[] d = s.getData();
            int n = Math.min(s.getInt("N", 0), d.length / 2);
            PdfLexer header = new PdfLexer(d, 0, d.length, false);
            for (int i = 0; i < n; i++) {
                Object a = header.next();
                if (a == PdfLexer.EOF) { // NOPMD CompareObjectsWithEquals - identity of a sentinel object is intended
                    break;
                }
                header.next();
                if (a instanceof Integer && !xref.containsKey(a)) {
                    xref.put((Integer) a, new int[] {-1, streamNum, i});
                }
            }
        } catch (IOException e) {
            // A broken object stream contributes nothing.
        }
    }

    private static boolean isRegular(byte b) {
        int c = b & 0xff;
        return !PdfFormat.isWhitespace(c) && !PdfFormat.isDelimiter(c);
    }

    private static boolean startsWith(byte[] d, int p, String s) {
        if (p < 0 || p + s.length() > d.length) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (d[p + i] != s.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    static int indexOf(byte[] d, String s, int from, int to) {
        int last = Math.min(to, d.length) - s.length();
        for (int i = Math.max(0, from); i <= last; i++) {
            if (startsWith(d, i, s)) {
                return i;
            }
        }
        return -1;
    }

    private static int lastIndexOf(byte[] d, String s, int from) {
        for (int i = d.length - s.length(); i >= from; i--) {
            if (startsWith(d, i, s)) {
                return i;
            }
        }
        return -1;
    }
}
