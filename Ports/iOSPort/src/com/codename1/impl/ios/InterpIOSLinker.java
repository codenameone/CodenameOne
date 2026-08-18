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

import com.codename1.impl.interp.InterpLinker;
import com.codename1.impl.interp.InterpValuesAccess;

/**
 * Binds interpreted code to the app on iOS.
 *
 * <p>The Android linker is {@code java.lang.reflect}; there is no equivalent
 * here. ParparVM's {@code Method} and {@code Constructor} are empty stubs and
 * {@code struct clazz} has no name-to-method table, so "call the method named
 * setTitle" is not a question the runtime can answer.</p>
 *
 * <p>What an interp-host build does provide is a per-method invoke thunk keyed
 * by a numeric id, plus a symbol table mapping JVM names and descriptors to
 * those ids. This class is the join: {@link InterpIOSSymbols} turns a name into
 * an id, and {@link InterpIOSNative} dispatches the thunk. Ids are memoised per
 * call site, because the symbol-table lookup walks a superclass chain and a
 * pushed program calls the same few framework methods in a loop.</p>
 *
 * <p>Values cross the native boundary unboxed -- primitives as raw bits in a
 * {@code long[]}, references in an {@code Object[]}, selected per argument by a
 * kind code. The C side cannot box or unbox, since doing so would need the very
 * reflection this exists to replace.</p>
 *
 * @author Shai Almog
 */
public class InterpIOSLinker implements InterpLinker {
    // Kind codes, matching com.codename1.impl.interp.InterpOpcodes.
    private static final int K_VOID = 0;
    private static final int K_INT = 1;
    private static final int K_LONG = 2;
    private static final int K_FLOAT = 3;
    private static final int K_DOUBLE = 4;
    private static final int K_OBJECT = 5;
    private static final int K_BOOLEAN = 6;
    private static final int K_BYTE = 7;
    private static final int K_CHAR = 8;
    private static final int K_SHORT = 9;

    private final InterpIOSSymbols symbols = InterpIOSSymbols.getInstance();
    private final java.util.Hashtable methodIdCache = new java.util.Hashtable();
    private final java.util.Hashtable classIdCache = new java.util.Hashtable();

    /** Whether this build can run pushed code at all. */
    public static boolean isAvailable() {
        return InterpIOSNative.isInterpHostBuild()
                && InterpIOSSymbols.getInstance().isAvailable();
    }

    public void initializeClass(String internalName) {
        // A real entry point rather than a side effect: the interp-host build
        // registers every class's __STATIC_INITIALIZER_ under its class id, and
        // that function is idempotent. Reading a static field would also run it
        // -- the generated accessor calls it first -- but a class can have an
        // observable static block and declare no static field at all, and then
        // there is nothing to read.
        //
        // A failure propagates. Java requires that a superclass initializer
        // throwing aborts the subclass's initialization, and swallowing it here
        // would let the subclass complete on top of a parent that never ran.
        // The whole chain, top down. A generated __STATIC_INITIALIZER_ runs its
        // own class's <clinit> and does not reach its parent's -- compiled code
        // never needs it to, because entering the parent's constructor or
        // reading its statics does that -- so initializing one class here would
        // leave a grandparent's static block unrun.
        // The whole chain, however long it is. A fixed array truncated it, and
        // the classes it dropped were the ones nearest java/lang/Object -- the
        // ones most likely to carry a static block something else depends on.
        java.util.Vector chain = new java.util.Vector();
        String at = internalName;
        while (at != null && !chain.contains(at)) {
            chain.addElement(at);
            at = symbols.superName(at);
        }
        for (int i = chain.size() - 1; i >= 0; i--) {
            int id = symbols.classId((String)chain.elementAt(i));
            if (id >= 0) {
                InterpIOSNative.initializeClassById(id);
            }
        }
    }

    public void initializeDefaultBearingInterfaces(String internalName) {
        // JLS 12.4.1: initializing a class initializes the superinterfaces that
        // declare a default method, and only those. The class row's eighth
        // column says which ones do, so this can honour the rule instead of
        // leaving each interface to initialize on its own first use -- which is
        // entry into one of its methods, and may never happen at all.
        int id = symbols.classId(internalName);
        if (id < 0) {
            return;
        }
        initializeDefaultBearing(id, new java.util.Hashtable(), true);
    }

    /// Superinterfaces first, then the interface itself when it declares a
    /// default method. Bounded by what has been seen: an interface hierarchy is
    /// a DAG whose diamonds would otherwise be walked twice, and a depth cap
    /// would answer wrongly on a hierarchy that is merely deep.
    private void initializeDefaultBearing(int classId, java.util.Hashtable visited,
                                          boolean root) {
        Integer key = Integer.valueOf(classId);
        if (visited.get(key) != null) {
            return;
        }
        visited.put(key, Boolean.TRUE);
        int[] ifaces = symbols.interfacesOf(classId);
        if (ifaces != null) {
            for (int i = 0; i < ifaces.length; i++) {
                initializeDefaultBearing(ifaces[i], visited, false);
            }
        }
        // The class this walk started from is initialized by initializeClass;
        // only the interfaces above it are this method's business.
        if (!root && symbols.declaresDefaultMethod(classId)) {
            InterpIOSNative.initializeClassById(classId);
        }
    }

    public Object findClass(String internalName) {
        // Array descriptors resolve too: the interp-host build emits a class
        // row per rank keyed by `[Ljava/lang/String;`, so `String[].class` is a
        // lookup like any other rather than a NoClassDefFoundError.
        Integer cached = (Integer)classIdCache.get(internalName);
        if (cached != null) {
            return cached.intValue() < 0 ? null : cached;
        }
        int id = symbols.classId(internalName);
        classIdCache.put(internalName, Integer.valueOf(id));
        return id < 0 ? null : Integer.valueOf(id);
    }

    /**
     * A host class is represented to the interpreter as its class id, boxed.
     * There is no {@code java.lang.Class} to hand back that could answer
     * anything useful -- ParparVM's Class has no member enumeration -- so the id
     * is the only handle with meaning.
     */
    private int idOf(Object hostClass) {
        return hostClass instanceof Integer ? ((Integer)hostClass).intValue() : -1;
    }

    private int methodId(String owner, String name, String descriptor) {
        String key = owner + '.' + name + descriptor;
        Integer cached = (Integer)methodIdCache.get(key);
        if (cached != null) {
            return cached.intValue();
        }
        int id = symbols.methodId(owner, name, descriptor);
        methodIdCache.put(key, Integer.valueOf(id));
        return id;
    }

    public Object construct(Object hostClass, String descriptor, Object[] args) throws Throwable {
        int classId = idOf(hostClass);
        String owner = classId < 0 ? null : symbols.classNameFor(classId);
        if (owner == null) {
            throw new NoClassDefFoundError("unknown host class");
        }
        // A constructor thunk allocates its own receiver and returns it, which
        // is why this passes no target and expects an object back.
        return invoke(owner, "<init>", descriptor, null, args, K_OBJECT);
    }

    public Object invokeVirtual(Object target, String owner, String name, String descriptor,
                                Object[] args) throws Throwable {
        if (target == null) {
            throw new NullPointerException(owner + "." + name);
        }
        // Resolution starts at the receiver's own class, not at the type the
        // call site was compiled against. `List.add(x)` names java.util.List,
        // and the first `add` found from there is AbstractList's, whose body
        // throws UnsupportedOperationException -- so every collection call from
        // interpreted code failed while Form.show(), whose call site names the
        // receiver's own class, worked. There is no vtable to consult from
        // here, so the walk up from the real class is the dispatch.
        return invoke(receiverClass(target, owner), name, descriptor, target, args,
                kindOf(InterpValuesAccess.returnType(descriptor)));
    }

    /// The receiver's actual class name, falling back to the declared owner.
    private String receiverClass(Object target, String owner) {
        int classId = InterpIOSNative.classIdOf(target);
        if (classId < 0) {
            return owner;
        }
        String name = symbols.classNameFor(classId);
        return name == null ? owner : name;
    }

    public Object invokeSpecial(Object target, String owner, String name, String descriptor,
                                Object[] args) throws Throwable {
        return invoke(owner, name, descriptor, target, args,
                kindOf(InterpValuesAccess.returnType(descriptor)));
    }

    public Object invokeStatic(String owner, String name, String descriptor, Object[] args)
            throws Throwable {
        return invoke(owner, name, descriptor, null, args,
                kindOf(InterpValuesAccess.returnType(descriptor)));
    }

    private Object invoke(String owner, String name, String descriptor, Object target,
                          Object[] args, int returnKind) throws Throwable {
        int id = methodId(owner, name, descriptor);
        if (id < 0) {
            throw new NoSuchMethodError(owner + "." + name + descriptor
                    + " is not present in the installed app");
        }
        String[] argTypes = InterpValuesAccess.argumentTypes(descriptor);
        int count = argTypes.length;
        long[] prims = new long[count == 0 ? 1 : count];
        Object[] objs = new Object[count == 0 ? 1 : count];
        int[] kinds = new int[count == 0 ? 1 : count];
        for (int i = 0; i < count; i++) {
            int k = kindOf(argTypes[i]);
            kinds[i] = k;
            Object a = args == null || i >= args.length ? null : args[i];
            if (k == K_OBJECT) {
                objs[i] = a;
            } else {
                prims[i] = rawOf(k, a);
            }
        }
        long[] out = new long[1];
        Object ref = InterpIOSNative.invokeById(id, target, prims, objs, kinds, count,
                returnKind, out);
        if (returnKind == K_OBJECT) {
            return ref;
        }
        return boxed(returnKind, out[0]);
    }

    /**
     * Reads a host static.
     *
     * <p>A static has no receiver to hang an offset off, so the instance-field
     * table cannot cover it. An interp-host build emits one accessor per static
     * instead, registered under the same id space, and this dispatches through
     * it. Reading that way also runs the declaring class's static initializer,
     * which is what a compiled GETSTATIC does.</p>
     */
    public Object getStatic(String owner, String name, String descriptor) throws Throwable {
        int fieldId = symbols.staticFieldId(owner, name);
        if (fieldId < 0) {
            throw new NoSuchFieldError(owner + "." + name
                    + " is not present in the installed app");
        }
        int kind = kindOf(descriptor);
        long[] out = new long[1];
        Object ref = InterpIOSNative.getStaticById(fieldId, kind, out);
        return kind == K_OBJECT ? ref : boxed(kind, out[0]);
    }

    public void setStatic(String owner, String name, String descriptor, Object value)
            throws Throwable {
        int fieldId = symbols.staticFieldId(owner, name);
        if (fieldId < 0) {
            throw new NoSuchFieldError(owner + "." + name
                    + " is not present in the installed app");
        }
        int kind = kindOf(descriptor);
        InterpIOSNative.setStaticById(fieldId, kind,
                kind == K_OBJECT ? 0 : rawOf(kind, value),
                kind == K_OBJECT ? value : null);
    }

    public Object getField(Object target, String owner, String name, String descriptor)
            throws Throwable {
        if (target == null) {
            throw new NullPointerException(owner + "." + name);
        }
        int fieldId = symbols.fieldId(owner, name);
        if (fieldId < 0) {
            throw new NoSuchFieldError(owner + "." + name);
        }
        int kind = kindOf(descriptor);
        long[] out = new long[1];
        Object ref = InterpIOSNative.getFieldById(fieldId, target, kind, out);
        return kind == K_OBJECT ? ref : boxed(kind, out[0]);
    }

    public void setField(Object target, String owner, String name, String descriptor, Object value)
            throws Throwable {
        if (target == null) {
            throw new NullPointerException(owner + "." + name);
        }
        int fieldId = symbols.fieldId(owner, name);
        if (fieldId < 0) {
            throw new NoSuchFieldError(owner + "." + name);
        }
        int kind = kindOf(descriptor);
        InterpIOSNative.setFieldById(fieldId, target, kind,
                kind == K_OBJECT ? 0 : rawOf(kind, value),
                kind == K_OBJECT ? value : null);
    }

    public boolean hasMethod(String owner, String name, String descriptor) {
        return symbols.methodId(owner, name, descriptor) >= 0;
    }

    public boolean isInstance(Object hostClass, Object value) {
        int id = idOf(hostClass);
        return id >= 0 && InterpIOSNative.isInstanceOfId(id, value);
    }

    public Object cloneArray(Object source) {
        // ParparVM arrays carry their element type in the object itself, which
        // Java cannot read but a native can. Allocating from the source's own
        // clazz is what keeps `String[] copy = original.clone()` a String[]:
        // an Object[] of the right length passes nothing that checks the type,
        // so the copy failed the first cast or host call it reached.
        if (!(source instanceof Object[])) {
            // Primitive arrays are copied by the caller, which knows their type
            // from Java.
            return null;
        }
        return InterpIOSNative.newArrayLike(source, ((Object[]) source).length);
    }

    public Object newArray(String componentDescriptor, int length) throws Throwable {
        int kind = kindOf(componentDescriptor);
        switch (kind) {
            case K_BOOLEAN: return new boolean[length];
            case K_BYTE:    return new byte[length];
            case K_CHAR:    return new char[length];
            case K_SHORT:   return new short[length];
            case K_INT:     return new int[length];
            case K_LONG:    return new long[length];
            case K_FLOAT:   return new float[length];
            case K_DOUBLE:  return new double[length];
            default:        return InterpIOSNative.newObjectArray(
                    arrayClassId(componentDescriptor), length);
        }
    }

    /**
     * The class id of an array with this component, or -1.
     *
     * <p>The interp-host build publishes a class row per array rank keyed by
     * the descriptor, so this is a lookup. It answers -1 for a component the
     * app does not have -- an array of a bundle-only class -- and the caller
     * then gets the untyped array the interpreter uses for its own.</p>
     */
    private int arrayClassId(String componentDescriptor) {
        return symbols.classId("[" + componentDescriptor);
    }

    public Object newMultiArray(String arrayDescriptor, int[] dimensions) throws Throwable {
        if (dimensions.length == 0) {
            return null;
        }
        if (dimensions.length == 1) {
            return newArray(arrayDescriptor.substring(1), dimensions[0]);
        }
        // The outer array's own type too, for the same reason: `(String[][]) v`
        // is a checkcast against the outer array class.
        Object outerArray = InterpIOSNative.newObjectArray(
                symbols.classId(arrayDescriptor), dimensions[0]);
        Object[] outer = (Object[]) outerArray;
        int[] rest = new int[dimensions.length - 1];
        System.arraycopy(dimensions, 1, rest, 0, rest.length);
        for (int i = 0; i < dimensions[0]; i++) {
            outer[i] = newMultiArray(arrayDescriptor.substring(1), rest);
        }
        return outer;
    }

    public Object classObject(Object hostClass) {
        int id = idOf(hostClass);
        return id < 0 ? null : InterpIOSNative.classObjectById(id);
    }

    // ------------------------------------------------------------ conversions

    private static int kindOf(String descriptor) {
        if (descriptor == null || descriptor.length() == 0) {
            return K_VOID;
        }
        switch (descriptor.charAt(0)) {
            case 'V': return K_VOID;
            case 'Z': return K_BOOLEAN;
            case 'B': return K_BYTE;
            case 'C': return K_CHAR;
            case 'S': return K_SHORT;
            case 'I': return K_INT;
            case 'J': return K_LONG;
            case 'F': return K_FLOAT;
            case 'D': return K_DOUBLE;
            default:  return K_OBJECT;
        }
    }

    private static long rawOf(int kind, Object value) {
        if (value == null) {
            return 0;
        }
        switch (kind) {
            case K_BOOLEAN: return ((Boolean)value).booleanValue() ? 1 : 0;
            case K_BYTE:    return ((Byte)value).byteValue();
            case K_CHAR:    return ((Character)value).charValue();
            case K_SHORT:   return ((Short)value).shortValue();
            case K_INT:     return ((Integer)value).intValue();
            case K_LONG:    return ((Long)value).longValue();
            case K_FLOAT:   return Float.floatToIntBits(((Float)value).floatValue()) & 0xffffffffL;
            case K_DOUBLE:  return Double.doubleToLongBits(((Double)value).doubleValue());
            default:        return 0;
        }
    }

    private static Object boxed(int kind, long raw) {
        switch (kind) {
            case K_BOOLEAN: return raw != 0 ? Boolean.TRUE : Boolean.FALSE;
            case K_BYTE:    return Byte.valueOf((byte)raw);
            case K_CHAR:    return Character.valueOf((char)raw);
            case K_SHORT:   return Short.valueOf((short)raw);
            case K_INT:     return Integer.valueOf((int)raw);
            case K_LONG:    return Long.valueOf(raw);
            case K_FLOAT:   return Float.valueOf(Float.intBitsToFloat((int)raw));
            case K_DOUBLE:  return Double.valueOf(Double.longBitsToDouble(raw));
            default:        return null;
        }
    }
}
