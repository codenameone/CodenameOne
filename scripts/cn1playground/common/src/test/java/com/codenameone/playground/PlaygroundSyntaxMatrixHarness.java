package com.codenameone.playground;

import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Table-driven syntax regression matrix for playground language support.
 * Each case records its OBSERVED outcome on the current runtime, not its
 * aspirational outcome. Fixes that flip a case (PARSE_ERROR or EVAL_ERROR
 * to SUCCESS) must update the expected outcome here in the same change.
 */
public final class PlaygroundSyntaxMatrixHarness {
    private PlaygroundSyntaxMatrixHarness() {
    }

    private enum ExpectedOutcome {
        SUCCESS,
        PARSE_ERROR,
        EVAL_ERROR
    }

    private static final class Case {
        final String category;
        final String name;
        final String sourceSnippet;
        final ExpectedOutcome expectedOutcome;
        final String expectedDiagnosticSubstring;

        Case(String category, String name, String sourceSnippet,
                ExpectedOutcome expectedOutcome, String expectedDiagnosticSubstring) {
            this.category = category;
            this.name = name;
            this.sourceSnippet = sourceSnippet;
            this.expectedOutcome = expectedOutcome;
            this.expectedDiagnosticSubstring = expectedDiagnosticSubstring;
        }
    }

    /** Standard imports + root container, returning root at the end. */
    private static String ui(String body) {
        return ""
                + "import com.codename1.ui.*;\n"
                + "import com.codename1.ui.events.*;\n"
                + "import com.codename1.ui.layouts.*;\n"
                + "import java.util.*;\n"
                + "import java.io.*;\n"
                + "Container root = new Container(BoxLayout.y());\n"
                + body
                + "\nroot;\n";
    }

    /** Raw snippet (no standard prefix). */
    private static String raw(String body) {
        return body;
    }

    public static void main(String[] args) {
        int exitCode = 0;
        try {
            List<Case> cases = new ArrayList<Case>();
            addCases(cases);

            List<String> failures = new ArrayList<String>();
            int passed = 0;
            for (Case testCase : cases) {
                PlaygroundRunner.RunResult result = runSnippet(testCase.sourceSnippet);
                ExpectedOutcome actual = classify(result);
                String diagnostic = firstDiagnosticMessage(result);

                boolean outcomeOk = actual == testCase.expectedOutcome;
                boolean diagnosticOk = true;
                if (testCase.expectedDiagnosticSubstring != null) {
                    diagnosticOk = diagnostic != null
                            && diagnostic.indexOf(testCase.expectedDiagnosticSubstring) >= 0;
                }
                if (outcomeOk && diagnosticOk) {
                    passed++;
                    continue;
                }

                StringBuilder msg = new StringBuilder();
                msg.append("[").append(testCase.category).append("/").append(testCase.name).append("] ");
                if (!outcomeOk) {
                    msg.append("outcome expected=").append(testCase.expectedOutcome)
                            .append(" actual=").append(actual);
                }
                if (!diagnosticOk) {
                    if (!outcomeOk) msg.append("; ");
                    msg.append("diagnostic expected~='")
                            .append(testCase.expectedDiagnosticSubstring)
                            .append("' actual='").append(diagnostic).append("'");
                }
                if (diagnostic != null) {
                    msg.append(" diag=").append(trim(diagnostic, 400));
                }
                failures.add(msg.toString());
            }

            System.out.println("Playground syntax matrix: "
                    + passed + "/" + cases.size() + " passed");
            if (!failures.isEmpty()) {
                System.out.println("Failures (" + failures.size() + "):");
                for (int i = 0; i < failures.size(); i++) {
                    System.out.println("  - " + failures.get(i));
                }
                throw new IllegalStateException("Matrix mismatches: " + failures.size()
                        + " of " + cases.size());
            }
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            exitCode = 1;
        } finally {
            System.exit(exitCode);
        }
    }

    private static void addCases(List<Case> cases) {
        addControl(cases);
        addLiterals(cases);
        addOperators(cases);
        addControlFlow(cases);
        addExceptions(cases);
        addGenerics(cases);
        addLambdas(cases);
        addMethodReferences(cases);
        addEnhancedFor(cases);
        addTryWithResources(cases);
        addClassDeclarations(cases);
        addInterfaceDeclarations(cases);
        addEnumDeclarations(cases);
        addRecords(cases);
        addSealed(cases);
        addSwitchExpressions(cases);
        addPatternMatching(cases);
        addTextBlocks(cases);
        addVarInference(cases);
    }

    // ------------------------------------------------------------------
    // Category: Control (sanity baselines that must always work)
    // ------------------------------------------------------------------
    private static void addControl(List<Case> cases) {
        String cat = "control";
        cases.add(new Case(cat, "lambda_listener", ui(""
                + "Button b = new Button(\"Go\");\n"
                + "b.addActionListener(e -> {});\n"
                + "root.add(b);"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "anonymous_listener", ui(""
                + "Button b = new Button(\"Go\");\n"
                + "b.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {} });\n"
                + "root.add(b);"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "classic_for_loop", ui(""
                + "int sum = 0;\n"
                + "for (int i = 0; i < 3; i++) sum += i;\n"
                + "root.add(new Label(\"sum=\" + sum));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "string_concat", ui(""
                + "String s = \"a\" + 1 + true + 3.14;\n"
                + "root.add(new Label(s));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "return_component_only", raw(""
                + "import com.codename1.ui.*;\n"
                + "import com.codename1.ui.layouts.*;\n"
                + "new Container(BoxLayout.y());\n"), ExpectedOutcome.SUCCESS, null));
    }

    // ------------------------------------------------------------------
    // Category: Literals
    // ------------------------------------------------------------------
    private static void addLiterals(List<Case> cases) {
        String cat = "literals";
        cases.add(new Case(cat, "int_decimal", ui(""
                + "int x = 42;\n"
                + "root.add(new Label(\"x=\" + x));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "int_hex", ui(""
                + "int x = 0xFF;\n"
                + "root.add(new Label(\"x=\" + x));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "int_binary", ui(""
                + "int x = 0b1010;\n"
                + "root.add(new Label(\"x=\" + x));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "int_underscored", ui(""
                + "int x = 1_000_000;\n"
                + "root.add(new Label(\"x=\" + x));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "long_suffix", ui(""
                + "long x = 9999999999L;\n"
                + "root.add(new Label(\"x=\" + x));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "double_literal", ui(""
                + "double x = 3.14;\n"
                + "root.add(new Label(\"x=\" + x));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "float_suffix", ui(""
                + "float x = 3.14f;\n"
                + "root.add(new Label(\"x=\" + x));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "char_simple", ui(""
                + "char c = 'a';\n"
                + "root.add(new Label(\"c=\" + c));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "char_escape", ui(""
                + "char c = '\\n';\n"
                + "root.add(new Label(\"len=\" + String.valueOf(c).length()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "char_unicode_escape", ui(""
                + "char c = '\\u00e9';\n"
                + "root.add(new Label(\"c=\" + c));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "string_escapes", ui(""
                + "String s = \"a\\tb\\n\\\"c\\\"\";\n"
                + "root.add(new Label(\"len=\" + s.length()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "boolean_null", ui(""
                + "boolean b = true;\n"
                + "Object o = null;\n"
                + "root.add(new Label(\"b=\" + b + \" o=\" + o));"), ExpectedOutcome.SUCCESS, null));
    }

    // ------------------------------------------------------------------
    // Category: Operators
    // ------------------------------------------------------------------
    private static void addOperators(List<Case> cases) {
        String cat = "operators";
        cases.add(new Case(cat, "arithmetic", ui(""
                + "int x = (3 + 4) * 2 - 1;\n"
                + "root.add(new Label(\"x=\" + x));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "conditional_ternary", ui(""
                + "int x = 5;\n"
                + "String s = x > 0 ? \"pos\" : \"neg\";\n"
                + "root.add(new Label(s));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "compound_assign", ui(""
                + "int x = 1;\n"
                + "x += 2; x *= 3; x -= 1;\n"
                + "root.add(new Label(\"x=\" + x));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "bitwise_and_or_xor", ui(""
                + "int x = 0xF0 & 0x0F | 0xAA ^ 0x55;\n"
                + "root.add(new Label(\"x=\" + x));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "shift_left_right", ui(""
                + "int x = (1 << 4) | (256 >> 2);\n"
                + "root.add(new Label(\"x=\" + x));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "unsigned_shift", ui(""
                + "int x = -1 >>> 28;\n"
                + "root.add(new Label(\"x=\" + x));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "instanceof_simple", ui(""
                + "Object o = \"hi\";\n"
                + "boolean b = o instanceof String;\n"
                + "root.add(new Label(\"b=\" + b));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "postfix_increment", ui(""
                + "int x = 1;\n"
                + "int y = x++;\n"
                + "root.add(new Label(\"x=\" + x + \" y=\" + y));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "prefix_decrement", ui(""
                + "int x = 5;\n"
                + "int y = --x;\n"
                + "root.add(new Label(\"x=\" + x + \" y=\" + y));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "cast_numeric", ui(""
                + "double d = 3.7;\n"
                + "int i = (int) d;\n"
                + "root.add(new Label(\"i=\" + i));"), ExpectedOutcome.SUCCESS, null));
    }

    // ------------------------------------------------------------------
    // Category: Control flow
    // ------------------------------------------------------------------
    private static void addControlFlow(List<Case> cases) {
        String cat = "control_flow";
        cases.add(new Case(cat, "if_else", ui(""
                + "int x = 5;\n"
                + "String s;\n"
                + "if (x > 0) s = \"pos\"; else if (x < 0) s = \"neg\"; else s = \"zero\";\n"
                + "root.add(new Label(s));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "while_loop", ui(""
                + "int i = 0, sum = 0;\n"
                + "while (i < 3) { sum += i; i++; }\n"
                + "root.add(new Label(\"sum=\" + sum));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "do_while", ui(""
                + "int i = 0, sum = 0;\n"
                + "do { sum += i; i++; } while (i < 3);\n"
                + "root.add(new Label(\"sum=\" + sum));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "empty_for_infinite_with_break", ui(""
                + "int i = 0;\n"
                + "for (;;) { if (i >= 3) break; i++; }\n"
                + "root.add(new Label(\"i=\" + i));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "labeled_break", ui(""
                + "int count = 0;\n"
                + "outer: for (int i = 0; i < 3; i++) {\n"
                + "  for (int j = 0; j < 3; j++) { count++; if (j == 1) break outer; }\n"
                + "}\n"
                + "root.add(new Label(\"count=\" + count));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "labeled_continue", ui(""
                + "int count = 0;\n"
                + "outer: for (int i = 0; i < 3; i++) {\n"
                + "  for (int j = 0; j < 3; j++) { if (j == 1) continue outer; count++; }\n"
                + "}\n"
                + "root.add(new Label(\"count=\" + count));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "classic_switch_int", ui(""
                + "int x = 2; String s;\n"
                + "switch (x) { case 1: s = \"one\"; break; case 2: s = \"two\"; break; default: s = \"other\"; }\n"
                + "root.add(new Label(s));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "classic_switch_string", ui(""
                + "String x = \"b\"; String s;\n"
                + "switch (x) { case \"a\": s = \"A\"; break; case \"b\": s = \"B\"; break; default: s = \"?\"; }\n"
                + "root.add(new Label(s));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "classic_switch_fallthrough", ui(""
                + "int x = 1; int hits = 0;\n"
                + "switch (x) { case 1: hits++; case 2: hits++; break; case 3: hits = 99; break; }\n"
                + "root.add(new Label(\"hits=\" + hits));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "return_value_as_component", raw(""
                + "import com.codename1.ui.*;\n"
                + "import com.codename1.ui.layouts.*;\n"
                + "Container c = new Container(BoxLayout.y());\n"
                + "c.add(new Label(\"yes\"));\n"
                + "c;"), ExpectedOutcome.SUCCESS, null));
    }

    // ------------------------------------------------------------------
    // Category: Exceptions
    // ------------------------------------------------------------------
    private static void addExceptions(List<Case> cases) {
        String cat = "exceptions";
        cases.add(new Case(cat, "try_catch_basic", ui(""
                + "String s;\n"
                + "try { s = Integer.toString(1/0); } catch (ArithmeticException ex) { s = \"caught\"; }\n"
                + "root.add(new Label(s));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "try_finally_only", ui(""
                + "String s = \"init\";\n"
                + "try { s = \"body\"; } finally { s = s + \"-fin\"; }\n"
                + "root.add(new Label(s));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "try_catch_finally", ui(""
                + "String s;\n"
                + "try { s = Integer.toString(1/0); } catch (ArithmeticException ex) { s = \"caught\"; } finally { s = s + \"-fin\"; }\n"
                + "root.add(new Label(s));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "multi_catch_two_types", ui(""
                + "String s;\n"
                + "try { s = Integer.toString(1/0); } catch (ArithmeticException | NullPointerException ex) { s = \"caught\"; }\n"
                + "root.add(new Label(s));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "throws_chain", ui(""
                + "String s;\n"
                + "try { throw new RuntimeException(\"boom\"); } catch (RuntimeException ex) { s = ex.getMessage(); }\n"
                + "root.add(new Label(s));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "nested_try_catch", ui(""
                + "String s = \"\";\n"
                + "try { try { throw new RuntimeException(\"i\"); } catch (RuntimeException e) { s += \"inner \"; throw new RuntimeException(\"o\"); } } catch (RuntimeException e) { s += \"outer\"; }\n"
                + "root.add(new Label(s));"), ExpectedOutcome.SUCCESS, null));
    }

    // ------------------------------------------------------------------
    // Category: Generics
    // ------------------------------------------------------------------
    private static void addGenerics(List<Case> cases) {
        String cat = "generics";
        // Collections currently hit a registry gap (ArrayList.add etc. not dispatched).
        // These will flip to SUCCESS when Phase 4 expands the access registry.
        cases.add(new Case(cat, "list_explicit_type", ui(""
                + "List<String> items = new ArrayList<String>();\n"
                + "items.add(\"a\"); items.add(\"b\");\n"
                + "root.add(new Label(\"n=\" + items.size()));"), ExpectedOutcome.EVAL_ERROR, "Generated instance dispatch not implemented"));
        cases.add(new Case(cat, "list_diamond", ui(""
                + "List<String> items = new ArrayList<>();\n"
                + "items.add(\"a\"); items.add(\"b\");\n"
                + "root.add(new Label(\"n=\" + items.size()));"), ExpectedOutcome.EVAL_ERROR, "Generated instance dispatch not implemented"));
        cases.add(new Case(cat, "map_nested_generics", ui(""
                + "Map<String, List<Integer>> m = new HashMap<String, List<Integer>>();\n"
                + "m.put(\"a\", new ArrayList<Integer>());\n"
                + "m.get(\"a\").add(1);\n"
                + "root.add(new Label(\"n=\" + m.get(\"a\").size()));"), ExpectedOutcome.EVAL_ERROR, "Generated instance dispatch not implemented"));
        // size() is dispatched through a different path than add(), so this already works.
        cases.add(new Case(cat, "wildcard_extends", ui(""
                + "List<? extends Number> nums = new ArrayList<Integer>();\n"
                + "root.add(new Label(\"n=\" + nums.size()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "wildcard_super", ui(""
                + "List<? super Integer> sink = new ArrayList<Number>();\n"
                + "sink.add(1);\n"
                + "root.add(new Label(\"ok\"));"), ExpectedOutcome.EVAL_ERROR, "Generated instance dispatch not implemented"));
        cases.add(new Case(cat, "raw_type_mix", ui(""
                + "List items = new ArrayList();\n"
                + "items.add(\"a\");\n"
                + "root.add(new Label(\"n=\" + items.size()));"), ExpectedOutcome.EVAL_ERROR, "Generated instance dispatch not implemented"));
    }

    // ------------------------------------------------------------------
    // Category: Lambdas
    // ------------------------------------------------------------------
    private static void addLambdas(List<Case> cases) {
        String cat = "lambdas";
        cases.add(new Case(cat, "single_param_bare", ui(""
                + "Button b = new Button(\"Go\");\n"
                + "b.addActionListener(e -> root.add(new Label(\"clicked\")));\n"
                + "root.add(b);"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "single_param_paren", ui(""
                + "Button b = new Button(\"Go\");\n"
                + "b.addActionListener((e) -> {});\n"
                + "root.add(b);"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "zero_arg", ui(""
                + "Runnable r = () -> {};\n"
                + "root.add(new Label(\"ok\"));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "zero_arg_invoked", ui(""
                + "String[] last = new String[]{\"\"};\n"
                + "Runnable r = () -> { last[0] = \"ran\"; };\n"
                + "r.run();\n"
                + "root.add(new Label(last[0]));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "capture_local", ui(""
                + "String prefix = \"P:\";\n"
                + "Button b = new Button(\"Go\");\n"
                + "b.addActionListener(e -> root.add(new Label(prefix + \"x\")));\n"
                + "root.add(b);"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "lambda_block_body", ui(""
                + "Button b = new Button(\"Go\");\n"
                + "b.addActionListener(e -> { int x = 1; root.add(new Label(\"x=\" + x)); });\n"
                + "root.add(b);"), ExpectedOutcome.SUCCESS, null));
    }

    // ------------------------------------------------------------------
    // Category: Method references
    // ------------------------------------------------------------------
    private static void addMethodReferences(List<Case> cases) {
        String cat = "method_ref";
        cases.add(new Case(cat, "static_type", ui(""
                + "Button b = new Button(\"Go\");\n"
                + "b.addActionListener(System.out::println);\n"
                + "root.add(b);"), ExpectedOutcome.PARSE_ERROR, "Parse error:"));
        cases.add(new Case(cat, "bound_instance", ui(""
                + "String prefix = \"X:\";\n"
                + "Button b = new Button(\"Go\");\n"
                + "b.addActionListener(prefix::concat);\n"
                + "root.add(b);"), ExpectedOutcome.PARSE_ERROR, "Parse error:"));
        cases.add(new Case(cat, "constructor_ref", ui(""
                + "import java.util.function.*;\n"
                + "Supplier<StringBuilder> ctor = StringBuilder::new;\n"
                + "root.add(new Label(ctor.get().toString()));"), ExpectedOutcome.PARSE_ERROR, "Parse error:"));
        cases.add(new Case(cat, "unbound_instance", ui(""
                + "import java.util.function.*;\n"
                + "Function<String, Integer> len = String::length;\n"
                + "root.add(new Label(\"n=\" + len.apply(\"abc\")));"), ExpectedOutcome.PARSE_ERROR, "Parse error:"));
    }

    // ------------------------------------------------------------------
    // Category: Enhanced for
    // ------------------------------------------------------------------
    private static void addEnhancedFor(List<Case> cases) {
        String cat = "enhanced_for";
        cases.add(new Case(cat, "primitive_array", ui(""
                + "int sum = 0;\n"
                + "for (int v : new int[]{1,2,3}) sum += v;\n"
                + "root.add(new Label(\"sum=\" + sum));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "object_array", ui(""
                + "String txt = \"\";\n"
                + "for (String v : new String[]{\"a\",\"b\"}) txt += v;\n"
                + "root.add(new Label(txt));"), ExpectedOutcome.SUCCESS, null));
        // List iteration hits the same registry gap as other collections today.
        cases.add(new Case(cat, "list_iteration", ui(""
                + "List<String> items = new ArrayList<String>();\n"
                + "items.add(\"a\"); items.add(\"b\");\n"
                + "String txt = \"\";\n"
                + "for (String v : items) txt += v;\n"
                + "root.add(new Label(txt));"), ExpectedOutcome.EVAL_ERROR, "Generated instance dispatch not implemented"));
        cases.add(new Case(cat, "nested", ui(""
                + "int count = 0;\n"
                + "for (int row : new int[]{1,2}) for (int col : new int[]{3,4}) count += row + col;\n"
                + "root.add(new Label(\"count=\" + count));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "null_array", ui(""
                + "int[] values = null;\n"
                + "for (int v : values) root.add(new Label(\"v=\" + v));"), ExpectedOutcome.EVAL_ERROR, "Evaluation error:"));
    }

    // ------------------------------------------------------------------
    // Category: Try-with-resources
    // ------------------------------------------------------------------
    private static void addTryWithResources(List<Case> cases) {
        String cat = "twr";
        cases.add(new Case(cat, "single_resource_stringreader", ui(""
                + "try (StringReader in = new StringReader(\"a\")) {\n"
                + "  root.add(new Label(\"ok\"));\n"
                + "}"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "multiple_resources", ui(""
                + "try (StringReader in = new StringReader(\"a\"); StringReader out = new StringReader(\"b\")) {\n"
                + "  root.add(new Label(\"ok\"));\n"
                + "}"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "trailing_semicolon", ui(""
                + "try (StringReader in = new StringReader(\"a\");) {\n"
                + "  root.add(new Label(\"ok\"));\n"
                + "}"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "with_catch", ui(""
                + "try (StringReader in = new StringReader(\"a\")) {\n"
                + "  root.add(new Label(\"ok\"));\n"
                + "} catch (RuntimeException ex) { root.add(new Label(\"caught\")); }"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "with_catch_and_finally", ui(""
                + "try (StringReader in = new StringReader(\"a\")) {\n"
                + "  root.add(new Label(\"ok\"));\n"
                + "} catch (RuntimeException ex) { root.add(new Label(\"c\")); } finally { root.add(new Label(\"f\")); }"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "nested_try_catch_finally", ui(""
                + "try (StringReader in = new StringReader(\"abc\")) {\n"
                + "  try { root.add(new Label(\"inner\")); } catch (RuntimeException ex) { root.add(new Label(\"c\")); } finally { root.add(new Label(\"f\")); }\n"
                + "}"), ExpectedOutcome.SUCCESS, null));
    }

    // ------------------------------------------------------------------
    // Category: Class declarations
    // ------------------------------------------------------------------
    private static void addClassDeclarations(List<Case> cases) {
        String cat = "class_decl";
        // Note these snippets must compute a Component result. The
        // unwrapSingleTopLevelClass path applies when there's a single class
        // and nothing else, so we always include a follow-up statement that
        // produces a component.
        cases.add(new Case(cat, "single_empty_top_level", ui(""
                + "class A {}\n"
                + "Object a = new A();\n"
                + "root.add(new Label(\"a=\" + (a != null)));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "multiple_top_level", ui(""
                + "class A {}\n"
                + "class B {}\n"
                + "Object a = new A();\n"
                + "root.add(new Label(\"got=\" + a.getClass().getSimpleName()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "single_with_method_only", ui(""
                + "class C { int get() { return 7; } }\n"
                + "C c = new C();\n"
                + "root.add(new Label(\"v=\" + c.get()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "single_with_fields", ui(""
                + "class C { int x; C() { x = 7; } int get() { return x; } }\n"
                + "C c = new C();\n"
                + "root.add(new Label(\"v=\" + c.get()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "ctor_with_args", ui(""
                + "class C { String s; C(String v) { s = v; } String get() { return s; } }\n"
                + "C c = new C(\"hi\");\n"
                + "root.add(new Label(c.get()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "static_inner", ui(""
                + "class Outer { static class Inner { String label() { return \"ok\"; } } }\n"
                + "root.add(new Label(new Outer.Inner().label()));"), ExpectedOutcome.EVAL_ERROR, null));
        cases.add(new Case(cat, "generic_class_usage", ui(""
                + "class Pair<T> { private final T value; Pair(T value) { this.value = value; } T get() { return value; } }\n"
                + "Pair<String> p = new Pair<String>(\"generic-ok\");\n"
                + "root.add(new Label(p.get()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "method_call_with_args", ui(""
                + "class C { int add(int a, int b) { return a + b; } }\n"
                + "C c = new C();\n"
                + "root.add(new Label(\"v=\" + c.add(2, 3)));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "field_read_external", ui(""
                + "class C { int x; C() { x = 42; } }\n"
                + "C c = new C();\n"
                + "root.add(new Label(\"x=\" + c.x));"), ExpectedOutcome.SUCCESS, null));
        // Inheritance is "shallow" — extends is parsed but the parent class's
        // methods are not auto-inherited yet. Subclass overrides work; tests
        // that depend on calling a *base* method via the subclass would fail.
        cases.add(new Case(cat, "inheritance_override_only", ui(""
                + "class Base { int x() { return 1; } }\n"
                + "class Derived extends Base { int x() { return 2; } }\n"
                + "Derived d = new Derived();\n"
                + "root.add(new Label(\"v=\" + d.x()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "abstract_class_basic", ui(""
                + "abstract class A { abstract int v(); }\n"
                + "class B extends A { int v() { return 5; } }\n"
                + "B b = new B();\n"
                + "root.add(new Label(\"v=\" + b.v()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "inheritance_calls_base_method", ui(""
                + "class Base { int hello() { return 11; } }\n"
                + "class Derived extends Base {}\n"
                + "Derived d = new Derived();\n"
                + "root.add(new Label(\"v=\" + d.hello()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "inheritance_with_field", ui(""
                + "class Base { int x; Base() { x = 9; } }\n"
                + "class Derived extends Base { int sum() { return x + 1; } }\n"
                + "Derived d = new Derived();\n"
                + "root.add(new Label(\"v=\" + d.sum()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "super_method_dispatch", ui(""
                + "class Base { int v() { return 10; } }\n"
                + "class Derived extends Base { int v() { return super.v() + 5; } }\n"
                + "Derived d = new Derived();\n"
                + "root.add(new Label(\"v=\" + d.v()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "two_instances_independent", ui(""
                + "class C { int x; C(int v) { x = v; } int get() { return x; } }\n"
                + "C a = new C(1);\n"
                + "C b = new C(2);\n"
                + "root.add(new Label(\"a=\" + a.get() + \" b=\" + b.get()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "method_calls_other_method", ui(""
                + "class C { int base() { return 7; } int boosted() { return base() + 1; } }\n"
                + "C c = new C();\n"
                + "root.add(new Label(\"v=\" + c.boosted()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "final_modifier", ui(""
                + "final class F { int v() { return 1; } }\n"
                + "F f = new F();\n"
                + "root.add(new Label(\"v=\" + f.v()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "inner_anonymous_known_sam", ui(""
                + "Button b = new Button(\"Go\");\n"
                + "b.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent evt) {} });\n"
                + "root.add(b);"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "inner_anonymous_runnable", ui(""
                + "Runnable r = new Runnable() { public void run() {} };\n"
                + "root.add(new Label(\"ok\"));"), ExpectedOutcome.EVAL_ERROR, null));
    }

    // ------------------------------------------------------------------
    // Category: Interface declarations
    // ------------------------------------------------------------------
    private static void addInterfaceDeclarations(List<Case> cases) {
        String cat = "interface_decl";
        cases.add(new Case(cat, "static_method", ui(""
                + "interface Util { static int one() { return 1; } }\n"
                + "root.add(new Label(\"v=\" + Util.one()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "static_method_with_args", ui(""
                + "interface M { static int add(int a, int b) { return a + b; } }\n"
                + "root.add(new Label(\"v=\" + M.add(2, 3)));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "declaration_only", ui(""
                + "interface Greet { String hello(); }\n"
                + "root.add(new Label(\"declared\"));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "cannot_instantiate", raw(""
                + "interface Greet { String hello(); }\n"
                + "new Greet();"), ExpectedOutcome.EVAL_ERROR, "Cannot instantiate scripted interface"));
        cases.add(new Case(cat, "anonymous_impl_not_supported", raw(""
                + "interface Greet { String hello(); }\n"
                + "Object g = new Greet(){public String hello(){return \"hi\";}};\n"
                + "g.toString();"), ExpectedOutcome.EVAL_ERROR, null));
    }

    // ------------------------------------------------------------------
    // Category: Enum declarations
    // ------------------------------------------------------------------
    private static void addEnumDeclarations(List<Case> cases) {
        String cat = "enum_decl";
        cases.add(new Case(cat, "simple_name", ui(""
                + "enum Color { RED, GREEN, BLUE }\n"
                + "root.add(new Label(Color.RED.name()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "values_count", ui(""
                + "enum Color { RED, GREEN, BLUE }\n"
                + "root.add(new Label(\"n=\" + Color.values().length));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "ordinal", ui(""
                + "enum Color { RED, GREEN, BLUE }\n"
                + "root.add(new Label(\"o=\" + Color.GREEN.ordinal()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "with_fields_unsupported", raw(""
                + "enum Prio { LOW(1), HIGH(10); final int v; Prio(int v){this.v=v;} }\n"
                + "Prio.LOW.v;"), ExpectedOutcome.EVAL_ERROR, null));
        cases.add(new Case(cat, "with_method_per_constant_unsupported", raw(""
                + "enum Op { ADD { public int apply(int a, int b){return a+b;} }; public abstract int apply(int a, int b); }\n"
                + "Op.ADD.apply(1,2);"), ExpectedOutcome.EVAL_ERROR, null));
    }

    // ------------------------------------------------------------------
    // Category: Records (Java 14+)
    // ------------------------------------------------------------------
    private static void addRecords(List<Case> cases) {
        String cat = "record";
        // `record` is currently tokenized as an ordinary identifier, so parsing
        // succeeds but resolution fails ("Class: record not found"). Once Phase 3
        // adds record support this flips to SUCCESS.
        cases.add(new Case(cat, "positional_simple", raw(""
                + "record Point(int x, int y) {}\n"
                + "new Point(1,2).x();"), ExpectedOutcome.EVAL_ERROR, "record"));
        cases.add(new Case(cat, "with_accessors", raw(""
                + "record Named(String n) {}\n"
                + "new Named(\"a\").n();"), ExpectedOutcome.EVAL_ERROR, "record"));
    }

    // ------------------------------------------------------------------
    // Category: Sealed classes (Java 17)
    // ------------------------------------------------------------------
    private static void addSealed(List<Case> cases) {
        String cat = "sealed";
        cases.add(new Case(cat, "sealed_with_permits", raw(""
                + "sealed interface Shape permits Circle, Square {}\n"
                + "final class Circle implements Shape {}\n"
                + "final class Square implements Shape {}\n"
                + "new Circle();"), ExpectedOutcome.PARSE_ERROR, null));
    }

    // ------------------------------------------------------------------
    // Category: Switch expressions (Java 14+)
    // ------------------------------------------------------------------
    private static void addSwitchExpressions(List<Case> cases) {
        String cat = "switch_expr";
        cases.add(new Case(cat, "arrow_int", ui(""
                + "int x = 2;\n"
                + "String s = switch (x) { case 1 -> \"one\"; case 2 -> \"two\"; default -> \"?\"; };\n"
                + "root.add(new Label(s));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "arrow_string", ui(""
                + "String x = \"b\";\n"
                + "String s = switch (x) { case \"a\" -> \"A\"; case \"b\" -> \"B\"; default -> \"?\"; };\n"
                + "root.add(new Label(s));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "arrow_statement", ui(""
                + "int x = 1;\n"
                + "String[] r = new String[]{\"\"};\n"
                + "switch (x) { case 1 -> r[0] = \"one\"; case 2 -> r[0] = \"two\"; default -> r[0] = \"?\"; }\n"
                + "root.add(new Label(r[0]));"), ExpectedOutcome.SUCCESS, null));
        // yield form remains unsupported — needs proper switch-expression
        // parser support, which the current source-rewrite path cannot produce.
        cases.add(new Case(cat, "yield_form_unsupported", ui(""
                + "int x = 2;\n"
                + "String s = switch (x) { case 1: yield \"one\"; case 2: yield \"two\"; default: yield \"?\"; };\n"
                + "root.add(new Label(s));"), ExpectedOutcome.PARSE_ERROR, null));
    }

    // ------------------------------------------------------------------
    // Category: Pattern matching (Java 16+)
    // ------------------------------------------------------------------
    private static void addPatternMatching(List<Case> cases) {
        String cat = "pattern";
        cases.add(new Case(cat, "instanceof_binding", ui(""
                + "Object o = \"hi\";\n"
                + "if (o instanceof String s) root.add(new Label(\"len=\" + s.length()));\n"
                + "else root.add(new Label(\"other\"));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "instanceof_binding_negative", ui(""
                + "Object o = Integer.valueOf(7);\n"
                + "if (o instanceof String s) root.add(new Label(\"len=\" + s.length()));\n"
                + "else root.add(new Label(\"other\"));"), ExpectedOutcome.SUCCESS, null));
    }

    // ------------------------------------------------------------------
    // Category: Text blocks (Java 15+)
    // ------------------------------------------------------------------
    private static void addTextBlocks(List<Case> cases) {
        String cat = "text_block";
        cases.add(new Case(cat, "basic", ui(""
                + "String s = \"\"\"\n"
                + "hello\n"
                + "world\"\"\";\n"
                + "root.add(new Label(\"len=\" + s.length()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "indented", ui(""
                + "String s = \"\"\"\n"
                + "   a\n"
                + "   b\"\"\";\n"
                + "root.add(new Label(\"len=\" + s.length()));"), ExpectedOutcome.SUCCESS, null));
    }

    // ------------------------------------------------------------------
    // Category: var inference (Java 10+)
    // ------------------------------------------------------------------
    private static void addVarInference(List<Case> cases) {
        String cat = "var";
        // BSH happens to treat `var` as a loose type, so these already work.
        cases.add(new Case(cat, "local_int", ui(""
                + "var x = 42;\n"
                + "root.add(new Label(\"x=\" + x));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "local_string", ui(""
                + "var s = \"hi\";\n"
                + "root.add(new Label(s));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "enhanced_for", ui(""
                + "int sum = 0;\n"
                + "for (var v : new int[]{1,2,3}) sum += v;\n"
                + "root.add(new Label(\"sum=\" + sum));"), ExpectedOutcome.SUCCESS, null));
    }

    // ------------------------------------------------------------------
    // Infrastructure
    // ------------------------------------------------------------------

    private static String trim(String s, int max) {
        if (s == null) return null;
        s = s.replace('\n', ' ').replace('\r', ' ');
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    private static ExpectedOutcome classify(PlaygroundRunner.RunResult result) {
        if (result.getComponent() != null) {
            return ExpectedOutcome.SUCCESS;
        }
        String message = firstDiagnosticMessage(result);
        if (message != null && message.startsWith("Parse error:")) {
            return ExpectedOutcome.PARSE_ERROR;
        }
        return ExpectedOutcome.EVAL_ERROR;
    }

    private static String firstDiagnosticMessage(PlaygroundRunner.RunResult result) {
        List<PlaygroundRunner.Diagnostic> diagnostics = result.getDiagnostics();
        if (!diagnostics.isEmpty()) {
            return diagnostics.get(0).message;
        }
        List<PlaygroundRunner.InlineMessage> messages = result.getMessages();
        if (!messages.isEmpty()) {
            return messages.get(0).text;
        }
        return null;
    }

    private static PlaygroundRunner.RunResult runSnippet(String script) {
        Display.init(null);
        Form host = new Form("Host", new BorderLayout());
        Container preview = new Container(new BorderLayout());
        host.add(BorderLayout.CENTER, preview);
        host.show();

        PlaygroundContext context = new PlaygroundContext(host, preview, null, new PlaygroundContext.Logger() {
            public void log(String message) {
            }
        });
        PlaygroundRunner runner = new PlaygroundRunner();
        return runner.run(script, context);
    }
}
