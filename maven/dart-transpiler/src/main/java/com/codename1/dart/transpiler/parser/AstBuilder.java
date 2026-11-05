/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.dart.transpiler.parser;

import com.codename1.dart.transpiler.api.Diagnostics;
import com.codename1.dart.transpiler.ast.Ast;
import com.codename1.dart.transpiler.ast.Ast.*;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts the ANTLR Dart parse tree into the transpiler's own AST
 * ({@link Ast}). This is the ONLY class that touches generated parser
 * contexts — grammar upgrades are absorbed here.
 *
 * <p>Constructs outside the supported subset produce a source-positioned
 * diagnostic instead of silently wrong output.</p>
 */
public final class AstBuilder {

    private final Diagnostics diags;
    private String file;
    /** The `import ... as p` prefixes of the library being built (imports come first). */
    private List<String> importPrefixes = new ArrayList<String>();
    /// The library being built, which collects the types aliased to built-in collections.
    private Library currentLib;
    private java.util.Map<String, String> prefixImports = new java.util.HashMap<String, String>();

    public AstBuilder(Diagnostics diags) {
        this.diags = diags;
    }

    // ------------------------------------------------------------------
    // Entry points
    // ------------------------------------------------------------------

    public Library parse(String fileName, String source) {
        this.file = fileName;
        Dart2Lexer lexer = new Dart2Lexer(CharStreams.fromString(source));
        Dart2Parser parser = new Dart2Parser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        lexer.removeErrorListeners();
        BaseErrorListener listener = new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                                    int charPositionInLine, String msg, RecognitionException e) {
                diags.error(fileName, line, charPositionInLine, "E0001", "Syntax error: " + msg);
            }
        };
        parser.addErrorListener(listener);
        lexer.addErrorListener(listener);
        Dart2Parser.CompilationUnitContext unit = parser.compilationUnit();
        Library lib = new Library();
        lib.fileName = fileName;
        currentLib = lib;
        importPrefixes = lib.importPrefixes;
        prefixImports = lib.prefixImports;
        pos(lib, unit);
        if (unit.libraryDeclaration() != null) {
            buildLibrary(unit.libraryDeclaration(), lib);
        } else if (unit.partDeclaration() != null) {
            // `part of` file: its declarations belong to the owning library, which
            // Program.add merges them into (a Dart library is its primary file plus
            // every part it names).
            Dart2Parser.PartDeclarationContext part = unit.partDeclaration();
            Dart2Parser.PartHeaderContext header = part.partHeader();
            if (header.uri() != null) {
                lib.partOfUri = stripQuotes(header.uri().getText());
            } else {
                lib.partOfName = header.dottedIdentifierList().getText();
            }
            buildDeclarations(part.topLevelDeclaration(), part.metadata(), lib);
        }
        return lib;
    }

    /** Parses a lone expression (used for string-interpolation fragments). */
    Expr parseExprFragment(String source, int line, int col) {
        Dart2Lexer lexer = new Dart2Lexer(CharStreams.fromString(source));
        Dart2Parser parser = new Dart2Parser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        lexer.removeErrorListeners();
        final String f = file;
        BaseErrorListener listener = new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int l,
                                    int c, String msg, RecognitionException e) {
                diags.error(f, line, col, "E0002", "Syntax error in string interpolation: " + msg);
            }
        };
        parser.addErrorListener(listener);
        lexer.addErrorListener(listener);
        return buildExpr(parser.expr());
    }

    // ------------------------------------------------------------------
    // Declarations
    // ------------------------------------------------------------------

    private static Ast.Directive directive(String uri, List<Dart2Parser.CombinatorContext> combinators) {
        Ast.Directive d = new Ast.Directive();
        d.uri = uri;
        for (Dart2Parser.CombinatorContext c : combinators) {
            List<String> names = new ArrayList<String>();
            for (Dart2Parser.IdentifierContext id : c.identifierList().identifier()) {
                names.add(id.getText());
            }
            if (c.SHOW_() != null) {
                if (d.show == null) {
                    d.show = names;
                } else {
                    d.show.retainAll(names);
                }
            } else {
                d.hide.addAll(names);
            }
        }
        return d;
    }

    private void buildLibrary(Dart2Parser.LibraryDeclarationContext ctx, Library lib) {
        for (Dart2Parser.ImportOrExportContext ie : ctx.importOrExport()) {
            if (ie.libraryImport() != null) {
                Dart2Parser.ImportSpecificationContext spec = ie.libraryImport().importSpecification();
                String uri = selectUri(spec.configurableUri());
                lib.imports.add(stripQuotes(uri));
                // `import '...' as prefix;` — record the prefix so prefixed member
                // access (prefix.topLevelConst) resolves against the whole program.
                if (spec.identifier() != null) {
                    lib.importPrefixes.add(spec.identifier().getText());
                    lib.prefixImports.put(spec.identifier().getText(), stripQuotes(uri));
                }
                Ast.Directive d = directive(stripQuotes(uri), spec.combinator());
                d.prefix = spec.identifier() == null ? null : spec.identifier().getText();
                lib.importDirectives.add(d);
            } else if (ie.libraryExport() != null) {
                Dart2Parser.LibraryExportContext ex = ie.libraryExport();
                lib.exportDirectives.add(directive(stripQuotes(selectUri(ex.configurableUri())), ex.combinator()));
            }
        }
        if (ctx.libraryName() != null) {
            // `library;` (Dart 2.19) has no name; it only carries doc comments and metadata.
            if (ctx.libraryName().dottedIdentifierList() != null) {
                lib.libraryName = ctx.libraryName().dottedIdentifierList().getText();
            }
        }
        for (Dart2Parser.PartDirectiveContext pd : ctx.partDirective()) {
            lib.partUris.add(stripQuotes(pd.uri().getText()));
        }
        buildDeclarations(ctx.topLevelDeclaration(), ctx.metadata(), lib);
    }

    /**
     * The {@code dart:} libraries this runtime implements, i.e. the {@code dart.library.X}
     * conditions that are true here. {@code dart:html}, {@code dart:ffi}
     * and the JS interop libraries are not provided, so a conditional import keyed on one
     * of them takes its default (usually the portable stub), exactly as Dart picks it on
     * a platform without that library.
     */
    private static final java.util.Set<String> SUPPORTED_DART_LIBRARIES = new java.util.HashSet<String>(
            java.util.Arrays.asList("core", "async", "collection", "convert", "developer", "io", "math", "typed_data", "ui"));

    /**
     * Picks the uri a configurable import/export names on this platform: the first
     * {@code if (test) 'uri'} clause whose test holds, else the default uri. getText() of
     * the whole rule would concatenate every alternative into one bogus path.
     */
    private String selectUri(Dart2Parser.ConfigurableUriContext ctx) {
        for (Dart2Parser.ConfigurationUriContext c : ctx.configurationUri()) {
            Dart2Parser.UriTestContext test = c.uriTest();
            String name = test.dottedIdentifierList().getText();
            String expected = test.stringLiteral() == null ? "true" : stripQuotes(test.stringLiteral().getText());
            String actual = "false";
            if (name.startsWith("dart.library.")
                    && SUPPORTED_DART_LIBRARIES.contains(name.substring("dart.library.".length()))) {
                actual = "true";
            }
            if (actual.equals(expected)) {
                return c.uri().getText();
            }
        }
        return ctx.uri().getText();
    }

    private void buildDeclarations(List<Dart2Parser.TopLevelDeclarationContext> decls,
                                   List<Dart2Parser.MetadataContext> metas, Library lib) {
        for (int i = 0; i < decls.size(); i++) {
            buildTopLevel(decls.get(i), metas.size() > i ? metas.get(i) : null, lib);
        }
    }

    private void buildTopLevel(Dart2Parser.TopLevelDeclarationContext ctx,
                               Dart2Parser.MetadataContext meta, Library lib) {
        String javaName = annotationArg(meta, "JavaName");
        if (ctx.classDeclaration() != null) {
            ClassDecl cd = buildClass(ctx.classDeclaration());
            if (cd != null) {
                cd.javaName = javaName;
                // `@ConstKeyed` in a stub: the Java class implements ConstCanon.Keyed.
                cd.constKeyed = hasAnnotation(meta, "ConstKeyed");
                lib.classes.add(cd);
            }
        } else if (ctx.extensionTypeDeclaration() != null) {
            buildExtensionType(ctx.extensionTypeDeclaration(), javaName, lib);
        } else if (ctx.extensionDeclaration() != null) {
            Dart2Parser.ExtensionDeclarationContext ext = ctx.extensionDeclaration();
            ClassDecl cd = new ClassDecl();
            pos(cd, ext);
            // An unnamed extension's Java class carries its library: two libraries with an
            // unnamed extension at the same offset produced one class name, and one lost.
            cd.unnamedExtension = ext.identifier() == null;
            cd.name = !cd.unnamedExtension ? ext.identifier().getText()
                    : "Ext$" + (lib.fileName == null ? ""
                    : com.codename1.dart.transpiler.analyze.Program.libClassName(lib.fileName) + "$")
                    + Integer.toHexString(ext.getStart().getStartIndex());
            cd.javaName = javaName;
            cd.extensionOn = buildType(ext.type());
            // `extension Picks<E> on List<E>`: kept so a call on a List<int> can bind E.
            if (ext.typeParameters() != null) {
                for (Dart2Parser.TypeParameterContext tp : ext.typeParameters().typeParameter()) {
                    cd.typeParams.add(tp.identifier().getText());
                    // `extension S<T extends num> on Iterable<T>`: `a < b` on two T needs it.
                    if (tp.typeNotVoid() != null) {
                        cd.typeParamBounds.put(tp.identifier().getText(), buildTypeNotVoid(tp.typeNotVoid()));
                    }
                }
            }
            java.util.List<Dart2Parser.ClassMemberDeclarationContext> members = ext.classMemberDeclaration();
            java.util.List<Dart2Parser.MetadataContext> metas = ext.metadata();
            for (int i = 0; i < members.size(); i++) {
                buildMember(members.get(i), metas.size() > i ? metas.get(i) : null, cd);
            }
            hoistStaticFields(cd, lib);
            lib.classes.add(cd);
        } else if (ctx.mixinDeclaration() != null) {
            Dart2Parser.MixinDeclarationContext mx = ctx.mixinDeclaration();
            ClassDecl cd = new ClassDecl();
            pos(cd, mx);
            cd.name = mx.typeIdentifier().getText();
            cd.isMixin = true;
            if (mx.ON_() != null && mx.typeNotVoidList() != null) {
                for (Dart2Parser.TypeNotVoidContext t : mx.typeNotVoidList().typeNotVoid()) {
                    cd.mixinOn.add(buildTypeNotVoid(t));
                }
            }
            // `mixin M implements Shape`: a value of M is a Shape
            if (mx.interfaces() != null) {
                for (Dart2Parser.TypeNotVoidContext t : mx.interfaces().typeNotVoidList().typeNotVoid()) {
                    cd.interfaces.add(buildTypeNotVoid(t));
                }
            }
            // `mixin Holder<H>`: dropped, the interface named an undeclared H and every
            // class applying the mixin failed to compile.
            if (mx.typeParameters() != null) {
                for (Dart2Parser.TypeParameterContext tp : mx.typeParameters().typeParameter()) {
                    cd.typeParams.add(tp.identifier().getText());
                    // `mixin M<E extends Item>`: members of E read through the bound
                    if (tp.typeNotVoid() != null) {
                        cd.typeParamBounds.put(tp.identifier().getText(), buildTypeNotVoid(tp.typeNotVoid()));
                    }
                }
            }
            cd.javaName = javaName;
            cd.javaName = javaName;
            java.util.List<Dart2Parser.ClassMemberDeclarationContext> members = mx.classMemberDeclaration();
            java.util.List<Dart2Parser.MetadataContext> metas = mx.metadata();
            for (int i = 0; i < members.size(); i++) {
                buildMember(members.get(i), metas.size() > i ? metas.get(i) : null, cd);
            }
            lib.classes.add(cd);
        } else if (ctx.enumType() != null) {
            EnumDecl ed = buildEnum(ctx.enumType());
            ed.javaName = javaName;
            lib.enums.add(ed);
        } else if (ctx.functionSignature() != null && ctx.EXTERNAL_() != null) {
            FunctionDecl fn = new FunctionDecl();
            pos(fn, ctx);
            buildFunctionSignature(ctx.functionSignature(), fn);
            fn.isExternal = true;
            fn.javaName = javaName;
            lib.functions.add(fn);
        } else if (ctx.functionSignature() != null && ctx.functionBody() != null) {
            FunctionDecl fn = new FunctionDecl();
            pos(fn, ctx);
            buildFunctionSignature(ctx.functionSignature(), fn);
            fn.javaName = javaName;
            buildFunctionBodyInto(ctx.functionBody(), fn);
            lib.functions.add(fn);
        } else if (ctx.initializedIdentifierList() != null || ctx.staticFinalDeclarationList() != null) {
            // top-level variables become statics on the library class
            TypeRef type = TypeRef.VAR;
            if (ctx.type() != null) {
                type = buildType(ctx.type());
            } else if (ctx.varOrType() != null && ctx.varOrType().type() != null) {
                type = buildType(ctx.varOrType().type());
            }
            boolean isFinal = ctx.FINAL_() != null || ctx.CONST_() != null;
            // `late` reaches here only through `LATE_ FINAL_ ... initializedIdentifierList`
            // or `LATE_? varOrType initializedIdentifierList` -- never through
            // staticFinalDeclarationList, which has no LATE_ alternative at all.
            boolean isLate = ctx.LATE_() != null;
            if (ctx.initializedIdentifierList() != null) {
                for (Dart2Parser.InitializedIdentifierContext ii : ctx.initializedIdentifierList().initializedIdentifier()) {
                    FieldDecl f = new FieldDecl();
                    pos(f, ii);
                    f.name = ii.identifier().getText();
                    f.type = type;
                    f.isFinal = isFinal;
                    f.isConst = ctx.CONST_() != null;
                    f.isStatic = true;
                    f.isLate = isLate;
                    f.javaName = javaName;
                    if (ii.expr() != null) {
                        f.initializer = buildExpr(ii.expr());
                        if (f.isConst) {
                            markConstContext(f.initializer);
                        }
                    }
                    lib.topLevelVars.add(f);
                }
            } else {
                for (Dart2Parser.StaticFinalDeclarationContext sf : ctx.staticFinalDeclarationList().staticFinalDeclaration()) {
                    FieldDecl f = new FieldDecl();
                    pos(f, sf);
                    f.name = sf.identifier().getText();
                    f.type = type;
                    f.isFinal = true;
                    f.isConst = ctx.CONST_() != null;
                    f.isStatic = true;
                    f.javaName = javaName;
                    f.initializer = buildExpr(sf.expr());
                    if (f.isConst) {
                        markConstContext(f.initializer);
                    }
                    lib.topLevelVars.add(f);
                }
            }
        } else if (ctx.typeAlias() != null) {
            TypedefDecl td = buildTypedef(ctx.typeAlias());
            if (td != null) {
                lib.typedefs.add(td);
            }
        } else if (ctx.getterSignature() != null && ctx.functionBody() != null) {
            // top-level getter: `T get x => ...;` -> a zero-arg static method on the library class
            FunctionDecl fn = new FunctionDecl();
            pos(fn, ctx);
            fn.name = ctx.getterSignature().identifier().getText();
            fn.returnType = ctx.getterSignature().type() != null
                    ? buildType(ctx.getterSignature().type()) : TypeRef.DYNAMIC;
            fn.isGetter = true;
            fn.javaName = javaName;
            buildFunctionBodyInto(ctx.functionBody(), fn);
            lib.functions.add(fn);
        } else if (ctx.setterSignature() != null && ctx.functionBody() != null) {
            // top-level setter: `set x(v) { ... }` -> a static void method on the library class
            FunctionDecl fn = new FunctionDecl();
            pos(fn, ctx);
            fn.name = ctx.setterSignature().identifier().getText();
            fn.returnType = TypeRef.VOID;
            fn.isSetter = true;
            fn.javaName = javaName;
            buildParams(ctx.setterSignature().formalParameterList(), fn.params);
            buildFunctionBodyInto(ctx.functionBody(), fn);
            lib.functions.add(fn);
        } else {
            unsupported(ctx, "E0103", "Unsupported top-level declaration: " + snippet(ctx));
        }
    }

    /**
     * `extension type Id(int value) implements I { ... }`. An extension type is erased to its
     * representation at run time, so it is lowered to an ordinary extension on the
     * representation type, whose members are then dispatched statically like any other
     * extension's, plus:
     * <ul>
     * <li>a getter for the representation field answering {@code this};</li>
     * <li>a top-level function named after the type for the primary constructor, returning
     *     its argument -- so {@code Id(3)} allocates nothing;</li>
     * <li>a static method of the extension for each named constructor or factory, whose
     *     result is the representation value an initializer, a redirect or a body gives.</li>
     * </ul>
     * Every type that names the extension type is erased to the representation once all
     * libraries are known (Program.eraseExtensionTypes); {@code implements} needs nothing,
     * because the representation's own members and the extensions of the extension types it
     * implements are found on the erased type.
     */
    private void buildExtensionType(Dart2Parser.ExtensionTypeDeclarationContext et, String javaName, Library lib) {
        ClassDecl cd = new ClassDecl();
        pos(cd, et);
        cd.name = et.typeIdentifier().getText();
        cd.javaName = javaName;
        cd.isExtensionType = true;
        List<Dart2Parser.IdentifierContext> ids = et.identifier();
        // ids: [`type`, optional primary constructor name, representation field name]
        String repName = ids.get(ids.size() - 1).getText();
        String primaryName = ids.size() > 2 ? ids.get(1).getText() : null;
        TypeRef rep = buildType(et.type());
        cd.extensionOn = rep;
        if (et.interfaces() != null) {
            for (Dart2Parser.TypeNotVoidContext t : et.interfaces().typeNotVoidList().typeNotVoid()) {
                cd.interfaces.add(buildTypeNotVoid(t));
            }
        }
        if (et.typeParameters() != null) {
            for (Dart2Parser.TypeParameterContext tp : et.typeParameters().typeParameter()) {
                cd.typeParams.add(tp.identifier().getText());
                if (tp.typeNotVoid() != null) {
                    cd.typeParamBounds.put(tp.identifier().getText(), buildTypeNotVoid(tp.typeNotVoid()));
                }
            }
        }
        java.util.List<Dart2Parser.ClassMemberDeclarationContext> members = et.classMemberDeclaration();
        java.util.List<Dart2Parser.MetadataContext> metas = et.metadata();
        // metadata(0) annotates the representation field; the members' follow it.
        for (int i = 0; i < members.size(); i++) {
            buildMember(members.get(i), metas.size() > i + 1 ? metas.get(i + 1) : null, cd);
        }
        MethodDecl getter = new MethodDecl();
        pos(getter, et);
        getter.name = repName;
        getter.isGetter = true;
        getter.returnType = rep;
        ThisExpr self = new ThisExpr();
        pos(self, et);
        getter.exprBody = self;
        cd.methods.add(getter);
        // The type the constructors return: `Box<T>`, erased with every other mention.
        TypeRef selfType = new TypeRef(cd.name);
        pos(selfType, et);
        for (String tp : cd.typeParams) {
            selfType.args.add(new TypeRef(tp));
        }
        lib.namedTypes.add(selfType);
        Param p = new Param();
        p.name = repName;
        p.type = rep;
        if (primaryName == null) {
            FunctionDecl fn = new FunctionDecl();
            pos(fn, et);
            fn.name = cd.name;
            fn.returnType = selfType;
            fn.typeParams.addAll(cd.typeParams);
            fn.typeParamBounds.putAll(cd.typeParamBounds);
            fn.params.add(p);
            fn.exprBody = ident(repName, et);
            lib.functions.add(fn);
        } else {
            MethodDecl m = new MethodDecl();
            pos(m, et);
            m.name = primaryName;
            m.isStatic = true;
            m.returnType = selfType;
            m.typeParams.addAll(cd.typeParams);
            m.params.add(p);
            m.exprBody = ident(repName, et);
            cd.methods.add(m);
        }
        for (CtorDecl c : cd.ctors) {
            MethodDecl m = new MethodDecl();
            m.file = c.file;
            m.line = c.line;
            m.col = c.col;
            m.name = c.name == null ? "new" : c.name;
            m.isStatic = true;
            m.returnType = selfType;
            m.typeParams.addAll(cd.typeParams);
            for (Param cp : c.params) {
                if (cp.isThis) {
                    // `Id.of(this.value)`: the parameter is the representation.
                    cp.isThis = false;
                    if (cp.type == null || cp.type.is("var")) {
                        cp.type = rep;
                    }
                }
                m.params.add(cp);
            }
            Expr result = null;
            for (Param cp : c.params) {
                if (cp.name.equals(repName)) {
                    result = ident(repName, et);
                }
            }
            for (FieldInit fi : c.fieldInits) {
                if (repName.equals(fi.field)) {
                    result = fi.value;
                }
            }
            if (c.redirect != null) {
                Call call = new Call();
                call.file = c.file;
                call.line = c.line;
                call.col = c.col;
                if (c.redirect.namedCtor == null) {
                    call.name = primaryName == null ? cd.name : primaryName;
                    if (primaryName != null) {
                        call.target = ident(cd.name, et);
                    }
                } else {
                    call.target = ident(cd.name, et);
                    call.name = c.redirect.namedCtor;
                }
                call.args = c.redirect.args;
                result = call;
            }
            if (c.isFactory && c.body != null && c.body.statements.size() == 1
                    && c.body.statements.get(0) instanceof ReturnStmt
                    && ((ReturnStmt) c.body.statements.get(0)).value instanceof CtorCall
                    && ((CtorCall) ((ReturnStmt) c.body.statements.get(0)).value).type.name.equals(cd.name)) {
                // A redirecting factory (`factory Id.of(int v) = Id;` / `= Id.named;`):
                // buildRedirectingFactory wrote `return new Id(v)`, but an extension type has
                // no class to construct -- its constructors are the static functions made
                // here, so the redirect calls the one it names.
                ReturnStmt ret = (ReturnStmt) c.body.statements.get(0);
                CtorCall cc = (CtorCall) ret.value;
                Call call = new Call();
                call.file = c.file;
                call.line = c.line;
                call.col = c.col;
                if (cc.ctorName == null && primaryName == null) {
                    call.name = cd.name;
                } else {
                    call.target = ident(cd.name, et);
                    call.name = cc.ctorName == null ? primaryName : cc.ctorName;
                }
                call.args = cc.args;
                ret.value = call;
            }
            if (c.isFactory || c.body != null && result == null) {
                // A factory's body already returns the value; a redirecting factory
                // (`factory Id.of(int v) = Id;`) has none and is not lowered.
                m.body = c.body;
            }
            if (m.body == null && m.exprBody == null) {
                if (result == null) {
                    result = new NullLit();
                }
                m.exprBody = result;
            }
            cd.methods.add(m);
        }
        cd.ctors.clear();
        hoistStaticFields(cd, lib);
        lib.classes.add(cd);
    }

    /**
     * An extension's static fields are top-level variables of its library under a name no
     * Dart declaration can spell (`Id$count`; `_Id$count` when either name is private, so
     * the library-privacy pass separates it like any private). Both are lazily initialized
     * globals in Dart, so a top-level variable gives `static int n = f();`, `const`, `final`,
     * assignment, compound assignment and `++` their exact semantics. The references are
     * rewritten in Program.lowerExtensionStatics, once every extension is known.
     */
    private static void hoistStaticFields(ClassDecl cd, Library lib) {
        for (java.util.Iterator<FieldDecl> it = cd.fields.iterator(); it.hasNext();) {
            FieldDecl f = it.next();
            if (!f.isStatic) {
                continue;
            }
            it.remove();
            boolean priv = cd.name.startsWith("_") || f.name.startsWith("_");
            String mangled = (priv ? "_" : "") + stripUnderscores(cd.name) + "$" + stripUnderscores(f.name);
            cd.hoistedStatics.put(f.name, mangled);
            f.name = mangled;
            f.isStatic = false;
            FieldDecl[] grown = java.util.Arrays.copyOf(cd.hoistedFields, cd.hoistedFields.length + 1);
            grown[cd.hoistedFields.length] = f;
            cd.hoistedFields = grown;
            lib.topLevelVars.add(f);
        }
    }

    private static String stripUnderscores(String s) {
        int i = 0;
        while (i < s.length() - 1 && s.charAt(i) == '_') {
            i++;
        }
        return s.substring(i);
    }

    private Ident ident(String name, org.antlr.v4.runtime.ParserRuleContext at) {
        Ident id = new Ident();
        pos(id, at);
        id.name = name;
        return id;
    }

    private TypedefDecl buildTypedef(Dart2Parser.TypeAliasContext ctx) {
        TypedefDecl td = new TypedefDecl();
        pos(td, ctx);
        if (ctx.functionTypeAlias() != null) {
            // old-style: typedef ReturnType Name(params);
            Dart2Parser.FunctionTypeAliasContext fta = ctx.functionTypeAlias();
            td.name = fta.functionPrefix().identifier().getText();
            td.returnType = fta.functionPrefix().type() != null
                    ? buildType(fta.functionPrefix().type()) : TypeRef.DYNAMIC;
            td.paramTypes = new java.util.ArrayList<TypeRef>();
            if (fta.formalParameterPart() != null
                    && fta.formalParameterPart().formalParameterList() != null) {
                java.util.List<Param> ps = new java.util.ArrayList<Param>();
                buildParams(fta.formalParameterPart().formalParameterList(), ps);
                int[] req = {-1};
                @SuppressWarnings("unchecked") List<String>[] rn$ = new List[1];
        td.funcNamed = shapeFromParams(ps, td.paramTypes, req, rn$);
        td.funcNamedRequired = rn$[0];
                td.funcRequired = req[0];
            }
            return td;
        }
        // new-style: typedef Name<T> = <type>;
        td.name = ctx.typeIdentifier().getText();
        if (ctx.typeParameters() != null) {
            for (Dart2Parser.TypeParameterContext tp : ctx.typeParameters().typeParameter()) {
                td.typeParams.add(tp.identifier().getText());
            }
        }
        Dart2Parser.TypeContext type = ctx.type();
        if (type != null && type.functionType() != null) {
            fillFunctionTypedef(td, type.functionType());
        } else if (type != null) {
            td.aliased = buildType(type);
        } else {
            td.aliased = TypeRef.DYNAMIC;
        }
        return td;
    }

    /** Extracts the (paramTypes, returnType) signature of a {@code Ret Function(A, B)} type. */
    private void fillFunctionTypedef(TypedefDecl td, Dart2Parser.FunctionTypeContext ft) {
        td.returnType = ft.typeNotFunction() != null
                ? buildTypeNotFunction(ft.typeNotFunction()) : TypeRef.DYNAMIC;
        td.paramTypes = new java.util.ArrayList<TypeRef>();
        int[] req = {-1};
        @SuppressWarnings("unchecked") List<String>[] rn$ = new List[1];
        td.funcNamed = parameterTypeShape(ft, td.paramTypes, req, rn$);
        td.funcNamedRequired = rn$[0];
        td.funcRequired = req[0];
    }

    /**
     * Collects every parameter type of a {@code Ret Function(...)} type into {@code out}:
     * required positional, then optional positional, then named. {@code required[0]} gets
     * the required count (-1 when nothing is optional); the named names are returned.
     */
    private List<String> parameterTypeShape(Dart2Parser.FunctionTypeContext ft, List<TypeRef> out,
            int[] required, List<String>[] requiredNamed) {
        Dart2Parser.FunctionTypeTailsContext tails = ft.functionTypeTails();
        if (tails == null || tails.functionTypeTail() == null) {
            return null;
        }
        Dart2Parser.ParameterTypeListContext ptl = tails.functionTypeTail().parameterTypeList();
        if (ptl == null) {
            return null;
        }
        if (ptl.normalParameterTypes() != null) {
            addNormalParameterTypes(ptl.normalParameterTypes(), out);
        }
        Dart2Parser.OptionalParameterTypesContext opt = ptl.optionalParameterTypes();
        if (opt == null) {
            return null;
        }
        required[0] = out.size();
        if (opt.optionalPositionalParameterTypes() != null) {
            addNormalParameterTypes(opt.optionalPositionalParameterTypes().normalParameterTypes(), out);
            return null;
        }
        List<String> names = new ArrayList<String>();
        for (Dart2Parser.NamedParameterTypeContext n : opt.namedParameterTypes().namedParameterType()) {
            Dart2Parser.TypedIdentifierContext ti = n.typedIdentifier();
            out.add(ti.type() != null ? buildType(ti.type()) : TypeRef.DYNAMIC);
            names.add(ti.identifier().getText());
            if (n.REQUIRED_() != null) {
                if (requiredNamed[0] == null) {
                    requiredNamed[0] = new ArrayList<String>();
                }
                requiredNamed[0].add(ti.identifier().getText());
            }
        }
        return names;
    }

    private void addNormalParameterTypes(Dart2Parser.NormalParameterTypesContext npts, List<TypeRef> out) {
        for (Dart2Parser.NormalParameterTypeContext npt : npts.normalParameterType()) {
            if (npt.type() != null) {
                out.add(buildType(npt.type()));
            } else if (npt.typedIdentifier() != null && npt.typedIdentifier().type() != null) {
                out.add(buildType(npt.typedIdentifier().type()));
            } else {
                out.add(TypeRef.DYNAMIC);
            }
        }
    }

    /** The same shape as {@link #parameterTypeShape} from a declared parameter list. */
    private static List<String> shapeFromParams(List<Param> ps, List<TypeRef> out, int[] required,
            List<String>[] requiredNamed) {
        List<String> names = null;
        for (Param p : ps) {
            if (!p.named && !p.optional && p.defaultValue == null) {
                out.add(p.type == null || p.type == TypeRef.VAR ? TypeRef.DYNAMIC : p.type);
            }
        }
        int req = out.size();
        for (Param p : ps) {
            if (!p.named && (p.optional || p.defaultValue != null)) {
                out.add(p.type == null || p.type == TypeRef.VAR ? TypeRef.DYNAMIC : p.type);
            }
        }
        for (Param p : ps) {
            if (p.named) {
                out.add(p.type == null || p.type == TypeRef.VAR ? TypeRef.DYNAMIC : p.type);
                if (names == null) {
                    names = new ArrayList<String>();
                }
                names.add(p.name);
                if (p.required) {
                    if (requiredNamed[0] == null) {
                        requiredNamed[0] = new ArrayList<String>();
                    }
                    requiredNamed[0].add(p.name);
                }
            }
        }
        required[0] = req == out.size() ? -1 : req;
        return names;
    }

    private ClassDecl buildClass(Dart2Parser.ClassDeclarationContext ctx) {
        if (ctx.mixinApplicationClass() != null) {
            return buildMixinApplicationClass(ctx, ctx.mixinApplicationClass());
        }
        ClassDecl cd = new ClassDecl();
        pos(cd, ctx);
        cd.name = ctx.typeIdentifier().getText();
        // NB: classModifiers is a (X)* rule, so each accessor returns a LIST —
        // `!= null` would be true even when the modifier is absent.
        Dart2Parser.ClassModifiersContext mods = ctx.classModifiers();
        cd.isAbstract = mods != null && !mods.ABSTRACT_().isEmpty();
        cd.isSealed = mods != null && !mods.SEALED_().isEmpty();
        if (cd.isSealed) {
            // a sealed class is implicitly abstract in Dart
            cd.isAbstract = true;
        }
        if (ctx.typeParameters() != null) {
            for (Dart2Parser.TypeParameterContext tp : ctx.typeParameters().typeParameter()) {
                cd.typeParams.add(tp.identifier().getText());
                if (tp.typeNotVoid() != null) {
                    cd.typeParamBounds.put(tp.identifier().getText(), buildTypeNotVoid(tp.typeNotVoid()));
                }
            }
        }
        if (ctx.superclass() != null) {
            if (ctx.superclass().mixins() != null) {
                for (Dart2Parser.TypeNotVoidContext t : ctx.superclass().mixins().typeNotVoidList().typeNotVoid()) {
                    cd.mixins.add(buildTypeNotVoid(t));
                }
            }
            if (ctx.superclass().typeNotVoid() != null) {
                cd.superclass = buildTypeNotVoid(ctx.superclass().typeNotVoid());
            }
        }
        if (ctx.interfaces() != null) {
            for (Dart2Parser.TypeNotVoidContext t : ctx.interfaces().typeNotVoidList().typeNotVoid()) {
                cd.interfaces.add(buildTypeNotVoid(t));
            }
        }
        List<Dart2Parser.ClassMemberDeclarationContext> members = ctx.classMemberDeclaration();
        List<Dart2Parser.MetadataContext> metas = ctx.metadata();
        for (int i = 0; i < members.size(); i++) {
            buildMember(members.get(i), metas.size() > i ? metas.get(i) : null, cd);
        }
        // `abstract mixin class M implements List<E>` with no superclass and no constructor is,
        // for every use but `extends M`, exactly `mixin M`: it can never be instantiated, and a
        // class applies it `with M` on top of its own superclass. Treated as a mixin, it gets
        // the mixin emission (an interface with default methods) that such an application
        // needs; `extends M` is normalized to `with M` by Program.
        if (cd.isAbstract && ctx.MIXIN_() != null && cd.superclass == null && cd.mixins.isEmpty()
                && cd.ctors.isEmpty()) {
            cd.isMixin = true;
        }
        return cd;
    }

    /**
     * {@code class C<T> = S with M1, M2 implements I;} is the class {@code C<T> extends S with
     * M1, M2 implements I {}} whose constructors forward to S's. The forwarding constructors
     * need S's declaration, so Program synthesizes them once every library is registered.
     */
    private ClassDecl buildMixinApplicationClass(Dart2Parser.ClassDeclarationContext outer,
            Dart2Parser.MixinApplicationClassContext ctx) {
        ClassDecl cd = new ClassDecl();
        pos(cd, ctx);
        cd.name = ctx.identifier().getText();
        Dart2Parser.ClassModifiersContext mods = outer.classModifiers();
        cd.isAbstract = mods != null && !mods.ABSTRACT_().isEmpty();
        cd.isSealed = mods != null && !mods.SEALED_().isEmpty();
        if (cd.isSealed) {
            cd.isAbstract = true;
        }
        if (ctx.typeParameters() != null) {
            for (Dart2Parser.TypeParameterContext tp : ctx.typeParameters().typeParameter()) {
                cd.typeParams.add(tp.identifier().getText());
                if (tp.typeNotVoid() != null) {
                    cd.typeParamBounds.put(tp.identifier().getText(), buildTypeNotVoid(tp.typeNotVoid()));
                }
            }
        }
        Dart2Parser.MixinApplicationContext app = ctx.mixinApplication();
        cd.superclass = buildTypeNotVoid(app.typeNotVoid());
        for (Dart2Parser.TypeNotVoidContext t : app.mixins().typeNotVoidList().typeNotVoid()) {
            cd.mixins.add(buildTypeNotVoid(t));
        }
        if (app.interfaces() != null) {
            for (Dart2Parser.TypeNotVoidContext t : app.interfaces().typeNotVoidList().typeNotVoid()) {
                cd.interfaces.add(buildTypeNotVoid(t));
            }
        }
        cd.forwardSuperCtors = true;
        return cd;
    }

    private EnumDecl buildEnum(Dart2Parser.EnumTypeContext ctx) {
        EnumDecl ed = new EnumDecl();
        pos(ed, ctx);
        ed.name = ctx.identifier().getText();
        for (Dart2Parser.EnumEntryContext e : ctx.enumEntry()) {
            // Dart 2.17 enhanced enums: the constant may carry constructor arguments
            // (`material('...')`) or a named ctor (`x.named(...)`); we keep only the
            // constant's own name. The first identifier is the constant.
            ed.entries.add(e.identifier(0).getText());
            Args entryArgs = null;
            String entryCtor = null;
            if (e.arguments() != null) {
                entryArgs = new Args();
                buildArgs(e.arguments(), entryArgs);
                if (e.identifier().size() == 2) {
                    entryCtor = e.identifier(1).getText();
                }
            }
            ed.entryArgs.add(entryArgs);
            ed.entryCtors.add(entryCtor);
        }
        // Enhanced-enum body: methods / getters, fields and constructors declared after the
        // trailing `;`. These share the class-member grammar, so build them into a throwaway
        // ClassDecl and lift the parsed members onto the enum.
        List<Dart2Parser.ClassMemberDeclarationContext> members = ctx.classMemberDeclaration();
        if (members != null && !members.isEmpty()) {
            List<Dart2Parser.MetadataContext> metas = ctx.metadata();
            ClassDecl holder = new ClassDecl();
            holder.name = ed.name;
            for (int i = 0; i < members.size(); i++) {
                buildMember(members.get(i), metas.size() > i ? metas.get(i) : null, holder);
            }
            ed.fields.addAll(holder.fields);
            ed.methods.addAll(holder.methods);
            ed.ctors.addAll(holder.ctors);
        }
        return ed;
    }

    /**
     * A stub's {@code @Unapplied('a, b')} names the parameters of the constructor or method
     * below it that are accepted and not applied, so a call passing one is reported (see
     * {@code JavaEmitter.warnUnapplied}) instead of silently doing less than it says.
     */
    private void buildMember(Dart2Parser.ClassMemberDeclarationContext ctx,
                             Dart2Parser.MetadataContext meta, ClassDecl cd) {
        int ctors = cd.ctors.size();
        int methods = cd.methods.size();
        buildMemberDecl(ctx, meta, cd);
        String unapplied = annotationArg(meta, "Unapplied");
        if (unapplied == null) {
            return;
        }
        java.util.Set<String> names = new java.util.LinkedHashSet<String>();
        for (String n : unapplied.split(",")) {
            if (!n.trim().isEmpty()) {
                names.add(n.trim());
            }
        }
        for (int i = ctors; i < cd.ctors.size(); i++) {
            cd.ctors.get(i).unapplied = names;
        }
        for (int i = methods; i < cd.methods.size(); i++) {
            cd.methods.get(i).unapplied = names;
        }
    }

    private void buildMemberDecl(Dart2Parser.ClassMemberDeclarationContext ctx,
                             Dart2Parser.MetadataContext meta, ClassDecl cd) {
        boolean override = hasAnnotation(meta, "override");
        if (ctx.methodSignature() != null) {
            buildMethodWithBody(ctx.methodSignature(), ctx.functionBody(), override, cd);
            return;
        }
        Dart2Parser.DeclarationContext d = ctx.declaration();
        if (d == null) {
            return;
        }
        if (d.constructorSignature() != null || d.constantConstructorSignature() != null) {
            // constructor without body (ends in ';')
            CtorDecl ctor = new CtorDecl();
            pos(ctor, d);
            Dart2Parser.ConstructorSignatureContext sig;
            if (d.constantConstructorSignature() != null) {
                ctor.isConst = true;
                sig = null;
                buildCtorName(d.constantConstructorSignature().constructorName(), ctor, cd);
                buildParams(d.constantConstructorSignature().formalParameterList(), ctor.params);
            } else {
                sig = d.constructorSignature();
                buildCtorName(sig.constructorName(), ctor, cd);
                buildParams(sig.formalParameterList(), ctor.params);
            }
            if (d.initializers() != null) {
                buildInitializers(d.initializers(), ctor);
            }
            if (d.redirection() != null) {
                Dart2Parser.RedirectionContext r = d.redirection();
                SuperInit target = new SuperInit();
                pos(target, r);
                target.namedCtor = r.identifier() != null ? r.identifier().getText() : null;
                buildArgs(r.arguments(), target.args);
                ctor.redirect = target;
            }
            cd.ctors.add(ctor);
            return;
        }
        if (d.redirectingFactoryConstructorSignature() != null) {
            cd.ctors.add(buildRedirectingFactory(d.redirectingFactoryConstructorSignature(), cd));
            return;
        }
        if (d.factoryConstructorSignature() != null) {
            // external/abstract factory (no body) — only meaningful in stubs
            CtorDecl ctor = new CtorDecl();
            pos(ctor, d);
            ctor.isFactory = true;
            buildCtorName(d.factoryConstructorSignature().constructorName(), ctor, cd);
            buildParams(d.factoryConstructorSignature().formalParameterList(), ctor.params);
            cd.ctors.add(ctor);
            return;
        }
        if (d.operatorSignature() != null) {
            // abstract or external operator overload (e.g. `external Offset operator +(Offset o);`)
            Dart2Parser.OperatorSignatureContext op = d.operatorSignature();
            String mangled = mangleOperator(op.operator().getText());
            if (mangled == null) {
                unsupported(d, "E0109", "Unsupported operator overload: " + op.operator().getText());
                return;
            }
            MethodDecl m = new MethodDecl();
            pos(m, d);
            m.isAbstract = true;
            m.isOverride = override;
            m.name = mangled;
            m.returnType = op.type() != null ? buildType(op.type()) : TypeRef.VAR;
            buildParams(op.formalParameterList(), m.params);
            cd.methods.add(m);
            return;
        }
        if (d.functionSignature() != null || d.getterSignature() != null || d.setterSignature() != null) {
            // abstract or external member
            MethodDecl m = new MethodDecl();
            pos(m, d);
            m.isAbstract = true;
            m.isStatic = d.STATIC_() != null;
            m.isOverride = override;
            if (d.functionSignature() != null) {
                FunctionDecl tmp = new FunctionDecl();
                buildFunctionSignature(d.functionSignature(), tmp);
                // Grammar ambiguity: a body-less constructor matches
                // functionSignature first. `Vec(this.x, this.y);` arrives as
                // name==class with no return type; `Vec.unit(...);` as
                // returnType==class. Reclassify as constructors.
                if (!m.isStatic && tmp.name.equals(cd.name)
                        && (tmp.returnType == null || tmp.returnType.is("var"))) {
                    CtorDecl ctor = new CtorDecl();
                    pos(ctor, d);
                    ctor.params = tmp.params;
                    if (d.initializers() != null) {
                        buildInitializers(d.initializers(), ctor);
                    }
                    cd.ctors.add(ctor);
                    return;
                }
                // Only `Vec.unit(...)` is a named constructor: `Pipe<T> mapEach(...);` and
                // `Vec unit();` are abstract methods that merely return the class.
                if (!m.isStatic && tmp.returnType != null && tmp.returnType.is(cd.name)
                        && d.EXTERNAL_() == null
                        && d.functionSignature().getText().startsWith(cd.name + "." + tmp.name)) {
                    CtorDecl ctor = new CtorDecl();
                    pos(ctor, d);
                    ctor.name = tmp.name;
                    ctor.params = tmp.params;
                    if (d.initializers() != null) {
                        buildInitializers(d.initializers(), ctor);
                    }
                    cd.ctors.add(ctor);
                    return;
                }
                m.name = tmp.name;
                m.returnType = tmp.returnType;
                m.params = tmp.params;
                m.typeParams = tmp.typeParams;
                m.typeParamBounds = tmp.typeParamBounds;
            } else if (d.getterSignature() != null) {
                m.isGetter = true;
                m.name = d.getterSignature().identifier().getText();
                m.returnType = d.getterSignature().type() != null ? buildType(d.getterSignature().type()) : TypeRef.DYNAMIC;
            } else {
                m.isSetter = true;
                m.name = d.setterSignature().identifier().getText();
                m.returnType = TypeRef.VOID;
                buildParams(d.setterSignature().formalParameterList(), m.params);
            }
            cd.methods.add(m);
            return;
        }
        // field declarations
        if (d.initializedIdentifierList() != null || d.staticFinalDeclarationList() != null) {
            boolean isStatic = d.STATIC_() != null;
            boolean isFinal = d.FINAL_() != null;
            boolean isConst = d.CONST_() != null;
            boolean isLate = d.LATE_() != null;
            TypeRef type = TypeRef.VAR;
            if (d.type() != null) {
                type = buildType(d.type());
            } else if (d.varOrType() != null && d.varOrType().type() != null) {
                type = buildType(d.varOrType().type());
            }
            if (d.initializedIdentifierList() != null && d.ABSTRACT_() != null && !isStatic) {
                // `abstract final T x;` declares no storage: it is an abstract
                // getter (plus an abstract setter unless final). Emitting a field
                // made a mixin's abstract field collide with the implementing
                // class's real one when the mixin was applied.
                for (Dart2Parser.InitializedIdentifierContext ii : d.initializedIdentifierList().initializedIdentifier()) {
                    MethodDecl g = new MethodDecl();
                    pos(g, ii);
                    g.isGetter = true;
                    g.isAbstract = true;
                    g.name = ii.identifier().getText();
                    g.returnType = type;
                    cd.methods.add(g);
                    if (!isFinal) {
                        MethodDecl st = new MethodDecl();
                        pos(st, ii);
                        st.isSetter = true;
                        st.isAbstract = true;
                        st.name = g.name;
                        st.returnType = TypeRef.VOID;
                        Param p = new Param();
                        pos(p, ii);
                        p.name = "value";
                        p.type = type;
                        st.params.add(p);
                        cd.methods.add(st);
                    }
                }
                return;
            }
            if (d.initializedIdentifierList() != null) {
                for (Dart2Parser.InitializedIdentifierContext ii : d.initializedIdentifierList().initializedIdentifier()) {
                    FieldDecl f = new FieldDecl();
                    pos(f, ii);
                    f.name = ii.identifier().getText();
                    f.type = type;
                    f.isFinal = isFinal;
                    f.isConst = isConst;
                    f.isStatic = isStatic;
                    f.isLate = isLate;
                    if (ii.expr() != null) {
                        f.initializer = buildExpr(ii.expr());
                        if (isConst) {
                            markConstContext(f.initializer);
                        }
                    }
                    cd.fields.add(f);
                }
            } else {
                for (Dart2Parser.StaticFinalDeclarationContext sf : d.staticFinalDeclarationList().staticFinalDeclaration()) {
                    FieldDecl f = new FieldDecl();
                    pos(f, sf);
                    f.name = sf.identifier().getText();
                    f.type = type;
                    f.isFinal = true;
                    f.isConst = isConst;
                    f.isStatic = isStatic;
                    f.initializer = buildExpr(sf.expr());
                    if (isConst) {
                        markConstContext(f.initializer);
                    }
                    cd.fields.add(f);
                }
            }
            return;
        }
        unsupported(d, "E0108", "Unsupported class member: " + snippet(d));
    }

    private void buildCtorName(Dart2Parser.ConstructorNameContext name, CtorDecl ctor, ClassDecl cd) {
        // constructorName : typeIdentifier (D identifier)?
        if (name.identifier() != null) {
            ctor.name = name.identifier().getText();
        }
    }

    /// True when a `constructorSignature` with a body is really an untyped method: a bare
    /// name (no `.named` part) that is not the enclosing class's name, and no initializer
    /// list (which only a constructor may have).
    private static boolean untypedMethodNotCtor(Dart2Parser.ConstructorNameContext name,
                                                Dart2Parser.MethodSignatureContext sig, ClassDecl cd) {
        return name.identifier() == null && cd.name != null && sig.initializers() == null
                && !name.typeIdentifier().getText().equals(cd.name);
    }

    private void buildMethodWithBody(Dart2Parser.MethodSignatureContext sig,
                                     Dart2Parser.FunctionBodyContext body,
                                     boolean override, ClassDecl cd) {
        if (sig.constructorSignature() != null
                && untypedMethodNotCtor(sig.constructorSignature().constructorName(), sig, cd)) {
            // Grammar ambiguity: `toString() => 'x';` (no declared return type) matches
            // constructorSignature, whose typeIdentifier is any name. Only the class's own
            // name opens a constructor; anything else is a method whose return type is
            // left to inference (the override's, else dynamic) -- TypeRef.VAR.
            Dart2Parser.ConstructorSignatureContext cs = sig.constructorSignature();
            MethodDecl m = new MethodDecl();
            pos(m, sig);
            m.isStatic = sig.STATIC_() != null;
            m.isOverride = override;
            m.name = cs.constructorName().typeIdentifier().getText();
            m.returnType = TypeRef.VAR;
            buildParams(cs.formalParameterList(), m.params);
            buildFunctionBodyIntoMethod(body, m);
            cd.methods.add(m);
            return;
        }
        if (sig.constructorSignature() != null) {
            CtorDecl ctor = new CtorDecl();
            pos(ctor, sig);
            buildCtorName(sig.constructorSignature().constructorName(), ctor, cd);
            buildParams(sig.constructorSignature().formalParameterList(), ctor.params);
            if (sig.initializers() != null) {
                buildInitializers(sig.initializers(), ctor);
            }
            ctor.body = buildBodyBlock(body);
            cd.ctors.add(ctor);
            return;
        }
        if (sig.factoryConstructorSignature() != null) {
            CtorDecl ctor = new CtorDecl();
            pos(ctor, sig);
            ctor.isFactory = true;
            buildCtorName(sig.factoryConstructorSignature().constructorName(), ctor, cd);
            buildParams(sig.factoryConstructorSignature().formalParameterList(), ctor.params);
            ctor.body = buildBodyBlock(body);
            // `factory X.f(...) => expr;` returns expr; a generative constructor's arrow
            // body is a statement, which is how buildBodyBlock reads every arrow body.
            if (body != null && body.expr() != null && ctor.body != null
                    && ctor.body.statements.size() == 1 && ctor.body.statements.get(0) instanceof ExprStmt) {
                ReturnStmt r = new ReturnStmt();
                pos(r, body);
                r.value = ((ExprStmt) ctor.body.statements.get(0)).expr;
                ctor.body.statements.set(0, r);
            }
            cd.ctors.add(ctor);
            return;
        }
        if (sig.operatorSignature() != null) {
            Dart2Parser.OperatorSignatureContext op = sig.operatorSignature();
            String mangled = mangleOperator(op.operator().getText());
            if (mangled == null) {
                unsupported(sig, "E0109", "Unsupported operator overload: " + op.operator().getText());
                return;
            }
            MethodDecl m = new MethodDecl();
            pos(m, sig);
            m.name = mangled;
            m.isOverride = override;
            m.returnType = op.type() != null ? buildType(op.type()) : TypeRef.VAR;
            buildParams(op.formalParameterList(), m.params);
            buildFunctionBodyIntoMethod(body, m);
            cd.methods.add(m);
            return;
        }
        MethodDecl m = new MethodDecl();
        pos(m, sig);
        m.isStatic = sig.STATIC_() != null;
        m.isOverride = override;
        if (sig.functionSignature() != null) {
            FunctionDecl tmp = new FunctionDecl();
            buildFunctionSignature(sig.functionSignature(), tmp);
            m.name = tmp.name;
            m.returnType = tmp.returnType;
            m.params = tmp.params;
            m.typeParams = tmp.typeParams;
                m.typeParamBounds = tmp.typeParamBounds;
        } else if (sig.getterSignature() != null) {
            m.isGetter = true;
            m.name = sig.getterSignature().identifier().getText();
            m.returnType = sig.getterSignature().type() != null ? buildType(sig.getterSignature().type()) : TypeRef.VAR;
        } else if (sig.setterSignature() != null) {
            m.isSetter = true;
            m.name = sig.setterSignature().identifier().getText();
            m.returnType = TypeRef.VOID;
            buildParams(sig.setterSignature().formalParameterList(), m.params);
        }
        buildFunctionBodyIntoMethod(body, m);
        cd.methods.add(m);
    }

    private void buildInitializers(Dart2Parser.InitializersContext inits, CtorDecl ctor) {
        for (Dart2Parser.InitializerListEntryContext e : inits.initializerListEntry()) {
            if (e.fieldInitializer() != null) {
                FieldInit fi = new FieldInit();
                pos(fi, e);
                fi.field = e.fieldInitializer().identifier().getText();
                Dart2Parser.InitializerExpressionContext ie = e.fieldInitializer().initializerExpression();
                if (ie.conditionalExpression() != null) {
                    fi.value = buildConditional(ie.conditionalExpression());
                } else if (ie.cascade() != null) {
                    fi.value = buildCascade(ie.cascade());
                } else {
                    fi.value = errExpr(ie);
                }
                ctor.fieldInits.add(fi);
            } else if (e.SUPER_() != null) {
                SuperInit si = new SuperInit();
                pos(si, e);
                if (e.identifier() != null) {
                    si.namedCtor = e.identifier().getText();
                }
                buildArgs(e.arguments(), si.args);
                ctor.superInit = si;
            } else if (e.assertion() != null) {
                // assert(...) is a debug-only runtime check with no bearing on transpiled
                // semantics; drop it silently (production Dart strips asserts too).
                continue;
            } else {
                unsupported(e, "E0111", "Unsupported initializer-list entry: " + snippet(e));
            }
        }
    }

    private void buildFunctionSignature(Dart2Parser.FunctionSignatureContext sig, FunctionDecl fn) {
        fn.returnType = sig.type() != null ? buildType(sig.type()) : TypeRef.VAR;
        fn.name = sig.identifier().getText();
        if (sig.formalParameterPart().typeParameters() != null) {
            for (Dart2Parser.TypeParameterContext tp
                    : sig.formalParameterPart().typeParameters().typeParameter()) {
                fn.typeParams.add(tp.identifier().getText());
                if (tp.typeNotVoid() != null) {
                    fn.typeParamBounds.put(tp.identifier().getText(), buildTypeNotVoid(tp.typeNotVoid()));
                }
            }
        }
        buildParams(sig.formalParameterPart().formalParameterList(), fn.params);
    }

    private void buildFunctionBodyInto(Dart2Parser.FunctionBodyContext body, FunctionDecl fn) {
        if (body.SYNC_() != null && body.ST() != null) {
            fn.isSyncStar = true;
        } else if (body.ASYNC_() != null && body.ST() != null && body.NATIVE_() == null) {
            fn.isAsyncStar = true;
        } else {
            fn.isAsync = checkBodyModifiers(body);
        }
        if (body.block() != null) {
            fn.body = buildBlock(body.block());
        } else if (body.expr() != null) {
            fn.exprBody = buildExpr(body.expr());
        }
    }

    private void buildFunctionBodyIntoMethod(Dart2Parser.FunctionBodyContext body, MethodDecl m) {
        if (body.SYNC_() != null && body.ST() != null) {
            // sync* generator: lowered by the emitter to a list-collecting body.
            m.isSyncStar = true;
        } else if (body.ASYNC_() != null && body.ST() != null && body.NATIVE_() == null) {
            // async* generator: lowered by the emitter onto dart.async.AsyncStar.
            m.isAsyncStar = true;
        } else {
            m.isAsync = checkBodyModifiers(body);
        }
        if (body.block() != null) {
            m.body = buildBlock(body.block());
        } else if (body.expr() != null) {
            m.exprBody = buildExpr(body.expr());
        }
    }

    /** Returns true when the body is async (plain `async`, not a generator). */
    private boolean checkBodyModifiers(Dart2Parser.FunctionBodyContext body) {
        if (body.ST() != null) {
            unsupported(body, "E0303", "async* generator bodies are not supported yet (M5)");
        }
        if (body.NATIVE_() != null) {
            unsupported(body, "E0113", "native bodies are not supported");
        }
        return body.ASYNC_() != null;
    }

    private void buildParams(Dart2Parser.FormalParameterListContext list, List<Param> out) {
        if (list == null) {
            return;
        }
        if (list.normalFormalParameters() != null) {
            for (Dart2Parser.NormalFormalParameterContext p : list.normalFormalParameters().normalFormalParameter()) {
                Param param = buildNormalParam(p.normalFormalParameterNoMetadata());
                if (param != null) {
                    out.add(param);
                }
            }
        }
        if (list.optionalOrNamedFormalParameters() != null) {
            Dart2Parser.OptionalOrNamedFormalParametersContext opt = list.optionalOrNamedFormalParameters();
            if (opt.namedFormalParameters() != null) {
                for (Dart2Parser.DefaultNamedParameterContext dn : opt.namedFormalParameters().defaultNamedParameter()) {
                    Param param = buildNormalParam(dn.normalFormalParameterNoMetadata());
                    if (param == null) {
                        continue;
                    }
                    param.named = true;
                    param.required = dn.REQUIRED_() != null;
                    if (dn.expr() != null) {
                        param.defaultValue = buildExpr(dn.expr());
                    }
                    out.add(param);
                }
            } else if (opt.optionalPositionalFormalParameters() != null) {
                for (Dart2Parser.DefaultFormalParameterContext dp
                        : opt.optionalPositionalFormalParameters().defaultFormalParameter()) {
                    Param param = buildNormalParam(dp.normalFormalParameter().normalFormalParameterNoMetadata());
                    if (param == null) {
                        continue;
                    }
                    param.optional = true;
                    if (dp.expr() != null) {
                        param.defaultValue = buildExpr(dp.expr());
                    }
                    out.add(param);
                }
            }
        }
    }

    private Param buildNormalParam(Dart2Parser.NormalFormalParameterNoMetadataContext ctx) {
        Param p = new Param();
        pos(p, ctx);
        if (ctx.fieldFormalParameter() != null) {
            Dart2Parser.FieldFormalParameterContext f = ctx.fieldFormalParameter();
            p.isThis = true;
            p.name = f.identifier().getText();
            p.type = f.finalConstVarOrType() != null ? buildFinalConstVarOrType(f.finalConstVarOrType()) : TypeRef.VAR;
            return p;
        }
        if (ctx.superFormalParameter() != null) {
            Dart2Parser.SuperFormalParameterContext f = ctx.superFormalParameter();
            p.isSuper = true;
            p.name = f.identifier().getText();
            p.type = f.finalConstVarOrType() != null ? buildFinalConstVarOrType(f.finalConstVarOrType()) : TypeRef.VAR;
            return p;
        }
        if (ctx.simpleFormalParameter() != null) {
            Dart2Parser.SimpleFormalParameterContext s = ctx.simpleFormalParameter();
            if (s.declaredIdentifier() != null) {
                p.name = s.declaredIdentifier().identifier().getText();
                p.type = buildFinalConstVarOrType(s.declaredIdentifier().finalConstVarOrType());
            } else {
                p.name = s.identifier().getText();
                p.type = TypeRef.VAR;
            }
            return p;
        }
        if (ctx.functionFormalParameter() != null) {
            // `int cb(String s)` is the older spelling of `int Function(String) cb`.
            Dart2Parser.FunctionFormalParameterContext f = ctx.functionFormalParameter();
            p.name = f.identifier().getText();
            p.type = functionTypeOf(f.type(), f.formalParameterPart(), f.QU() != null, f);
            return p;
        }
        return null;
    }

    /**
     * The inline function type a function-typed formal ({@code R name(params)}) declares.
     * Optional parameters are dropped from the signature, as an inline function type with
     * only positional parameters is what codegen renders as a Funcs SAM.
     */
    private TypeRef functionTypeOf(Dart2Parser.TypeContext ret, Dart2Parser.FormalParameterPartContext part,
            boolean nullable, ParserRuleContext at) {
        TypeRef t = new TypeRef("Function");
        pos(t, at);
        t.nullable = nullable;
        t.funcReturn = ret != null ? buildType(ret) : TypeRef.DYNAMIC;
        t.funcParams = new java.util.ArrayList<TypeRef>();
        List<Param> params = new ArrayList<Param>();
        buildParams(part.formalParameterList(), params);
        int[] req = {-1};
        @SuppressWarnings("unchecked") List<String>[] rn$ = new List[1];
        t.funcNamed = shapeFromParams(params, t.funcParams, req, rn$);
        t.funcNamedRequired = rn$[0];
        t.funcRequired = req[0];
        return t;
    }

    private TypeRef buildFinalConstVarOrType(Dart2Parser.FinalConstVarOrTypeContext ctx) {
        if (ctx.type() != null) {
            return buildType(ctx.type());
        }
        if (ctx.varOrType() != null && ctx.varOrType().type() != null) {
            return buildType(ctx.varOrType().type());
        }
        return TypeRef.VAR;
    }

    // ------------------------------------------------------------------
    // Types
    // ------------------------------------------------------------------

    private TypeRef buildType(Dart2Parser.TypeContext ctx) {
        if (ctx.functionType() != null) {
            // Inline function type (e.g. `void Function(int)`): preserve the signature so
            // codegen can render a real Funcs.* SAM instead of falling back to Object.
            TypeRef t = new TypeRef("Function");
            pos(t, ctx);
            t.nullable = ctx.QU() != null;
            fillFunctionSignature(t, ctx.functionType());
            return t;
        }
        return buildTypeNotFunction(ctx.typeNotFunction());
    }

    /** Captures the (paramTypes, returnType) of an inline {@code Ret Function(A, B)} type onto a TypeRef. */
    private void fillFunctionSignature(TypeRef t, Dart2Parser.FunctionTypeContext ft) {
        t.funcReturn = ft.typeNotFunction() != null
                ? buildTypeNotFunction(ft.typeNotFunction()) : TypeRef.DYNAMIC;
        t.funcParams = new java.util.ArrayList<TypeRef>();
        int[] req = {-1};
        @SuppressWarnings("unchecked") List<String>[] rn$ = new List[1];
        t.funcNamed = parameterTypeShape(ft, t.funcParams, req, rn$);
        t.funcNamedRequired = rn$[0];
        t.funcRequired = req[0];
        Dart2Parser.FunctionTypeTailsContext tails = ft.functionTypeTails();
        if (tails != null && tails.functionTypeTail() != null && tails.functionTypeTail().typeParameters() != null) {
            t.funcTypeParams = new ArrayList<String>();
            for (Dart2Parser.TypeParameterContext tp : tails.functionTypeTail().typeParameters().typeParameter()) {
                t.funcTypeParams.add(tp.identifier().getText());
            }
        }
    }

    private TypeRef buildTypeNotFunction(Dart2Parser.TypeNotFunctionContext ctx) {
        if (ctx == null) {
            return TypeRef.DYNAMIC;
        }
        if (ctx.VOID_() != null) {
            return TypeRef.VOID;
        }
        return buildTypeNotVoidNotFunction(ctx.typeNotVoidNotFunction());
    }

    private TypeRef buildTypeNotVoid(Dart2Parser.TypeNotVoidContext ctx) {
        if (ctx.functionType() != null) {
            TypeRef t = new TypeRef("Function");
            pos(t, ctx);
            fillFunctionSignature(t, ctx.functionType());
            return t;
        }
        return buildTypeNotVoidNotFunction(ctx.typeNotVoidNotFunction());
    }

    /**
     * dart:collection's concrete collections, as a type annotation, are the built-in Map,
     * Set and List: they are emitted as those (SplayTree* as sorted subclasses of them), so
     * every member the emitter knows on the built-in types works on a variable declared
     * with one of these names.
     */
    public static String collectionAlias(String name) {
        if (name.equals("HashMap") || name.equals("LinkedHashMap") || name.equals("SplayTreeMap")
                || name.equals("UnmodifiableMapView") || name.equals("MapView")) {
            return "Map";
        }
        if (name.equals("HashSet") || name.equals("LinkedHashSet") || name.equals("SplayTreeSet")) {
            return "Set";
        }
        if (name.equals("Queue") || name.equals("ListQueue") || name.equals("DoubleLinkedQueue")
                || name.equals("UnmodifiableListView")) {
            return "List";
        }
        return name;
    }

    private TypeRef buildTypeNotVoidNotFunction(Dart2Parser.TypeNotVoidNotFunctionContext ctx) {
        if (ctx == null) {
            // Error recovery (a syntax the grammar lacks, e.g. a record type after `as`)
            // can leave the type node out; the parser's E0001 already reports it.
            return TypeRef.DYNAMIC;
        }
        if (ctx.recordType() != null) {
            TypeRef t = buildRecordType(ctx.recordType());
            t.nullable = ctx.QU() != null;
            return t;
        }
        if (ctx.typeName() == null) {
            TypeRef t = new TypeRef("Function");
            pos(t, ctx);
            return t;
        }
        String writtenName = ctx.typeName().getText();
        TypeRef t = new TypeRef(collectionAlias(writtenName));
        if (!t.name.equals(writtenName) && currentLib != null) {
            // A program class named Queue may shadow the alias; decided once every
            // library is known (Program.restoreShadowedAliases).
            t.aliasedFrom = writtenName;
            currentLib.aliasedTypes.add(t);
        }
        if (currentLib != null) {
            currentLib.namedTypes.add(t);
        }
        pos(t, ctx);
        if (ctx.typeArguments() != null) {
            for (Dart2Parser.TypeContext a : ctx.typeArguments().typeList().type()) {
                t.args.add(buildType(a));
            }
        }
        t.nullable = ctx.QU() != null;
        return t;
    }

    /**
     * A Dart 3 record type becomes the structural shape class the emitter generates for
     * record literals: {@code Rec$<positional>$<sorted named...>}, component types as
     * type arguments in the same order (positional, then named sorted by name).
     */
    private TypeRef buildRecordType(Dart2Parser.RecordTypeContext ctx) {
        List<TypeRef> positional = new ArrayList<TypeRef>();
        for (Dart2Parser.RecordTypeFieldContext f : ctx.recordTypeField()) {
            positional.add(buildType(f.type()));
        }
        java.util.TreeMap<String, TypeRef> named = new java.util.TreeMap<String, TypeRef>();
        if (ctx.recordTypeNamedFields() != null) {
            for (Dart2Parser.RecordTypeNamedFieldContext f : ctx.recordTypeNamedFields().recordTypeNamedField()) {
                named.put(f.identifier().getText(), buildType(f.type()));
            }
        }
        StringBuilder n = new StringBuilder("Rec$").append(positional.size());
        for (String nm : named.keySet()) {
            n.append('$').append(nm);
        }
        TypeRef t = new TypeRef(n.toString());
        pos(t, ctx);
        t.args.addAll(positional);
        t.args.addAll(named.values());
        return t;
    }

    // ------------------------------------------------------------------
    // Statements
    // ------------------------------------------------------------------

    private Block buildBodyBlock(Dart2Parser.FunctionBodyContext body) {
        if (body == null) {
            return null;
        }
        checkBodyModifiers(body);
        if (body.block() != null) {
            return buildBlock(body.block());
        }
        if (body.expr() != null) {
            Block b = new Block();
            pos(b, body);
            ExprStmt es = new ExprStmt();
            pos(es, body);
            es.expr = buildExpr(body.expr());
            b.statements.add(es);
            return b;
        }
        return null;
    }

    private Block buildBlock(Dart2Parser.BlockContext ctx) {
        Block b = new Block();
        pos(b, ctx);
        if (ctx.statements() != null) {
            for (Dart2Parser.StatementContext s : ctx.statements().statement()) {
                Stmt st = buildStatement(s);
                if (st != null) {
                    b.statements.add(st);
                }
            }
        }
        return b;
    }

    private Stmt buildStatement(Dart2Parser.StatementContext ctx) {
        Stmt st = buildUnlabeledStatement(ctx);
        // `outer: for (...)` -- the labels are kept so `break outer;` / `continue outer;`
        // reach the statement they name (they were dropped, leaving a plain break that
        // left only the innermost loop).
        if (st != null && ctx != null && !ctx.label().isEmpty()) {
            st.labels = new ArrayList<String>();
            for (Dart2Parser.LabelContext l : ctx.label()) {
                st.labels.add(l.identifier().getText());
            }
        }
        return st;
    }

    private Stmt buildUnlabeledStatement(Dart2Parser.StatementContext ctx) {
        if (ctx == null) {
            return null;
        }
        Dart2Parser.NonLabelledStatementContext s = ctx.nonLabelledStatement();
        if (s == null) {
            // parser error-recovery can leave an empty statement node; a preceding
            // syntax (E0001) diagnostic already flags the real cause.
            return null;
        }
        if (s.block() != null) {
            return buildBlock(s.block());
        }
        if (s.localVariableDeclaration() != null) {
            return buildLocalVar(s.localVariableDeclaration());
        }
        if (s.expressionStatement() != null) {
            if (s.expressionStatement().expr() == null) {
                return null;
            }
            ExprStmt es = new ExprStmt();
            pos(es, s);
            es.expr = buildExpr(s.expressionStatement().expr());
            return es;
        }
        if (s.yieldStatement() != null) {
            YieldStmt y = new YieldStmt();
            pos(y, s);
            y.value = buildExpr(s.yieldStatement().expr());
            return y;
        }
        if (s.yieldEachStatement() != null) {
            YieldStmt y = new YieldStmt();
            pos(y, s);
            y.star = true;
            y.value = buildExpr(s.yieldEachStatement().expr());
            return y;
        }
        if (s.returnStatement() != null) {
            ReturnStmt r = new ReturnStmt();
            pos(r, s);
            if (s.returnStatement().expr() != null) {
                r.value = buildExpr(s.returnStatement().expr());
            }
            return r;
        }
        if (s.ifStatement() != null) {
            IfStmt i = new IfStmt();
            pos(i, s);
            i.condition = buildExpr(s.ifStatement().expr());
            // Dart 3 if-case: `if (expr case pattern [when guard]) ...`
            if (s.ifStatement().guardedPattern() != null) {
                Dart2Parser.GuardedPatternContext gp = s.ifStatement().guardedPattern();
                i.casePattern = buildPattern(gp.pattern());
                if (gp.WHEN_() != null && gp.expr() != null) {
                    i.caseGuard = buildExpr(gp.expr());
                }
            }
            i.thenStmt = buildStatement(s.ifStatement().statement(0));
            if (s.ifStatement().statement().size() > 1) {
                i.elseStmt = buildStatement(s.ifStatement().statement(1));
            }
            return i;
        }
        if (s.whileStatement() != null) {
            WhileStmt w = new WhileStmt();
            pos(w, s);
            w.condition = buildExpr(s.whileStatement().expr());
            w.body = buildStatement(s.whileStatement().statement());
            return w;
        }
        if (s.doStatement() != null) {
            WhileStmt w = new WhileStmt();
            pos(w, s);
            w.isDo = true;
            w.condition = buildExpr(s.doStatement().expr());
            w.body = buildStatement(s.doStatement().statement());
            return w;
        }
        if (s.forStatement() != null) {
            return buildFor(s.forStatement());
        }
        if (s.tryStatement() != null) {
            return buildTry(s.tryStatement());
        }
        if (s.breakStatement() != null) {
            BreakStmt b = new BreakStmt();
            pos(b, s);
            if (s.breakStatement().identifier() != null) {
                b.label = s.breakStatement().identifier().getText();
            }
            return b;
        }
        if (s.continueStatement() != null) {
            ContinueStmt c = new ContinueStmt();
            pos(c, s);
            if (s.continueStatement().identifier() != null) {
                c.label = s.continueStatement().identifier().getText();
            }
            return c;
        }
        if (s.switchStatement() != null) {
            return buildSwitch(s.switchStatement());
        }
        if (s.assertStatement() != null) {
            // assert(...) is a debug-only runtime check with no bearing on transpiled
            // semantics; drop it (production Dart strips asserts too), mirroring the
            // initializer-list assert case. Blocks skip null statements.
            return null;
        }
        if (s.localFunctionDeclaration() != null) {
            return buildLocalFunction(s.localFunctionDeclaration());
        }
        if (s.patternVariableDeclaration() != null) {
            Dart2Parser.PatternVariableDeclarationContext pd = s.patternVariableDeclaration();
            PatternDeclStmt d = new PatternDeclStmt();
            pos(d, s);
            Pattern p;
            if (pd.recordPattern() != null) {
                p = buildRecordPattern(pd.recordPattern());
            } else if (pd.listPattern() != null) {
                p = buildListPattern(pd.listPattern());
            } else if (pd.mapPattern() != null) {
                p = buildMapPattern(pd.mapPattern());
            } else {
                p = buildObjectPattern(pd.objectPattern());
            }
            d.pattern = p;
            d.init = buildExpr(pd.expr());
            return d;
        }
        if (s.patternAssignment() != null) {
            return buildPatternAssignment(s.patternAssignment(), s);
        }
        if (s.rethrowStatement() != null) {
            RethrowStmt r = new RethrowStmt();
            pos(r, s);
            return r;
        }
        unsupported(s, "E0115", "Unsupported statement: " + snippet(s));
        return null;
    }

    private int patternAssignCounter;

    /**
     * A pattern assignment, `(a, b) = (b, a + b);`. Dart evaluates the right-hand side,
     * destructures it completely, and only then assigns the variables, so the statement
     * becomes a destructuring declaration of fresh locals followed by plain assignments
     * from them -- which also keeps every assignment on the ordinary path (captured
     * variables, async frames, coercions).
     */
    private Stmt buildPatternAssignment(Dart2Parser.PatternAssignmentContext pa, ParserRuleContext at) {
        Pattern p;
        if (pa.recordPattern() != null) {
            p = buildRecordPattern(pa.recordPattern());
        } else if (pa.listPattern() != null) {
            p = buildListPattern(pa.listPattern());
        } else if (pa.objectPattern() != null) {
            p = buildObjectPattern(pa.objectPattern());
        } else {
            p = buildMapPattern(pa.mapPattern());
        }
        Block b = new Block();
        pos(b, at);
        PatternDeclStmt d = new PatternDeclStmt();
        pos(d, at);
        d.pattern = p;
        d.init = buildExpr(pa.expr());
        b.statements.add(d);
        List<VariablePattern> targets = new ArrayList<VariablePattern>();
        collectAssignTargets(p, targets);
        int n = patternAssignCounter++;
        for (VariablePattern v : targets) {
            String target = v.name;
            v.name = "$pa" + n + "$" + target;
            Ident lhs = new Ident();
            pos(lhs, at);
            lhs.name = target;
            Ident rhs = new Ident();
            pos(rhs, at);
            rhs.name = v.name;
            Assign as = new Assign();
            pos(as, at);
            as.lhs = lhs;
            as.op = "=";
            as.rhs = rhs;
            ExprStmt es = new ExprStmt();
            pos(es, at);
            es.expr = as;
            b.statements.add(es);
        }
        return b;
    }

    private static void collectAssignTargets(Pattern p, List<VariablePattern> out) {
        if (p instanceof VariablePattern) {
            if (!((VariablePattern) p).wildcard) {
                out.add((VariablePattern) p);
            }
        } else if (p instanceof RecordPattern) {
            for (PatternField f : ((RecordPattern) p).fields) {
                collectAssignTargets(f.pattern, out);
            }
        } else if (p instanceof ObjectPattern) {
            for (PatternField f : ((ObjectPattern) p).fields) {
                collectAssignTargets(f.pattern, out);
            }
        } else if (p instanceof ListPattern) {
            for (Pattern e : ((ListPattern) p).elements) {
                collectAssignTargets(e, out);
            }
        } else if (p instanceof MapPattern) {
            for (Pattern e : ((MapPattern) p).values) {
                collectAssignTargets(e, out);
            }
        } else if (p instanceof RestPattern) {
            if (((RestPattern) p).inner != null) {
                collectAssignTargets(((RestPattern) p).inner, out);
            }
        } else if (p instanceof NullCheckPattern) {
            collectAssignTargets(((NullCheckPattern) p).inner, out);
        } else if (p instanceof CastPattern) {
            collectAssignTargets(((CastPattern) p).inner, out);
        }
    }

    /** A nested function declaration: `Ret name(params) { ... }` inside a body. */
    private Stmt buildLocalFunction(Dart2Parser.LocalFunctionDeclarationContext ctx) {
        LocalFunc lf = new LocalFunc();
        pos(lf, ctx);
        Dart2Parser.FunctionSignatureContext sig = ctx.functionSignature();
        lf.returnType = sig.type() != null ? buildType(sig.type()) : TypeRef.VAR;
        lf.name = sig.identifier().getText();
        buildParams(sig.formalParameterPart().formalParameterList(), lf.params);
        if (sig.formalParameterPart().typeParameters() != null) {
            // `T id<T>(T t) => t;` is a local bound to a Funcs.* lambda, which cannot be
            // generic: its type parameters are erased to dynamic, as a generic closure's
            // are (buildLambda), and the call site coerces the dynamic result.
            List<String> tps = new ArrayList<String>();
            for (Dart2Parser.TypeParameterContext tp
                    : sig.formalParameterPart().typeParameters().typeParameter()) {
                tps.add(tp.identifier().getText());
            }
            lf.typeParams = tps;
            lf.typeVarSlots.add(typeVarName(lf.returnType, tps));
            for (Param p : lf.params) {
                lf.typeVarSlots.add(typeVarName(p.type, tps));
                p.type = eraseTypeParams(p.type, tps);
            }
            lf.returnType = eraseTypeParams(lf.returnType, tps);
        }
        Dart2Parser.FunctionBodyContext body = ctx.functionBody();
        if (body.ST() != null && body.block() != null && body.NATIVE_() == null) {
            // A local generator is lowered like a generator closure (see buildLambda).
            lf.isSyncStar = body.SYNC_() != null;
            lf.isAsyncStar = body.SYNC_() == null;
        } else {
            lf.isAsync = checkBodyModifiers(body);
        }
        if (body.block() != null) {
            lf.body = buildBlock(body.block());
        } else if (body.expr() != null) {
            lf.exprBody = buildExpr(body.expr());
        }
        return lf;
    }

    // ------------------------------------------------------------------
    // Dart 3: switch statements / expressions and patterns
    // ------------------------------------------------------------------

    private Stmt buildSwitch(Dart2Parser.SwitchStatementContext ctx) {
        SwitchStmt sw = new SwitchStmt();
        pos(sw, ctx);
        sw.subject = buildExpr(ctx.expr());
        for (Dart2Parser.SwitchCaseContext cc : ctx.switchCase()) {
            SwitchCase c = new SwitchCase();
            pos(c, cc);
            c.pattern = buildPattern(cc.guardedPattern().pattern());
            if (cc.guardedPattern().WHEN_() != null && cc.guardedPattern().expr() != null) {
                c.guard = buildExpr(cc.guardedPattern().expr());
            }
            addStatements(cc.statements(), c.body);
            c.labels = caseLabels(cc.label());
            sw.cases.add(c);
        }
        if (ctx.defaultCase() != null) {
            SwitchCase c = new SwitchCase();
            pos(c, ctx.defaultCase());
            c.isDefault = true;
            addStatements(ctx.defaultCase().statements(), c.body);
            c.labels = caseLabels(ctx.defaultCase().label());
            sw.cases.add(c);
        }
        return sw;
    }

    private static List<String> caseLabels(List<Dart2Parser.LabelContext> ls) {
        if (ls == null || ls.isEmpty()) {
            return null;
        }
        List<String> out = new ArrayList<String>();
        for (Dart2Parser.LabelContext l : ls) {
            out.add(l.identifier().getText());
        }
        return out;
    }

    private void addStatements(Dart2Parser.StatementsContext ctx, List<Stmt> out) {
        if (ctx == null) {
            return;
        }
        for (Dart2Parser.StatementContext st : ctx.statement()) {
            Stmt s = buildStatement(st);
            if (s != null) {
                out.add(s);
            }
        }
    }

    private Expr buildSwitchExpr(Dart2Parser.SwitchExpressionContext ctx) {
        SwitchExpr sw = new SwitchExpr();
        pos(sw, ctx);
        sw.subject = buildExpr(ctx.expr());
        for (Dart2Parser.SwitchExpressionCaseContext ec : ctx.switchExpressionCase()) {
            SwitchExprCase c = new SwitchExprCase();
            pos(c, ec);
            c.pattern = buildPattern(ec.guardedPattern().pattern());
            if (ec.guardedPattern().WHEN_() != null && ec.guardedPattern().expr() != null) {
                c.guard = buildExpr(ec.guardedPattern().expr());
            }
            c.value = buildExpr(ec.expr());
            if (c.guard == null && c.pattern instanceof VariablePattern
                    && ((VariablePattern) c.pattern).wildcard) {
                c.isDefault = true;
            }
            sw.cases.add(c);
        }
        return sw;
    }

    private Pattern buildPattern(Dart2Parser.PatternContext ctx) {
        return buildOrPattern(ctx.logicalOrPattern());
    }

    private Pattern buildOrPattern(Dart2Parser.LogicalOrPatternContext ctx) {
        List<Dart2Parser.LogicalAndPatternContext> ands = ctx.logicalAndPattern();
        if (ands.size() == 1) {
            return buildAndPattern(ands.get(0));
        }
        OrPattern or = new OrPattern();
        pos(or, ctx);
        for (Dart2Parser.LogicalAndPatternContext a : ands) {
            or.alternatives.add(buildAndPattern(a));
        }
        return or;
    }

    private Pattern buildAndPattern(Dart2Parser.LogicalAndPatternContext ctx) {
        List<Dart2Parser.RelationalPatternContext> rels = ctx.relationalPattern();
        if (rels.size() == 1) {
            return buildRelationalPattern(rels.get(0));
        }
        AndPattern and = new AndPattern();
        pos(and, ctx);
        for (Dart2Parser.RelationalPatternContext r : rels) {
            and.parts.add(buildRelationalPattern(r));
        }
        return and;
    }

    private Pattern buildRelationalPattern(Dart2Parser.RelationalPatternContext ctx) {
        if (ctx.unaryPattern() != null) {
            return buildUnaryPattern(ctx.unaryPattern());
        }
        RelationalPattern r = new RelationalPattern();
        pos(r, ctx);
        if (ctx.EE() != null) {
            r.op = "==";
        } else if (ctx.NE() != null) {
            r.op = "!=";
        } else if (ctx.LTE() != null) {
            r.op = "<=";
        } else if (ctx.GT() != null && ctx.EQ() != null) {
            r.op = ">=";
        } else if (ctx.LT() != null) {
            r.op = "<";
        } else {
            r.op = ">";
        }
        r.operand = buildBitwiseOr(ctx.bitwiseOrExpression());
        return r;
    }

    private Pattern buildUnaryPattern(Dart2Parser.UnaryPatternContext ctx) {
        Pattern p = buildPrimaryPattern(ctx.primaryPattern());
        if (ctx.AS_() != null && ctx.type() != null) {
            CastPattern c = new CastPattern();
            pos(c, ctx);
            c.inner = p;
            c.type = buildType(ctx.type());
            return c;
        }
        if (ctx.QU() != null || ctx.NOT() != null) {
            NullCheckPattern n = new NullCheckPattern();
            pos(n, ctx);
            n.inner = p;
            n.assertNonNull = ctx.NOT() != null;
            return n;
        }
        return p;
    }

    private Pattern buildPrimaryPattern(Dart2Parser.PrimaryPatternContext ctx) {
        if (ctx.constantPattern() != null) {
            ConstantPattern c = new ConstantPattern();
            pos(c, ctx);
            Dart2Parser.ConstantPatternContext cp = ctx.constantPattern();
            c.value = parseExprFragment(cp.getText(), cp.getStart().getLine(),
                    cp.getStart().getCharPositionInLine());
            return c;
        }
        if (ctx.objectPattern() != null) {
            return buildObjectPattern(ctx.objectPattern());
        }
        if (ctx.recordPattern() != null) {
            return buildRecordPattern(ctx.recordPattern());
        }
        if (ctx.listPattern() != null) {
            return buildListPattern(ctx.listPattern());
        }
        if (ctx.mapPattern() != null) {
            return buildMapPattern(ctx.mapPattern());
        }
        if (ctx.variablePattern() != null) {
            return buildVariablePattern(ctx.variablePattern());
        }
        if (ctx.pattern() != null) {
            return buildPattern(ctx.pattern());
        }
        unsupported(ctx, "E0430", "Unsupported pattern: " + snippet(ctx));
        return null;
    }

    private Pattern buildVariablePattern(Dart2Parser.VariablePatternContext ctx) {
        VariablePattern v = new VariablePattern();
        pos(v, ctx);
        v.name = ctx.identifier().getText();
        if ("_".equals(v.name)) {
            v.wildcard = true;
        }
        if (ctx.type() != null) {
            v.type = buildType(ctx.type());
        }
        return v;
    }

    private Pattern buildObjectPattern(Dart2Parser.ObjectPatternContext ctx) {
        ObjectPattern o = new ObjectPattern();
        pos(o, ctx);
        o.type = new TypeRef(ctx.typeName().getText());
        if (currentLib != null) {
            // erased like any other type mention when it names an extension type
            currentLib.namedTypes.add(o.type);
        }
        if (ctx.typeArguments() != null) {
            for (Dart2Parser.TypeContext t : ctx.typeArguments().typeList().type()) {
                o.type.args.add(buildType(t));
            }
        }
        for (Dart2Parser.PatternFieldContext pf : ctx.patternField()) {
            o.fields.add(buildPatternField(pf));
        }
        return o;
    }

    private Pattern buildRecordPattern(Dart2Parser.RecordPatternContext ctx) {
        RecordPattern r = new RecordPattern();
        pos(r, ctx);
        for (Dart2Parser.PatternFieldContext pf : ctx.patternField()) {
            r.fields.add(buildPatternField(pf));
        }
        return r;
    }

    private Pattern buildMapPattern(Dart2Parser.MapPatternContext ctx) {
        MapPattern m = new MapPattern();
        pos(m, ctx);
        if (ctx.typeArguments() != null) {
            m.type = new TypeRef("Map");
            for (Dart2Parser.TypeContext t : ctx.typeArguments().typeList().type()) {
                m.type.args.add(buildType(t));
            }
        }
        for (Dart2Parser.MapPatternEntryContext e : ctx.mapPatternEntry()) {
            Dart2Parser.ConstantPatternContext cp = e.constantPattern();
            m.keys.add(parseExprFragment(cp.getText(), cp.getStart().getLine(),
                    cp.getStart().getCharPositionInLine()));
            m.values.add(buildPattern(e.pattern()));
        }
        return m;
    }

    private Pattern buildListPattern(Dart2Parser.ListPatternContext ctx) {
        ListPattern l = new ListPattern();
        pos(l, ctx);
        for (Dart2Parser.ListPatternElementContext e : ctx.listPatternElement()) {
            if (e.DDD() != null) {
                RestPattern r = new RestPattern();
                pos(r, e);
                r.inner = e.pattern() == null ? null : buildPattern(e.pattern());
                l.elements.add(r);
            } else {
                l.elements.add(buildPattern(e.pattern()));
            }
        }
        return l;
    }

    private Expr buildRecordLit(Dart2Parser.RecordLiteralContext ctx) {
        RecordLit r = new RecordLit();
        pos(r, ctx);
        // 2nd grammar alt: a leading `identifier : expr` named field
        if (ctx.identifier() != null && ctx.expr() != null) {
            RecordField f = new RecordField();
            pos(f, ctx);
            f.name = ctx.identifier().getText();
            f.value = buildExpr(ctx.expr());
            r.fields.add(f);
        }
        for (Dart2Parser.RecordFieldContext rf : ctx.recordField()) {
            RecordField f = new RecordField();
            pos(f, rf);
            if (rf.identifier() != null) {
                f.name = rf.identifier().getText();
            }
            f.value = buildExpr(rf.expr());
            r.fields.add(f);
        }
        if (ctx.CONST_() != null) {
            markConstContext(r);
        }
        return r;
    }

    private PatternField buildPatternField(Dart2Parser.PatternFieldContext ctx) {
        PatternField f = new PatternField();
        pos(f, ctx);
        if (ctx.identifier() != null) {
            f.name = ctx.identifier().getText();
        }
        f.pattern = buildPattern(ctx.pattern());
        // `Circle(:var radius)` shorthand — a colon with no name defaults to the bound variable's
        // name. This must NOT fire for positional record fields like `(var x, var y)`, which have no
        // colon and stay positional.
        Pattern named = f.pattern;
        while (named instanceof NullCheckPattern || named instanceof CastPattern) {
            named = named instanceof NullCheckPattern ? ((NullCheckPattern) named).inner : ((CastPattern) named).inner;
        }
        if (f.name == null && ctx.CO() != null && named instanceof VariablePattern) {
            f.name = ((VariablePattern) named).name;
        }
        return f;
    }

    private Stmt buildTry(Dart2Parser.TryStatementContext ctx) {
        TryStmt t = new TryStmt();
        pos(t, ctx);
        t.tryBlock = buildBlock(ctx.block());
        if (ctx.onPart() != null) {
            for (Dart2Parser.OnPartContext op : ctx.onPart()) {
                CatchClause cc = new CatchClause();
                pos(cc, op);
                if (op.typeNotVoid() != null) {
                    cc.onType = buildTypeNotVoid(op.typeNotVoid());
                }
                if (op.catchPart() != null) {
                    cc.exceptionVar = op.catchPart().identifier(0).getText();
                    if (op.catchPart().identifier().size() > 1) {
                        cc.stackVar = op.catchPart().identifier(1).getText();
                    }
                }
                cc.body = buildBlock(op.block());
                t.catches.add(cc);
            }
        }
        if (ctx.finallyPart() != null) {
            t.finallyBlock = buildBlock(ctx.finallyPart().block());
        }
        return t;
    }

    private Stmt buildLocalVar(Dart2Parser.LocalVariableDeclarationContext ctx) {
        Dart2Parser.InitializedVariableDeclarationContext iv = ctx.initializedVariableDeclaration();
        Dart2Parser.DeclaredIdentifierContext di = iv.declaredIdentifier();
        // `await x;` on a bare variable is ambiguous in the grammar with a declaration
        // of a variable x of type `await`, and the parser takes the declaration. Dart
        // does not: await is reserved in an async body, and no type can be named it.
        // Read as a declaration it was emitted as `await x = null;`, which does not
        // compile, so an `await someFuture;` statement broke the whole build.
        Dart2Parser.FinalConstVarOrTypeContext fct = di.finalConstVarOrType();
        if (iv.expr() == null && iv.initializedIdentifier().isEmpty()
                && fct.FINAL_() == null && fct.CONST_() == null && fct.LATE_() == null
                && "await".equals(fct.getText())) {
            Ident operand = new Ident();
            pos(operand, di.identifier());
            operand.name = di.identifier().getText();
            AwaitExpr aw = new AwaitExpr();
            pos(aw, ctx);
            aw.operand = operand;
            ExprStmt es = new ExprStmt();
            pos(es, ctx);
            es.expr = aw;
            return es;
        }
        VarDeclStmt v = new VarDeclStmt();
        pos(v, ctx);
        v.name = di.identifier().getText();
        v.type = buildFinalConstVarOrType(di.finalConstVarOrType());
        v.isFinal = di.finalConstVarOrType().FINAL_() != null || di.finalConstVarOrType().CONST_() != null;
        v.isLate = di.finalConstVarOrType().LATE_() != null;
        boolean constLocal = di.finalConstVarOrType().CONST_() != null;
        if (iv.expr() != null) {
            v.initializer = buildExpr(iv.expr());
            if (constLocal) {
                markConstContext(v.initializer);
            }
        }
        if (iv.initializedIdentifier().isEmpty()) {
            return v;
        }
        // int a = 1, b = 2; — group of sibling declarations sharing the type
        VarDeclGroup group = new VarDeclGroup();
        pos(group, ctx);
        group.decls.add(v);
        for (Dart2Parser.InitializedIdentifierContext ii : iv.initializedIdentifier()) {
            VarDeclStmt extra = new VarDeclStmt();
            pos(extra, ii);
            extra.name = ii.identifier().getText();
            extra.type = v.type;
            extra.isFinal = v.isFinal;
            extra.isLate = v.isLate;
            if (ii.expr() != null) {
                extra.initializer = buildExpr(ii.expr());
                if (constLocal) {
                    markConstContext(extra.initializer);
                }
            }
            group.decls.add(extra);
        }
        return group;
    }

    private Stmt buildFor(Dart2Parser.ForStatementContext ctx) {
        Dart2Parser.ForLoopPartsContext parts = ctx.forLoopParts();
        if (ctx.AWAIT_() != null && parts.IN_() == null) {
            unsupported(ctx, "E0302", "await is only valid on a for-in loop");
            return null;
        }
        if (parts.IN_() != null) {
            ForInStmt fi = new ForInStmt();
            pos(fi, ctx);
            fi.isAwait = ctx.AWAIT_() != null;
            if (parts.pattern() != null) {
                // Dart 3 pattern for-in: `for (final (a, b) in xs)` / `for (var [x] in xs)`
                fi.pattern = buildPattern(parts.pattern());
                fi.varType = TypeRef.VAR;
            } else if (parts.declaredIdentifier() != null) {
                fi.varName = parts.declaredIdentifier().identifier().getText();
                fi.varType = buildFinalConstVarOrType(parts.declaredIdentifier().finalConstVarOrType());
            } else {
                fi.varName = parts.identifier().getText();
                fi.varType = TypeRef.VAR;
            }
            fi.iterable = buildExpr(parts.expr());
            fi.body = buildStatement(ctx.statement());
            return fi;
        }
        ForStmt f = new ForStmt();
        pos(f, ctx);
        Dart2Parser.ForInitializerStatementContext init = parts.forInitializerStatement();
        if (init != null) {
            if (init.localVariableDeclaration() != null) {
                f.init = buildLocalVar(init.localVariableDeclaration());
            } else if (init.expr() != null) {
                ExprStmt es = new ExprStmt();
                pos(es, init);
                es.expr = buildExpr(init.expr());
                f.init = es;
            }
        }
        if (parts.expr() != null) {
            f.condition = buildExpr(parts.expr());
        }
        if (parts.expressionList() != null) {
            for (Dart2Parser.ExprContext e : parts.expressionList().expr()) {
                f.updates.add(buildExpr(e));
            }
        }
        f.body = buildStatement(ctx.statement());
        if (f.init instanceof VarDeclGroup) {
            // `for (var i = 0, j = n - 1; ...)`: the declarations go ahead of the loop, in a
            // block of their own so their scope still ends with it.
            Block b = new Block();
            pos(b, ctx);
            b.statements.addAll(((VarDeclGroup) f.init).decls);
            f.init = null;
            b.statements.add(f);
            return b;
        }
        return f;
    }

    // ------------------------------------------------------------------
    // Expressions
    // ------------------------------------------------------------------

    private Expr buildExpr(Dart2Parser.ExprContext ctx) {
        if (ctx == null) {
            return errExpr(null);
        }
        if (ctx.assignableExpression() != null && ctx.assignmentOperator() != null) {
            Assign a = new Assign();
            pos(a, ctx);
            a.lhs = buildAssignable(ctx.assignableExpression());
            a.op = ctx.assignmentOperator().getText();
            a.rhs = buildExpr(ctx.expr());
            return arrowBodyAssign(a);
        }
        if (ctx.conditionalExpression() != null) {
            return buildConditional(ctx.conditionalExpression());
        }
        if (ctx.cascade() != null) {
            return buildCascade(ctx.cascade());
        }
        if (ctx.throwExpression() != null) {
            ThrowExpr t = new ThrowExpr();
            pos(t, ctx);
            t.value = buildExpr(ctx.throwExpression().expr());
            return t;
        }
        return errExpr(ctx);
    }

    /**
     * a..b(x)..c = y — flattens the left-recursive cascade chain; each
     * section becomes an Expr tree rooted at a CascadeTarget marker that
     * the emitter substitutes with the once-evaluated receiver temp.
     */
    private Expr buildCascade(Dart2Parser.CascadeContext ctx) {
        // walk down the left recursion collecting sections in source order
        java.util.ArrayList<Dart2Parser.CascadeSectionContext> sections =
                new java.util.ArrayList<Dart2Parser.CascadeSectionContext>();
        Dart2Parser.CascadeContext cur = ctx;
        while (cur.cascade() != null) {
            sections.add(0, cur.cascadeSection());
            cur = cur.cascade();
        }
        sections.add(0, cur.cascadeSection());
        Cascade cas = new Cascade();
        pos(cas, ctx);
        cas.nullAware = cur.QUDD() != null;
        cas.target = buildConditional(cur.conditionalExpression());
        for (Dart2Parser.CascadeSectionContext s : sections) {
            cas.sections.add(buildCascadeSection(s));
        }
        return cas;
    }

    private Expr buildCascadeSection(Dart2Parser.CascadeSectionContext ctx) {
        CascadeTarget marker = new CascadeTarget();
        pos(marker, ctx);
        Expr base;
        Dart2Parser.CascadeSelectorContext sel = ctx.cascadeSelector();
        if (sel.identifier() != null) {
            PropertyGet pg = new PropertyGet();
            pos(pg, sel);
            pg.target = marker;
            pg.name = sel.identifier().getText();
            base = pg;
        } else {
            IndexGet ig = new IndexGet();
            pos(ig, sel);
            ig.target = marker;
            ig.index = buildExpr(sel.expr());
            base = ig;
        }
        Dart2Parser.CascadeSectionTailContext tail = ctx.cascadeSectionTail();
        if (tail.selector() != null) {
            for (Dart2Parser.SelectorContext s : tail.selector()) {
                base = applySelector(base, s);
            }
        }
        if (tail.assignableSelector() != null) {
            base = applyAssignableSelector(base, tail.assignableSelector());
        }
        if (tail.cascadeAssignment() != null) {
            Assign a = new Assign();
            pos(a, tail);
            a.lhs = base;
            a.op = tail.cascadeAssignment().assignmentOperator().getText();
            a.rhs = buildExprWithoutCascade(tail.cascadeAssignment().expressionWithoutCascade());
            return a;
        }
        return base;
    }

    /**
     * {@code () => o.p = v} is a closure whose body assigns, but the grammar's
     * {@code expr} alternatives are ambiguous there and ANTLR takes the first: an
     * assignment whose target is {@code (() => o).p}. Re-associates that shape -- a
     * selector chain rooted at an unparenthesized arrow closure -- into the closure
     * with {@code o.p = v} as its body, which is the only reading Dart has.
     */
    private static Expr arrowBodyAssign(Assign a) {
        Expr holder = null;
        Expr e = a.lhs;
        while (true) {
            Expr next;
            if (e instanceof PropertyGet) {
                next = ((PropertyGet) e).target;
            } else if (e instanceof IndexGet) {
                next = ((IndexGet) e).target;
            } else if (e instanceof Call) {
                next = ((Call) e).target;
            } else {
                break;
            }
            if (next == null) {
                return a;
            }
            holder = e;
            e = next;
        }
        if (holder == null || !(e instanceof Lambda) || ((Lambda) e).exprBody == null) {
            return a;
        }
        Lambda l = (Lambda) e;
        if (holder instanceof PropertyGet) {
            ((PropertyGet) holder).target = l.exprBody;
        } else if (holder instanceof IndexGet) {
            ((IndexGet) holder).target = l.exprBody;
        } else {
            ((Call) holder).target = l.exprBody;
        }
        l.exprBody = arrowBodyAssign(a);
        return l;
    }

    private Expr buildExprWithoutCascade(Dart2Parser.ExpressionWithoutCascadeContext ctx) {
        if (ctx.assignableExpression() != null && ctx.assignmentOperator() != null) {
            Assign a = new Assign();
            pos(a, ctx);
            a.lhs = buildAssignable(ctx.assignableExpression());
            a.op = ctx.assignmentOperator().getText();
            a.rhs = buildExprWithoutCascade(ctx.expressionWithoutCascade());
            return arrowBodyAssign(a);
        }
        if (ctx.conditionalExpression() != null) {
            return buildConditional(ctx.conditionalExpression());
        }
        if (ctx.throwExpressionWithoutCascade() != null) {
            ThrowExpr t = new ThrowExpr();
            pos(t, ctx);
            t.value = buildExprWithoutCascade(ctx.throwExpressionWithoutCascade().expressionWithoutCascade());
            return t;
        }
        return errExpr(ctx);
    }

    private Expr buildAssignable(Dart2Parser.AssignableExpressionContext ctx) {
        if (ctx.identifier() != null) {
            Ident id = new Ident();
            pos(id, ctx);
            id.name = ctx.identifier().getText();
            return id;
        }
        if (ctx.SUPER_() != null) {
            // `super.x = v` / `super[i] = v`: the inherited setter or index operator.
            SuperExpr sup = new SuperExpr();
            pos(sup, ctx);
            return applyUnconditional(sup, ctx.unconditionalAssignableSelector());
        }
        // primary assignableSelectorPart : selector* assignableSelector
        Expr base = buildPrimary(ctx.primary());
        Dart2Parser.AssignableSelectorPartContext part = ctx.assignableSelectorPart();
        for (Dart2Parser.SelectorContext s : part.selector()) {
            base = applySelector(base, s);
        }
        return applyAssignableSelector(base, part.assignableSelector());
    }

    private Expr buildConditional(Dart2Parser.ConditionalExpressionContext ctx) {
        Expr cond = buildIfNull(ctx.ifNullExpression());
        if (ctx.expressionWithoutCascade() != null && !ctx.expressionWithoutCascade().isEmpty()) {
            Conditional c = new Conditional();
            pos(c, ctx);
            c.condition = cond;
            c.thenExpr = buildExprWithoutCascade(ctx.expressionWithoutCascade(0));
            c.elseExpr = buildExprWithoutCascade(ctx.expressionWithoutCascade(1));
            return c;
        }
        return cond;
    }

    private Expr buildIfNull(Dart2Parser.IfNullExpressionContext ctx) {
        Expr left = buildLogicalOr(ctx.logicalOrExpression(0));
        for (int i = 1; i < ctx.logicalOrExpression().size(); i++) {
            Binary b = new Binary();
            pos(b, ctx);
            b.left = left;
            b.op = "??";
            b.right = buildLogicalOr(ctx.logicalOrExpression(i));
            left = b;
        }
        return left;
    }

    private Expr buildLogicalOr(Dart2Parser.LogicalOrExpressionContext ctx) {
        Expr left = buildLogicalAnd(ctx.logicalAndExpression(0));
        for (int i = 1; i < ctx.logicalAndExpression().size(); i++) {
            left = binary(ctx, left, "||", buildLogicalAnd(ctx.logicalAndExpression(i)));
        }
        return left;
    }

    private Expr buildLogicalAnd(Dart2Parser.LogicalAndExpressionContext ctx) {
        Expr left = buildEquality(ctx.equalityExpression(0));
        for (int i = 1; i < ctx.equalityExpression().size(); i++) {
            left = binary(ctx, left, "&&", buildEquality(ctx.equalityExpression(i)));
        }
        return left;
    }

    private Expr buildEquality(Dart2Parser.EqualityExpressionContext ctx) {
        if (ctx.SUPER_() != null) {
            // `super == other`: the emitter calls the superclass's equals.
            SuperExpr sup = new SuperExpr();
            pos(sup, ctx);
            return binary(ctx, sup, ctx.equalityOperator().getText(),
                    buildRelational(ctx.relationalExpression(0)));
        }
        Expr left = buildRelational(ctx.relationalExpression(0));
        if (ctx.equalityOperator() != null) {
            left = binary(ctx, left, ctx.equalityOperator().getText(),
                    buildRelational(ctx.relationalExpression(1)));
        }
        return left;
    }

    private Expr buildRelational(Dart2Parser.RelationalExpressionContext ctx) {
        if (ctx.SUPER_() != null) {
            unsupported(ctx, "E0120", "super relational ops are not supported");
            return errExpr(ctx);
        }
        Expr left = buildBitwiseOr(ctx.bitwiseOrExpression(0));
        if (ctx.typeTest() != null) {
            IsTest t = new IsTest();
            pos(t, ctx);
            t.operand = left;
            t.negated = ctx.typeTest().isOperator().NOT() != null;
            t.type = buildTypeNotVoid(ctx.typeTest().typeNotVoid());
            return t;
        }
        if (ctx.typeCast() != null) {
            AsCast c = new AsCast();
            pos(c, ctx);
            c.operand = left;
            c.type = buildTypeNotVoid(ctx.typeCast().typeNotVoid());
            return c;
        }
        if (ctx.relationalOperator() != null) {
            left = binary(ctx, left, ctx.relationalOperator().getText(),
                    buildBitwiseOr(ctx.bitwiseOrExpression(1)));
        }
        return left;
    }

    private Expr buildBitwiseOr(Dart2Parser.BitwiseOrExpressionContext ctx) {
        if (ctx.SUPER_() != null) {
            unsupported(ctx, "E0120", "super bitwise ops are not supported");
            return errExpr(ctx);
        }
        Expr left = buildBitwiseXor(ctx.bitwiseXorExpression(0));
        for (int i = 1; i < ctx.bitwiseXorExpression().size(); i++) {
            left = binary(ctx, left, "|", buildBitwiseXor(ctx.bitwiseXorExpression(i)));
        }
        return left;
    }

    private Expr buildBitwiseXor(Dart2Parser.BitwiseXorExpressionContext ctx) {
        if (ctx.SUPER_() != null) {
            unsupported(ctx, "E0120", "super bitwise ops are not supported");
            return errExpr(ctx);
        }
        Expr left = buildBitwiseAnd(ctx.bitwiseAndExpression(0));
        for (int i = 1; i < ctx.bitwiseAndExpression().size(); i++) {
            left = binary(ctx, left, "^", buildBitwiseAnd(ctx.bitwiseAndExpression(i)));
        }
        return left;
    }

    private Expr buildBitwiseAnd(Dart2Parser.BitwiseAndExpressionContext ctx) {
        if (ctx.SUPER_() != null) {
            unsupported(ctx, "E0120", "super bitwise ops are not supported");
            return errExpr(ctx);
        }
        Expr left = buildShift(ctx.shiftExpression(0));
        for (int i = 1; i < ctx.shiftExpression().size(); i++) {
            left = binary(ctx, left, "&", buildShift(ctx.shiftExpression(i)));
        }
        return left;
    }

    private Expr buildShift(Dart2Parser.ShiftExpressionContext ctx) {
        if (ctx.SUPER_() != null) {
            unsupported(ctx, "E0120", "super shift ops are not supported");
            return errExpr(ctx);
        }
        Expr left = buildAdditive(ctx.additiveExpression(0));
        for (int i = 1; i < ctx.additiveExpression().size(); i++) {
            left = binary(ctx, left, ctx.shiftOperator(i - 1).getText(),
                    buildAdditive(ctx.additiveExpression(i)));
        }
        return left;
    }

    private Expr buildAdditive(Dart2Parser.AdditiveExpressionContext ctx) {
        if (ctx.SUPER_() != null) {
            unsupported(ctx, "E0120", "super arithmetic is not supported");
            return errExpr(ctx);
        }
        Expr left = buildMultiplicative(ctx.multiplicativeExpression(0));
        for (int i = 1; i < ctx.multiplicativeExpression().size(); i++) {
            left = binary(ctx, left, ctx.additiveOperator(i - 1).getText(),
                    buildMultiplicative(ctx.multiplicativeExpression(i)));
        }
        return left;
    }

    private Expr buildMultiplicative(Dart2Parser.MultiplicativeExpressionContext ctx) {
        if (ctx.SUPER_() != null) {
            unsupported(ctx, "E0120", "super arithmetic is not supported");
            return errExpr(ctx);
        }
        Expr left = buildUnary(ctx.unaryExpression(0));
        for (int i = 1; i < ctx.unaryExpression().size(); i++) {
            left = binary(ctx, left, ctx.multiplicativeOperator(i - 1).getText(),
                    buildUnary(ctx.unaryExpression(i)));
        }
        return left;
    }

    private Expr buildUnary(Dart2Parser.UnaryExpressionContext ctx) {
        if (ctx.prefixOperator() != null) {
            Unary u = new Unary();
            pos(u, ctx);
            u.op = ctx.prefixOperator().getText();
            u.operand = buildUnary(ctx.unaryExpression());
            return u;
        }
        if (ctx.awaitExpression() != null) {
            AwaitExpr a = new AwaitExpr();
            pos(a, ctx);
            a.operand = buildUnary(ctx.awaitExpression().unaryExpression());
            return a;
        }
        if (ctx.incrementOperator() != null && ctx.assignableExpression() != null) {
            IncDec id = new IncDec();
            pos(id, ctx);
            id.prefix = true;
            id.increment = ctx.incrementOperator().getText().equals("++");
            id.operand = buildAssignable(ctx.assignableExpression());
            return id;
        }
        if (ctx.postfixExpression() != null) {
            return buildPostfix(ctx.postfixExpression());
        }
        unsupported(ctx, "E0121", "Unsupported unary expression: " + snippet(ctx));
        return errExpr(ctx);
    }

    private Expr buildPostfix(Dart2Parser.PostfixExpressionContext ctx) {
        if (ctx.assignableExpression() != null && ctx.postfixOperator() != null) {
            IncDec id = new IncDec();
            pos(id, ctx);
            id.prefix = false;
            id.increment = ctx.postfixOperator().getText().equals("++");
            id.operand = buildAssignable(ctx.assignableExpression());
            return id;
        }
        Expr base = buildPrimary(ctx.primary());
        for (Dart2Parser.SelectorContext s : ctx.selector()) {
            base = applySelector(base, s);
        }
        return base;
    }

    /** Applies one selector (call / property / index / null-assert) to a base expression. */
    private Expr applySelector(Expr base, Dart2Parser.SelectorContext s) {
        if (s.NOT() != null) {
            NotNullAssert n = new NotNullAssert();
            pos(n, s);
            n.operand = base;
            return n;
        }
        if (s.argumentPart() != null) {
            // call on the base: fold `Ident(args)` and `x.name(args)` into Call nodes
            Call call = new Call();
            pos(call, s);
            if (s.argumentPart().typeArguments() != null) {
                for (Dart2Parser.TypeContext t : s.argumentPart().typeArguments().typeList().type()) {
                    call.typeArgs.add(buildType(t));
                }
            }
            buildArgs(s.argumentPart().arguments(), call.args);
            if (base instanceof Ident) {
                call.name = ((Ident) base).name;
            } else if (base instanceof PropertyGet) {
                PropertyGet pg = (PropertyGet) base;
                call.target = pg.target;
                call.name = pg.name;
                call.nullAware = pg.nullAware;
            } else {
                // calling an arbitrary expression value (closure invoke)
                call.target = base;
                call.name = null;
            }
            return call;
        }
        if (s.assignableSelector() == null) {
            // `f<int>` without a call: an explicit instantiation. Java erases the type
            // arguments, so the instantiated tear-off is the plain tear-off -- except on a
            // class name, where `Cell<int>.new` keeps them for the constructor tear-off.
            if (base instanceof Ident && ((Ident) base).typeArgs == null) {
                Ident id = (Ident) base;
                id.typeArgs = new ArrayList<TypeRef>();
                for (Dart2Parser.TypeContext t : s.typeArguments().typeList().type()) {
                    id.typeArgs.add(buildType(t));
                }
            } else if (base instanceof PropertyGet && ((PropertyGet) base).typeArgs == null) {
                // `K.s<int>`: a generic static method instantiated explicitly, which the
                // tear-off is keyed and witnessed by.
                PropertyGet pg = (PropertyGet) base;
                pg.typeArgs = new ArrayList<TypeRef>();
                for (Dart2Parser.TypeContext t : s.typeArguments().typeList().type()) {
                    pg.typeArgs.add(buildType(t));
                }
            }
            return base;
        }
        return applyAssignableSelector(base, s.assignableSelector());
    }

    private Expr applyUnconditional(Expr base, Dart2Parser.UnconditionalAssignableSelectorContext u) {
        if (u.identifier() != null || u.NEW_() != null) {
            PropertyGet pg = new PropertyGet();
            pos(pg, u);
            pg.target = base;
            pg.name = u.identifier() != null ? u.identifier().getText() : "new";
            return pg;
        }
        IndexGet ig = new IndexGet();
        pos(ig, u);
        ig.target = base;
        ig.index = buildExpr(u.expr());
        return ig;
    }

    private Expr applyAssignableSelector(Expr base, Dart2Parser.AssignableSelectorContext sel) {
        if (sel.unconditionalAssignableSelector() != null) {
            return applyUnconditional(base, sel.unconditionalAssignableSelector());
        }
        if (sel.QUD() != null) {
            PropertyGet pg = new PropertyGet();
            pos(pg, sel);
            pg.target = base;
            pg.name = sel.identifier().getText();
            pg.nullAware = true;
            return pg;
        }
        // QU OB expr CB — null-aware index
        IndexGet ig = new IndexGet();
        pos(ig, sel);
        ig.target = base;
        ig.index = buildExpr(sel.expr());
        return ig;
    }

    private void buildArgs(Dart2Parser.ArgumentsContext ctx, Args out) {
        if (ctx == null || ctx.argumentList() == null) {
            return;
        }
        Dart2Parser.ArgumentListContext list = ctx.argumentList();
        // Named arguments written before positional ones (Dart 2.17): positional order is
        // the written order, and every argument keeps its source position, which the
        // emitter's left-to-right evaluation ordering reads.
        for (Dart2Parser.MixedArgumentContext m : list.mixedArgument()) {
            if (m.namedArgument() != null) {
                NamedArg na = new NamedArg();
                na.name = m.namedArgument().label().identifier().getText();
                na.value = buildExpr(m.namedArgument().expr());
                out.named.add(na);
            } else {
                out.positional.add(buildExpr(m.expr()));
            }
        }
        if (list.expressionList() != null) {
            for (Dart2Parser.ExprContext e : list.expressionList().expr()) {
                out.positional.add(buildExpr(e));
            }
        }
        for (Dart2Parser.NamedArgumentContext n : list.namedArgument()) {
            NamedArg na = new NamedArg();
            na.name = n.label().identifier().getText();
            na.value = buildExpr(n.expr());
            out.named.add(na);
        }
    }

    private Expr buildPrimary(Dart2Parser.PrimaryContext ctx) {
        if (ctx == null) {
            // parser error-recovery can hand us a null primary; a preceding syntax
            // (E0001) diagnostic already flags the real cause, so degrade gracefully.
            return errExpr(null);
        }
        if (ctx.thisExpression() != null) {
            ThisExpr t = new ThisExpr();
            pos(t, ctx);
            return t;
        }
        if (ctx.SUPER_() != null) {
            SuperExpr sup = new SuperExpr();
            pos(sup, ctx);
            if (ctx.unconditionalAssignableSelector() != null) {
                return applyUnconditional(sup, ctx.unconditionalAssignableSelector());
            }
            unsupported(ctx, "E0122", "super(...) calls outside initializer lists are not supported");
            return errExpr(ctx);
        }
        if (ctx.functionExpression() != null) {
            return buildLambda(ctx.functionExpression());
        }
        if (ctx.literal() != null) {
            return buildLiteral(ctx.literal());
        }
        if (ctx.identifier() != null) {
            Ident id = new Ident();
            pos(id, ctx);
            id.name = ctx.identifier().getText();
            return id;
        }
        if (ctx.newExpression() != null) {
            return buildCtorFromDesignation(ctx.newExpression().constructorDesignation(),
                    ctx.newExpression().arguments(), false, ctx);
        }
        if (ctx.constObjectExpression() != null) {
            return buildCtorFromDesignation(ctx.constObjectExpression().constructorDesignation(),
                    ctx.constObjectExpression().arguments(), true, ctx);
        }
        if (ctx.constructorInvocation() != null) {
            Dart2Parser.ConstructorInvocationContext ci = ctx.constructorInvocation();
            CtorCall cc = new CtorCall();
            pos(cc, ctx);
            cc.type = new TypeRef(ci.typeName().getText());
            if (currentLib != null) {
                currentLib.namedTypes.add(cc.type);
            }
            int pdot = cc.type.name.indexOf('.');
            String puri = pdot > 0 ? prefixImports.get(cc.type.name.substring(0, pdot)) : null;
            if (puri != null && puri.startsWith("dart:")) {
                // `col.Queue<int>.from(..)`: the runtime type, as markRuntimePrefix says
                String written = cc.type.name;
                cc.type.name = withoutImportPrefix(written);
                markRuntimePrefix(cc.type, written);
            }
            for (Dart2Parser.TypeContext t : ci.typeArguments().typeList().type()) {
                cc.type.args.add(buildType(t));
            }
            cc.ctorName = ci.identifier().getText();
            buildArgs(ci.arguments(), cc.args);
            return cc;
        }
        if (ctx.switchExpression() != null) {
            return buildSwitchExpr(ctx.switchExpression());
        }
        if (ctx.recordLiteral() != null) {
            return buildRecordLit(ctx.recordLiteral());
        }
        if (ctx.genericTearOff() != null) {
            // `Cell<int>.new` / `Cell<int>.named`: a constructor tear-off of an instantiated
            // type. The emitter erases the type arguments' effect on the parameters.
            Dart2Parser.GenericTearOffContext gt = ctx.genericTearOff();
            Ident cls = new Ident();
            pos(cls, gt);
            cls.name = gt.typeIdentifier().getText();
            cls.typeArgs = new ArrayList<TypeRef>();
            for (Dart2Parser.TypeContext t : gt.typeArguments().typeList().type()) {
                cls.typeArgs.add(buildType(t));
            }
            PropertyGet pg = new PropertyGet();
            pos(pg, gt);
            pg.target = cls;
            pg.name = gt.identifier() != null ? gt.identifier().getText() : "new";
            return pg;
        }
        if (ctx.expr() != null) {
            ParenExpr p = new ParenExpr();
            pos(p, ctx);
            p.inner = buildExpr(ctx.expr());
            return p;
        }
        unsupported(ctx, "E0123", "Unsupported primary expression: " + snippet(ctx));
        return errExpr(ctx);
    }

    private Expr buildCtorFromDesignation(Dart2Parser.ConstructorDesignationContext d,
                                          Dart2Parser.ArgumentsContext args, boolean isConst,
                                          ParserRuleContext posCtx) {
        CtorCall cc = new CtorCall();
        pos(cc, posCtx);
        cc.isConst = isConst;
        if (d.typeIdentifier() != null) {
            cc.type = new TypeRef(d.typeIdentifier().getText());
            if (currentLib != null) {
                currentLib.namedTypes.add(cc.type);
            }
        } else if (d.qualifiedName() != null) {
            // X.named -- type X, named ctor; p.X or p.X.named through an import prefix.
            // The prefix only selects the library, and the emitter resolves the bare class
            // name against the library's imports. Kept, `new p.X()` looked for a
            // constructor X on a class p and failed to resolve.
            String text = withoutImportPrefix(d.qualifiedName().getText());
            int dot = text.indexOf('.');
            if (dot < 0) {
                cc.type = new TypeRef(text);
                if (currentLib != null) {
                    currentLib.namedTypes.add(cc.type);
                }
            } else {
                cc.type = new TypeRef(text.substring(0, dot));
                if (currentLib != null) {
                    currentLib.namedTypes.add(cc.type);
                }
                cc.ctorName = text.substring(dot + 1);
            }
            markRuntimePrefix(cc.type, d.qualifiedName().getText());
        } else {
            cc.type = new TypeRef(withoutImportPrefix(d.typeName().getText()));
            if (currentLib != null) {
                currentLib.namedTypes.add(cc.type);
            }
            markRuntimePrefix(cc.type, d.typeName().getText());
            if (d.typeArguments() != null) {
                for (Dart2Parser.TypeContext t : d.typeArguments().typeList().type()) {
                    cc.type.args.add(buildType(t));
                }
            }
            if (d.identifier() != null) {
                cc.ctorName = d.identifier().getText();
            }
        }
        buildArgs(args, cc.args);
        if (isConst) {
            markConstArgs(cc.args);
        }
        return cc;
    }

    /**
     * A constructor's type written through the prefix of a {@code dart:} import
     * ({@code col.Queue<int>.from(..)}): a dart: library declares no program class, so the
     * type is the runtime one even beside a program class of the same name.
     */
    private void markRuntimePrefix(TypeRef t, String written) {
        int dot = written.indexOf('.');
        String uri = dot > 0 ? prefixImports.get(written.substring(0, dot)) : null;
        if (uri != null && uri.startsWith("dart:")) {
            t.runtimeOnly = true;
            t.name = collectionAlias(t.name);
        }
    }

    /** {@code p.X...} with a leading import prefix {@code p.} removed. */
    private String withoutImportPrefix(String text) {
        int dot = text.indexOf('.');
        if (dot > 0 && importPrefixes.contains(text.substring(0, dot))) {
            return text.substring(dot + 1);
        }
        return text;
    }

    /** {@code t} with every reference to one of {@code names} replaced by dynamic. */
    /// The type parameter {@code t} is written as (`T`, or `T?` when nullable), or null when
    /// it is none of {@code names}.
    /// How a generic function's signature slot of type {@code t} prints when it names one of
    /// the function's type parameters {@code names}, bare or nested (`T`, `List<K>`,
    /// `Map<K, V>`, `K Function(V)` as `(V) => K`), in Dart's own spelling; the runtime
    /// renames the parameters to Y0, Y1... Null when the slot names none of them, so it
    /// prints from its witness.
    private static String typeVarName(TypeRef t, List<String> names) {
        if (t == null || !mentionsAny(t, names)) {
            return null;
        }
        return dartSpelling(t);
    }

    private static boolean mentionsAny(TypeRef t, List<String> names) {
        if (t == null) {
            return false;
        }
        if (t.args.isEmpty() && t.funcParams == null && names.contains(t.name)) {
            return true;
        }
        for (TypeRef a : t.args) {
            if (mentionsAny(a, names)) {
                return true;
            }
        }
        if (t.funcParams != null) {
            for (TypeRef a : t.funcParams) {
                if (mentionsAny(a, names)) {
                    return true;
                }
            }
            return mentionsAny(t.funcReturn, names);
        }
        return false;
    }

    /// The Dart spelling of a type (`List<T>`, `(V) => K`), as a generic signature slot.
    public static String dartSpelling(TypeRef t) {
        if (t == null || t.is("var")) {
            return "dynamic";
        }
        if (t.funcParams != null) {
            StringBuilder sb = new StringBuilder("(");
            for (int i = 0; i < t.funcParams.size(); i++) {
                sb.append(i > 0 ? ", " : "").append(dartSpelling(t.funcParams.get(i)));
            }
            sb.append(") => ").append(dartSpelling(t.funcReturn));
            return t.nullable ? "(" + sb + ")?" : sb.toString();
        }
        StringBuilder sb = new StringBuilder(t.name);
        if (!t.args.isEmpty()) {
            sb.append('<');
            for (int i = 0; i < t.args.size(); i++) {
                sb.append(i > 0 ? ", " : "").append(dartSpelling(t.args.get(i)));
            }
            sb.append('>');
        }
        return t.nullable ? sb.append('?').toString() : sb.toString();
    }

    private static TypeRef eraseTypeParams(TypeRef t, List<String> names) {
        if (t == null) {
            return null;
        }
        if (t.args.isEmpty() && t.funcParams == null && names.contains(t.name)) {
            return TypeRef.DYNAMIC;
        }
        for (int i = 0; i < t.args.size(); i++) {
            t.args.set(i, eraseTypeParams(t.args.get(i), names));
        }
        if (t.funcParams != null) {
            for (int i = 0; i < t.funcParams.size(); i++) {
                t.funcParams.set(i, eraseTypeParams(t.funcParams.get(i), names));
            }
            t.funcReturn = eraseTypeParams(t.funcReturn, names);
        }
        return t;
    }

    /**
     * {@code factory C.x(params) = D.y;} is a factory whose body is
     * {@code return D.y(params);}, every parameter passed on as it was declared: positional
     * ones in order, named ones by name. freezed generates one per model class.
     */
    private CtorDecl buildRedirectingFactory(Dart2Parser.RedirectingFactoryConstructorSignatureContext sig,
            ClassDecl cd) {
        CtorDecl ctor = new CtorDecl();
        pos(ctor, sig);
        ctor.isFactory = true;
        ctor.isConst = sig.CONST_() != null;
        buildCtorName(sig.constructorName(), ctor, cd);
        buildParams(sig.formalParameterList(), ctor.params);
        Dart2Parser.ConstructorDesignationContext des = sig.constructorDesignation();
        CtorCall call = new CtorCall();
        pos(call, des);
        String className;
        if (des.typeName() != null) {
            className = withoutImportPrefix(des.typeName().getText());
            call.ctorName = des.identifier() != null ? des.identifier().getText() : null;
        } else {
            String text = withoutImportPrefix(des.getText());
            int dot = text.indexOf('.');
            className = dot < 0 ? text : text.substring(0, dot);
            call.ctorName = dot < 0 ? null : text.substring(dot + 1);
        }
        call.type = new TypeRef(className);
        if (currentLib != null) {
            currentLib.namedTypes.add(call.type);
        }
        pos(call.type, des);
        if (des.typeArguments() != null) {
            for (Dart2Parser.TypeContext a : des.typeArguments().typeList().type()) {
                call.type.args.add(buildType(a));
            }
        }
        for (Param p : ctor.params) {
            Ident id = new Ident();
            pos(id, des);
            id.name = p.name;
            if (p.named) {
                NamedArg na = new NamedArg();
                na.name = p.name;
                na.value = id;
                call.args.named.add(na);
            } else {
                call.args.positional.add(id);
            }
        }
        ReturnStmt ret = new ReturnStmt();
        pos(ret, des);
        ret.value = call;
        ctor.body = new Block();
        pos(ctor.body, des);
        ctor.body.statements.add(ret);
        return ctor;
    }

    private Expr buildLambda(Dart2Parser.FunctionExpressionContext ctx) {
        Lambda l = new Lambda();
        pos(l, ctx);
        buildParams(ctx.formalParameterPart().formalParameterList(), l.params);
        if (ctx.formalParameterPart().typeParameters() != null) {
            // A generic closure (`<T>(T x) => ...`) cannot be a generic Java lambda; its type
            // parameters are erased to dynamic, which is what an uninstantiated call sees.
            for (Dart2Parser.TypeParameterContext tp : ctx.formalParameterPart().typeParameters().typeParameter()) {
                l.typeParams.add(tp.identifier().getText());
                if (tp.typeNotVoid() != null) {
                    l.typeParamBounds.put(tp.identifier().getText(), buildTypeNotVoid(tp.typeNotVoid()));
                }
            }
            l.typeVarSlots.add(null);
            for (Param p : l.params) {
                String slot = typeVarName(p.type, l.typeParams);
                l.typeVarSlots.add(slot);
                // A bare `T x` is typed by its slot (the instantiation's witness); a type
                // built from one (`List<T> xs`) is kept, so the body types it and a call
                // instantiates it with the call's type arguments.
                if (p.type != null && p.type.args.isEmpty() && p.type.funcParams == null
                        && l.typeParams.contains(p.type.name)) {
                    p.type = eraseTypeParams(p.type, l.typeParams);
                }
            }
        }
        Dart2Parser.FunctionExpressionBodyContext body = ctx.functionExpressionBody();
        if (body.ST() != null) {
            if (body.block() == null) {
                unsupported(body, "E0303", "A generator closure needs a block body");
            } else if (body.SYNC_() != null) {
                l.isSyncStar = true;
            } else {
                l.isAsyncStar = true;
            }
        }
        l.isAsync = body.ASYNC_() != null && body.ST() == null;
        if (body.block() != null) {
            l.body = buildBlock(body.block());
        } else if (body.expr() != null) {
            l.exprBody = buildExpr(body.expr());
        }
        return l;
    }

    // ------------------------------------------------------------------
    // Literals
    // ------------------------------------------------------------------

    private Expr buildLiteral(Dart2Parser.LiteralContext ctx) {
        if (ctx.nullLiteral() != null) {
            NullLit n = new NullLit();
            pos(n, ctx);
            return n;
        }
        if (ctx.booleanLiteral() != null) {
            BoolLit b = new BoolLit();
            pos(b, ctx);
            b.value = ctx.booleanLiteral().TRUE_() != null;
            return b;
        }
        if (ctx.numericLiteral() != null) {
            String text = ctx.numericLiteral().getText();
            if (ctx.numericLiteral().HEX_NUMBER() != null) {
                IntLit i = new IntLit();
                pos(i, ctx);
                // Dart hex ints are 64-bit wrapping; parse as unsigned when needed
                String hex = text.substring(2);
                i.value = Long.parseUnsignedLong(hex, 16);
                return i;
            }
            if (text.contains(".") || text.contains("e") || text.contains("E")) {
                DoubleLit d = new DoubleLit();
                pos(d, ctx);
                d.value = Double.parseDouble(text);
                return d;
            }
            IntLit i = new IntLit();
            pos(i, ctx);
            i.value = Long.parseLong(text);
            return i;
        }
        if (ctx.stringLiteral() != null) {
            return buildString(ctx.stringLiteral());
        }
        if (ctx.listLiteral() != null) {
            return buildListLiteral(ctx.listLiteral());
        }
        if (ctx.setOrMapLiteral() != null) {
            return buildSetOrMapLiteral(ctx.setOrMapLiteral());
        }
        if (ctx.symbolLiteral() != null) {
            // #name: a dart.core.Symbol (the runtime class: ==, hashCode, toString
            // Symbol("name")), built as the call Symbol("name") so `is Symbol` holds.
            StringLit sl = new StringLit();
            pos(sl, ctx);
            sl.parts.add(ctx.symbolLiteral().getText().substring(1).replace(" ", ""));
            Call sym = new Call();
            pos(sym, ctx);
            sym.name = "Symbol";
            sym.symbolLiteral = true;
            sym.args.positional.add(sl);
            return sym;
        }
        unsupported(ctx, "E0124", "Unsupported literal: " + snippet(ctx));
        return errExpr(ctx);
    }

    private Expr buildListLiteral(Dart2Parser.ListLiteralContext ctx) {
        ListLit l = new ListLit();
        pos(l, ctx);
        l.isConst = ctx.CONST_() != null;
        if (ctx.typeArguments() != null) {
            l.elementType = buildType(ctx.typeArguments().typeList().type(0));
        }
        if (ctx.elements() != null) {
            for (Dart2Parser.ElementContext e : ctx.elements().element()) {
                Expr el = buildElement(e);
                if (el != null) {
                    l.elements.add(el);
                }
            }
        }
        if (l.isConst) {
            markConstContext(l);
        }
        return l;
    }

    /**
     * Marks every collection literal in a Dart CONST CONTEXT as const, as the language
     * does implicitly: the elements of a const literal, the arguments of a const
     * constructor call, and the initializer of a const variable. Only the outermost
     * {@code const} is usually written -- {@code const xs = <int>[1]}, {@code const
     * [[1]]}, {@code const Foo([1])} -- and the emitter makes a const literal
     * unmodifiable, so an unmarked inner literal would have stayed growable. Collection
     * literals are marked; constructor and function calls are only looked through, so
     * their own emission is unchanged. A collection-if's condition is a bool, never a
     * collection, and is left alone.
     */
    static void markConstContext(Expr e) {
        if (e == null) {
            return;
        }
        if (e instanceof ListLit) {
            ListLit l = (ListLit) e;
            l.isConst = true;
            for (Expr x : l.elements) {
                markConstContext(x);
            }
        } else if (e instanceof SetLit) {
            SetLit st = (SetLit) e;
            st.isConst = true;
            for (Expr x : st.elements) {
                markConstContext(x);
            }
        } else if (e instanceof MapLit) {
            MapLit m = (MapLit) e;
            m.isConst = true;
            for (Expr x : m.keys) {
                markConstContext(x);
            }
            for (Expr x : m.values) {
                markConstContext(x);
            }
            for (Expr x : m.elements) {
                markConstContext(x);
            }
        } else if (e instanceof Ast.MapEntry) {
            markConstContext(((Ast.MapEntry) e).key);
            markConstContext(((Ast.MapEntry) e).value);
        } else if (e instanceof SpreadElement) {
            markConstContext(((SpreadElement) e).expr);
        } else if (e instanceof IfElement) {
            markConstContext(((IfElement) e).thenElement);
            markConstContext(((IfElement) e).elseElement);
        } else if (e instanceof RecordLit) {
            ((RecordLit) e).isConst = true;
            for (RecordField f : ((RecordLit) e).fields) {
                markConstContext(f.value);
            }
        } else if (e instanceof ParenExpr) {
            markConstContext(((ParenExpr) e).inner);
        } else if (e instanceof Conditional) {
            markConstContext(((Conditional) e).thenExpr);
            markConstContext(((Conditional) e).elseExpr);
        } else if (e instanceof CtorCall) {
            markConstArgs(((CtorCall) e).args);
        } else if (e instanceof Call) {
            markConstArgs(((Call) e).args);
        }
    }

    private static void markConstArgs(Args args) {
        if (args == null) {
            return;
        }
        for (Expr x : args.positional) {
            markConstContext(x);
        }
        for (NamedArg na : args.named) {
            markConstContext(na.value);
        }
    }

    /** One collection-literal element: plain expression, spread, if or for. */
    private Expr buildElement(Dart2Parser.ElementContext e) {
        if (e.expressionElement() != null) {
            return buildExpr(e.expressionElement().expr());
        }
        if (e.spreadElement() != null) {
            SpreadElement s = new SpreadElement();
            pos(s, e);
            s.nullAware = e.spreadElement().DDDQ() != null;
            s.expr = buildExpr(e.spreadElement().expr());
            return s;
        }
        if (e.ifElement() != null) {
            IfElement i = new IfElement();
            pos(i, e);
            i.condition = buildExpr(e.ifElement().expr());
            i.thenElement = buildElement(e.ifElement().element(0));
            if (e.ifElement().element().size() > 1) {
                i.elseElement = buildElement(e.ifElement().element(1));
            }
            return i;
        }
        if (e.forElement() != null) {
            Dart2Parser.ForElementContext f = e.forElement();
            if (f.AWAIT_() != null) {
                unsupported(f, "E0302", "await for elements are not supported yet (M3)");
                return null;
            }
            ForElement fe = new ForElement();
            pos(fe, e);
            Dart2Parser.ForLoopPartsContext parts = f.forLoopParts();
            if (parts.IN_() != null) {
                if (parts.pattern() != null) {
                    // Dart 3 pattern for-in element: `for (final (i, x) in xs.indexed) ...`
                    fe.pattern = buildPattern(parts.pattern());
                    fe.varType = TypeRef.VAR;
                } else if (parts.declaredIdentifier() != null) {
                    fe.varName = parts.declaredIdentifier().identifier().getText();
                    fe.varType = buildFinalConstVarOrType(parts.declaredIdentifier().finalConstVarOrType());
                } else {
                    fe.varName = parts.identifier().getText();
                    fe.varType = TypeRef.VAR;
                }
                fe.iterable = buildExpr(parts.expr());
            } else {
                Dart2Parser.ForInitializerStatementContext init = parts.forInitializerStatement();
                if (init != null) {
                    if (init.localVariableDeclaration() != null) {
                        fe.init = buildLocalVar(init.localVariableDeclaration());
                    } else if (init.expr() != null) {
                        ExprStmt es = new ExprStmt();
                        pos(es, init);
                        es.expr = buildExpr(init.expr());
                        fe.init = es;
                    }
                }
                if (parts.expr() != null) {
                    fe.condition = buildExpr(parts.expr());
                }
                if (parts.expressionList() != null) {
                    for (Dart2Parser.ExprContext u : parts.expressionList().expr()) {
                        fe.updates.add(buildExpr(u));
                    }
                }
            }
            fe.body = buildElement(f.element());
            return fe;
        }
        if (e.mapElement() != null) {
            // A key:value entry — valid when this element is (transitively) inside a map literal.
            MapEntry me = new MapEntry();
            pos(me, e);
            me.key = buildExpr(e.mapElement().expr(0));
            me.value = buildExpr(e.mapElement().expr(1));
            return me;
        }
        return null;
    }

    /**
     * Whether an element is a `k: v` entry, looking through collection-if and
     * collection-for: `{for (var x in xs) x.id: x}` is a map, not a set.
     */
    private static boolean isMapElement(Dart2Parser.ElementContext e) {
        if (e == null) {
            return false;
        }
        if (e.mapElement() != null) {
            return true;
        }
        if (e.forElement() != null) {
            return isMapElement(e.forElement().element());
        }
        if (e.ifElement() != null) {
            for (Dart2Parser.ElementContext b : e.ifElement().element()) {
                if (isMapElement(b)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Expr buildSetOrMapLiteral(Dart2Parser.SetOrMapLiteralContext ctx) {
        // Set vs map disambiguation (matching Dart): explicit <T>{...} or <K,V>{...} decides;
        // otherwise a top-level `k: v` entry means a map, and any other non-empty body is a set.
        List<Dart2Parser.TypeContext> typeArgs = ctx.typeArguments() != null
                ? ctx.typeArguments().typeList().type() : null;
        boolean hasMapEntry = false;
        boolean hasAnyElement = ctx.elements() != null && !ctx.elements().element().isEmpty();
        if (ctx.elements() != null) {
            for (Dart2Parser.ElementContext e : ctx.elements().element()) {
                if (isMapElement(e)) {
                    hasMapEntry = true;
                    break;
                }
            }
        }
        boolean isSet;
        if (typeArgs != null) {
            isSet = typeArgs.size() == 1;
        } else {
            isSet = hasAnyElement && !hasMapEntry;
        }
        if (isSet) {
            SetLit s = new SetLit();
            pos(s, ctx);
            s.isConst = ctx.CONST_() != null;
            if (typeArgs != null && typeArgs.size() == 1) {
                s.elementType = buildType(typeArgs.get(0));
            }
            if (ctx.elements() != null) {
                for (Dart2Parser.ElementContext e : ctx.elements().element()) {
                    Expr el = buildElement(e);
                    if (el != null) {
                        s.elements.add(el);
                    }
                }
            }
            if (s.isConst) {
                markConstContext(s);
            }
            return s;
        }
        MapLit m = new MapLit();
        pos(m, ctx);
        m.isConst = ctx.CONST_() != null;
        if (typeArgs != null && typeArgs.size() >= 2) {
            m.keyType = buildType(typeArgs.get(0));
            m.valueType = buildType(typeArgs.get(1));
        }
        boolean structured = false;
        if (ctx.elements() != null) {
            for (Dart2Parser.ElementContext e : ctx.elements().element()) {
                if (e.ifElement() != null || e.forElement() != null || e.spreadElement() != null) {
                    structured = true;
                    break;
                }
            }
        }
        if (structured) {
            // Collection if/for/spread in a map literal: keep an ordered element list and let the
            // emitter lower it to a DartMap builder (conditional / looped put()).
            m.structured = true;
            if (ctx.elements() != null) {
                for (Dart2Parser.ElementContext e : ctx.elements().element()) {
                    Expr el = buildElement(e);
                    if (el != null) {
                        m.elements.add(el);
                    }
                }
            }
            if (m.isConst) {
                markConstContext(m);
            }
            return m;
        }
        if (ctx.elements() != null) {
            for (Dart2Parser.ElementContext e : ctx.elements().element()) {
                if (e.mapElement() != null) {
                    m.keys.add(buildExpr(e.mapElement().expr(0)));
                    m.values.add(buildExpr(e.mapElement().expr(1)));
                } else {
                    unsupported(e, "E0204", "Map entries are only supported directly inside map literals");
                }
            }
        }
        if (m.isConst) {
            markConstContext(m);
        }
        return m;
    }

    /**
     * Parses a Dart string token (with quotes) into literal/interpolation
     * parts. Handles ', ", r-prefixed raw strings and \ escapes.
     */
    private Expr buildString(Dart2Parser.StringLiteralContext ctx) {
        StringLit lit = new StringLit();
        pos(lit, ctx);
        // adjacent string literals concatenate
        for (int i = 0; i < ctx.getChildCount(); i++) {
            String token = ctx.getChild(i).getText();
            parseStringToken(token, lit, ctx);
        }
        return lit;
    }

    private void parseStringToken(String token, StringLit lit, ParserRuleContext ctx) {
        boolean raw = token.startsWith("r");
        String body = raw ? token.substring(1) : token;
        if (body.length() >= 6 && (body.startsWith("'''") || body.startsWith("\"\"\""))) {
            body = body.substring(3, body.length() - 3);
            if (raw) {
                lit.parts.add(body);
                return;
            }
        } else {
            body = body.substring(1, body.length() - 1);
            if (raw) {
                lit.parts.add(body);
                return;
            }
        }
        StringBuilder cur = new StringBuilder();
        int i = 0;
        int n = body.length();
        while (i < n) {
            char c = body.charAt(i);
            if (c == '\\' && i + 1 < n) {
                char e = body.charAt(i + 1);
                switch (e) {
                    case 'n': cur.append('\n'); break;
                    case 't': cur.append('\t'); break;
                    case 'r': cur.append('\r'); break;
                    case 'b': cur.append('\b'); break;
                    case 'f': cur.append('\f'); break;
                    case '\\': cur.append('\\'); break;
                    case '\'': cur.append('\''); break;
                    case '"': cur.append('"'); break;
                    case '$': cur.append('$'); break;
                    case 'u': {
                        if (i + 2 < n && body.charAt(i + 2) == '{') {
                            int close = body.indexOf('}', i + 3);
                            int cp = Integer.parseInt(body.substring(i + 3, close), 16);
                            cur.appendCodePoint(cp);
                            i = close - 1;
                        } else {
                            int cp = Integer.parseInt(body.substring(i + 2, i + 6), 16);
                            cur.appendCodePoint(cp);
                            i += 4;
                        }
                        break;
                    }
                    default: cur.append(e); break;
                }
                i += 2;
                continue;
            }
            if (c == '$') {
                if (i + 1 < n && body.charAt(i + 1) == '{') {
                    // ${expr} — find matching close brace (no nested strings-with-braces in M1)
                    int depth = 1;
                    int j = i + 2;
                    while (j < n && depth > 0) {
                        char cj = body.charAt(j);
                        if (cj == '{') {
                            depth++;
                        } else if (cj == '}') {
                            depth--;
                        }
                        j++;
                    }
                    if (cur.length() > 0) {
                        lit.parts.add(cur.toString());
                        cur.setLength(0);
                    }
                    String frag = body.substring(i + 2, j - 1);
                    lit.parts.add(parseExprFragment(frag, ctx.getStart().getLine(),
                            ctx.getStart().getCharPositionInLine()));
                    i = j;
                    continue;
                }
                // $identifier
                int j = i + 1;
                while (j < n && (Character.isLetterOrDigit(body.charAt(j)) || body.charAt(j) == '_')) {
                    j++;
                }
                if (j > i + 1) {
                    if (cur.length() > 0) {
                        lit.parts.add(cur.toString());
                        cur.setLength(0);
                    }
                    String simpleName = body.substring(i + 1, j);
                    if ("this".equals(simpleName)) {
                        // `$this` is the receiver itself, never an identifier lookup
                        ThisExpr te = new ThisExpr();
                        te.at(file, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
                        lit.parts.add(te);
                    } else {
                        Ident id = new Ident();
                        id.at(file, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
                        id.name = Dart2LexerBase.mangleObjectMember(simpleName);
                        lit.parts.add(id);
                    }
                    i = j;
                    continue;
                }
            }
            cur.append(c);
            i++;
        }
        if (cur.length() > 0) {
            lit.parts.add(cur.toString());
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Binary binary(ParserRuleContext ctx, Expr left, String op, Expr right) {
        Binary b = new Binary();
        pos(b, ctx);
        b.left = left;
        b.op = op;
        b.right = right;
        return b;
    }

    private String annotationArg(Dart2Parser.MetadataContext meta, String name) {
        if (meta == null) {
            return null;
        }
        for (Dart2Parser.MetadatumContext m : meta.metadatum()) {
            if (m.getText().startsWith(name + "(")) {
                return annotationStringArg(m);
            }
        }
        return null;
    }

    private boolean hasAnnotation(Dart2Parser.MetadataContext meta, String name) {
        if (meta == null) {
            return false;
        }
        for (Dart2Parser.MetadatumContext m : meta.metadatum()) {
            if (m.getText().equals(name) || m.getText().startsWith(name + "(")) {
                return true;
            }
        }
        return false;
    }

    /** Extracts the single string argument of an annotation like @JavaName('x'). */
    public static String annotationStringArg(Dart2Parser.MetadatumContext m) {
        String text = m.getText();
        int open = text.indexOf('(');
        if (open < 0) {
            return null;
        }
        String arg = text.substring(open + 1, text.length() - 1).trim();
        if (arg.length() >= 2 && (arg.charAt(0) == '\'' || arg.charAt(0) == '"')) {
            return arg.substring(1, arg.length() - 1);
        }
        return null;
    }

    private void pos(Node node, ParserRuleContext ctx) {
        node.file = file;
        if (ctx != null) {
            Token t = ctx.getStart();
            node.line = t.getLine();
            node.col = t.getCharPositionInLine();
        }
    }

    private void unsupported(ParserRuleContext ctx, String code, String message) {
        diags.error(file, ctx == null ? 0 : ctx.getStart().getLine(),
                ctx == null ? 0 : ctx.getStart().getCharPositionInLine(), code, message);
    }

    private Expr errExpr(ParserRuleContext ctx) {
        NullLit n = new NullLit();
        if (ctx != null) {
            pos(n, ctx);
        }
        return n;
    }

    /** Dart operator token → mangled Java method name (per the plan's table). */
    public static String mangleOperator(String op) {
        if (op.equals("+")) {
            return "$plus";
        }
        if (op.equals("-")) {
            return "$minus";
        }
        if (op.equals("*")) {
            return "$times";
        }
        if (op.equals("/")) {
            return "$div";
        }
        if (op.equals("~/")) {
            return "$tdiv";
        }
        if (op.equals("%")) {
            return "$mod";
        }
        if (op.equals("[]")) {
            return "$index";
        }
        if (op.equals("[]=")) {
            return "$indexSet";
        }
        if (op.equals("<")) {
            return "$lt";
        }
        if (op.equals(">")) {
            return "$gt";
        }
        if (op.equals("<=")) {
            return "$le";
        }
        if (op.equals(">=")) {
            return "$ge";
        }
        if (op.equals("==")) {
            return "$eq";
        }
        if (op.equals("~")) {
            return "$bitNot";
        }
        if (op.equals("&")) {
            return "$bitAnd";
        }
        if (op.equals("|")) {
            return "$bitOr";
        }
        if (op.equals("^")) {
            return "$bitXor";
        }
        if (op.equals("<<")) {
            return "$shl";
        }
        if (op.equals(">>")) {
            return "$shr";
        }
        if (op.equals(">>>")) {
            return "$ushr";
        }
        return null;
    }

    private String stripQuotes(String uri) {
        if (uri.length() >= 2 && (uri.charAt(0) == '\'' || uri.charAt(0) == '"')) {
            return uri.substring(1, uri.length() - 1);
        }
        return uri;
    }

    private String snippet(ParserRuleContext ctx) {
        String t = ctx.getText();
        return t.length() > 40 ? t.substring(0, 40) + "…" : t;
    }
}
