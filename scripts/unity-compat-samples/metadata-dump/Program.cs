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
using System;
using System.Collections.Immutable;
using System.IO;
using System.Reflection.Metadata;
using System.Reflection.Metadata.Ecma335;
using System.Reflection.PortableExecutable;
using System.Text;

// Prints an assembly's types, fields and methods in the format the Java
// metadata reader's own dump uses. Keep the two in step.
public sealed class Names : ISignatureTypeProvider<string, object>
{
    public static string Builtin(string fullName)
    {
        switch (fullName)
        {
            case "System.Boolean": return "boolean";
            case "System.Char": return "char";
            case "System.SByte": return "i1";
            case "System.Byte": return "u1";
            case "System.Int16": return "i2";
            case "System.UInt16": return "u2";
            case "System.Int32": return "i4";
            case "System.UInt32": return "u4";
            case "System.Int64": return "i8";
            case "System.UInt64": return "u8";
            case "System.Single": return "r4";
            case "System.Double": return "r8";
            case "System.IntPtr": return "i";
            case "System.UIntPtr": return "u";
            case "System.String": return "string";
            case "System.Object": return "object";
            case "System.Void": return "void";
            default: return null;
        }
    }

    public static string FullName(MetadataReader reader, TypeDefinitionHandle handle)
    {
        TypeDefinition def = reader.GetTypeDefinition(handle);
        string name = reader.GetString(def.Name);
        TypeDefinitionHandle outer = def.GetDeclaringType();
        if (!outer.IsNil)
        {
            return FullName(reader, outer) + "/" + name;
        }
        string ns = reader.GetString(def.Namespace);
        return ns.Length == 0 ? name : ns + "." + name;
    }

    public static string FullName(MetadataReader reader, TypeReferenceHandle handle)
    {
        TypeReference r = reader.GetTypeReference(handle);
        string name = reader.GetString(r.Name);
        if (r.ResolutionScope.Kind == HandleKind.TypeReference && !r.ResolutionScope.IsNil)
        {
            return FullName(reader, (TypeReferenceHandle)r.ResolutionScope) + "/" + name;
        }
        string ns = reader.GetString(r.Namespace);
        return ns.Length == 0 ? name : ns + "." + name;
    }

    private static string Named(string fullName, byte rawTypeKind)
    {
        string builtin = Builtin(fullName);
        if (builtin != null)
        {
            return builtin;
        }
        return rawTypeKind == 0x11 ? "valuetype " + fullName : fullName;
    }

    // A base type or interface: a bare token, printed without the value type marker.
    public string Plain(MetadataReader reader, EntityHandle handle)
    {
        switch (handle.Kind)
        {
            case HandleKind.TypeDefinition:
                return Named(FullName(reader, (TypeDefinitionHandle)handle), 0);
            case HandleKind.TypeReference:
                return Named(FullName(reader, (TypeReferenceHandle)handle), 0);
            default:
            {
                string text = reader.GetTypeSpecification((TypeSpecificationHandle)handle).DecodeSignature(this, null);
                return text.StartsWith("valuetype ") ? text.Substring("valuetype ".Length) : text;
            }
        }
    }

    public string GetPrimitiveType(PrimitiveTypeCode typeCode)
    {
        switch (typeCode)
        {
            case PrimitiveTypeCode.Void: return "void";
            case PrimitiveTypeCode.Boolean: return "boolean";
            case PrimitiveTypeCode.Char: return "char";
            case PrimitiveTypeCode.SByte: return "i1";
            case PrimitiveTypeCode.Byte: return "u1";
            case PrimitiveTypeCode.Int16: return "i2";
            case PrimitiveTypeCode.UInt16: return "u2";
            case PrimitiveTypeCode.Int32: return "i4";
            case PrimitiveTypeCode.UInt32: return "u4";
            case PrimitiveTypeCode.Int64: return "i8";
            case PrimitiveTypeCode.UInt64: return "u8";
            case PrimitiveTypeCode.Single: return "r4";
            case PrimitiveTypeCode.Double: return "r8";
            case PrimitiveTypeCode.IntPtr: return "i";
            case PrimitiveTypeCode.UIntPtr: return "u";
            case PrimitiveTypeCode.String: return "string";
            case PrimitiveTypeCode.Object: return "object";
            case PrimitiveTypeCode.TypedReference: return "typedbyref";
            default: throw new InvalidOperationException(typeCode.ToString());
        }
    }

    public string GetTypeFromDefinition(MetadataReader reader, TypeDefinitionHandle handle, byte rawTypeKind)
    {
        return Named(FullName(reader, handle), rawTypeKind);
    }

    public string GetTypeFromReference(MetadataReader reader, TypeReferenceHandle handle, byte rawTypeKind)
    {
        return Named(FullName(reader, handle), rawTypeKind);
    }

    public string GetTypeFromSpecification(MetadataReader reader, object genericContext,
        TypeSpecificationHandle handle, byte rawTypeKind)
    {
        return reader.GetTypeSpecification(handle).DecodeSignature(this, genericContext);
    }

    public string GetSZArrayType(string elementType) { return elementType + "[]"; }
    public string GetArrayType(string elementType, ArrayShape shape) { return elementType + "[rank " + shape.Rank + "]"; }
    public string GetByReferenceType(string elementType) { return elementType + "&"; }
    public string GetPointerType(string elementType) { return elementType + "*"; }
    public string GetPinnedType(string elementType) { return elementType; }
    public string GetModifiedType(string modifier, string unmodifiedType, bool isRequired) { return unmodifiedType; }
    public string GetFunctionPointerType(MethodSignature<string> signature) { return "fnptr"; }
    public string GetGenericTypeParameter(object genericContext, int index) { return "!" + index; }
    public string GetGenericMethodParameter(object genericContext, int index) { return "!!" + index; }

    public string GetGenericInstantiation(string genericType, ImmutableArray<string> typeArguments)
    {
        return genericType + "<" + string.Join(",", typeArguments) + ">";
    }
}

public static class Program
{
    public static int Main(string[] args)
    {
        if (args.Length != 1)
        {
            Console.Error.WriteLine("usage: MetadataDump <assembly>");
            return 2;
        }
        using (FileStream stream = File.OpenRead(args[0]))
        using (PEReader pe = new PEReader(stream))
        using (StreamWriter output = new StreamWriter(Console.OpenStandardOutput(), new UTF8Encoding(false)))
        {
            output.NewLine = "\n";
            MetadataReader md = pe.GetMetadataReader();
            Names names = new Names();
            foreach (TypeDefinitionHandle handle in md.TypeDefinitions)
            {
                TypeDefinition type = md.GetTypeDefinition(handle);
                output.WriteLine("T " + Names.FullName(md, handle) + " flags=" + ((int)type.Attributes).ToString("x")
                    + " base=" + (type.BaseType.IsNil ? "-" : names.Plain(md, type.BaseType))
                    + " generic=" + type.GetGenericParameters().Count);
                foreach (InterfaceImplementationHandle i in type.GetInterfaceImplementations())
                {
                    output.WriteLine("  I " + names.Plain(md, md.GetInterfaceImplementation(i).Interface));
                }
                foreach (FieldDefinitionHandle f in type.GetFields())
                {
                    FieldDefinition field = md.GetFieldDefinition(f);
                    output.WriteLine("  F " + md.GetString(field.Name) + " flags=" + ((int)field.Attributes).ToString("x")
                        + " : " + field.DecodeSignature(names, null));
                }
                foreach (MethodDefinitionHandle m in type.GetMethods())
                {
                    MethodDefinition method = md.GetMethodDefinition(m);
                    MethodSignature<string> sig = method.DecodeSignature(names, null);
                    string code = " code=-1 locals=0 clauses=0";
                    if (method.RelativeVirtualAddress != 0)
                    {
                        MethodBodyBlock body = pe.GetMethodBody(method.RelativeVirtualAddress);
                        int locals = 0;
                        if (!body.LocalSignature.IsNil)
                        {
                            locals = md.GetStandaloneSignature(body.LocalSignature).DecodeLocalSignature(names, null).Length;
                        }
                        code = " code=" + body.GetILReader().Length + " locals=" + locals
                            + " clauses=" + body.ExceptionRegions.Length;
                    }
                    output.WriteLine("  M " + md.GetString(method.Name) + " flags=" + ((int)method.Attributes).ToString("x")
                        + " (" + string.Join(", ", sig.ParameterTypes) + ") : " + sig.ReturnType + code);
                }
            }
        }
        return 0;
    }
}
