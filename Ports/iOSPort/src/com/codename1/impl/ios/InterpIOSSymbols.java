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
package com.codename1.impl.ios;

import java.util.Hashtable;

/**
 * The translator's symbol table, as the interpreter needs it.
 *
 * <p>ParparVM has no reflection: {@code struct clazz} carries no name-to-method
 * mapping, so nothing can call a method it did not name at compile time. What an
 * interp-host build does emit is a symbol table -- class, method and field rows
 * carrying JVM names and descriptors against numeric ids -- plus a per-method
 * invoke thunk registered under the same method id. Together they are enough to
 * go from "com/codename1/ui/Form.setTitle(Ljava/lang/String;)V" to a call.</p>
 *
 * <p>Parsing happens here, in Java, rather than in C. The table is a few
 * megabytes of tab-separated text and the work is string splitting and hashing,
 * which is miserable to write in C against a runtime with no string library
 * worth the name. The native layer's only job is to hand over the bytes and to
 * dispatch a thunk once an id is known.</p>
 *
 * @author Shai Almog
 */
class InterpIOSSymbols {
    /** "owner.name+descriptor" -> method id. */
    private final Hashtable methodIds = new Hashtable();

    /** "owner#name" -> instance field id. */
    private final Hashtable fieldIds = new Hashtable();

    /**
     * "owner#name" -> static field id.
     *
     * Kept apart from the instance map because the two are reached by different
     * native calls -- an instance field by offset from a receiver, a static
     * through a generated accessor -- and a class may legally declare a static
     * and an instance field of the same name.
     */
    private final Hashtable staticFieldIds = new Hashtable();

    /** JVM internal class name -> class id. */
    private final Hashtable classIds = new Hashtable();

    /** class id -> JVM internal class name. */
    private final Hashtable classNames = new Hashtable();

    /** class id -> superclass id, as an Integer. */
    private final Hashtable superIds = new Hashtable();

    /**
     * class id -> the ids of the interfaces it implements, as an int[].
     *
     * <p>Needed because a default method lives on an interface and nowhere in
     * the superclass chain. {@code new ArrayList().sort(c)} targets
     * {@code java/util/List.sort}; a walk that knows only about superclasses
     * visits AbstractList and Object, finds nothing, and reports NoSuchMethod
     * for a method the app certainly has.</p>
     */
    private final Hashtable interfaceIds = new Hashtable();

    private static InterpIOSSymbols instance;

    /** Loads the table on first use; it is immutable afterwards. */
    static synchronized InterpIOSSymbols getInstance() {
        if (instance == null) {
            instance = new InterpIOSSymbols();
            instance.load();
        }
        return instance;
    }

    /** True when this build carries a symbol table at all. */
    boolean isAvailable() {
        return !classIds.isEmpty();
    }

    private void load() {
        String table = InterpIOSNative.symbolTable();
        if (table == null || table.length() == 0) {
            return;
        }
        int pos = 0;
        int len = table.length();
        while (pos < len) {
            int nl = table.indexOf('\n', pos);
            if (nl < 0) {
                nl = len;
            }
            parseRow(table, pos, nl);
            pos = nl + 1;
        }
    }

    private void parseRow(String table, int start, int end) {
        if (end - start < 5) {
            return;
        }
        // Rows are tab separated and the first column is the kind. Only the
        // three kinds the interpreter dispatches on are kept; line and var rows
        // are for the debugger and would triple the memory held here.
        if (table.charAt(start) == 'c' && table.startsWith("class\t", start)) {
            String[] p = split(table.substring(start, end));
            if (p.length >= 6) {
                Integer id = Integer.valueOf(p[1]);
                classIds.put(p[5], id);
                classNames.put(id, p[5]);
                // A class row carries no superclass id when it has no super --
                // java.lang.Object, and the interfaces.
                if (p[4].length() > 0) {
                    superIds.put(id, Integer.valueOf(p[4]));
                }
                if (p.length >= 7 && p[6].length() > 0) {
                    interfaceIds.put(id, parseIds(p[6]));
                }
            }
        } else if (table.charAt(start) == 'm' && table.startsWith("method\t", start)) {
            String[] p = split(table.substring(start, end));
            if (p.length >= 5) {
                String ownerName = (String)classNames.get(Integer.valueOf(p[2]));
                if (ownerName != null) {
                    // The translator spells a constructor __INIT__; the bundle
                    // and every call site use the JVM's <init>.
                    String name = "__INIT__".equals(p[3]) ? "<init>" : p[3];
                    methodIds.put(ownerName + "." + name + p[4], Integer.valueOf(p[1]));
                }
            }
        } else if (table.charAt(start) == 'f' && table.startsWith("field\t", start)) {
            String[] p = split(table.substring(start, end));
            if (p.length >= 5) {
                String ownerName = (String)classNames.get(Integer.valueOf(p[1]));
                if (ownerName != null) {
                    fieldIds.put(ownerName + "#" + p[3], Integer.valueOf(p[2]));
                }
            }
        } else if (table.charAt(start) == 's' && table.startsWith("sfield\t", start)) {
            String[] p = split(table.substring(start, end));
            if (p.length >= 5) {
                String ownerName = (String)classNames.get(Integer.valueOf(p[1]));
                if (ownerName != null) {
                    staticFieldIds.put(ownerName + "#" + p[3], Integer.valueOf(p[2]));
                }
            }
        }
    }

    private static String[] split(String row) {
        // A hand-rolled split: String.split takes a regex, and the regex engine
        // is not something to run several hundred thousand times at startup.
        int count = 1;
        for (int i = 0; i < row.length(); i++) {
            if (row.charAt(i) == '\t') {
                count++;
            }
        }
        String[] out = new String[count];
        int idx = 0;
        int from = 0;
        for (int i = 0; i < row.length(); i++) {
            if (row.charAt(i) == '\t') {
                out[idx++] = row.substring(from, i);
                from = i + 1;
            }
        }
        out[idx] = row.substring(from);
        return out;
    }

    /** A comma-separated id list, as an int[]. */
    private static int[] parseIds(String list) {
        int count = 1;
        for (int i = 0; i < list.length(); i++) {
            if (list.charAt(i) == ',') {
                count++;
            }
        }
        int[] out = new int[count];
        int idx = 0;
        int from = 0;
        for (int i = 0; i < list.length(); i++) {
            if (list.charAt(i) == ',') {
                out[idx++] = Integer.parseInt(list.substring(from, i));
                from = i + 1;
            }
        }
        out[idx] = Integer.parseInt(list.substring(from));
        return out;
    }

    /** The JVM internal name for a class id, or null. */
    String classNameFor(int classId) {
        return (String)classNames.get(Integer.valueOf(classId));
    }

    /** The class id for a JVM internal name, or -1. */
    int classId(String internalName) {
        Integer id = (Integer)classIds.get(internalName);
        return id == null ? -1 : id.intValue();
    }

    /**
     * The method id for a call, searching up the superclass chain.
     *
     * <p>The chain walk is what makes an inherited method reachable: a call site
     * naming {@code Interp_Form.show()} has to find {@code Form.show()}, which
     * only the superclass declares.</p>
     */
    int methodId(String owner, String name, String descriptor) {
        String currentOwner = owner;
        int guard = 0;
        while (currentOwner != null && guard++ < 64) {
            Integer id = (Integer)methodIds.get(currentOwner + "." + name + descriptor);
            if (id != null) {
                return id.intValue();
            }
            // Interfaces of this class before moving up, because that is where
            // a default method lives and no superclass declares it.
            int fromInterface = interfaceMethodId(currentOwner, name, descriptor, 0);
            if (fromInterface >= 0) {
                return fromInterface;
            }
            currentOwner = superName(currentOwner);
        }
        return -1;
    }

    /** Searches a class's interfaces, and theirs, for a method. */
    private int interfaceMethodId(String owner, String name, String descriptor, int depth) {
        if (depth > 16) {
            return -1;
        }
        Integer ownerId = (Integer)classIds.get(owner);
        if (ownerId == null) {
            return -1;
        }
        int[] ifaces = (int[])interfaceIds.get(ownerId);
        if (ifaces == null) {
            return -1;
        }
        for (int i = 0; i < ifaces.length; i++) {
            String ifaceName = (String)classNames.get(Integer.valueOf(ifaces[i]));
            if (ifaceName == null) {
                continue;
            }
            Integer id = (Integer)methodIds.get(ifaceName + "." + name + descriptor);
            if (id != null) {
                return id.intValue();
            }
            int deeper = interfaceMethodId(ifaceName, name, descriptor, depth + 1);
            if (deeper >= 0) {
                return deeper;
            }
        }
        return -1;
    }

    /** The instance field id for an access, searching up the superclass chain. */
    int fieldId(String owner, String name) {
        return lookupField(fieldIds, owner, name);
    }

    /**
     * The static field id for an access, searching up the superclass chain.
     *
     * <p>The chain walk matters as much here as for methods: a pushed program
     * may read {@code SomeSubclass.SOME_CONSTANT} where only the superclass
     * declares it.</p>
     */
    int staticFieldId(String owner, String name) {
        return lookupField(staticFieldIds, owner, name);
    }

    private int lookupField(Hashtable table, String owner, String name) {
        String currentOwner = owner;
        int guard = 0;
        while (currentOwner != null && guard++ < 64) {
            Integer id = (Integer)table.get(currentOwner + "#" + name);
            if (id != null) {
                return id.intValue();
            }
            currentOwner = superName(currentOwner);
        }
        return -1;
    }

    private String superName(String internalName) {
        Integer id = (Integer)classIds.get(internalName);
        if (id == null) {
            return null;
        }
        Integer superId = (Integer)superIds.get(id);
        if (superId == null) {
            return null;
        }
        return (String)classNames.get(superId);
    }
}
