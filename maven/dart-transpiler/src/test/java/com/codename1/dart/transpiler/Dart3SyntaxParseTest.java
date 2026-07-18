package com.codename1.dart.transpiler;

import com.codename1.dart.transpiler.parser.Dart2Lexer;
import com.codename1.dart.transpiler.parser.Dart2Parser;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The vendored grammar is spec-Dart-2.15; Dart 3 syntax (class modifiers,
 * patterns, switch expressions, records) is a CN1 extension. These parse-level
 * tests pin that the extension keeps accepting Dart 3 — and that the soft
 * keywords it introduces (base/sealed/when) still work as identifiers.
 */
public class Dart3SyntaxParseTest {

    private void parses(String source) {
        Dart2Lexer lexer = new Dart2Lexer(CharStreams.fromString(source));
        Dart2Parser parser = new Dart2Parser(new CommonTokenStream(lexer));
        final List<String> errors = new ArrayList<String>();
        BaseErrorListener listener = new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> r, Object sym, int line, int col,
                                    String msg, RecognitionException e) {
                errors.add(line + ":" + col + " " + msg);
            }
        };
        parser.removeErrorListeners();
        lexer.removeErrorListeners();
        parser.addErrorListener(listener);
        lexer.addErrorListener(listener);
        parser.compilationUnit();
        assertTrue(errors.isEmpty(), "syntax errors: " + errors + "\nin:\n" + source);
    }

    // ------------------------------------------------------------------
    // Class modifiers
    // ------------------------------------------------------------------

    @Test
    public void sealedClassHierarchy() {
        parses("sealed class Shape {}\n"
                + "final class Circle extends Shape { final double r; Circle(this.r); }\n"
                + "final class Square extends Shape { final double side; Square(this.side); }\n");
    }

    @Test
    public void otherClassModifiers() {
        parses("base class A {}\n"
                + "interface class B {}\n"
                + "abstract base class C {}\n"
                + "final class D {}\n");
    }

    // ------------------------------------------------------------------
    // Switch expressions + patterns
    // ------------------------------------------------------------------

    @Test
    public void switchExpressionWithObjectPatterns() {
        parses("sealed class Shape {}\n"
                + "double area(Shape s) => switch (s) {\n"
                + "  Circle(r: var r) => 3.14 * r * r,\n"
                + "  Square(side: var x) => x * x,\n"
                + "};\n");
    }

    @Test
    public void switchExpressionWithConstantAndWildcard() {
        parses("String name(int i) => switch (i) {\n"
                + "  0 => 'zero',\n"
                + "  1 => 'one',\n"
                + "  _ => 'many',\n"
                + "};\n");
    }

    @Test
    public void switchExpressionWithGuard() {
        parses("String size(int i) => switch (i) {\n"
                + "  int n when n > 100 => 'big',\n"
                + "  _ => 'small',\n"
                + "};\n");
    }

    @Test
    public void switchStatementWithPatternsAndGuards() {
        parses("void f(Object o) {\n"
                + "  switch (o) {\n"
                + "    case int n when n > 0:\n"
                + "      print('pos');\n"
                + "    case String s:\n"
                + "      print(s);\n"
                + "    default:\n"
                + "      print('other');\n"
                + "  }\n"
                + "}\n");
    }

    @Test
    public void relationalAndLogicalPatterns() {
        parses("String f(int i) => switch (i) {\n"
                + "  < 0 => 'neg',\n"
                + "  == 0 => 'zero',\n"
                + "  _ => 'pos',\n"
                + "};\n");
    }

    // ------------------------------------------------------------------
    // Records
    // ------------------------------------------------------------------

    @Test
    public void positionalRecordLiteral() {
        parses("void f() {\n"
                + "  var pair = (1, 'a');\n"
                + "  print(pair);\n"
                + "}\n");
    }

    @Test
    public void namedRecordLiteral() {
        parses("void f() {\n"
                + "  var p = (x: 1, y: 2);\n"
                + "  print(p);\n"
                + "}\n");
    }

    @Test
    public void parenthesizedExpressionStillParsesAsSuch() {
        // the record grammar must not swallow ordinary parentheses
        parses("void f() {\n"
                + "  var x = (1 + 2) * 3;\n"
                + "  print(x);\n"
                + "}\n");
    }

    // ------------------------------------------------------------------
    // Soft keywords stay usable as identifiers
    // ------------------------------------------------------------------

    @Test
    public void softKeywordsRemainIdentifiers() {
        parses("void f() {\n"
                + "  var base = 1;\n"
                + "  var sealed = 2;\n"
                + "  var when = 3;\n"
                + "  print(base + sealed + when);\n"
                + "}\n");
    }
}
