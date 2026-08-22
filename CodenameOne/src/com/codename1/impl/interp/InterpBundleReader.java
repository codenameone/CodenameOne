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
package com.codename1.impl.interp;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

/// Reads a `.cn1ip` bundle.
///
/// Deliberately dull: every structure is a length followed by that many fixed
/// records, so the reader is a loop over `readInt` with no lookahead and no
/// allocation beyond the arrays it is filling. It has to run on ParparVM, where
/// `java.io` is a subset -- `DataInputStream` over a byte stream is available
/// and is all this uses.
///
/// @author Shai Almog
public final class InterpBundleReader {
    private InterpBundleReader() {
    }

    /// Reads a bundle and links its interpreted classes to each other. Host
    /// symbols stay unresolved until first use.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the stream is truncated, not a bundle, or a version
    ///   this runtime does not know
    public static InterpBundle read(InputStream rawIn) throws IOException {
        DataInputStream in = new DataInputStream(rawIn);
        InterpBundle b = new InterpBundle();

        int magic = in.readInt();
        if (magic != InterpBundle.MAGIC) {
            throw new IOException("not a Codename One interpreter bundle");
        }
        int version = in.readInt();
        if (version != InterpBundle.VERSION) {
            throw new IOException("bundle format version " + version
                    + " but this runtime speaks " + InterpBundle.VERSION
                    + " -- rebuild the bundle against the installed app");
        }
        String main = in.readUTF();
        b.mainClass = main.length() == 0 ? null : main;

        int stringCount = in.readInt();
        b.strings = new String[stringCount];
        for (int i = 0; i < stringCount; i++) {
            b.strings[i] = in.readUTF();
        }

        int externCount = in.readInt();
        b.externKind = new int[externCount];
        b.externOwner = new int[externCount];
        b.externName = new int[externCount];
        b.externDesc = new int[externCount];
        b.externResolved = new Object[externCount];
        b.externResolveAttempted = new boolean[externCount];
        for (int i = 0; i < externCount; i++) {
            b.externKind[i] = in.readInt();
            b.externOwner[i] = in.readInt();
            b.externName[i] = in.readInt();
            b.externDesc[i] = in.readInt();
        }

        int classCount = in.readInt();
        b.classes = new InterpClass[classCount];
        // Two passes: create every class first so a forward reference between
        // interpreted classes resolves without ordering constraints.
        String[][] pendingSupers = new String[classCount][];
        for (int i = 0; i < classCount; i++) {
            pendingSupers[i] = readClass(in, b, i);
        }
        for (int i = 0; i < classCount; i++) {
            InterpClass c = b.classes[i];
            String superName = pendingSupers[i][0];
            if (superName != null) {
                c.superInterp = b.findClass(superName);
                if (c.superInterp == null) {
                    throw new IOException("bundle names interpreted superclass "
                            + superName + " but does not contain it");
                }
            }
            int ifaceCount = pendingSupers[i].length - 1;
            c.interpInterfaces = new InterpClass[ifaceCount];
            for (int j = 0; j < ifaceCount; j++) {
                c.interpInterfaces[j] = b.findClass(pendingSupers[i][j + 1]);
                if (c.interpInterfaces[j] == null) {
                    throw new IOException("bundle names interpreted interface "
                            + pendingSupers[i][j + 1] + " but does not contain it");
                }
            }
        }
        // Field bases depend on the superclass chain, so they can only be
        // computed once every link above exists.
        for (int i = 0; i < classCount; i++) {
            assignFieldBase(b.classes[i]);
        }

        int sourceCount = in.readInt();
        for (int i = 0; i < sourceCount; i++) {
            String fileName = in.readUTF();
            int len = in.readInt();
            byte[] utf8 = new byte[len];
            in.readFully(utf8);
            b.sources.put(fileName, new String(utf8, "UTF-8"));
        }

        // The version is an exact match by the check above, so the section is
        // always present -- a bundle from an older desktop was already refused.
        int resourceCount = in.readInt();
        for (int i = 0; i < resourceCount; i++) {
            String path = in.readUTF();
            int len = in.readInt();
            byte[] data = new byte[len];
            in.readFully(data);
            b.resources.put(path, data);
        }

        requireSourcesFor(b);
        return b;
    }

    private static void assignFieldBase(InterpClass c) {
        if (c.superInterp == null) {
            c.fieldBase = 0;
            return;
        }
        assignFieldBase(c.superInterp);
        c.fieldBase = c.superInterp.fieldBase + c.superInterp.fieldNames.length;
    }

    /// The runtime will not execute code whose source the user cannot read.
    ///
    /// This is the condition Apple attaches to running downloaded code at all
    /// (App Store Review Guideline 2.5.2: an app that downloads code for
    /// teaching or testing "must make the source code provided by the app
    /// completely viewable and editable by the user"). Enforcing it here rather
    /// than trusting the tool chain means a bundle built by any other route
    /// still cannot bypass it.
    private static void requireSourcesFor(InterpBundle b) throws IOException {
        for (int i = 0; i < b.classes.length; i++) {
            InterpClass c = b.classes[i];
            if (c.sourceFile == null || c.sourceFile.length() == 0) {
                throw new IOException("class " + c.name
                        + " was compiled without source information; the device runtime "
                        + "only executes code whose source it can show");
            }
            // Keyed by package, not by file name: two classes named Util in
            // different packages both declare "Util.java", and a bare-name map
            // keeps only one of them.
            if (b.sources.get(sourceKey(c)) == null) {
                throw new IOException("bundle is missing the source file "
                        + sourceKey(c) + " for class " + c.name
                        + "; the device runtime only executes code whose source it can show");
            }
        }
    }

    /// The bundle key for a class's source: its package, then the file name the
    /// SourceFile attribute records. Mirrors InterpBundleWriter.sourceKey.
    static String sourceKey(InterpClass c) {
        int slash = c.name.lastIndexOf('/');
        if (slash < 0) {
            return c.sourceFile;
        }
        return c.name.substring(0, slash + 1) + c.sourceFile;
    }

    private static String[] readClass(DataInputStream in, InterpBundle b, int index)
            throws IOException {
        String name = b.strings[in.readInt()];
        InterpClass c = new InterpClass(name);
        c.accessFlags = in.readInt();
        String src = in.readUTF();
        c.sourceFile = src.length() == 0 ? null : src;
        // What javac's InnerClasses attribute called this class. The flag says
        // whether there was an entry at all: without one the class is top-level
        // and its simple name is the last segment of its binary name, `$` and
        // all; with one but no name it is anonymous, and its simple name is
        // genuinely empty.
        boolean recorded = in.readBoolean();
        String simple = in.readUTF();
        c.simpleName = recorded ? simple : null;

        boolean superInterpreted = in.readBoolean();
        int superRef = in.readInt();
        String superInterpName = null;
        if (superInterpreted) {
            superInterpName = b.strings[superRef];
        } else {
            c.superExtern = superRef;
        }

        int interpIfaceCount = in.readInt();
        String[] interpIfaceNames = new String[interpIfaceCount];
        for (int i = 0; i < interpIfaceCount; i++) {
            interpIfaceNames[i] = b.strings[in.readInt()];
        }
        int hostIfaceCount = in.readInt();
        c.hostInterfaces = new int[hostIfaceCount];
        for (int i = 0; i < hostIfaceCount; i++) {
            c.hostInterfaces[i] = in.readInt();
        }

        int instanceFieldCount = in.readInt();
        c.fieldNames = new String[instanceFieldCount];
        c.fieldDescs = new String[instanceFieldCount];
        c.fieldAccess = new int[instanceFieldCount];
        for (int i = 0; i < instanceFieldCount; i++) {
            c.fieldNames[i] = b.strings[in.readInt()];
            c.fieldDescs[i] = b.strings[in.readInt()];
            // Access flags: only `ACC_VOLATILE` matters at run time -- the
            // interpreter wraps a volatile get/put in a `synchronized (io)`
            // so happens-before ordering matches Java's contract.
            c.fieldAccess[i] = in.readInt();
        }
        int staticFieldCount = in.readInt();
        for (int i = 0; i < staticFieldCount; i++) {
            String fname = b.strings[in.readInt()];
            String fdesc = b.strings[in.readInt()];
            int faccess = in.readInt();
            c.setStaticValue(fname, InterpValues.defaultValue(fdesc));
            if ((faccess & InterpClass.ACC_VOLATILE) != 0) {
                c.markStaticVolatile(fname);
            }
        }

        int methodCount = in.readInt();
        c.methods = new InterpMethod[methodCount];
        for (int i = 0; i < methodCount; i++) {
            c.methods[i] = readMethod(in, b, c);
        }

        b.classes[index] = c;
        b.classesByName.put(name, c);

        String[] result = new String[interpIfaceCount + 1];
        result[0] = superInterpName;
        for (int i = 0; i < interpIfaceCount; i++) {
            result[i + 1] = interpIfaceNames[i];
        }
        return result;
    }

    private static InterpMethod readMethod(DataInputStream in, InterpBundle b, InterpClass owner)
            throws IOException {
        InterpMethod m = new InterpMethod(owner);
        m.name = b.strings[in.readInt()];
        m.desc = b.strings[in.readInt()];
        m.accessFlags = in.readInt();
        m.maxStack = in.readInt();
        m.maxLocals = in.readInt();

        int offsetCount = in.readInt();
        m.instructionOffsets = new int[offsetCount];
        for (int i = 0; i < offsetCount; i++) {
            m.instructionOffsets[i] = in.readInt();
        }
        int codeLen = in.readInt();
        m.code = new int[codeLen];
        for (int i = 0; i < codeLen; i++) {
            m.code[i] = in.readInt();
        }
        int excCount = in.readInt();
        m.exceptionTable = new int[excCount * 4];
        for (int i = 0; i < m.exceptionTable.length; i++) {
            m.exceptionTable[i] = in.readInt();
        }
        int lineCount = in.readInt();
        m.lineTable = new int[lineCount * 2];
        for (int i = 0; i < m.lineTable.length; i++) {
            m.lineTable[i] = in.readInt();
        }

        m.argKinds = InterpValues.argumentKinds(m.desc);
        m.returnKind = InterpValues.returnKind(m.desc);
        return m;
    }


    /// Reads every {@code .cn1ip} entry name in a bundle, for diagnostics.
    static Vector classNames(InterpBundle b) {
        Vector v = new Vector();
        for (int i = 0; i < b.classes.length; i++) {
            v.addElement(b.classes[i].name);
        }
        return v;
    }
}
