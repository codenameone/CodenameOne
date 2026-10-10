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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The logical layer of one assembly: its types, their members, and the
/// answer to "what does this token mean".
///
/// An assembly is read once, eagerly, apart from method bodies. References to
/// types in other assemblies are resolved by name through the [Universe] the
/// assembly was loaded into, which is the only way to learn whether a
/// referenced type is a class, a struct or an interface: a reference row
/// records the name and nothing else.
public final class CilAssembly {

    public static final int TYPE_INTERFACE = 0x20;
    public static final int TYPE_ABSTRACT = 0x80;
    public static final int TYPE_SEALED = 0x100;

    public static final int METHOD_ACCESS_MASK = 0x7;
    public static final int METHOD_PRIVATE = 0x1;
    public static final int METHOD_STATIC = 0x10;
    public static final int METHOD_FINAL = 0x20;
    public static final int METHOD_VIRTUAL = 0x40;
    public static final int METHOD_NEW_SLOT = 0x100;
    public static final int METHOD_ABSTRACT = 0x400;

    public static final int FIELD_STATIC = 0x10;
    public static final int FIELD_LITERAL = 0x40;

    public static final int SEMANTICS_ADD_ON = 0x8;
    public static final int SEMANTICS_REMOVE_ON = 0x10;

    /// A type this assembly defines.
    public static final class TypeDef {
        public final CilAssembly assembly;
        public final int row;
        public final String namespace;
        public final String name;
        public final int flags;
        public TypeDef enclosing;
        /// Null only for `System.Object` and for interfaces.
        public CilType baseType;
        public final List<CilType> interfaces = new ArrayList<CilType>();
        public final List<FieldDef> fields = new ArrayList<FieldDef>();
        public final List<MethodDef> methods = new ArrayList<MethodDef>();
        public final List<MethodImpl> methodImpls = new ArrayList<MethodImpl>();
        public int genericParamCount;
        private String fullName;

        TypeDef(CilAssembly assembly, int row, String namespace, String name, int flags) {
            this.assembly = assembly;
            this.row = row;
            this.namespace = namespace;
            this.name = name;
            this.flags = flags;
        }

        public String fullName() {
            if (fullName == null) {
                if (enclosing != null) {
                    fullName = enclosing.fullName() + "/" + name;
                } else {
                    fullName = namespace.length() == 0 ? name : namespace + "." + name;
                }
            }
            return fullName;
        }

        public boolean isInterface() {
            return (flags & TYPE_INTERFACE) != 0;
        }

        public boolean isAbstract() {
            return (flags & TYPE_ABSTRACT) != 0;
        }

        private String baseName() {
            return baseType == null ? null : baseType.typeName();
        }

        public boolean isEnum() {
            return "System.Enum".equals(baseName());
        }

        /// A struct or an enum. `System.Enum` itself derives from
        /// `System.ValueType` and is a class.
        public boolean isValueType() {
            String base = baseName();
            if ("System.Enum".equals(base)) {
                return true;
            }
            return "System.ValueType".equals(base) && !"System.Enum".equals(fullName());
        }

        public boolean isDelegate() {
            return "System.MulticastDelegate".equals(baseName());
        }

        /// The integer type an enum is stored as: the type of its one
        /// instance field.
        public CilType enumUnderlyingType() {
            for (FieldDef f : fields) {
                if (!f.isStatic()) {
                    return f.type;
                }
            }
            throw new CilFormatException(assembly.source() + ": enum " + fullName() + " has no value field");
        }

        public CilType asType() {
            CilType builtin = CilType.builtin(fullName());
            return builtin != null ? builtin : CilType.named(fullName(), isValueType());
        }

        public FieldDef field(String fieldName) {
            for (FieldDef f : fields) {
                if (f.name.equals(fieldName)) {
                    return f;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return fullName();
        }
    }

    public static final class FieldDef {
        public final TypeDef owner;
        public final int row;
        public final String name;
        public final int flags;
        public final CilType type;
        /// Where the initial data of a mapped field lives, or 0.
        public int rva;
        /// The value of a literal field of an integer type -- a member of an
        /// enum, a `const int` -- or null.
        public Long constant;

        FieldDef(TypeDef owner, int row, String name, int flags, CilType type) {
            this.owner = owner;
            this.row = row;
            this.name = name;
            this.flags = flags;
            this.type = type;
        }

        public boolean isStatic() {
            return (flags & FIELD_STATIC) != 0;
        }

        public boolean isLiteral() {
            return (flags & FIELD_LITERAL) != 0;
        }

        public int token() {
            return (MetadataReader.FIELD << 24) | row;
        }
    }

    public static final class MethodDef {
        public final TypeDef owner;
        public final int row;
        public final String name;
        public final int flags;
        public final int rva;
        public final MethodSig sig;
        public int genericParamCount;
        /// `SEMANTICS_*` bits when this is an event accessor, else 0.
        public int semantics;
        /// Name of the event or property this method is an accessor of.
        public String associationName;
        private MethodBody body;

        MethodDef(TypeDef owner, int row, String name, int flags, int rva, MethodSig sig) {
            this.owner = owner;
            this.row = row;
            this.name = name;
            this.flags = flags;
            this.rva = rva;
            this.sig = sig;
        }

        public boolean isStatic() {
            return (flags & METHOD_STATIC) != 0;
        }

        public boolean isVirtual() {
            return (flags & METHOD_VIRTUAL) != 0;
        }

        public boolean isAbstract() {
            return (flags & METHOD_ABSTRACT) != 0;
        }

        public boolean isNewSlot() {
            return (flags & METHOD_NEW_SLOT) != 0;
        }

        public boolean isPrivate() {
            return (flags & METHOD_ACCESS_MASK) == METHOD_PRIVATE;
        }

        public boolean hasBody() {
            return rva != 0;
        }

        public MethodBody body() {
            if (body == null) {
                body = new MethodBody(owner.assembly, rva);
            }
            return body;
        }

        public int token() {
            return (MetadataReader.METHOD_DEF << 24) | row;
        }

        @Override
        public String toString() {
            return owner.fullName() + "::" + name + sig;
        }
    }

    /// An explicit override: `body` is this type's implementation of
    /// `declaration`, whatever the two are called.
    public static final class MethodImpl {
        public final MethodRef body;
        public final MethodRef declaration;

        MethodImpl(MethodRef body, MethodRef declaration) {
            this.body = body;
            this.declaration = declaration;
        }
    }

    public static final class MethodSig {
        public final boolean hasThis;
        public final int genericParamCount;
        public final CilType returnType;
        public final CilType[] params;

        MethodSig(boolean hasThis, int genericParamCount, CilType returnType, CilType[] params) {
            this.hasThis = hasThis;
            this.genericParamCount = genericParamCount;
            this.returnType = returnType;
            this.params = params;
        }

        public boolean sameShape(MethodSig other) {
            if (hasThis != other.hasThis || genericParamCount != other.genericParamCount
                    || params.length != other.params.length || !returnType.equals(other.returnType)) {
                return false;
            }
            for (int i = 0; i < params.length; i++) {
                if (!params[i].equals(other.params[i])) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("(");
            for (int i = 0; i < params.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(params[i]);
            }
            return sb.append(") : ").append(returnType).toString();
        }
    }

    /// A method as an instruction names it: where it is declared, what it is
    /// called and its signature exactly as declared, type variables included.
    /// The instantiation, when there is one, is carried beside the signature
    /// and never substituted into it, because the erased JVM descriptor is
    /// derived from the declaration while boxing decisions need the arguments.
    public static final class MethodRef {
        public final CilType declaringType;
        public final String name;
        public final MethodSig sig;
        public final CilType[] methodArgs;
        /// The definition, when the reference is to this assembly's own method.
        public final MethodDef def;

        MethodRef(CilType declaringType, String name, MethodSig sig, CilType[] methodArgs, MethodDef def) {
            this.declaringType = declaringType;
            this.name = name;
            this.sig = sig;
            this.methodArgs = methodArgs;
            this.def = def;
        }

        public CilType[] typeArgs() {
            return declaringType.kind == CilType.Kind.GENERICINST ? declaringType.args : null;
        }

        @Override
        public String toString() {
            return declaringType + "::" + name + sig;
        }
    }

    public static final class FieldRef {
        public final CilType declaringType;
        public final String name;
        public final CilType type;
        public final FieldDef def;

        FieldRef(CilType declaringType, String name, CilType type, FieldDef def) {
            this.declaringType = declaringType;
            this.name = name;
            this.type = type;
            this.def = def;
        }

        @Override
        public String toString() {
            return declaringType + "::" + name;
        }
    }

    private final MetadataReader md;
    private final String name;
    private Universe universe;
    private final List<TypeDef> types = new ArrayList<TypeDef>();
    private final Map<String, TypeDef> typesByName = new HashMap<String, TypeDef>();
    private FieldDef[] fieldsByRow;
    private MethodDef[] methodsByRow;
    private final Map<Integer, Object> tokenCache = new HashMap<Integer, Object>();
    private Map<Integer, List<String>> attributes;
    private PortablePdb debug;

    public CilAssembly(byte[] data, String source) {
        md = new MetadataReader(data, source);
        if (md.rowCount(MetadataReader.ASSEMBLY) > 0) {
            name = md.string(md.cell(MetadataReader.ASSEMBLY, 1, 7));
        } else {
            name = md.string(md.cell(MetadataReader.MODULE, 1, 1));
        }
    }

    public MetadataReader metadata() {
        return md;
    }

    /// Gives the assembly the portable PDB the compiler wrote beside it,
    /// so that [#sourceLocation(MethodDef, int)] has something to answer
    /// with.
    public void debugInformation(PortablePdb pdb) {
        debug = pdb;
    }

    /// Where in the C# source an instruction of a method came from, as
    /// `File.cs:line`, or null when the assembly came without a PDB or the
    /// PDB does not say.
    public String sourceLocation(MethodDef method, int ilOffset) {
        return debug == null ? null : debug.location(method.row, ilOffset);
    }

    /// The C# file a type was written in, as the path the compiler was
    /// given with `/` between its parts, or null when the assembly came
    /// without a PDB or no method of the type has source of its own. Read
    /// from the first method that says, then from the types nested in it,
    /// which is where a lambda or an iterator of the type lives.
    public String sourceFile(TypeDef type) {
        if (debug == null) {
            return null;
        }
        for (MethodDef m : type.methods) {
            String document = debug.document(m.row);
            if (document != null) {
                return document;
            }
        }
        for (TypeDef t : types) {
            if (t.enclosing == type) { // NOPMD CompareObjectsWithEquals
                String document = sourceFile(t);
                if (document != null) {
                    return document;
                }
            }
        }
        return null;
    }

    public String source() {
        return md.source();
    }

    public String name() {
        return name;
    }

    public List<TypeDef> types() {
        return Collections.unmodifiableList(types);
    }

    public TypeDef type(String fullName) {
        return typesByName.get(fullName);
    }

    /// Reads the types. Split from construction because signatures mention
    /// types of other assemblies, which have to be registered first.
    void load(Universe owner) {
        universe = owner;
        int typeCount = md.rowCount(MetadataReader.TYPE_DEF);
        for (int row = 1; row <= typeCount; row++) {
            types.add(new TypeDef(this, row, md.string(md.cell(MetadataReader.TYPE_DEF, row, 2)),
                    md.string(md.cell(MetadataReader.TYPE_DEF, row, 1)), md.cell(MetadataReader.TYPE_DEF, row, 0)));
        }
        for (int row = 1; row <= md.rowCount(MetadataReader.NESTED_CLASS); row++) {
            typeAt(md.cell(MetadataReader.NESTED_CLASS, row, 0)).enclosing =
                    typeAt(md.cell(MetadataReader.NESTED_CLASS, row, 1));
        }
        for (TypeDef t : types) {
            typesByName.put(t.fullName(), t);
        }
    }

    /// Reads what every type derives from. Whether a type is a value type is
    /// read off its base, and other assemblies ask that question while they
    /// link, so the bases of all assemblies are in place before any links.
    void linkBases() {
        for (TypeDef t : types) {
            int extendsValue = md.cell(MetadataReader.TYPE_DEF, t.row, 3);
            int extendsToken = md.codedToken(MetadataReader.TYPE_DEF_OR_REF, extendsValue);
            if (MetadataReader.tokenRow(extendsToken) != 0) {
                t.baseType = baseTypeFromToken(extendsToken);
            }
        }
    }

    /// Reads everything that needs other assemblies' type names to be known.
    void link() {
        for (int row = 1; row <= md.rowCount(MetadataReader.GENERIC_PARAM); row++) {
            int owner = md.codedToken(MetadataReader.TYPE_OR_METHOD_DEF, md.cell(MetadataReader.GENERIC_PARAM, row, 2));
            if (MetadataReader.tokenTable(owner) == MetadataReader.TYPE_DEF) {
                typeAt(MetadataReader.tokenRow(owner)).genericParamCount++;
            }
        }
        fieldsByRow = new FieldDef[md.rowCount(MetadataReader.FIELD) + 1];
        methodsByRow = new MethodDef[md.rowCount(MetadataReader.METHOD_DEF) + 1];
        for (TypeDef t : types) {
            int fieldEnd = md.listEnd(MetadataReader.TYPE_DEF, t.row, 4, MetadataReader.FIELD);
            for (int f = md.cell(MetadataReader.TYPE_DEF, t.row, 4); f < fieldEnd; f++) {
                int[] blob = md.blob(md.cell(MetadataReader.FIELD, f, 2));
                SignatureParser parser = new SignatureParser(this, blob[0]);
                FieldDef field = new FieldDef(t, f, md.string(md.cell(MetadataReader.FIELD, f, 1)),
                        md.cell(MetadataReader.FIELD, f, 0), parser.fieldSig());
                t.fields.add(field);
                fieldsByRow[f] = field;
            }
            int methodEnd = md.listEnd(MetadataReader.TYPE_DEF, t.row, 5, MetadataReader.METHOD_DEF);
            for (int m = md.cell(MetadataReader.TYPE_DEF, t.row, 5); m < methodEnd; m++) {
                int[] blob = md.blob(md.cell(MetadataReader.METHOD_DEF, m, 4));
                MethodDef method = new MethodDef(t, m, md.string(md.cell(MetadataReader.METHOD_DEF, m, 3)),
                        md.cell(MetadataReader.METHOD_DEF, m, 2),
                        md.cell(MetadataReader.METHOD_DEF, m, 0), new SignatureParser(this, blob[0]).methodSig());
                t.methods.add(method);
                methodsByRow[m] = method;
            }
        }
        for (int row = 1; row <= md.rowCount(MetadataReader.GENERIC_PARAM); row++) {
            int owner = md.codedToken(MetadataReader.TYPE_OR_METHOD_DEF, md.cell(MetadataReader.GENERIC_PARAM, row, 2));
            if (MetadataReader.tokenTable(owner) == MetadataReader.METHOD_DEF) {
                methodsByRow[MetadataReader.tokenRow(owner)].genericParamCount++;
            }
        }
        for (int row = 1; row <= md.rowCount(MetadataReader.INTERFACE_IMPL); row++) {
            int token = md.codedToken(MetadataReader.TYPE_DEF_OR_REF, md.cell(MetadataReader.INTERFACE_IMPL, row, 1));
            typeAt(md.cell(MetadataReader.INTERFACE_IMPL, row, 0)).interfaces.add(typeFromToken(token));
        }
        for (int row = 1; row <= md.rowCount(MetadataReader.FIELD_RVA); row++) {
            fieldsByRow[md.cell(MetadataReader.FIELD_RVA, row, 1)].rva = md.cell(MetadataReader.FIELD_RVA, row, 0);
        }
        for (int row = 1; row <= md.rowCount(MetadataReader.CONSTANT); row++) {
            int parent = md.codedToken(MetadataReader.HAS_CONSTANT, md.cell(MetadataReader.CONSTANT, row, 1));
            if (MetadataReader.tokenTable(parent) != MetadataReader.FIELD) {
                continue;
            }
            int[] span = md.blob(md.cell(MetadataReader.CONSTANT, row, 2));
            int at = span[0];
            long value;
            // The element type of the constant: the integers, signed and not.
            switch (md.cell(MetadataReader.CONSTANT, row, 0) & 0xFF) {
                case 0x02:
                case 0x05:
                    value = md.u1(at);
                    break;
                case 0x04:
                    value = (byte) md.u1(at);
                    break;
                case 0x03:
                case 0x07:
                    value = md.u2(at);
                    break;
                case 0x06:
                    value = (short) md.u2(at);
                    break;
                case 0x08:
                    value = md.i4(at);
                    break;
                case 0x09:
                    value = md.i4(at) & 0xFFFFFFFFL;
                    break;
                case 0x0A:
                case 0x0B:
                    value = md.i8(at);
                    break;
                default:
                    continue;
            }
            fieldsByRow[MetadataReader.tokenRow(parent)].constant = Long.valueOf(value);
        }
        for (int row = 1; row <= md.rowCount(MetadataReader.METHOD_SEMANTICS); row++) {
            MethodDef method = methodsByRow[md.cell(MetadataReader.METHOD_SEMANTICS, row, 1)];
            int association = md.codedToken(MetadataReader.HAS_SEMANTICS,
                    md.cell(MetadataReader.METHOD_SEMANTICS, row, 2));
            method.semantics = md.cell(MetadataReader.METHOD_SEMANTICS, row, 0);
            int table = MetadataReader.tokenTable(association);
            method.associationName = md.string(md.cell(table, MetadataReader.tokenRow(association), 1));
        }
    }

    /// Method overrides name methods of other types, whose own members must
    /// have been read already, so this runs after every assembly is linked.
    void linkOverrides() {
        for (int row = 1; row <= md.rowCount(MetadataReader.METHOD_IMPL); row++) {
            TypeDef owner = typeAt(md.cell(MetadataReader.METHOD_IMPL, row, 0));
            MethodRef body = methodRef(md.codedToken(MetadataReader.METHOD_DEF_OR_REF,
                    md.cell(MetadataReader.METHOD_IMPL, row, 1)));
            MethodRef declaration = methodRef(md.codedToken(MetadataReader.METHOD_DEF_OR_REF,
                    md.cell(MetadataReader.METHOD_IMPL, row, 2)));
            owner.methodImpls.add(new MethodImpl(body, declaration));
        }
    }

    private TypeDef typeAt(int row) {
        return types.get(row - 1);
    }

    // The base type decides whether a type is a value type, so resolving it
    // must not ask that question of the type being linked. A base is always a
    // class, which is all the answer that is needed here.
    private CilType baseTypeFromToken(int token) {
        int table = MetadataReader.tokenTable(token);
        if (table == MetadataReader.TYPE_SPEC) {
            return typeFromToken(token);
        }
        String fullName = table == MetadataReader.TYPE_DEF ? typeAt(MetadataReader.tokenRow(token)).fullName()
                : typeRefName(MetadataReader.tokenRow(token));
        CilType builtin = CilType.builtin(fullName);
        return builtin != null ? builtin : CilType.named(fullName, false);
    }

    String typeRefName(int row) {
        int scope = md.codedToken(MetadataReader.RESOLUTION_SCOPE, md.cell(MetadataReader.TYPE_REF, row, 0));
        String typeName = md.string(md.cell(MetadataReader.TYPE_REF, row, 1));
        String namespace = md.string(md.cell(MetadataReader.TYPE_REF, row, 2));
        if (MetadataReader.tokenTable(scope) == MetadataReader.TYPE_REF && MetadataReader.tokenRow(scope) != 0) {
            return typeRefName(MetadataReader.tokenRow(scope)) + "/" + typeName;
        }
        return namespace.length() == 0 ? typeName : namespace + "." + typeName;
    }

    /// Full name of the type a TypeDef or TypeRef token names.
    String typeNameFromToken(int token) {
        int table = MetadataReader.tokenTable(token);
        if (table == MetadataReader.TYPE_DEF) {
            return typeAt(MetadataReader.tokenRow(token)).fullName();
        }
        if (table == MetadataReader.TYPE_REF) {
            return typeRefName(MetadataReader.tokenRow(token));
        }
        throw new CilFormatException(source() + ": token 0x" + Integer.toHexString(token) + " does not name a type");
    }

    /// The type a TypeDef, TypeRef or TypeSpec token names.
    public CilType typeFromToken(int token) {
        Integer key = Integer.valueOf(token);
        Object cached = tokenCache.get(key);
        if (cached != null) {
            return (CilType) cached;
        }
        CilType result;
        int table = MetadataReader.tokenTable(token);
        if (table == MetadataReader.TYPE_SPEC) {
            int[] blob = md.blob(md.cell(MetadataReader.TYPE_SPEC, MetadataReader.tokenRow(token), 0));
            result = new SignatureParser(this, blob[0]).type();
        } else {
            String fullName = typeNameFromToken(token);
            result = CilType.builtin(fullName);
            if (result == null) {
                result = CilType.named(fullName, universe.require(fullName, source()).isValueType());
            }
        }
        tokenCache.put(key, result);
        return result;
    }

    CilType[] localsFromToken(int token) {
        int[] blob = md.blob(md.cell(MetadataReader.STANDALONE_SIG, MetadataReader.tokenRow(token), 0));
        return new SignatureParser(this, blob[0]).locals();
    }

    /// The method a MethodDef, MemberRef or MethodSpec token names.
    public MethodRef methodRef(int token) {
        Integer key = Integer.valueOf(token);
        Object cached = tokenCache.get(key);
        if (cached != null) {
            return (MethodRef) cached;
        }
        MethodRef result;
        int table = MetadataReader.tokenTable(token);
        int row = MetadataReader.tokenRow(token);
        if (table == MetadataReader.METHOD_DEF) {
            MethodDef def = methodsByRow[row];
            result = new MethodRef(def.owner.asType(), def.name, def.sig, null, def);
        } else if (table == MetadataReader.MEMBER_REF) {
            CilType parent = memberRefParent(row);
            int[] blob = md.blob(md.cell(MetadataReader.MEMBER_REF, row, 2));
            result = new MethodRef(parent, md.string(md.cell(MetadataReader.MEMBER_REF, row, 1)),
                    new SignatureParser(this, blob[0]).methodSig(), null, null);
        } else if (table == MetadataReader.METHOD_SPEC) {
            MethodRef generic = methodRef(md.codedToken(MetadataReader.METHOD_DEF_OR_REF,
                    md.cell(MetadataReader.METHOD_SPEC, row, 0)));
            int[] blob = md.blob(md.cell(MetadataReader.METHOD_SPEC, row, 1));
            result = new MethodRef(generic.declaringType, generic.name, generic.sig,
                    new SignatureParser(this, blob[0]).methodSpec(), generic.def);
        } else {
            throw new CilFormatException(source() + ": token 0x" + Integer.toHexString(token)
                    + " does not name a method");
        }
        tokenCache.put(key, result);
        return result;
    }

    /// The field a Field or MemberRef token names.
    public FieldRef fieldRef(int token) {
        Integer key = Integer.valueOf(token);
        Object cached = tokenCache.get(key);
        if (cached != null) {
            return (FieldRef) cached;
        }
        FieldRef result;
        int table = MetadataReader.tokenTable(token);
        int row = MetadataReader.tokenRow(token);
        if (table == MetadataReader.FIELD) {
            FieldDef def = fieldsByRow[row];
            result = new FieldRef(def.owner.asType(), def.name, def.type, def);
        } else if (table == MetadataReader.MEMBER_REF) {
            CilType parent = memberRefParent(row);
            int[] blob = md.blob(md.cell(MetadataReader.MEMBER_REF, row, 2));
            result = new FieldRef(parent, md.string(md.cell(MetadataReader.MEMBER_REF, row, 1)),
                    new SignatureParser(this, blob[0]).fieldSig(), null);
        } else {
            throw new CilFormatException(source() + ": token 0x" + Integer.toHexString(token)
                    + " does not name a field");
        }
        tokenCache.put(key, result);
        return result;
    }

    private CilType memberRefParent(int row) {
        int parent = md.codedToken(MetadataReader.MEMBER_REF_PARENT, md.cell(MetadataReader.MEMBER_REF, row, 0));
        int table = MetadataReader.tokenTable(parent);
        if (table == MetadataReader.TYPE_DEF || table == MetadataReader.TYPE_REF
                || table == MetadataReader.TYPE_SPEC) {
            return typeFromToken(parent);
        }
        throw new CilFormatException(source() + ": member reference " + row
                + " has a parent this reader does not model (table 0x" + Integer.toHexString(table) + ")");
    }

    /// True when a `ldtoken` operand names a field (a static array
    /// initialiser) and not a type or a method.
    public boolean isFieldToken(int token) {
        int table = MetadataReader.tokenTable(token);
        if (table == MetadataReader.FIELD) {
            return true;
        }
        if (table != MetadataReader.MEMBER_REF) {
            return false;
        }
        int[] blob = md.blob(md.cell(MetadataReader.MEMBER_REF, MetadataReader.tokenRow(token), 2));
        return md.u1(blob[0]) == 0x06;
    }

    /// The initial bytes of a field that has them (a compiler-generated
    /// array initialiser).
    public byte[] fieldData(FieldDef field, int length) {
        if (field.rva == 0) {
            throw new CilFormatException(source() + ": field " + field.name + " has no initial data");
        }
        return md.bytes(md.rvaToOffset(field.rva), length);
    }

    /// Full names of the attribute types applied to a type, field or method.
    public List<String> attributes(int token) {
        if (attributes == null) {
            attributes = new HashMap<Integer, List<String>>();
            for (int row = 1; row <= md.rowCount(MetadataReader.CUSTOM_ATTRIBUTE); row++) {
                int parent = md.codedToken(MetadataReader.HAS_CUSTOM_ATTRIBUTE,
                        md.cell(MetadataReader.CUSTOM_ATTRIBUTE, row, 0));
                int ctor = md.codedToken(MetadataReader.CUSTOM_ATTRIBUTE_TYPE,
                        md.cell(MetadataReader.CUSTOM_ATTRIBUTE, row, 1));
                String attributeType;
                if (MetadataReader.tokenTable(ctor) == MetadataReader.METHOD_DEF) {
                    attributeType = methodsByRow[MetadataReader.tokenRow(ctor)].owner.fullName();
                } else {
                    int owner = md.codedToken(MetadataReader.MEMBER_REF_PARENT,
                            md.cell(MetadataReader.MEMBER_REF, MetadataReader.tokenRow(ctor), 0));
                    int ownerTable = MetadataReader.tokenTable(owner);
                    if (ownerTable != MetadataReader.TYPE_DEF && ownerTable != MetadataReader.TYPE_REF) {
                        continue;
                    }
                    attributeType = typeNameFromToken(owner);
                }
                Integer key = Integer.valueOf(parent);
                List<String> list = attributes.get(key);
                if (list == null) {
                    list = new ArrayList<String>();
                    attributes.put(key, list);
                }
                list.add(attributeType);
            }
        }
        List<String> found = attributes.get(Integer.valueOf(token));
        return found == null ? Collections.<String>emptyList() : found;
    }

    public MethodDef entryPoint() {
        int token = md.entryPointToken();
        if (MetadataReader.tokenTable(token) != MetadataReader.METHOD_DEF || MetadataReader.tokenRow(token) == 0) {
            return null;
        }
        return methodsByRow[MetadataReader.tokenRow(token)];
    }

    public String userString(int token) {
        return md.userString(token);
    }
}
