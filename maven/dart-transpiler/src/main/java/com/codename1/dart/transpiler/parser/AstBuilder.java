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
        pos(lib, unit);
        if (unit.libraryDeclaration() != null) {
            buildLibrary(unit.libraryDeclaration(), lib);
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

    private void buildLibrary(Dart2Parser.LibraryDeclarationContext ctx, Library lib) {
        for (Dart2Parser.ImportOrExportContext ie : ctx.importOrExport()) {
            if (ie.libraryImport() != null) {
                String uri = ie.libraryImport().importSpecification().configurableUri().getText();
                lib.imports.add(stripQuotes(uri));
            }
        }
        List<Dart2Parser.TopLevelDeclarationContext> decls = ctx.topLevelDeclaration();
        List<Dart2Parser.MetadataContext> metas = ctx.metadata();
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
                lib.classes.add(cd);
            }
        } else if (ctx.extensionDeclaration() != null) {
            Dart2Parser.ExtensionDeclarationContext ext = ctx.extensionDeclaration();
            ClassDecl cd = new ClassDecl();
            pos(cd, ext);
            cd.name = ext.identifier() != null ? ext.identifier().getText()
                    : "Ext$" + Integer.toHexString(ext.getStart().getStartIndex());
            cd.extensionOn = buildType(ext.type());
            java.util.List<Dart2Parser.ClassMemberDeclarationContext> members = ext.classMemberDeclaration();
            java.util.List<Dart2Parser.MetadataContext> metas = ext.metadata();
            for (int i = 0; i < members.size(); i++) {
                buildMember(members.get(i), metas.size() > i ? metas.get(i) : null, cd);
            }
            lib.classes.add(cd);
        } else if (ctx.mixinDeclaration() != null) {
            Dart2Parser.MixinDeclarationContext mx = ctx.mixinDeclaration();
            ClassDecl cd = new ClassDecl();
            pos(cd, mx);
            cd.name = mx.typeIdentifier().getText();
            cd.isMixin = true;
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
            if (ctx.initializedIdentifierList() != null) {
                for (Dart2Parser.InitializedIdentifierContext ii : ctx.initializedIdentifierList().initializedIdentifier()) {
                    FieldDecl f = new FieldDecl();
                    pos(f, ii);
                    f.name = ii.identifier().getText();
                    f.type = type;
                    f.isFinal = isFinal;
                    f.isStatic = true;
                    if (ii.expr() != null) {
                        f.initializer = buildExpr(ii.expr());
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
                    f.isStatic = true;
                    f.initializer = buildExpr(sf.expr());
                    lib.topLevelVars.add(f);
                }
            }
        } else {
            unsupported(ctx, "E0103", "Unsupported top-level declaration: " + snippet(ctx));
        }
    }

    private ClassDecl buildClass(Dart2Parser.ClassDeclarationContext ctx) {
        if (ctx.mixinApplicationClass() != null) {
            unsupported(ctx, "E0104", "Mixin application classes are not supported yet (M4)");
            return null;
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
        return cd;
    }

    private EnumDecl buildEnum(Dart2Parser.EnumTypeContext ctx) {
        EnumDecl ed = new EnumDecl();
        pos(ed, ctx);
        ed.name = ctx.identifier().getText();
        for (Dart2Parser.EnumEntryContext e : ctx.enumEntry()) {
            ed.entries.add(e.identifier().getText());
        }
        return ed;
    }

    private void buildMember(Dart2Parser.ClassMemberDeclarationContext ctx,
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
                unsupported(d, "E0106", "Redirecting constructors are not supported yet (M2)");
            }
            cd.ctors.add(ctor);
            return;
        }
        if (d.redirectingFactoryConstructorSignature() != null) {
            unsupported(d, "E0106", "Redirecting factory constructors are not supported yet");
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
                if (!m.isStatic && tmp.returnType != null && tmp.returnType.is(cd.name)
                        && d.EXTERNAL_() == null) {
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

    private void buildMethodWithBody(Dart2Parser.MethodSignatureContext sig,
                                     Dart2Parser.FunctionBodyContext body,
                                     boolean override, ClassDecl cd) {
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
                } else {
                    unsupported(ie, "E0110", "Cascades in initializer lists are not supported");
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
            } else {
                unsupported(e, "E0111", "assert(...) in initializer lists is ignored");
            }
        }
    }

    private void buildFunctionSignature(Dart2Parser.FunctionSignatureContext sig, FunctionDecl fn) {
        fn.returnType = sig.type() != null ? buildType(sig.type()) : TypeRef.VAR;
        fn.name = sig.identifier().getText();
        if (sig.formalParameterPart().typeParameters() != null) {
            unsupported(sig, "E0112", "Generic methods are not supported yet (M2)");
        }
        buildParams(sig.formalParameterPart().formalParameterList(), fn.params);
    }

    private void buildFunctionBodyInto(Dart2Parser.FunctionBodyContext body, FunctionDecl fn) {
        fn.isAsync = checkBodyModifiers(body);
        if (body.block() != null) {
            fn.body = buildBlock(body.block());
        } else if (body.expr() != null) {
            fn.exprBody = buildExpr(body.expr());
        }
    }

    private void buildFunctionBodyIntoMethod(Dart2Parser.FunctionBodyContext body, MethodDecl m) {
        m.isAsync = checkBodyModifiers(body);
        if (body.block() != null) {
            m.body = buildBlock(body.block());
        } else if (body.expr() != null) {
            m.exprBody = buildExpr(body.expr());
        }
    }

    /** Returns true when the body is async (plain `async`, not a generator). */
    private boolean checkBodyModifiers(Dart2Parser.FunctionBodyContext body) {
        if (body.ST() != null) {
            unsupported(body, "E0303", "Generator bodies (sync*/async*) are not supported yet (M5)");
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
            unsupported(ctx, "E0114", "Function-typed parameter syntax is not supported yet; use a typedef-style type");
            return null;
        }
        return null;
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
            // Function types appear in stubs (e.g. VoidCallback typedefs cover most cases).
            TypeRef t = new TypeRef("Function");
            pos(t, ctx);
            t.nullable = ctx.QU() != null;
            return t;
        }
        return buildTypeNotFunction(ctx.typeNotFunction());
    }

    private TypeRef buildTypeNotFunction(Dart2Parser.TypeNotFunctionContext ctx) {
        if (ctx.VOID_() != null) {
            return TypeRef.VOID;
        }
        return buildTypeNotVoidNotFunction(ctx.typeNotVoidNotFunction());
    }

    private TypeRef buildTypeNotVoid(Dart2Parser.TypeNotVoidContext ctx) {
        if (ctx.functionType() != null) {
            TypeRef t = new TypeRef("Function");
            pos(t, ctx);
            return t;
        }
        return buildTypeNotVoidNotFunction(ctx.typeNotVoidNotFunction());
    }

    private TypeRef buildTypeNotVoidNotFunction(Dart2Parser.TypeNotVoidNotFunctionContext ctx) {
        if (ctx.typeName() == null) {
            TypeRef t = new TypeRef("Function");
            pos(t, ctx);
            return t;
        }
        TypeRef t = new TypeRef(ctx.typeName().getText());
        pos(t, ctx);
        if (ctx.typeArguments() != null) {
            for (Dart2Parser.TypeContext a : ctx.typeArguments().typeList().type()) {
                t.args.add(buildType(a));
            }
        }
        t.nullable = ctx.QU() != null;
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
        Dart2Parser.NonLabelledStatementContext s = ctx.nonLabelledStatement();
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
        if (s.forStatement() != null) {
            return buildFor(s.forStatement());
        }
        if (s.tryStatement() != null) {
            return buildTry(s.tryStatement());
        }
        if (s.breakStatement() != null) {
            BreakStmt b = new BreakStmt();
            pos(b, s);
            return b;
        }
        if (s.continueStatement() != null) {
            ContinueStmt c = new ContinueStmt();
            pos(c, s);
            return c;
        }
        unsupported(s, "E0115", "Unsupported statement: " + snippet(s));
        return null;
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
        VarDeclStmt v = new VarDeclStmt();
        pos(v, ctx);
        v.name = di.identifier().getText();
        v.type = buildFinalConstVarOrType(di.finalConstVarOrType());
        v.isFinal = di.finalConstVarOrType().FINAL_() != null || di.finalConstVarOrType().CONST_() != null;
        if (iv.expr() != null) {
            v.initializer = buildExpr(iv.expr());
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
            if (ii.expr() != null) {
                extra.initializer = buildExpr(ii.expr());
            }
            group.decls.add(extra);
        }
        return group;
    }

    private Stmt buildFor(Dart2Parser.ForStatementContext ctx) {
        if (ctx.AWAIT_() != null) {
            unsupported(ctx, "E0302", "await for is not supported yet (M3)");
            return null;
        }
        Dart2Parser.ForLoopPartsContext parts = ctx.forLoopParts();
        if (parts.IN_() != null) {
            ForInStmt fi = new ForInStmt();
            pos(fi, ctx);
            if (parts.declaredIdentifier() != null) {
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
            return a;
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
        if (cur.QUDD() != null) {
            unsupported(ctx, "E0203", "Null-aware cascades (?..) are not supported yet");
        }
        Cascade cas = new Cascade();
        pos(cas, ctx);
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

    private Expr buildExprWithoutCascade(Dart2Parser.ExpressionWithoutCascadeContext ctx) {
        if (ctx.assignableExpression() != null && ctx.assignmentOperator() != null) {
            Assign a = new Assign();
            pos(a, ctx);
            a.lhs = buildAssignable(ctx.assignableExpression());
            a.op = ctx.assignmentOperator().getText();
            a.rhs = buildExprWithoutCascade(ctx.expressionWithoutCascade());
            return a;
        }
        if (ctx.conditionalExpression() != null) {
            return buildConditional(ctx.conditionalExpression());
        }
        unsupported(ctx, "E0118", "throw expressions are not supported yet (M2)");
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
            unsupported(ctx, "E0119", "Assignment through 'super' is not supported");
            return errExpr(ctx);
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
            unsupported(ctx, "E0120", "super == is not supported");
            return errExpr(ctx);
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
        return applyAssignableSelector(base, s.assignableSelector());
    }

    private Expr applyUnconditional(Expr base, Dart2Parser.UnconditionalAssignableSelectorContext u) {
        if (u.identifier() != null) {
            PropertyGet pg = new PropertyGet();
            pos(pg, u);
            pg.target = base;
            pg.name = u.identifier().getText();
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
            for (Dart2Parser.TypeContext t : ci.typeArguments().typeList().type()) {
                cc.type.args.add(buildType(t));
            }
            cc.ctorName = ci.identifier().getText();
            buildArgs(ci.arguments(), cc.args);
            return cc;
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
        } else if (d.qualifiedName() != null) {
            // X.named — type X, named ctor
            String text = d.qualifiedName().getText();
            int dot = text.indexOf('.');
            cc.type = new TypeRef(text.substring(0, dot));
            cc.ctorName = text.substring(dot + 1);
        } else {
            cc.type = new TypeRef(d.typeName().getText());
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
        return cc;
    }

    private Expr buildLambda(Dart2Parser.FunctionExpressionContext ctx) {
        Lambda l = new Lambda();
        pos(l, ctx);
        if (ctx.formalParameterPart().typeParameters() != null) {
            unsupported(ctx, "E0112", "Generic closures are not supported");
        }
        buildParams(ctx.formalParameterPart().formalParameterList(), l.params);
        Dart2Parser.FunctionExpressionBodyContext body = ctx.functionExpressionBody();
        if (body.SYNC_() != null || body.ST() != null) {
            unsupported(body, "E0303", "Generator closures are not supported");
        }
        l.isAsync = body.ASYNC_() != null;
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
        return l;
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
                if (parts.declaredIdentifier() != null) {
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
            unsupported(e, "E0204", "Map entries are only supported directly inside map literals");
            return null;
        }
        return null;
    }

    private Expr buildSetOrMapLiteral(Dart2Parser.SetOrMapLiteralContext ctx) {
        MapLit m = new MapLit();
        pos(m, ctx);
        m.isConst = ctx.CONST_() != null;
        if (ctx.typeArguments() != null) {
            List<Dart2Parser.TypeContext> args = ctx.typeArguments().typeList().type();
            if (args.size() == 1) {
                unsupported(ctx, "E0202", "Set literals are not supported yet (M2)");
                return errExpr(ctx);
            }
            m.keyType = buildType(args.get(0));
            m.valueType = buildType(args.get(1));
        }
        if (ctx.elements() != null) {
            for (Dart2Parser.ElementContext e : ctx.elements().element()) {
                if (e.mapElement() != null) {
                    m.keys.add(buildExpr(e.mapElement().expr(0)));
                    m.values.add(buildExpr(e.mapElement().expr(1)));
                } else if (e.expressionElement() != null) {
                    unsupported(e, "E0202", "Set literals are not supported yet (M2)");
                } else {
                    unsupported(e, "E0201", "Collection if/for/spread elements are not supported yet (M2)");
                }
            }
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
                    Ident id = new Ident();
                    id.at(file, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
                    id.name = body.substring(i + 1, j);
                    lit.parts.add(id);
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
