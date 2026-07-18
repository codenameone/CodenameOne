package com.codename1.dart.transpiler.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * The transpiler's own immutable-ish AST. The ANTLR parse tree is converted
 * to these nodes by AstBuilder and never escapes the parser package — this
 * is the seam that absorbs grammar churn.
 *
 * <p>Node classes are deliberately compact: public fields, one file. Only
 * the M1 language subset is modeled; AstBuilder reports a source-positioned
 * diagnostic for anything else.</p>
 */
public final class Ast {

    private Ast() {
    }

    /** Source position carried by every node for diagnostics/source maps. */
    public static class Node {
        public String file;
        public int line;
        public int col;

        public <T extends Node> T at(String file, int line, int col) {
            this.file = file;
            this.line = line;
            this.col = col;
            @SuppressWarnings("unchecked")
            T self = (T) this;
            return self;
        }
    }

    // ------------------------------------------------------------------
    // Types
    // ------------------------------------------------------------------

    /** A resolved-enough Dart type reference: name, args, nullability. */
    public static class TypeRef extends Node {
        public String name;                       // "int", "String", "List", "Widget", "MyApp", "void", "var", "dynamic"
        public List<TypeRef> args = new ArrayList<TypeRef>();
        public boolean nullable;

        public TypeRef(String name) {
            this.name = name;
        }

        public static TypeRef of(String name, TypeRef... args) {
            TypeRef t = new TypeRef(name);
            for (TypeRef a : args) {
                t.args.add(a);
            }
            return t;
        }

        public boolean is(String n) {
            return name.equals(n);
        }

        public TypeRef arg(int i) {
            return i < args.size() ? args.get(i) : DYNAMIC;
        }

        public static final TypeRef VAR = new TypeRef("var");
        public static final TypeRef DYNAMIC = new TypeRef("dynamic");
        public static final TypeRef VOID = new TypeRef("void");
        public static final TypeRef INT = new TypeRef("int");
        public static final TypeRef DOUBLE = new TypeRef("double");
        public static final TypeRef BOOL = new TypeRef("bool");
        public static final TypeRef STRING = new TypeRef("String");
        public static final TypeRef NULL = new TypeRef("Null");

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(name);
            if (!args.isEmpty()) {
                sb.append('<');
                for (int i = 0; i < args.size(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(args.get(i));
                }
                sb.append('>');
            }
            if (nullable) {
                sb.append('?');
            }
            return sb.toString();
        }
    }

    // ------------------------------------------------------------------
    // Declarations
    // ------------------------------------------------------------------

    public static class Library extends Node {
        public String fileName;                   // e.g. "main.dart" (relative to source root)
        public List<String> imports = new ArrayList<String>();
        public List<ClassDecl> classes = new ArrayList<ClassDecl>();
        public List<EnumDecl> enums = new ArrayList<EnumDecl>();
        public List<FunctionDecl> functions = new ArrayList<FunctionDecl>();
        public List<FieldDecl> topLevelVars = new ArrayList<FieldDecl>();
    }

    public static class ClassDecl extends Node {
        public String name;
        public String javaName;                   // from @JavaName('...') in stub files
        public boolean isAbstract;
        public boolean isSealed;                  // Dart 3: sealed class C { }
        public boolean isMixin;                   // mixin M { }
        public TypeRef extensionOn;               // extension X on T { } — non-null marks an extension
        public List<TypeRef> mixins = new ArrayList<TypeRef>();  // class C with M1, M2
        public TypeRef superclass;                // null if none/Object
        public List<TypeRef> interfaces = new ArrayList<TypeRef>();
        public List<String> typeParams = new ArrayList<String>();
        public List<FieldDecl> fields = new ArrayList<FieldDecl>();
        public List<CtorDecl> ctors = new ArrayList<CtorDecl>();
        public List<MethodDecl> methods = new ArrayList<MethodDecl>();

        public FieldDecl field(String name) {
            for (FieldDecl f : fields) {
                if (f.name.equals(name)) {
                    return f;
                }
            }
            return null;
        }

        public MethodDecl method(String name) {
            for (MethodDecl m : methods) {
                if (m.name.equals(name) && !m.isGetter && !m.isSetter) {
                    return m;
                }
            }
            return null;
        }

        public MethodDecl getter(String name) {
            for (MethodDecl m : methods) {
                if (m.name.equals(name) && m.isGetter) {
                    return m;
                }
            }
            return null;
        }

        /** The canonical constructor parameter order: positional then named (declared order). */
        public CtorDecl defaultCtor() {
            for (CtorDecl c : ctors) {
                if (c.name == null) {
                    return c;
                }
            }
            return null;
        }

        public CtorDecl namedCtor(String name) {
            for (CtorDecl c : ctors) {
                if (name.equals(c.name)) {
                    return c;
                }
            }
            return null;
        }

        public boolean hasNamedNonFactoryCtor() {
            for (CtorDecl c : ctors) {
                if (c.name != null && !c.isFactory) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class EnumDecl extends Node {
        public String name;
        public String javaName;                   // from @JavaName('...') in stub files
        public List<String> entries = new ArrayList<String>();
    }

    public static class FieldDecl extends Node {
        public TypeRef type;                      // may be VAR
        public String name;
        public Expr initializer;                  // nullable
        public boolean isFinal;
        public boolean isConst;
        public boolean isStatic;
        public boolean isLate;
    }

    public static class Param extends Node {
        public TypeRef type;                      // may be VAR (inferred from field for this./super.)
        public String name;
        public boolean named;
        public boolean required;
        public boolean isThis;                    // this.x
        public boolean isSuper;                   // super.x
        public Expr defaultValue;                 // nullable
    }

    public static class CtorDecl extends Node {
        public String name;                       // named constructor, null for unnamed
        public boolean isConst;
        public boolean isFactory;
        public List<Param> params = new ArrayList<Param>();
        public List<FieldInit> fieldInits = new ArrayList<FieldInit>();  // initializer list entries
        public SuperInit superInit;               // nullable
        public Block body;                        // nullable (";" body)
    }

    public static class FieldInit extends Node {
        public String field;
        public Expr value;
    }

    public static class SuperInit extends Node {
        public String namedCtor;                  // nullable
        public Args args = new Args();
    }

    public static class MethodDecl extends Node {
        public TypeRef returnType;                // may be VAR (=> inferred) or VOID
        public String name;
        public List<Param> params = new ArrayList<Param>();
        public boolean isStatic;
        public boolean isGetter;
        public boolean isSetter;
        public boolean isOverride;                // had @override metadata
        public boolean isAbstract;                // no body
        public boolean isAsync;
        public Block body;                        // nullable when isAbstract or expression-bodied
        public Expr exprBody;                     // for `=> expr`
    }

    public static class FunctionDecl extends Node {
        public TypeRef returnType;
        public String name;
        public String javaName;                   // from @JavaName('...') in stub files
        public boolean isExternal;
        public boolean isAsync;
        public List<Param> params = new ArrayList<Param>();
        public Block body;
        public Expr exprBody;
    }

    // ------------------------------------------------------------------
    // Statements
    // ------------------------------------------------------------------

    public static abstract class Stmt extends Node {
    }

    public static class Block extends Stmt {
        public List<Stmt> statements = new ArrayList<Stmt>();
    }

    public static class ExprStmt extends Stmt {
        public Expr expr;
    }

    public static class VarDeclStmt extends Stmt {
        public TypeRef type;                      // may be VAR
        public boolean isFinal;
        public String name;
        public Expr initializer;                  // nullable
    }

    /** int a = 1, b = 2; — emitted as sibling declarations, no brace scope. */
    public static class VarDeclGroup extends Stmt {
        public List<VarDeclStmt> decls = new ArrayList<VarDeclStmt>();
    }

    public static class IfStmt extends Stmt {
        public Expr condition;
        public Stmt thenStmt;
        public Stmt elseStmt;                     // nullable
    }

    public static class WhileStmt extends Stmt {
        public Expr condition;
        public Stmt body;
    }

    public static class ForStmt extends Stmt {
        public Stmt init;                         // VarDeclStmt or ExprStmt or null
        public Expr condition;                    // nullable
        public List<Expr> updates = new ArrayList<Expr>();
        public Stmt body;
    }

    public static class ForInStmt extends Stmt {
        public TypeRef varType;                   // may be VAR
        public String varName;
        public Expr iterable;
        public Stmt body;
    }

    public static class ReturnStmt extends Stmt {
        public Expr value;                        // nullable
    }

    public static class BreakStmt extends Stmt {
    }

    public static class ContinueStmt extends Stmt {
    }

    // ------------------------------------------------------------------
    // Expressions
    // ------------------------------------------------------------------

    public static abstract class Expr extends Node {
    }

    public static class IntLit extends Expr {
        public long value;
    }

    public static class DoubleLit extends Expr {
        public double value;
    }

    public static class BoolLit extends Expr {
        public boolean value;
    }

    public static class NullLit extends Expr {
    }

    /** String literal made of literal text parts and interpolated expressions. */
    public static class StringLit extends Expr {
        public List<Object> parts = new ArrayList<Object>();  // String | Expr
    }

    public static class ListLit extends Expr {
        public TypeRef elementType;               // nullable
        public List<Expr> elements = new ArrayList<Expr>();
        public boolean isConst;
    }

    public static class MapLit extends Expr {
        public TypeRef keyType;                   // nullable
        public TypeRef valueType;
        public List<Expr> keys = new ArrayList<Expr>();
        public List<Expr> values = new ArrayList<Expr>();
        public boolean isConst;
    }

    public static class Ident extends Expr {
        public String name;
    }

    public static class ThisExpr extends Expr {
    }

    public static class SuperExpr extends Expr {
    }

    /** throw expr (statement position only in M2). */
    public static class ThrowExpr extends Expr {
        public Expr value;
    }

    /** await expr */
    public static class AwaitExpr extends Expr {
        public Expr operand;
    }

    public static class CatchClause extends Node {
        public TypeRef onType;                    // null for bare catch
        public String exceptionVar;               // null when `on T { }` has no catch part
        public String stackVar;                   // nullable
        public Block body;
    }

    public static class TryStmt extends Stmt {
        public Block tryBlock;
        public List<CatchClause> catches = new ArrayList<CatchClause>();
        public Block finallyBlock;                // nullable
    }

    /** target..a()..b = c — target evaluated once, sections applied to it. */
    public static class Cascade extends Expr {
        public Expr target;
        /** Each section is an Expr tree rooted at a CascadeTarget marker. */
        public List<Expr> sections = new ArrayList<Expr>();
    }

    /** Marker for the implicit receiver inside a cascade section. */
    public static class CascadeTarget extends Expr {
    }

    /** ...expr / ...?expr inside a collection literal. */
    public static class SpreadElement extends Expr {
        public Expr expr;
        public boolean nullAware;
    }

    /** if (cond) elem [else elem] inside a collection literal. */
    public static class IfElement extends Expr {
        public Expr condition;
        public Expr thenElement;
        public Expr elseElement;              // nullable
    }

    /** for (…) elem inside a collection literal (for-in or classic). */
    public static class ForElement extends Expr {
        public TypeRef varType;               // for-in var (may be VAR); null for classic
        public String varName;                // for-in variable; null for classic
        public Expr iterable;                 // for-in source; null for classic
        public Stmt init;                     // classic parts (VarDeclStmt/ExprStmt)
        public Expr condition;
        public List<Expr> updates = new ArrayList<Expr>();
        public Expr body;                     // the element produced per iteration
    }

    /** target.name (or target?.name when nullAware). */
    public static class PropertyGet extends Expr {
        public Expr target;
        public String name;
        public boolean nullAware;
    }

    /** target.name(args) — method call, or function/ctor call when target == null. */
    public static class Call extends Expr {
        public Expr target;                       // null for bare calls
        public String name;                       // method or function/class name; null when calling an expression value
        public Args args = new Args();
        public boolean nullAware;
        public List<TypeRef> typeArgs = new ArrayList<TypeRef>();
    }

    /** Explicit `new X(...)`/`const X(...)`/`X.named(...)` when syntactically unambiguous. */
    public static class CtorCall extends Expr {
        public TypeRef type;
        public String ctorName;                   // nullable (unnamed)
        public Args args = new Args();
        public boolean isConst;
    }

    public static class Args {
        public List<Expr> positional = new ArrayList<Expr>();
        public List<NamedArg> named = new ArrayList<NamedArg>();
    }

    public static class NamedArg {
        public String name;
        public Expr value;
    }

    public static class IndexGet extends Expr {
        public Expr target;
        public Expr index;
    }

    public static class Assign extends Expr {
        public Expr lhs;                          // Ident | PropertyGet | IndexGet
        public String op;                         // "=", "+=", "-=", "*=", "/=", "~/=", "%=", "??="
        public Expr rhs;
    }

    public static class Binary extends Expr {
        public Expr left;
        public String op;                         // + - * / ~/ % == != < > <= >= && || ??
        public Expr right;
    }

    public static class Unary extends Expr {
        public String op;                         // "-", "!", "~"
        public Expr operand;
    }

    /** ++x / --x / x++ / x-- */
    public static class IncDec extends Expr {
        public Expr operand;
        public boolean increment;
        public boolean prefix;
    }

    public static class Conditional extends Expr {
        public Expr condition;
        public Expr thenExpr;
        public Expr elseExpr;
    }

    /** x! */
    public static class NotNullAssert extends Expr {
        public Expr operand;
    }

    /** x is T / x is! T */
    public static class IsTest extends Expr {
        public Expr operand;
        public TypeRef type;
        public boolean negated;
    }

    /** x as T */
    public static class AsCast extends Expr {
        public Expr operand;
        public TypeRef type;
    }

    public static class Lambda extends Expr {
        public boolean isAsync;
        public List<Param> params = new ArrayList<Param>();
        public Block body;                        // nullable
        public Expr exprBody;                     // for `=> expr`
    }

    public static class ParenExpr extends Expr {
        public Expr inner;
    }
}
