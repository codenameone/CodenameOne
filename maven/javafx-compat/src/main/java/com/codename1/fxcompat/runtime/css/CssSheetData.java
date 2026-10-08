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
package com.codename1.fxcompat.runtime.css;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

/// A compiled style sheet: what the build writes for every `.css` file of
/// an application and the device reads in its place. Both directions are
/// here, in one class, so that the two cannot drift apart.
///
/// #### What it holds
///
/// - the sheets it imports, as resource paths, in the order written;
/// - its `@font-face` rules, as a family and the resource path of a font;
/// - its rules, in the order written: a selector and the number of the
///   declaration block it belongs to. A rule with a list of selectors
///   (`a, b { ... }`) is one entry per selector, all naming one block, so a
///   block is stored once.
///
/// #### The file
///
/// Big endian, as `DataOutputStream` writes: the magic number `CNFS`, a
/// format version byte, a table of every string in the sheet, then the
/// imports, the font faces, the declaration blocks and the rules, each as a
/// count followed by its entries, with every string given as its number in
/// the table. The format is private to one build of this layer -- the
/// reader and the writer always ship together -- so the version only
/// guards against a stale file, which is rejected rather than misread.
///
/// A sheet ships under the name [#compiledName(String)] gives its path.
public final class CssSheetData {

    /// What follows the flat name of a style sheet in the name its compiled
    /// form ships under: `styles/app.css` is flattened to `styles__app.css`
    /// like any resource and its compiled form is `styles__app.css.cn1css`,
    /// at the root of the application bundle.
    public static final String SUFFIX = ".cn1css";

    private static final int MAGIC = 0x434E4653;
    private static final int VERSION = 1;

    private final String[] imports;
    private final String[] fontFamilies;
    private final String[] fontSources;
    private final CssSelector[] selectors;
    private final int[] selectorBlocks;
    private final CssDeclaration[][] blocks;

    /// Creates a sheet. `selectorBlocks[i]` is the index in `blocks` of the
    /// declarations `selectors[i]` belongs to; `fontSources[i]` is the font
    /// of `fontFamilies[i]`. The arrays are taken as they are.
    public CssSheetData(String[] imports, String[] fontFamilies, String[] fontSources, CssSelector[] selectors,
            int[] selectorBlocks, CssDeclaration[][] blocks) {
        this.imports = imports;
        this.fontFamilies = fontFamilies;
        this.fontSources = fontSources;
        this.selectors = selectors;
        this.selectorBlocks = selectorBlocks;
        this.blocks = blocks;
    }

    /// The name the compiled form of the style sheet at `flatName` -- the
    /// name `com.codename1.compat.jdk.ResourceNames.flatName` gives its
    /// resource path -- ships under.
    public static String compiledName(String flatName) {
        return flatName + SUFFIX;
    }

    /// How many sheets this one imports.
    public int importCount() {
        return imports.length;
    }

    /// The resource path, without a leading slash, of import `i`.
    public String importPath(int i) {
        return imports[i];
    }

    /// How many `@font-face` rules the sheet has.
    public int fontCount() {
        return fontFamilies.length;
    }

    /// The family font `i` is declared as.
    public String fontFamily(int i) {
        return fontFamilies[i];
    }

    /// The resource path, without a leading slash, of font `i`.
    public String fontSource(int i) {
        return fontSources[i];
    }

    /// How many selectors the sheet has; a rule counts once per selector.
    public int ruleCount() {
        return selectors.length;
    }

    /// Selector `i`, in the order written.
    public CssSelector selector(int i) {
        return selectors[i];
    }

    /// The index of the declaration block selector `i` belongs to.
    public int blockOf(int i) {
        return selectorBlocks[i];
    }

    /// How many declaration blocks the sheet has.
    public int blockCount() {
        return blocks.length;
    }

    /// How many declarations block `block` has.
    public int declarationCount(int block) {
        return blocks[block].length;
    }

    /// Declaration `i` of block `block`.
    public CssDeclaration declaration(int block, int i) {
        return blocks[block][i];
    }

    // --------------------------------------------------------------- writing

    private static final class Strings {
        final HashMap<String, Integer> index = new HashMap<String, Integer>();
        final ArrayList<String> list = new ArrayList<String>();

        int of(String s) {
            if (s == null) {
                return 0;
            }
            Integer at = index.get(s);
            if (at == null) {
                list.add(s);
                at = Integer.valueOf(list.size());
                index.put(s, at);
            }
            return at.intValue();
        }
    }

    /// Writes the sheet. The stream is flushed, not closed.
    public void write(OutputStream to) throws IOException {
        Strings strings = new Strings();
        for (int i = 0; i < imports.length; i++) {
            strings.of(imports[i]);
        }
        for (int i = 0; i < fontFamilies.length; i++) {
            strings.of(fontFamilies[i]);
            strings.of(fontSources[i]);
        }
        for (int b = 0; b < blocks.length; b++) {
            for (int i = 0; i < blocks[b].length; i++) {
                strings.of(blocks[b][i].name());
                collect(blocks[b][i].value(), strings);
            }
        }
        for (int i = 0; i < selectors.length; i++) {
            CssSelector s = selectors[i];
            for (int c = 0; c < s.size(); c++) {
                strings.of(s.type(c));
                strings.of(s.id(c));
                for (int j = 0; j < s.classCount(c); j++) {
                    strings.of(s.styleClass(c, j));
                }
                for (int j = 0; j < s.pseudoCount(c); j++) {
                    strings.of(s.pseudo(c, j));
                }
            }
        }
        DataOutputStream out = new DataOutputStream(to);
        out.writeInt(MAGIC);
        out.writeByte(VERSION);
        out.writeInt(strings.list.size());
        for (int i = 0; i < strings.list.size(); i++) {
            out.writeUTF(strings.list.get(i));
        }
        out.writeInt(imports.length);
        for (int i = 0; i < imports.length; i++) {
            out.writeInt(strings.of(imports[i]));
        }
        out.writeInt(fontFamilies.length);
        for (int i = 0; i < fontFamilies.length; i++) {
            out.writeInt(strings.of(fontFamilies[i]));
            out.writeInt(strings.of(fontSources[i]));
        }
        out.writeInt(blocks.length);
        for (int b = 0; b < blocks.length; b++) {
            out.writeInt(blocks[b].length);
            for (int i = 0; i < blocks[b].length; i++) {
                CssDeclaration d = blocks[b][i];
                out.writeInt(strings.of(d.name()));
                out.writeByte(d.kind());
                out.writeBoolean(d.important());
                write(d.value(), out, strings);
            }
        }
        out.writeInt(selectors.length);
        for (int i = 0; i < selectors.length; i++) {
            CssSelector s = selectors[i];
            out.writeInt(selectorBlocks[i]);
            out.writeByte(s.size());
            for (int c = 0; c < s.size(); c++) {
                out.writeByte(c == 0 ? 0 : s.combinator(c - 1));
                out.writeInt(strings.of(s.type(c)));
                out.writeInt(strings.of(s.id(c)));
                out.writeByte(s.classCount(c));
                for (int j = 0; j < s.classCount(c); j++) {
                    out.writeInt(strings.of(s.styleClass(c, j)));
                }
                out.writeByte(s.pseudoCount(c));
                for (int j = 0; j < s.pseudoCount(c); j++) {
                    out.writeInt(strings.of(s.pseudo(c, j)));
                }
            }
        }
        out.flush();
    }

    private static void collect(CssValue v, Strings strings) {
        strings.of(v.text());
        for (int i = 0; i < v.partCount(); i++) {
            if (v.part(i) != null) {
                collect(v.part(i), strings);
            }
        }
    }

    private static void write(CssValue v, DataOutputStream out, Strings strings) throws IOException {
        out.writeByte(v.type());
        out.writeInt(v.flags());
        out.writeByte(v.count());
        boolean units = false;
        for (int i = 0; i < v.count(); i++) {
            out.writeDouble(v.num(i));
            units |= v.unit(i) != CssValue.UNIT_NONE;
        }
        out.writeBoolean(units);
        if (units) {
            for (int i = 0; i < v.count(); i++) {
                out.writeByte(v.unit(i));
            }
        }
        out.writeInt(strings.of(v.text()));
        out.writeByte(v.partCount());
        for (int i = 0; i < v.partCount(); i++) {
            out.writeBoolean(v.part(i) != null);
            if (v.part(i) != null) {
                write(v.part(i), out, strings);
            }
        }
    }

    // --------------------------------------------------------------- reading

    /// Reads a sheet [#write(OutputStream)] wrote. Fails with an
    /// `IOException` for anything else, a file of another format version
    /// included. The stream is not closed.
    public static CssSheetData read(InputStream from) throws IOException {
        DataInputStream in = new DataInputStream(from);
        if (in.readInt() != MAGIC) {
            throw new IOException("Not a compiled style sheet");
        }
        int version = in.readUnsignedByte();
        if (version != VERSION) {
            throw new IOException("Compiled style sheet of format " + version + ", expected " + VERSION
                    + "; rebuild the application");
        }
        String[] strings = new String[count(in) + 1];
        for (int i = 1; i < strings.length; i++) {
            strings[i] = in.readUTF();
        }
        String[] imports = new String[count(in)];
        for (int i = 0; i < imports.length; i++) {
            imports[i] = string(in, strings);
        }
        int fonts = count(in);
        String[] families = new String[fonts];
        String[] sources = new String[fonts];
        for (int i = 0; i < fonts; i++) {
            families[i] = string(in, strings);
            sources[i] = string(in, strings);
        }
        CssDeclaration[][] blocks = new CssDeclaration[count(in)][];
        for (int b = 0; b < blocks.length; b++) {
            blocks[b] = new CssDeclaration[count(in)];
            for (int i = 0; i < blocks[b].length; i++) {
                String name = string(in, strings);
                int kind = in.readUnsignedByte();
                boolean important = in.readBoolean();
                blocks[b][i] = new CssDeclaration(name, kind, value(in, strings, 0), important);
            }
        }
        int rules = count(in);
        CssSelector[] selectors = new CssSelector[rules];
        int[] selectorBlocks = new int[rules];
        for (int i = 0; i < rules; i++) {
            selectorBlocks[i] = in.readInt();
            if (selectorBlocks[i] < 0 || selectorBlocks[i] >= blocks.length) {
                throw new IOException("Corrupt compiled style sheet");
            }
            int size = in.readUnsignedByte();
            String[] types = new String[size];
            String[] ids = new String[size];
            String[][] classes = new String[size][];
            String[][] pseudos = new String[size][];
            byte[] combinators = new byte[Math.max(0, size - 1)];
            for (int c = 0; c < size; c++) {
                byte combinator = in.readByte();
                if (c > 0) {
                    combinators[c - 1] = combinator;
                }
                types[c] = string(in, strings);
                ids[c] = string(in, strings);
                classes[c] = new String[in.readUnsignedByte()];
                for (int j = 0; j < classes[c].length; j++) {
                    classes[c][j] = string(in, strings);
                }
                pseudos[c] = new String[in.readUnsignedByte()];
                for (int j = 0; j < pseudos[c].length; j++) {
                    pseudos[c][j] = string(in, strings);
                }
            }
            selectors[i] = new CssSelector(types, ids, classes, pseudos, combinators);
        }
        return new CssSheetData(imports, families, sources, selectors, selectorBlocks, blocks);
    }

    /// A count, which a truncated or foreign file could make absurd.
    private static int count(DataInputStream in) throws IOException {
        int n = in.readInt();
        if (n < 0 || n > 1000000) {
            throw new IOException("Corrupt compiled style sheet");
        }
        return n;
    }

    private static String string(DataInputStream in, String[] strings) throws IOException {
        int at = in.readInt();
        if (at < 0 || at >= strings.length) {
            throw new IOException("Corrupt compiled style sheet");
        }
        return strings[at];
    }

    private static CssValue value(DataInputStream in, String[] strings, int depth) throws IOException {
        if (depth > 16) {
            throw new IOException("Corrupt compiled style sheet");
        }
        int type = in.readUnsignedByte();
        int flags = in.readInt();
        double[] nums = new double[in.readUnsignedByte()];
        for (int i = 0; i < nums.length; i++) {
            nums[i] = in.readDouble();
        }
        byte[] units = null;
        if (in.readBoolean()) {
            units = new byte[nums.length];
            for (int i = 0; i < units.length; i++) {
                units[i] = in.readByte();
            }
        }
        String text = string(in, strings);
        CssValue[] parts = new CssValue[in.readUnsignedByte()];
        for (int i = 0; i < parts.length; i++) {
            if (in.readBoolean()) {
                parts[i] = value(in, strings, depth + 1);
            }
        }
        return new CssValue(type, flags, nums, units, text, parts);
    }
}
