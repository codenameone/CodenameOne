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
package com.codename1.cil.metadata;

import java.nio.charset.StandardCharsets;

/// The part of a portable PDB that says which line of which source file an
/// instruction came from.
///
/// A portable PDB is the file the C# compiler writes beside an assembly
/// under `<DebugType>portable</DebugType>`. It is laid out as an assembly's
/// own metadata is (ECMA-335, II.24): the same root, the same heaps and a
/// table stream -- holding tables of its own, numbered from 0x30, in place
/// of the type system's. Two of them are read here:
///
/// - `Document` (0x30): a source file, its name a blob of parts.
/// - `MethodDebugInformation` (0x31): one row for each row of the
///   assembly's `MethodDef` table, in the same order, with the method's
///   *sequence points* -- the places a debugger can stop, each an IL offset
///   and the source range it stands for.
///
/// Those are the first two tables of the stream and neither has a column
/// that refers to a later one, so nothing else needs to be understood to
/// find them. The format is public: "Portable PDB v1.0: Format
/// Specification", in the `dotnet/runtime` documentation.
///
/// It is only ever a help. A file that is absent, or is not what this
/// expects, gives no locations and no error: the translation it would have
/// described is unchanged.
public final class PortablePdb {
    private static final int DOCUMENT = 0x30;
    private static final int METHOD_DEBUG_INFORMATION = 0x31;
    /// The line a compiler gives a sequence point that stands for no source.
    private static final int HIDDEN_LINE = 0xFEEFEE;

    private final byte[] data;
    private int stringsEnd;
    private int blobOffset;
    private int blobEnd;
    private boolean wideBlobs;
    private int documents;
    private int documentsAt;
    private int documentRow;
    private int methods;
    private int methodsAt;
    private int methodRow;
    private String[] documentNames;
    private int at;

    private PortablePdb(byte[] data) {
        this.data = data;
    }

    /// Reads a PDB, or answers null for bytes that are not one.
    public static PortablePdb read(byte[] data) {
        PortablePdb pdb = new PortablePdb(data);
        try {
            return pdb.readRoot() ? pdb : null;
        } catch (IndexOutOfBoundsException e) {
            // Truncated, or not laid out as expected: no locations.
            return null;
        }
    }

    private int u1(int p) {
        return data[p] & 0xFF;
    }

    private int u2(int p) {
        return (data[p] & 0xFF) | ((data[p + 1] & 0xFF) << 8);
    }

    private int i4(int p) {
        return (data[p] & 0xFF) | ((data[p + 1] & 0xFF) << 8) | ((data[p + 2] & 0xFF) << 16)
                | ((data[p + 3] & 0xFF) << 24);
    }

    private boolean readRoot() {
        if (data.length < 32 || i4(0) != 0x424A5342) {
            return false;
        }
        int p = 16 + i4(12);
        int streams = u2(p + 2);
        p += 4;
        int tables = -1;
        boolean pdbStream = false;
        for (int i = 0; i < streams; i++) {
            int offset = i4(p);
            int size = i4(p + 4);
            p += 8;
            int nameStart = p;
            while (data[p] != 0) {
                p++;
            }
            String name = new String(data, nameStart, p - nameStart, StandardCharsets.US_ASCII);
            p = nameStart + ((p - nameStart + 4) & ~3);
            if ("#~".equals(name)) {
                tables = offset;
            } else if ("#Blob".equals(name)) {
                blobOffset = offset;
                blobEnd = offset + size;
            } else if ("#Strings".equals(name)) {
                stringsEnd = offset + size;
            } else if ("#Pdb".equals(name)) {
                pdbStream = true;
            }
        }
        if (tables < 0 || !pdbStream || blobEnd > data.length || stringsEnd > data.length) {
            return false;
        }
        int heapSizes = u1(tables + 6);
        boolean wideGuids = (heapSizes & 2) != 0;
        wideBlobs = (heapSizes & 4) != 0;
        long valid = (i4(tables + 8) & 0xFFFFFFFFL) | ((long) i4(tables + 12) << 32);
        // A stand-alone PDB carries the debug tables alone. One merged into
        // an assembly's own metadata would need every earlier table's row
        // size, which is the other reader's business.
        if ((valid & ((1L << DOCUMENT) - 1)) != 0) {
            return false;
        }
        p = tables + 24;
        int present = 0;
        for (int t = DOCUMENT; t < 64; t++) {
            if ((valid & (1L << t)) == 0) {
                continue;
            }
            if (t == DOCUMENT) {
                documents = i4(p);
            } else if (t == METHOD_DEBUG_INFORMATION) {
                methods = i4(p);
            }
            p += 4;
            present++;
        }
        if (present == 0) {
            return false;
        }
        int blob = wideBlobs ? 4 : 2;
        int guid = wideGuids ? 4 : 2;
        documentRow = blob + guid + blob + guid;
        documentsAt = p;
        methodRow = (documents < 65536 ? 2 : 4) + blob;
        methodsAt = documentsAt + documents * documentRow;
        if (methodsAt + methods * methodRow > data.length) {
            return false;
        }
        documentNames = new String[documents + 1];
        return true;
    }

    private int index(int p, boolean wide) {
        return wide ? i4(p) : u2(p);
    }

    /// An unsigned compressed integer (ECMA-335, II.23.2) at [#at], which
    /// moves past it.
    private int compressed() {
        int first = u1(at);
        if ((first & 0x80) == 0) {
            at += 1;
            return first;
        }
        if ((first & 0xC0) == 0x80) {
            int v = ((first & 0x3F) << 8) | u1(at + 1);
            at += 2;
            return v;
        }
        int v = ((first & 0x1F) << 24) | (u1(at + 1) << 16) | (u1(at + 2) << 8) | u1(at + 3);
        at += 4;
        return v;
    }

    /// A signed compressed integer: the sign is rotated into the lowest
    /// bit, and how many bits the rest has depends on how many bytes the
    /// number took.
    private int signedCompressed() {
        int start = at;
        int raw = compressed();
        int value = raw >>> 1;
        if ((raw & 1) == 0) {
            return value;
        }
        int width = at - start;
        return value - (width == 1 ? 0x40 : width == 2 ? 0x2000 : 0x10000000);
    }

    /// The name of a document: a separator, then for each part of the path
    /// the index of a blob holding it. Only the last part is kept -- the
    /// rest is a directory of the machine that compiled it.
    private String documentName(int row) {
        if (row < 1 || row > documents) {
            return null;
        }
        if (documentNames[row] != null) {
            return documentNames[row];
        }
        int saved = at;
        at = blobOffset + index(documentsAt + (row - 1) * documentRow, wideBlobs);
        int length = compressed();
        int end = at + length;
        int separator = length > 0 ? u1(at) : 0;
        at++;
        String last = "";
        while (at < end) {
            int part = compressed();
            if (part == 0) {
                continue;
            }
            int resume = at;
            at = blobOffset + part;
            int partLength = compressed();
            String text = new String(data, at, partLength, StandardCharsets.UTF_8);
            at = resume;
            if (text.length() > 0) {
                last = text;
            }
        }
        if (separator == 0) {
            // No separator: the name is one part, a whole path.
            int slash = Math.max(last.lastIndexOf('/'), last.lastIndexOf('\\'));
            last = slash >= 0 ? last.substring(slash + 1) : last;
        }
        at = saved;
        documentNames[row] = last;
        return last;
    }

    /// Where in the source the instruction at an offset of a method came
    /// from, as `File.cs:line`, or null when the PDB does not say.
    /// `methodRow` is the method's row in the assembly's `MethodDef` table.
    ///
    /// The answer is the last sequence point at or before the offset that
    /// stands for source, since an instruction belongs to the statement
    /// that began before it; failing that -- an offset inside the code a
    /// compiler puts ahead of the first statement -- the first that does.
    public String location(int methodDefRow, int ilOffset) {
        try {
            return find(methodDefRow, ilOffset);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    private String find(int methodDefRow, int ilOffset) {
        if (methodDefRow < 1 || methodDefRow > methods) {
            return null;
        }
        int row = methodsAt + (methodDefRow - 1) * methodRow;
        int document = documents < 65536 ? u2(row) : i4(row);
        int blob = index(row + (documents < 65536 ? 2 : 4), wideBlobs);
        if (blob == 0) {
            return null;
        }
        at = blobOffset + blob;
        int length = compressed();
        int end = at + length;
        // The header: the signature of the locals, then the document when
        // the row itself names none because the method spans several.
        compressed();
        if (document == 0) {
            document = compressed();
        }
        String best = null;
        String first = null;
        int offset = 0;
        int line = 0;
        int column = 0;
        boolean firstRecord = true;
        boolean firstVisible = true;
        while (at < end) {
            int deltaOffset = compressed();
            if (!firstRecord && deltaOffset == 0) {
                // Not a point: the points that follow are in another file.
                document = compressed();
                continue;
            }
            firstRecord = false;
            offset += deltaOffset;
            int deltaLines = compressed();
            int deltaColumns = deltaLines == 0 ? compressed() : signedCompressed();
            if (deltaLines == 0 && deltaColumns == 0) {
                // A hidden point: code that stands for no source.
                continue;
            }
            if (firstVisible) {
                line = compressed();
                column = compressed();
                firstVisible = false;
            } else {
                line += signedCompressed();
                column += signedCompressed();
            }
            if (line == HIDDEN_LINE) {
                continue;
            }
            String here = documentName(document);
            here = here == null ? null : here + ":" + line;
            if (first == null) {
                first = here;
            }
            if (offset <= ilOffset) {
                best = here;
            } else {
                break;
            }
        }
        return best != null ? best : first;
    }
}
