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
package com.codename1.tools.javac;

import java.util.ArrayList;
import java.util.List;

/**
 * The abstract syntax tree. Types are expressions, as in javac: a type is an
 * {@link Ident}, {@link Select}, {@link TypeApply}, {@link ArrayTypeTree},
 * {@link PrimitiveTypeTree} or {@link Wildcard}. Attribution fills in
 * {@code type} on every expression and {@code sym} on every name.
 */
abstract class Tree {
    int pos;
    /** The attributed type; null until attribution. */
    Type type;
    /** The value of a constant expression (Integer, Long, Float, Double, Character, Boolean, String), else null. */
    Object constant;

    // ------------------------------------------------------------------ declarations

    static final class CompilationUnit extends Tree {
        Source source;
        String packageName = "";
        final List<Import> imports = new ArrayList<Import>();
        final List<ClassDecl> types = new ArrayList<ClassDecl>();
    }

    static final class Import extends Tree {
        String name;
        boolean isStatic;
        boolean onDemand;
        /** Added by the compiler (a script's default imports), not written in the source. */
        boolean implicit;
    }

    static final class Modifiers extends Tree {
        int flags;
        final List<Annotation> annotations = new ArrayList<Annotation>();
    }

    static final class Annotation extends Tree {
        /** The annotation interface's name, as written; attribution resolves it. */
        Tree name;
        final List<Tree> args = new ArrayList<Tree>();
    }

    enum ClassKind { CLASS, INTERFACE, ENUM, RECORD, ANNOTATION }

    static final class ClassDecl extends Tree {
        Modifiers mods;
        ClassKind kind = ClassKind.CLASS;
        String name;
        final List<TypeParameter> typeParams = new ArrayList<TypeParameter>();
        Tree extending;
        final List<Tree> implementing = new ArrayList<Tree>();
        final List<Tree> permitting = new ArrayList<Tree>();
        final List<VarDef> recordComponents = new ArrayList<VarDef>();
        final List<Tree> members = new ArrayList<Tree>();
        /** Set for an anonymous or local class: the enclosing method body context. */
        boolean local;
        boolean anonymous;
    }

    static final class TypeParameter extends Tree {
        String name;
        final List<Tree> bounds = new ArrayList<Tree>();
        Type.TypeVar tvar;
    }

    static final class MethodDecl extends Tree {
        Modifiers mods;
        final List<TypeParameter> typeParams = new ArrayList<TypeParameter>();
        /** Null for a constructor. */
        Tree returnType;
        String name;
        final List<VarDef> params = new ArrayList<VarDef>();
        final List<Tree> thrown = new ArrayList<Tree>();
        Block body;
        boolean varargs;
        /** A record's compact canonical constructor: parameters are implicit. */
        boolean compactConstructor;
        MethodSymbol sym;
    }

    static final class VarDef extends Tree {
        /** The declared type of a pattern or lambda parameter after inference. */
        Type declaredType;
        Modifiers mods;
        /** Null when declared with {@code var} or as an implicitly typed lambda parameter. */
        Tree vartype;
        String name;
        Tree init;
        /** Catch parameter alternatives for a multi-catch. */
        List<Tree> unionTypes;
        VarSymbol sym;
    }

    // ------------------------------------------------------------------ statements

    static final class Block extends Tree {
        final List<Tree> stats = new ArrayList<Tree>();
        boolean isStatic;
        int endPos;
    }

    static final class ExpressionStatement extends Tree {
        Tree expr;
    }

    static final class If extends Tree {
        Tree cond;
        Tree thenPart;
        Tree elsePart;
    }

    static final class WhileLoop extends Tree {
        Tree cond;
        Tree body;
    }

    static final class DoLoop extends Tree {
        Tree body;
        Tree cond;
    }

    static final class ForLoop extends Tree {
        final List<Tree> init = new ArrayList<Tree>();
        Tree cond;
        final List<Tree> step = new ArrayList<Tree>();
        Tree body;
    }

    static final class ForEach extends Tree {
        VarDef var;
        Tree expr;
        Tree body;
    }

    static final class Labeled extends Tree {
        String label;
        Tree body;
    }

    /** A switch statement or switch expression. */
    static final class Switch extends Tree {
        /** Selector kind decided by attribution: see {@link Attr}'s SW_ constants. */
        int switchKind;
        /** A switch expression or pattern switch that needs a synthetic default throwing an error. */
        boolean needsDefaultThrow;
        Tree selector;
        final List<Case> cases = new ArrayList<Case>();
        boolean isExpression;
        /** Arrow form ({@code case X ->}) rather than colon form. */
        boolean arrows;
    }

    static final class Case extends Tree {
        /** Constant expressions, enum names or patterns; empty for {@code default}. */
        final List<Tree> labels = new ArrayList<Tree>();
        boolean isDefault;
        /** {@code case null} (possibly with default). */
        boolean hasNull;
        Tree guard;
        /** Colon form: the statements; arrow form: one Block, ExpressionStatement or Throw. */
        final List<Tree> stats = new ArrayList<Tree>();
        /** Arrow form with an expression body (switch expression value). */
        Tree arrowExpr;
    }

    static final class Return extends Tree {
        Tree expr;
    }

    static final class Break extends Tree {
        String label;
    }

    static final class Continue extends Tree {
        String label;
    }

    static final class Yield extends Tree {
        Tree value;
    }

    static final class Throw extends Tree {
        Tree expr;
    }

    static final class Try extends Tree {
        final List<Tree> resources = new ArrayList<Tree>();
        Block body;
        final List<Catch> catches = new ArrayList<Catch>();
        Block finalizer;
    }

    static final class Catch extends Tree {
        VarDef param;
        Block body;
    }

    static final class Synchronized extends Tree {
        Tree lock;
        Block body;
    }

    static final class Assert extends Tree {
        Tree cond;
        Tree detail;
    }

    static final class Empty extends Tree {
    }

    // ------------------------------------------------------------------ expressions

    static final class Ident extends Tree {
        /** For a field or method reached through an enclosing instance: the class whose {@code this} is the receiver. */
        ClassSymbol site;
        String name;
        Symbol sym;
    }

    static final class Select extends Tree {
        /** True when {@code selected} names a type or package rather than a value. */
        boolean staticRef;
        Tree selected;
        String name;
        Symbol sym;
        /** Position of the '.' before {@code name}, where javac reports a missing package; -1 when unknown. */
        int dotPos = -1;
    }

    static final class Literal extends Tree {
        Token.Kind kind;
        Object value;
    }

    static final class Parens extends Tree {
        Tree expr;
    }

    static final class Assign extends Tree {
        Tree lhs;
        Tree rhs;
    }

    static final class CompoundAssign extends Tree {
        Token.Kind op;
        Tree lhs;
        Tree rhs;
        /** The binary operator's operand type (after promotion). */
        Type operandType;
    }

    static final class Unary extends Tree {
        /** PLUS, SUB, BANG, TILDE, or PLUSPLUS/SUBSUB with {@link #postfix}. */
        Token.Kind op;
        boolean postfix;
        Tree arg;
        Type operandType;
    }

    static final class Binary extends Tree {
        Token.Kind op;
        Tree lhs;
        Tree rhs;
        /** Type both operands are converted to (or the String type for concatenation). */
        Type operandType;
    }

    static final class Conditional extends Tree {
        Tree cond;
        Tree truePart;
        Tree falsePart;
    }

    static final class InstanceOf extends Tree {
        Tree expr;
        /** A type, or a pattern ({@link BindingPattern} / {@link RecordPattern}). */
        Tree pattern;
    }

    static final class BindingPattern extends Tree {
        VarDef var;
    }

    static final class RecordPattern extends Tree {
        Tree deconstructor;
        final List<Tree> nested = new ArrayList<Tree>();
        ClassSymbol record;
    }

    static final class Cast extends Tree {
        Tree clazz;
        Tree expr;
    }

    static final class MethodCall extends Tree {
        /** The class named in the Methodref constant. */
        ClassSymbol qualifier;
        /** For an unqualified call to an enclosing instance's method: that class. */
        ClassSymbol site;
        /** Null for an unqualified call; the qualifier expression or type otherwise. */
        Tree receiver;
        String name;
        final List<Tree> typeArgs = new ArrayList<Tree>();
        final List<Tree> args = new ArrayList<Tree>();
        MethodSymbol sym;
        /** Arguments collected into an array for a variable-arity call. */
        boolean varargsCall;
        /** {@code super.m()} or {@code X.super.m()}: an invokespecial. */
        boolean superCall;
        /** Position of the '.' before the name of a qualified call, where javac reports it; -1 otherwise. */
        int dotPos = -1;
        /** For Outer.super.m() from an inner class: the static accessor in Outer that makes the call. */
        MethodSymbol superAccessor;
        /** For a variable-arity call: the inferred element type of the array the arguments go in. */
        Type varargsElem;
    }

    static final class NewClass extends Tree {
        /** The class instantiated (the anonymous class itself when there is a body). */
        ClassSymbol clazzSym;
        Tree outer;
        Tree clazz;
        final List<Tree> typeArgs = new ArrayList<Tree>();
        final List<Tree> args = new ArrayList<Tree>();
        ClassDecl body;
        MethodSymbol constructor;
        boolean varargsCall;
        /** For a variable-arity call: the inferred element type of the array the arguments go in. */
        Type varargsElem;
    }

    static final class NewArray extends Tree {
        /** Element type tree; null for a bare initializer {@code {1, 2}}. */
        Tree elemType;
        final List<Tree> dims = new ArrayList<Tree>();
        int extraDims;
        /** Null unless written with an initializer. */
        List<Tree> elems;
    }

    static final class ArrayAccess extends Tree {
        Tree array;
        Tree index;
    }

    static final class Lambda extends Tree {
        final List<VarDef> params = new ArrayList<VarDef>();
        boolean explicitParams;
        /** An expression or a {@link Block}. */
        Tree body;
        /** The functional interface method this lambda implements. */
        MethodSymbol target;
        /** Captured locals, in the order the synthetic method takes them first. */
        List<VarSymbol> captured;
        boolean capturesThis;
        /** A bound method reference's receiver: evaluated (and null-checked) when the lambda is created. */
        Tree boundExpr;
        /** The parameter the bound receiver arrives in, first among the captured values. */
        VarSymbol boundVar;
        /** The functional interface type implemented (erased for the indy descriptor). */
        Type targetType;
    }

    static final class MethodRef extends Tree {
        /** An expression (bound receiver) or a type. */
        Tree qualifier;
        boolean qualifierIsType;
        String name;
        final List<Tree> typeArgs = new ArrayList<Tree>();
        /** The equivalent lambda the reference was rewritten to; code is generated from it. */
        Lambda lambda;
    }

    static final class ClassLiteral extends Tree {
        Tree clazz;
    }

    static final class TypeApply extends Tree {
        Tree clazz;
        final List<Tree> args = new ArrayList<Tree>();
    }

    static final class ArrayTypeTree extends Tree {
        Tree elem;
    }

    static final class PrimitiveTypeTree extends Tree {
        Token.Kind tag;
    }

    static final class Wildcard extends Tree {
        /** EXTENDS, SUPER, or null for an unbounded {@code ?}. */
        Token.Kind boundKind;
        Tree bound;
    }

    /** {@code A & B} in a cast or a type-parameter bound. */
    static final class IntersectionTypeTree extends Tree {
        final List<Tree> bounds = new ArrayList<Tree>();
    }

    /** A type the compiler already resolved, standing in a synthesized tree (method reference bodies). */
    static final class ResolvedType extends Tree {
        ResolvedType(Type t, int pos) {
            this.type = t;
            this.pos = pos;
        }
    }

    /** The element of an annotation or array initializer that the parser could not type yet. */
    static final class Erroneous extends Tree {
    }
}
