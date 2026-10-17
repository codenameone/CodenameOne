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

/// The physical layer of a .NET assembly: the PE container, the metadata
/// streams and the metadata tables, as ECMA-335 partition II describes them.
///
/// Nothing here interprets a row. A table is addressed by its number, a row by
/// its 1-based index and a column by its position, and the answer is the raw
/// integer stored there. [CilAssembly] turns those into types and methods.
///
/// The schema below has to list every table the standard defines, used or not:
/// rows are packed back to back with no per-table offset, so the position of a
/// table is only known once the width of every table before it is.
public final class MetadataReader {
    public static final int MODULE = 0x00;
    public static final int TYPE_REF = 0x01;
    public static final int TYPE_DEF = 0x02;
    public static final int FIELD = 0x04;
    public static final int METHOD_DEF = 0x06;
    public static final int PARAM = 0x08;
    public static final int INTERFACE_IMPL = 0x09;
    public static final int MEMBER_REF = 0x0A;
    public static final int CONSTANT = 0x0B;
    public static final int CUSTOM_ATTRIBUTE = 0x0C;
    public static final int CLASS_LAYOUT = 0x0F;
    public static final int STANDALONE_SIG = 0x11;
    public static final int EVENT_MAP = 0x12;
    public static final int EVENT = 0x14;
    public static final int PROPERTY_MAP = 0x15;
    public static final int PROPERTY = 0x17;
    public static final int METHOD_SEMANTICS = 0x18;
    public static final int METHOD_IMPL = 0x19;
    public static final int MODULE_REF = 0x1A;
    public static final int TYPE_SPEC = 0x1B;
    public static final int FIELD_RVA = 0x1D;
    public static final int ASSEMBLY = 0x20;
    public static final int ASSEMBLY_REF = 0x23;
    public static final int FILE = 0x26;
    public static final int EXPORTED_TYPE = 0x27;
    public static final int NESTED_CLASS = 0x29;
    public static final int GENERIC_PARAM = 0x2A;
    public static final int METHOD_SPEC = 0x2B;
    public static final int GENERIC_PARAM_CONSTRAINT = 0x2C;

    private static final int TABLE_COUNT = 0x2D;

    // Column kinds. A positive value is a fixed width in bytes; the heap
    // indexes are negative; a simple table index is T + table and a coded
    // index is C + its position in CODED.
    private static final int STR = -1;
    private static final int GUID = -2;
    private static final int BLOB = -3;
    private static final int T = 100;
    private static final int C = 200;

    public static final int TYPE_DEF_OR_REF = 0;
    public static final int HAS_CONSTANT = 1;
    public static final int HAS_CUSTOM_ATTRIBUTE = 2;
    public static final int HAS_FIELD_MARSHAL = 3;
    public static final int HAS_DECL_SECURITY = 4;
    public static final int MEMBER_REF_PARENT = 5;
    public static final int HAS_SEMANTICS = 6;
    public static final int METHOD_DEF_OR_REF = 7;
    public static final int MEMBER_FORWARDED = 8;
    public static final int IMPLEMENTATION = 9;
    public static final int CUSTOM_ATTRIBUTE_TYPE = 10;
    public static final int RESOLUTION_SCOPE = 11;
    public static final int TYPE_OR_METHOD_DEF = 12;

    // The tables each coded index can point into, in tag order. -1 is a tag
    // value the standard leaves unused.
    private static final int[][] CODED = {
        {TYPE_DEF, TYPE_REF, TYPE_SPEC},
        {FIELD, PARAM, PROPERTY},
        {METHOD_DEF, FIELD, TYPE_REF, TYPE_DEF, PARAM, INTERFACE_IMPL, MEMBER_REF, MODULE, 0x0E, PROPERTY, EVENT,
            STANDALONE_SIG, MODULE_REF, TYPE_SPEC, ASSEMBLY, ASSEMBLY_REF, FILE, EXPORTED_TYPE, 0x28, GENERIC_PARAM,
            GENERIC_PARAM_CONSTRAINT, METHOD_SPEC},
        {FIELD, PARAM},
        {TYPE_DEF, METHOD_DEF, ASSEMBLY},
        {TYPE_DEF, TYPE_REF, MODULE_REF, METHOD_DEF, TYPE_SPEC},
        {EVENT, PROPERTY},
        {METHOD_DEF, MEMBER_REF},
        {FIELD, METHOD_DEF},
        {FILE, ASSEMBLY_REF, EXPORTED_TYPE},
        {-1, -1, METHOD_DEF, MEMBER_REF, -1},
        {MODULE, MODULE_REF, ASSEMBLY_REF, TYPE_REF},
        {TYPE_DEF, METHOD_DEF},
    };

    private static final int[][] COLUMNS = new int[TABLE_COUNT][];

    static {
        COLUMNS[0x00] = new int[] {2, STR, GUID, GUID, GUID};
        COLUMNS[0x01] = new int[] {C + RESOLUTION_SCOPE, STR, STR};
        COLUMNS[0x02] = new int[] {4, STR, STR, C + TYPE_DEF_OR_REF, T + FIELD, T + METHOD_DEF};
        COLUMNS[0x03] = new int[] {T + FIELD};
        COLUMNS[0x04] = new int[] {2, STR, BLOB};
        COLUMNS[0x05] = new int[] {T + METHOD_DEF};
        COLUMNS[0x06] = new int[] {4, 2, 2, STR, BLOB, T + PARAM};
        COLUMNS[0x07] = new int[] {T + PARAM};
        COLUMNS[0x08] = new int[] {2, 2, STR};
        COLUMNS[0x09] = new int[] {T + TYPE_DEF, C + TYPE_DEF_OR_REF};
        COLUMNS[0x0A] = new int[] {C + MEMBER_REF_PARENT, STR, BLOB};
        COLUMNS[0x0B] = new int[] {2, C + HAS_CONSTANT, BLOB};
        COLUMNS[0x0C] = new int[] {C + HAS_CUSTOM_ATTRIBUTE, C + CUSTOM_ATTRIBUTE_TYPE, BLOB};
        COLUMNS[0x0D] = new int[] {C + HAS_FIELD_MARSHAL, BLOB};
        COLUMNS[0x0E] = new int[] {2, C + HAS_DECL_SECURITY, BLOB};
        COLUMNS[0x0F] = new int[] {2, 4, T + TYPE_DEF};
        COLUMNS[0x10] = new int[] {4, T + FIELD};
        COLUMNS[0x11] = new int[] {BLOB};
        COLUMNS[0x12] = new int[] {T + TYPE_DEF, T + EVENT};
        COLUMNS[0x13] = new int[] {T + EVENT};
        COLUMNS[0x14] = new int[] {2, STR, C + TYPE_DEF_OR_REF};
        COLUMNS[0x15] = new int[] {T + TYPE_DEF, T + PROPERTY};
        COLUMNS[0x16] = new int[] {T + PROPERTY};
        COLUMNS[0x17] = new int[] {2, STR, BLOB};
        COLUMNS[0x18] = new int[] {2, T + METHOD_DEF, C + HAS_SEMANTICS};
        COLUMNS[0x19] = new int[] {T + TYPE_DEF, C + METHOD_DEF_OR_REF, C + METHOD_DEF_OR_REF};
        COLUMNS[0x1A] = new int[] {STR};
        COLUMNS[0x1B] = new int[] {BLOB};
        COLUMNS[0x1C] = new int[] {2, C + MEMBER_FORWARDED, STR, T + MODULE_REF};
        COLUMNS[0x1D] = new int[] {4, T + FIELD};
        COLUMNS[0x1E] = new int[] {4, 4};
        COLUMNS[0x1F] = new int[] {4};
        COLUMNS[0x20] = new int[] {4, 2, 2, 2, 2, 4, BLOB, STR, STR};
        COLUMNS[0x21] = new int[] {4};
        COLUMNS[0x22] = new int[] {4, 4, 4};
        COLUMNS[0x23] = new int[] {2, 2, 2, 2, 4, BLOB, STR, STR, BLOB};
        COLUMNS[0x24] = new int[] {4, T + ASSEMBLY_REF};
        COLUMNS[0x25] = new int[] {4, 4, 4, T + ASSEMBLY_REF};
        COLUMNS[0x26] = new int[] {4, STR, BLOB};
        COLUMNS[0x27] = new int[] {4, 4, STR, STR, C + IMPLEMENTATION};
        COLUMNS[0x28] = new int[] {4, 4, STR, C + IMPLEMENTATION};
        COLUMNS[0x29] = new int[] {T + TYPE_DEF, T + TYPE_DEF};
        COLUMNS[0x2A] = new int[] {2, 2, C + TYPE_OR_METHOD_DEF, STR};
        COLUMNS[0x2B] = new int[] {C + METHOD_DEF_OR_REF, BLOB};
        COLUMNS[0x2C] = new int[] {T + GENERIC_PARAM, C + TYPE_DEF_OR_REF};
    }

    private final byte[] data;
    private final String source;

    private int[] sectionRva;
    private int[] sectionSize;
    private int[] sectionOffset;

    private int stringsOffset;
    private int userStringsOffset;
    private int blobOffset;
    private boolean wideStrings;
    private boolean wideGuids;
    private boolean wideBlobs;

    private final int[] rowCounts = new int[TABLE_COUNT];
    private final int[] tableOffsets = new int[TABLE_COUNT];
    private final int[] rowSizes = new int[TABLE_COUNT];
    private final int[][] columnOffsets = new int[TABLE_COUNT][];
    private final int[][] columnSizes = new int[TABLE_COUNT][];

    private int entryPointToken;

    public MetadataReader(byte[] data, String source) {
        this.data = data;
        this.source = source;
        readPe();
    }

    public String source() {
        return source;
    }

    private CilFormatException bad(String what) {
        return new CilFormatException(source + ": " + what);
    }

    public int u1(int at) {
        return data[at] & 0xFF;
    }

    public int u2(int at) {
        return (data[at] & 0xFF) | ((data[at + 1] & 0xFF) << 8);
    }

    public int i4(int at) {
        return (data[at] & 0xFF) | ((data[at + 1] & 0xFF) << 8) | ((data[at + 2] & 0xFF) << 16)
                | ((data[at + 3] & 0xFF) << 24);
    }

    public long i8(int at) {
        return (i4(at) & 0xFFFFFFFFL) | ((long) i4(at + 4) << 32);
    }

    public byte[] bytes(int at, int length) {
        byte[] out = new byte[length];
        System.arraycopy(data, at, out, 0, length);
        return out;
    }

    private void readPe() {
        if (data.length < 0x40 || u2(0) != 0x5A4D) {
            throw bad("not a PE file");
        }
        int pe = i4(0x3C);
        if (pe <= 0 || pe + 24 > data.length || i4(pe) != 0x00004550) {
            throw bad("missing PE signature");
        }
        int coff = pe + 4;
        int sections = u2(coff + 2);
        int optionalSize = u2(coff + 16);
        int optional = coff + 20;
        int magic = u2(optional);
        int directories;
        if (magic == 0x10B) {
            directories = optional + 96;
        } else if (magic == 0x20B) {
            directories = optional + 112;
        } else {
            throw bad("unknown optional header magic 0x" + Integer.toHexString(magic));
        }
        int sectionTable = optional + optionalSize;
        sectionRva = new int[sections];
        sectionSize = new int[sections];
        sectionOffset = new int[sections];
        for (int i = 0; i < sections; i++) {
            int s = sectionTable + i * 40;
            int virtualSize = i4(s + 8);
            int rawSize = i4(s + 16);
            sectionRva[i] = i4(s + 12);
            sectionSize[i] = Math.max(virtualSize, rawSize);
            sectionOffset[i] = i4(s + 20);
        }
        // Directory 14 is the CLI header; a native image has none.
        int cliRva = i4(directories + 14 * 8);
        if (cliRva == 0) {
            throw bad("not a managed assembly (no CLI header)");
        }
        int cli = rvaToOffset(cliRva);
        int metadata = rvaToOffset(i4(cli + 8));
        entryPointToken = i4(cli + 20);
        readMetadataRoot(metadata);
    }

    /// Maps a relative virtual address, which is how the metadata names a
    /// method body or a field's initial data, to a position in the file.
    public int rvaToOffset(int rva) {
        for (int i = 0; i < sectionRva.length; i++) {
            if (rva >= sectionRva[i] && rva < sectionRva[i] + sectionSize[i]) {
                return rva - sectionRva[i] + sectionOffset[i];
            }
        }
        throw bad("RVA 0x" + Integer.toHexString(rva) + " is outside every section");
    }

    private void readMetadataRoot(int root) {
        if (i4(root) != 0x424A5342) {
            throw bad("bad metadata signature");
        }
        int versionLength = i4(root + 12);
        int at = root + 16 + versionLength;
        int streams = u2(at + 2);
        at += 4;
        int tables = -1;
        for (int i = 0; i < streams; i++) {
            int offset = i4(at);
            at += 8;
            int nameStart = at;
            while (data[at] != 0) {
                at++;
            }
            String name = new String(data, nameStart, at - nameStart, StandardCharsets.US_ASCII);
            // The name is NUL terminated and then padded to a 4 byte boundary.
            at = nameStart + ((at - nameStart + 4) & ~3);
            if ("#~".equals(name) || "#-".equals(name)) {
                tables = root + offset;
            } else if ("#Strings".equals(name)) {
                stringsOffset = root + offset;
            } else if ("#US".equals(name)) {
                userStringsOffset = root + offset;
            } else if ("#Blob".equals(name)) {
                blobOffset = root + offset;
            }
        }
        if (tables < 0) {
            throw bad("no metadata table stream");
        }
        readTables(tables);
    }

    private void readTables(int at) {
        int heapSizes = u1(at + 6);
        wideStrings = (heapSizes & 1) != 0;
        wideGuids = (heapSizes & 2) != 0;
        wideBlobs = (heapSizes & 4) != 0;
        long valid = i8(at + 8);
        int p = at + 24;
        for (int t = 0; t < 64; t++) {
            if ((valid & (1L << t)) == 0) {
                continue;
            }
            if (t >= TABLE_COUNT) {
                throw bad("unknown metadata table 0x" + Integer.toHexString(t));
            }
            rowCounts[t] = i4(p);
            p += 4;
        }
        for (int t = 0; t < TABLE_COUNT; t++) {
            int[] kinds = COLUMNS[t];
            int[] offsets = new int[kinds.length];
            int[] sizes = new int[kinds.length];
            int width = 0;
            for (int c = 0; c < kinds.length; c++) {
                offsets[c] = width;
                sizes[c] = columnSize(kinds[c]);
                width += sizes[c];
            }
            columnOffsets[t] = offsets;
            columnSizes[t] = sizes;
            rowSizes[t] = width;
            tableOffsets[t] = p;
            p += width * rowCounts[t];
        }
    }

    private int columnSize(int kind) {
        if (kind > 0 && kind < T) {
            return kind;
        }
        if (kind == STR) {
            return wideStrings ? 4 : 2;
        }
        if (kind == GUID) {
            return wideGuids ? 4 : 2;
        }
        if (kind == BLOB) {
            return wideBlobs ? 4 : 2;
        }
        if (kind >= C) {
            int[] targets = CODED[kind - C];
            int max = 0;
            for (int target : targets) {
                if (target >= 0) {
                    max = Math.max(max, rowCounts[target]);
                }
            }
            return max < (1 << (16 - tagBits(targets.length))) ? 2 : 4;
        }
        return rowCounts[kind - T] < 65536 ? 2 : 4;
    }

    private static int tagBits(int choices) {
        int bits = 0;
        while ((1 << bits) < choices) {
            bits++;
        }
        return bits;
    }

    public int rowCount(int table) {
        return rowCounts[table];
    }

    /// The raw value of one cell. `row` is 1-based, as every reference in the
    /// metadata is; row 0 means "none" and is never stored.
    public int cell(int table, int row, int column) {
        if (row < 1 || row > rowCounts[table]) {
            throw bad("row " + row + " is outside table 0x" + Integer.toHexString(table) + " (" + rowCounts[table]
                    + " rows)");
        }
        int at = tableOffsets[table] + (row - 1) * rowSizes[table] + columnOffsets[table][column];
        return columnSizes[table][column] == 2 ? u2(at) : i4(at);
    }

    /// Decodes a coded index into a token: the table number in the top byte
    /// and the row below it, which is also how IL operands name a row.
    public int codedToken(int coded, int value) {
        int[] targets = CODED[coded];
        int bits = tagBits(targets.length);
        int tag = value & ((1 << bits) - 1);
        int row = value >>> bits;
        if (tag >= targets.length || targets[tag] < 0) {
            throw bad("coded index tag " + tag + " is not defined for coded index " + coded);
        }
        return (targets[tag] << 24) | row;
    }

    public static int tokenTable(int token) {
        return token >>> 24;
    }

    public static int tokenRow(int token) {
        return token & 0xFFFFFF;
    }

    /// The first row past a row's member list. A list column (the fields or
    /// methods of a type) holds only where the run starts; it ends where the
    /// next row's run starts, or at the end of the member table.
    public int listEnd(int table, int row, int column, int memberTable) {
        if (row < rowCounts[table]) {
            return cell(table, row + 1, column);
        }
        return rowCounts[memberTable] + 1;
    }

    public String string(int index) {
        int start = stringsOffset + index;
        int end = start;
        while (data[end] != 0) {
            end++;
        }
        return new String(data, start, end - start, StandardCharsets.UTF_8);
    }

    /// Position and length of a blob, as `{offset, length}`.
    public int[] blob(int index) {
        return compressedLengthPrefixed(blobOffset + index);
    }

    private int[] compressedLengthPrefixed(int at) {
        int first = u1(at);
        if ((first & 0x80) == 0) {
            return new int[] {at + 1, first};
        }
        if ((first & 0xC0) == 0x80) {
            return new int[] {at + 2, ((first & 0x3F) << 8) | u1(at + 1)};
        }
        return new int[] {at + 4, ((first & 0x1F) << 24) | (u1(at + 1) << 16) | (u1(at + 2) << 8) | u1(at + 3)};
    }

    /// The string an `ldstr` token names. User strings are UTF-16LE with one
    /// trailing flag byte that is not part of the text.
    public String userString(int token) {
        int[] span = compressedLengthPrefixed(userStringsOffset + tokenRow(token));
        int chars = span[1] / 2;
        char[] out = new char[chars];
        for (int i = 0; i < chars; i++) {
            out[i] = (char) u2(span[0] + i * 2);
        }
        return new String(out);
    }

    public int entryPointToken() {
        return entryPointToken;
    }
}
