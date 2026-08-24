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

    /**
     * The ids of methods that cannot be reached by interface dispatch --
     * static and private members. Kept apart from {@link #methodIds} because
     * an explicit {@code invokestatic Interface.staticM()} still has to
     * resolve the id, while a virtual call through an implementor must not
     * pick it up over a same-descriptor default declared on another
     * superinterface. Filtering in the collection step is what enforces
     * "static and private interface methods are not inherited" without
     * hiding the ids from every dispatch.
     */
    private final Hashtable interfaceIneligible = new Hashtable();

    /**
     * Method ids the sidecar marked as static.
     *
     * <p>An instance invocation site (invokevirtual, invokespecial) has to
     * refuse a static method or Method.invoke would ignore the receiver and
     * silently run it as a class-level call. The JVM raises
     * IncompatibleClassChangeError; the linker consults this to do the same.</p>
     */
    private final Hashtable staticMethods = new Hashtable();

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

    /**
     * owner -> "id|descriptor" for one of its static fields.
     *
     * <p>Reading a static goes through a generated accessor, and that accessor
     * runs the class's initializer first. It is the only handle Java has on
     * ParparVM's per-class initializer, which is otherwise reached implicitly
     * by entering a method of the class.</p>
     */
    private final Hashtable oneStaticField = new Hashtable();

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

    /**
     * The ids of interfaces that declare a default method.
     *
     * <p>JLS 12.4.1 initializes an interface when a class implementing it is
     * initialized only when it declares one, so this is what separates the
     * interfaces that have to be initialized with an implementing class from
     * the ones that must not be. The interp-host build writes it as the class
     * row's eighth column, because nothing else in the table records access
     * flags.</p>
     */
    private final Hashtable defaultBearing = new Hashtable();

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
                if (p.length >= 8 && "1".equals(p[7])) {
                    defaultBearing.put(id, Boolean.TRUE);
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
                    Integer methodId = Integer.valueOf(p[1]);
                    methodIds.put(ownerName + "." + name + p[4], methodId);
                    // Columns 5 (isStatic) and 6 (isPrivate), each optional
                    // for compatibility with older sidecars that stopped at
                    // column 4 or 5. A "1" in either marks the id as not
                    // reachable by interface dispatch.
                    boolean staticFlag = p.length >= 6 && "1".equals(p[5]);
                    boolean privateFlag = p.length >= 7 && "1".equals(p[6]);
                    if (staticFlag || privateFlag) {
                        interfaceIneligible.put(methodId, Boolean.TRUE);
                    }
                    if (staticFlag) {
                        staticMethods.put(methodId, Boolean.TRUE);
                    }
                }
            }
        } else if (table.charAt(start) == 'f' && table.startsWith("field\t", start)) {
            String[] p = split(table.substring(start, end));
            if (p.length >= 5) {
                String ownerName = (String)classNames.get(Integer.valueOf(p[1]));
                if (ownerName != null) {
                    // Descriptor in the key. A rebuilt host that changed a
                    // field's type but kept the name would otherwise bind and
                    // the caller would read a primitive slot as an object
                    // reference (or the other way).
                    fieldIds.put(ownerName + "#" + p[3] + "#" + p[4], Integer.valueOf(p[2]));
                }
            }
        } else if (table.charAt(start) == 's' && table.startsWith("sfield\t", start)) {
            String[] p = split(table.substring(start, end));
            if (p.length >= 5) {
                String ownerName = (String)classNames.get(Integer.valueOf(p[1]));
                if (ownerName != null) {
                    staticFieldIds.put(ownerName + "#" + p[3] + "#" + p[4], Integer.valueOf(p[2]));
                    if (oneStaticField.get(ownerName) == null) {
                        oneStaticField.put(ownerName, p[2] + "|" + p[4]);
                    }
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
        // Two passes, in JLS 5.4.3.3 order: class methods first, the whole
        // superclass chain, then interface defaults if none is found.
        // Reversing this -- looking at each class's interfaces before moving
        // up -- can pick a default over a concrete method the superclass
        // inherited, which Java gives precedence to. A subinterface overriding
        // an Object method with a default while the concrete class inherits
        // Object's implementation is the shape that breaks.
        //
        // Bounded by what has been seen rather than by a count in both passes:
        // a superclass chain is finite and a self-referential table is the
        // only way the walk could not end. A number would instead stop partway
        // up a merely deep hierarchy and answer "no such method" for a method
        // the app has.
        String currentOwner = owner;
        Hashtable classSeen = new Hashtable();
        while (currentOwner != null && classSeen.get(currentOwner) == null) {
            classSeen.put(currentOwner, Boolean.TRUE);
            Integer id = (Integer)methodIds.get(currentOwner + "." + name + descriptor);
            if (id != null) {
                return id.intValue();
            }
            currentOwner = superName(currentOwner);
        }
        // No class method: consult interfaces. Candidates from every class in
        // the chain are pooled first, then the maximally specific one is
        // selected across the whole set -- selecting per class would return
        // `I.m` from a receiver's direct `implements I` and never see the
        // superclass's `J extends I` override. A shared visited set across
        // the walk means a diamond (a class and its superclass both reaching
        // the same interface) is walked once.
        java.util.Vector candidateOwners = new java.util.Vector();
        java.util.Vector candidateIds = new java.util.Vector();
        Hashtable interfaceVisited = new Hashtable();
        currentOwner = owner;
        Hashtable classSeenAgain = new Hashtable();
        while (currentOwner != null && classSeenAgain.get(currentOwner) == null) {
            classSeenAgain.put(currentOwner, Boolean.TRUE);
            collectInterfaceCandidates(currentOwner, name, descriptor, interfaceVisited,
                    candidateOwners, candidateIds);
            currentOwner = superName(currentOwner);
        }
        return selectMaximallySpecific(candidateOwners, candidateIds);
    }

    /**
     * The maximally specific candidate from a pooled set, or -1 when empty.
     *
     * <p>JLS 5.4.3.3: keep only candidates no other candidate's declaring
     * interface subtypes. Ties are arbitrary per JLS -- taking the first
     * pooled candidate makes the answer deterministic within a build.</p>
     */
    private int selectMaximallySpecific(java.util.Vector candidateOwners,
                                        java.util.Vector candidateIds) {
        int count = candidateOwners.size();
        if (count == 0) {
            return -1;
        }
        if (count == 1) {
            return ((Integer)candidateIds.elementAt(0)).intValue();
        }
        // Collect all maximally specific candidates, deduplicating by
        // declaring interface (the same interface's default appears once).
        java.util.Vector maximalOwners = new java.util.Vector();
        java.util.Vector maximalIds = new java.util.Vector();
        for (int i = 0; i < count; i++) {
            String candidate = (String)candidateOwners.elementAt(i);
            if (maximalOwners.contains(candidate)) {
                continue;
            }
            boolean dominated = false;
            for (int j = 0; j < count; j++) {
                if (i == j) {
                    continue;
                }
                // If some other candidate's declaring interface is a proper
                // subinterface of this one, this one is not maximally
                // specific -- Java would take the subinterface's method.
                if (isSubinterfaceOf(
                        (String)candidateOwners.elementAt(j), candidate)) {
                    dominated = true;
                    break;
                }
            }
            if (!dominated) {
                maximalOwners.addElement(candidate);
                maximalIds.addElement(candidateIds.elementAt(i));
            }
        }
        if (maximalOwners.size() == 1) {
            return ((Integer)maximalIds.elementAt(0)).intValue();
        }
        // Multiple maximally specific non-dominated candidates is
        // IncompatibleClassChangeError per JVMS 5.4.3.3 -- possible after
        // binary-compatible interface evolution, and silently picking one
        // would run an arbitrary body the JVM refuses. Throwing a
        // RuntimeException here propagates back through the linker to
        // dispatch, where the interpreter's usual host-exception path picks
        // it up.
        if (maximalOwners.size() > 1) {
            StringBuilder message = new StringBuilder();
            for (int i = 0; i < maximalOwners.size(); i++) {
                if (i > 0) {
                    message.append(", ");
                }
                message.append(((String)maximalOwners.elementAt(i)).replace('/', '.'));
            }
            throw new IncompatibleClassChangeError("conflicting default methods: " + message);
        }
        return ((Integer)candidateIds.elementAt(0)).intValue();
    }

    private void collectInterfaceCandidates(String owner, String name, String descriptor,
                                            Hashtable visited,
                                            java.util.Vector candidateOwners,
                                            java.util.Vector candidateIds) {
        if (visited.get(owner) != null) {
            return;
        }
        visited.put(owner, Boolean.TRUE);
        Integer ownerId = (Integer)classIds.get(owner);
        if (ownerId == null) {
            return;
        }
        int[] ifaces = (int[])interfaceIds.get(ownerId);
        if (ifaces == null) {
            return;
        }
        for (int i = 0; i < ifaces.length; i++) {
            String ifaceName = (String)classNames.get(Integer.valueOf(ifaces[i]));
            if (ifaceName == null) {
                continue;
            }
            Integer id = (Integer)methodIds.get(ifaceName + "." + name + descriptor);
            // Only instance, non-private members are inherited through an
            // interface. A static or private declaration keeps its id in the
            // table for explicit invokestatic / invokespecial resolution, but
            // must not compete as a candidate for virtual dispatch through an
            // implementor -- otherwise a static `A.m()` gets picked over a
            // default `B.m()` when the receiver implements both.
            if (id != null && interfaceIneligible.get(id) == null) {
                candidateOwners.addElement(ifaceName);
                candidateIds.addElement(id);
            }
            // Keep walking even when this interface declares the method: a
            // subinterface below may override it, and that override is the
            // one JLS would pick.
            collectInterfaceCandidates(ifaceName, name, descriptor, visited,
                    candidateOwners, candidateIds);
        }
    }

    /// Whether {@code candidate} is a proper subinterface of {@code parent}.
    /// A fresh visited set per call -- reusing the candidate-collection one
    /// would cut this walk short at an already-considered interface.
    private boolean isSubinterfaceOf(String candidate, String parent) {
        if (candidate.equals(parent)) {
            return false;
        }
        Hashtable seen = new Hashtable();
        return walkSuperinterfacesFor(candidate, parent, seen);
    }

    private boolean walkSuperinterfacesFor(String owner, String target, Hashtable seen) {
        if (seen.get(owner) != null) {
            return false;
        }
        seen.put(owner, Boolean.TRUE);
        Integer ownerId = (Integer)classIds.get(owner);
        if (ownerId == null) {
            return false;
        }
        int[] ifaces = (int[])interfaceIds.get(ownerId);
        if (ifaces == null) {
            return false;
        }
        for (int i = 0; i < ifaces.length; i++) {
            String ifaceName = (String)classNames.get(Integer.valueOf(ifaces[i]));
            if (ifaceName == null) {
                continue;
            }
            if (target.equals(ifaceName)) {
                return true;
            }
            if (walkSuperinterfacesFor(ifaceName, target, seen)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The method id declared exactly on this class, or -1.
     *
     * <p>No superclass walk. Constructor resolution has to use this: a
     * subclass whose exact {@code <init>} descriptor is missing (an SDK newer
     * than the installed runtime, a class that declares only some
     * constructors) has to fail loudly rather than silently pick up the
     * parent's {@code <init>} and hand back a base-class instance for
     * {@code new Sub(args)}.</p>
     */
    int declaredMethodId(String owner, String name, String descriptor) {
        Integer id = (Integer)methodIds.get(owner + "." + name + descriptor);
        return id == null ? -1 : id.intValue();
    }

    /** True when the sidecar marked this id as a static method. */
    boolean isStaticMethod(int id) {
        return staticMethods.get(Integer.valueOf(id)) != null;
    }

    /**
     * "id|descriptor" for one static field this class declares, or null.
     *
     * <p>Declared by this class exactly, not inherited: reading an inherited
     * static initializes the class that declares it, which is the wrong one.</p>
     */
    String anyStaticField(String owner) {
        return (String)oneStaticField.get(owner);
    }

    /** The instance field id for an access, searching up the superclass chain. */
    int fieldId(String owner, String name, String descriptor) {
        return lookupField(fieldIds, owner, name, descriptor);
    }

    /**
     * The static field id for an access, searching up the superclass chain.
     *
     * <p>The chain walk matters as much here as for methods: a pushed program
     * may read {@code SomeSubclass.SOME_CONSTANT} where only the superclass
     * declares it.</p>
     */
    int staticFieldId(String owner, String name, String descriptor) {
        return lookupField(staticFieldIds, owner, name, descriptor);
    }

    private int lookupField(Hashtable table, String owner, String name, String descriptor) {
        String currentOwner = owner;
        String suffix = "#" + name + "#" + descriptor;
        // See methodId: the walk ends because the chain does, not at a count.
        Hashtable seen = new Hashtable();
        while (currentOwner != null && seen.get(currentOwner) == null) {
            seen.put(currentOwner, Boolean.TRUE);
            Integer id = (Integer)table.get(currentOwner + suffix);
            if (id != null) {
                return id.intValue();
            }
            // Interfaces as well as superclasses: a constant declared on an
            // interface is read through whatever implements it, and an
            // interface reached through another interface is ordinary Java.
            // A superclass-only walk answers -1 for a field the app has.
            int fromInterface = interfaceFieldId(table, currentOwner, suffix, new Hashtable());
            if (fromInterface >= 0) {
                return fromInterface;
            }
            currentOwner = superName(currentOwner);
        }
        return -1;
    }

    /** Searches a class's interfaces, and theirs, for a field. */
    private int interfaceFieldId(Hashtable table, String owner, String suffix,
                                 Hashtable visited) {
        // As with methods: a visited set terminates without inventing a maximum
        // depth for somebody else's interface hierarchy.
        if (visited.get(owner) != null) {
            return -1;
        }
        visited.put(owner, Boolean.TRUE);
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
            Integer id = (Integer)table.get(ifaceName + suffix);
            if (id != null) {
                return id.intValue();
            }
            int deeper = interfaceFieldId(table, ifaceName, suffix, visited);
            if (deeper >= 0) {
                return deeper;
            }
        }
        return -1;
    }

    /** Whether this interface declares a default method. */
    boolean declaresDefaultMethod(int classId) {
        return defaultBearing.get(Integer.valueOf(classId)) != null;
    }

    /** The ids of the interfaces this class implements directly, or null. */
    int[] interfacesOf(int classId) {
        return (int[])interfaceIds.get(Integer.valueOf(classId));
    }

    String superName(String internalName) {
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
