package com.codenameone.playground;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
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
            int paintAttempts = 0;
            int paintFailures = 0;
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
                // Extra UI-path check: for SUCCESS cases, try to paint the
                // produced Component into an offscreen image. Exceptions
                // during paint mean the snippet evaluated but its UI is
                // broken — a distinct failure from a missing syntax.
                String paintError = null;
                if (outcomeOk && testCase.expectedOutcome == ExpectedOutcome.SUCCESS
                        && result.getComponent() != null) {
                    paintAttempts++;
                    paintError = tryPaintHeadless(result.getComponent());
                    if (paintError != null) paintFailures++;
                }
                if (outcomeOk && diagnosticOk && paintError == null) {
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
                if (paintError != null) {
                    if (!outcomeOk || !diagnosticOk) msg.append("; ");
                    msg.append("paint failure: ").append(trim(paintError, 200));
                }
                if (diagnostic != null) {
                    msg.append(" diag=").append(trim(diagnostic, 400));
                }
                failures.add(msg.toString());
            }

            System.out.println("Playground syntax matrix: "
                    + passed + "/" + cases.size() + " passed; UI paint: "
                    + (paintAttempts - paintFailures) + "/" + paintAttempts + " clean");
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
        addIntegrationCases(cases);
        addParseDiagnostics(cases);
        addExtendedCoverage(cases);
    }

    // ------------------------------------------------------------------
    // Category: Parse-error diagnostics — the messages themselves are
    // tested to confirm the enriched formatting (location + caret +
    // token-specific hint) actually surfaces.
    // ------------------------------------------------------------------
    private static void addParseDiagnostics(List<Case> cases) {
        String cat = "parse_diagnostics";
        cases.add(new Case(cat, "missing_closing_brace_reports_eof", ui(""
                + "class Bad { public String hello() { return \"hi\";\n"),
                ExpectedOutcome.PARSE_ERROR, "end-of-input"));
        cases.add(new Case(cat, "missing_semicolon_hint_mentions_preceding", ui(""
                + "int a = 1\n"
                + "int b = 2;\n"
                + "root.add(new Label(\"\" + a + b));"),
                ExpectedOutcome.PARSE_ERROR, "Syntax error"));
        cases.add(new Case(cat, "unterminated_paren_hint", ui(""
                + "int a = (1 + 2;\n"
                + "root.add(new Label(\"\" + a));"),
                ExpectedOutcome.PARSE_ERROR, "Syntax error"));
        cases.add(new Case(cat, "parse_error_includes_line_number", ui(""
                + "int a = 1;\n"
                + "if a > 0) { }\n"
                + "root.add(new Label(\"\" + a));"),
                ExpectedOutcome.PARSE_ERROR, "line "));
    }

    // ------------------------------------------------------------------
    // Category: Extended coverage — CN1-supported features and edge
    // cases that weren't previously exercised. All cases operate
    // strictly within CN1's runtime surface.
    // ------------------------------------------------------------------
    private static void addExtendedCoverage(List<Case> cases) {
        String cat = "extended";
        // String methods on CN1-supported String API.
        cases.add(new Case(cat, "string_contains", ui(""
                + "String s = \"hello world\";\n"
                + "boolean has = s.indexOf(\"world\") >= 0;\n"
                + "root.add(new Label(\"has=\" + has));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "string_replace_char", ui(""
                + "String s = \"abc\".replace('b', 'X');\n"
                + "root.add(new Label(s));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "stringbuilder_append_chain", ui(""
                + "StringBuilder b = new StringBuilder();\n"
                + "b.append(\"x=\").append(42).append('!');\n"
                + "root.add(new Label(b.toString()));"), ExpectedOutcome.SUCCESS, null));
        // Integer parsing and math.
        cases.add(new Case(cat, "integer_parse_and_math", ui(""
                + "int n = Integer.parseInt(\"42\");\n"
                + "int sq = n * n;\n"
                + "root.add(new Label(\"sq=\" + sq));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "math_min_max_abs", ui(""
                + "int v = Math.max(Math.abs(-5), Math.min(10, 3));\n"
                + "root.add(new Label(\"v=\" + v));"), ExpectedOutcome.SUCCESS, null));
        // Arrays utilities over an Object[] (CN1's reduced runtime
        // supports Object[] arrays, and now primitive int[] iteration
        // via enhanced-for).
        cases.add(new Case(cat, "arrays_asList_and_sum", ui(""
                + "import java.util.*;\n"
                + "List<Integer> xs = Arrays.asList(1, 2, 3, 4);\n"
                + "int sum = 0; for (Object x : xs) sum += ((Integer) x).intValue();\n"
                + "root.add(new Label(\"sum=\" + sum));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "enhanced_for_int_array", ui(""
                + "int[] arr = {1, 2, 3, 4};\n"
                + "int sum = 0; for (int x : arr) sum += x;\n"
                + "root.add(new Label(\"sum=\" + sum));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "enhanced_for_long_array", ui(""
                + "long[] arr = {10L, 20L, 30L};\n"
                + "long sum = 0; for (long x : arr) sum += x;\n"
                + "root.add(new Label(\"sum=\" + sum));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "enhanced_for_char_array", ui(""
                + "char[] chars = {'a', 'b', 'c'};\n"
                + "StringBuilder sb = new StringBuilder();\n"
                + "for (char c : chars) sb.append(c).append('-');\n"
                + "root.add(new Label(sb.toString()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "enhanced_for_boolean_array", ui(""
                + "boolean[] flags = {true, false, true};\n"
                + "int trueCount = 0; for (boolean b : flags) if (b) trueCount++;\n"
                + "root.add(new Label(\"n=\" + trueCount));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "enhanced_for_double_array", ui(""
                + "double[] vals = {1.5, 2.5, 4.0};\n"
                + "double sum = 0; for (double v : vals) sum += v;\n"
                + "root.add(new Label(\"sum=\" + sum));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "enhanced_for_string_array", ui(""
                + "String[] names = {\"Alice\", \"Bob\", \"Cara\"};\n"
                + "StringBuilder sb = new StringBuilder();\n"
                + "for (String n : names) sb.append(n).append(',');\n"
                + "root.add(new Label(sb.toString()));"), ExpectedOutcome.SUCCESS, null));
        // HashMap iteration via entrySet.
        cases.add(new Case(cat, "hashmap_entryset_walk", ui(""
                + "import java.util.*;\n"
                + "Map<String, Integer> m = new LinkedHashMap<>();\n"
                + "m.put(\"a\", 1); m.put(\"b\", 2);\n"
                + "int total = 0;\n"
                + "for (Map.Entry<String, Integer> e : m.entrySet()) total += e.getValue();\n"
                + "root.add(new Label(\"total=\" + total));"), ExpectedOutcome.SUCCESS, null));
        // String.format via formatted CN1 API (if present).
        cases.add(new Case(cat, "locale_independent_format", ui(""
                + "String s = Integer.toHexString(255);\n"
                + "root.add(new Label(\"hex=\" + s));"), ExpectedOutcome.SUCCESS, null));
        // Button listener with anonymous class full-paths.
        cases.add(new Case(cat, "button_listener_chain", ui(""
                + "Button b1 = new Button(\"A\");\n"
                + "Button b2 = new Button(\"B\");\n"
                + "b1.addActionListener(e -> b2.setText(\"X\"));\n"
                + "root.add(b1); root.add(b2);"), ExpectedOutcome.SUCCESS, null));
        // Nested try/catch with finally rethrowing into an outer
        // catch. Exercises the full try stack unwinding and finally
        // semantics with an explicitly thrown script-level exception.
        cases.add(new Case(cat, "nested_try_catch_finally", ui(""
                + "int r = -1;\n"
                + "try {\n"
                + "    try { throw new IllegalStateException(\"boom\"); }\n"
                + "    catch (IllegalStateException ex) { r = 0; throw new RuntimeException(\"x\"); }\n"
                + "    finally { r++; }\n"
                + "} catch (RuntimeException ex) { r = r + 10; }\n"
                + "root.add(new Label(\"r=\" + r));"), ExpectedOutcome.SUCCESS, null));
        // Enhanced-for over an array and mutation.
        cases.add(new Case(cat, "enhanced_for_over_array", ui(""
                + "int[] arr = {1, 2, 3, 4};\n"
                + "int p = 1;\n"
                + "for (int x : arr) p *= x;\n"
                + "root.add(new Label(\"p=\" + p));"), ExpectedOutcome.SUCCESS, null));
        // Ternary chains.
        cases.add(new Case(cat, "ternary_chain", ui(""
                + "int n = 7;\n"
                + "String label = n < 0 ? \"neg\" : n == 0 ? \"zero\" : n < 10 ? \"small\" : \"big\";\n"
                + "root.add(new Label(label));"), ExpectedOutcome.SUCCESS, null));
        // Bitwise ops on int.
        cases.add(new Case(cat, "bitwise_ops", ui(""
                + "int r = (0b1010 | 0b0101) & 0xFF;\n"
                + "int s = 1 << 4;\n"
                + "root.add(new Label(\"r=\" + r + \" s=\" + s));"), ExpectedOutcome.SUCCESS, null));
        // Indexed iteration of a String via charAt.
        cases.add(new Case(cat, "string_indexed_charat", ui(""
                + "String text = \"abc\";\n"
                + "StringBuilder sb = new StringBuilder();\n"
                + "for (int i = 0; i < text.length(); i++) sb.append(text.charAt(i)).append('-');\n"
                + "root.add(new Label(sb.toString()));"), ExpectedOutcome.SUCCESS, null));
        // toCharArray into enhanced-for — uses the new primitive-array
        // iterator path.
        cases.add(new Case(cat, "string_to_char_array", ui(""
                + "char[] chars = \"abc\".toCharArray();\n"
                + "StringBuilder sb = new StringBuilder();\n"
                + "for (char c : chars) sb.append(c).append('-');\n"
                + "root.add(new Label(sb.toString()));"), ExpectedOutcome.SUCCESS, null));
        // ArrayList sort via Comparator.
        cases.add(new Case(cat, "arraylist_sort_comparator", ui(""
                + "import java.util.*;\n"
                + "List<Integer> xs = new ArrayList<>();\n"
                + "xs.add(3); xs.add(1); xs.add(2);\n"
                + "Collections.sort(xs, (a, b) -> ((Integer) a).compareTo((Integer) b));\n"
                + "root.add(new Label(xs.toString()));"), ExpectedOutcome.SUCCESS, null));
        // Deeply-nested scripted classes.
        cases.add(new Case(cat, "three_level_class_hierarchy", ui(""
                + "class A { int v() { return 1; } }\n"
                + "class B extends A { public int v() { return super.v() + 10; } }\n"
                + "class C extends B { public int v() { return super.v() + 100; } }\n"
                + "C c = new C();\n"
                + "root.add(new Label(\"v=\" + c.v()));"), ExpectedOutcome.SUCCESS, null));
        // Enum with abstract per-constant method.
        cases.add(new Case(cat, "enum_per_constant_abstract_method", ui(""
                + "enum Op { ADD { public int apply(int a, int b) { return a + b; } },"
                + " MUL { public int apply(int a, int b) { return a * b; } };"
                + " public abstract int apply(int a, int b); }\n"
                + "int r = Op.ADD.apply(2, 3) + Op.MUL.apply(2, 3);\n"
                + "root.add(new Label(\"r=\" + r));"), ExpectedOutcome.SUCCESS, null));
        // CN1 UI components beyond Label/Button.
        cases.add(new Case(cat, "text_field_and_text_area", ui(""
                + "TextField tf = new TextField(\"hello\");\n"
                + "TextArea ta = new TextArea(\"line1\\nline2\");\n"
                + "root.add(tf); root.add(ta);\n"
                + "root.add(new Label(\"tf=\" + tf.getText()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "checkbox_and_radio", ui(""
                + "CheckBox cb = new CheckBox(\"check\");\n"
                + "cb.setSelected(true);\n"
                + "RadioButton rb = new RadioButton(\"radio\");\n"
                + "root.add(cb); root.add(rb);\n"
                + "root.add(new Label(\"cb=\" + cb.isSelected()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "slider_with_value", ui(""
                + "Slider s = new Slider();\n"
                + "s.setMinValue(0); s.setMaxValue(100); s.setProgress(42);\n"
                + "root.add(s);\n"
                + "root.add(new Label(\"v=\" + s.getProgress()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "combobox_items", ui(""
                + "ComboBox cb = new ComboBox(\"a\", \"b\", \"c\");\n"
                + "cb.setSelectedIndex(1);\n"
                + "root.add(cb);\n"
                + "root.add(new Label(\"sel=\" + cb.getSelectedItem()));"), ExpectedOutcome.SUCCESS, null));
        // Layout variations.
        cases.add(new Case(cat, "border_layout_east_west", ui(""
                + "Container c = new Container(new BorderLayout());\n"
                + "c.add(BorderLayout.WEST, new Label(\"L\"));\n"
                + "c.add(BorderLayout.CENTER, new Label(\"C\"));\n"
                + "c.add(BorderLayout.EAST, new Label(\"R\"));\n"
                + "root.add(c);"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "grid_layout_2x2", ui(""
                + "Container c = new Container(new GridLayout(2, 2));\n"
                + "for (int i = 0; i < 4; i++) c.add(new Label(\"\" + i));\n"
                + "root.add(c);"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "flow_layout_centered", ui(""
                + "Container c = new Container(new FlowLayout(Component.CENTER));\n"
                + "c.add(new Button(\"One\"));\n"
                + "c.add(new Button(\"Two\"));\n"
                + "root.add(c);"), ExpectedOutcome.SUCCESS, null));
        // Style API.
        cases.add(new Case(cat, "style_set_fg_color", ui(""
                + "Label l = new Label(\"styled\");\n"
                + "l.getAllStyles().setFgColor(0xff0000);\n"
                + "root.add(l);"), ExpectedOutcome.SUCCESS, null));
        // Event listener beyond ActionListener.
        cases.add(new Case(cat, "textfield_datachanged_listener", ui(""
                + "TextField tf = new TextField(\"x\");\n"
                + "int[] count = {0};\n"
                + "tf.addDataChangedListener((type, index) -> count[0]++);\n"
                + "root.add(tf);\n"
                + "root.add(new Label(\"c=\" + count[0]));"), ExpectedOutcome.SUCCESS, null));
        // Date/time (CN1 ships java.util.Date and Calendar).
        cases.add(new Case(cat, "date_via_calendar", ui(""
                + "import java.util.Calendar;\n"
                + "import java.util.Date;\n"
                + "Calendar cal = Calendar.getInstance();\n"
                + "cal.set(Calendar.YEAR, 2026);\n"
                + "cal.set(Calendar.MONTH, 0);\n"
                + "cal.set(Calendar.DAY_OF_MONTH, 15);\n"
                + "Date d = cal.getTime();\n"
                + "root.add(new Label(\"year=\" + cal.get(Calendar.YEAR)));"), ExpectedOutcome.SUCCESS, null));
        // Math.sqrt/floor/ceil — CN1's Math exposes these double ops.
        cases.add(new Case(cat, "math_sqrt_and_floor", ui(""
                + "double v = Math.sqrt(50.0);\n"
                + "double floored = Math.floor(v);\n"
                + "root.add(new Label(\"floor=\" + floored));"), ExpectedOutcome.SUCCESS, null));
        // HashMap put + get + remove.
        cases.add(new Case(cat, "hashmap_put_remove_size", ui(""
                + "import java.util.*;\n"
                + "Map<String, Integer> m = new HashMap<>();\n"
                + "m.put(\"a\", 1); m.put(\"b\", 2); m.put(\"c\", 3);\n"
                + "m.remove(\"b\");\n"
                + "root.add(new Label(\"size=\" + m.size() + \" c=\" + m.get(\"c\")));"), ExpectedOutcome.SUCCESS, null));
        // Nested class with generic parameter reference.
        cases.add(new Case(cat, "class_generic_holder", ui(""
                + "class Holder<T> { T value; void set(T v) { value = v; } T get() { return value; } }\n"
                + "Holder h = new Holder();\n"
                + "h.set(\"hello\");\n"
                + "root.add(new Label(\"v=\" + h.get()));"), ExpectedOutcome.SUCCESS, null));
        // Array field initializer in a class.
        cases.add(new Case(cat, "class_with_int_array_field", ui(""
                + "class Stats { int[] buckets = new int[4]; void add(int i) { buckets[i]++; } }\n"
                + "Stats s = new Stats();\n"
                + "s.add(0); s.add(2); s.add(2);\n"
                + "int sum = 0; for (int b : s.buckets) sum += b;\n"
                + "root.add(new Label(\"sum=\" + sum));"), ExpectedOutcome.SUCCESS, null));
        // Exception message and stack.
        cases.add(new Case(cat, "runtime_exception_message", ui(""
                + "try { throw new RuntimeException(\"explanation\"); }\n"
                + "catch (RuntimeException ex) { root.add(new Label(ex.getMessage())); }"), ExpectedOutcome.SUCCESS, null));
        // Iterator explicit use with remove.
        cases.add(new Case(cat, "iterator_remove_path", ui(""
                + "import java.util.*;\n"
                + "List<Integer> xs = new ArrayList<>();\n"
                + "xs.add(1); xs.add(2); xs.add(3); xs.add(4);\n"
                + "Iterator it = xs.iterator();\n"
                + "while (it.hasNext()) { int v = (Integer) it.next(); if (v % 2 == 0) it.remove(); }\n"
                + "root.add(new Label(xs.toString()));"), ExpectedOutcome.SUCCESS, null));
        // Boolean short-circuit + parenthesised expr.
        cases.add(new Case(cat, "short_circuit_evaluation", ui(""
                + "int[] counter = {0};\n"
                + "Runnable inc = () -> counter[0]++;\n"
                + "boolean ok = false || (counter[0] == 0 && true);\n"
                + "root.add(new Label(\"ok=\" + ok + \" c=\" + counter[0]));"), ExpectedOutcome.SUCCESS, null));
        // Nested enhanced-for with primitive arrays.
        cases.add(new Case(cat, "nested_enhanced_for_int_arrays", ui(""
                + "int[][] grid = { {1,2}, {3,4}, {5,6} };\n"
                + "int sum = 0;\n"
                + "for (int[] row : grid) for (int v : row) sum += v;\n"
                + "root.add(new Label(\"sum=\" + sum));"), ExpectedOutcome.SUCCESS, null));
        // Recursive method.
        cases.add(new Case(cat, "recursive_factorial", ui(""
                + "class F { int fact(int n) { return n <= 1 ? 1 : n * fact(n - 1); } }\n"
                + "F f = new F();\n"
                + "root.add(new Label(\"5!=\" + f.fact(5)));"), ExpectedOutcome.SUCCESS, null));
    }

    // ------------------------------------------------------------------
    // Category: Integration — exercises multiple features together.
    // ------------------------------------------------------------------
    private static void addIntegrationCases(List<Case> cases) {
        String cat = "integration";
        cases.add(new Case(cat, "lambda_to_collections_sort", ui(""
                + "import java.util.*;\n"
                + "List<String> items = new ArrayList<>();\n"
                + "items.add(\"bb\"); items.add(\"a\"); items.add(\"ccc\");\n"
                + "Collections.sort(items, (a, b) -> a.length() - b.length());\n"
                + "root.add(new Label(items.toString()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "record_in_list", ui(""
                + "import java.util.*;\n"
                + "record Pt(int x, int y) {}\n"
                + "List<Pt> pts = new ArrayList<>();\n"
                + "pts.add(new Pt(1,2)); pts.add(new Pt(3,4));\n"
                + "int sum = 0;\n"
                + "for (Pt p : pts) sum += p.x() + p.y();\n"
                + "root.add(new Label(\"sum=\" + sum));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "two_level_inheritance", ui(""
                + "class A { int v() { return 1; } }\n"
                + "class B extends A { int extra() { return v() * 10; } }\n"
                + "class C extends B { int v() { return 5; } }\n"
                + "C c = new C();\n"
                + "root.add(new Label(\"v=\" + c.v() + \" extra=\" + c.extra()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "interface_with_default_used", ui(""
                + "interface Greeter { default String greet() { return \"hello \" + name(); } String name(); }\n"
                + "Greeter g = new Greeter() { public String name() { return \"world\"; } };\n"
                + "root.add(new Label(g.greet()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "switch_expr_in_method_return", ui(""
                + "class C { String label(int x) { String s = switch (x) { case 1 -> \"one\"; default -> \"?\"; }; return s; } }\n"
                + "C c = new C();\n"
                + "root.add(new Label(c.label(1)));"), ExpectedOutcome.SUCCESS, null));
        // CN1's Collection backport doesn't expose stream() natively, so
        // the playground installs a minimal CN1StreamBridge shim that
        // supports filter / map / forEach / count / collect / reduce.
        cases.add(new Case(cat, "stream_filter_count", ui(""
                + "import java.util.*;\n"
                + "List<String> items = new ArrayList<>();\n"
                + "items.add(\"aa\"); items.add(\"b\"); items.add(\"ccc\");\n"
                + "long n = items.stream().filter(s -> ((String) s).length() > 1).count();\n"
                + "root.add(new Label(\"n=\" + n));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "stream_map_collect", ui(""
                + "import java.util.*;\n"
                + "List<String> items = new ArrayList<>();\n"
                + "items.add(\"a\"); items.add(\"bb\"); items.add(\"ccc\");\n"
                + "List lens = items.stream().map(s -> ((String) s).length()).toList();\n"
                + "root.add(new Label(\"lens=\" + lens));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "stream_sorted_distinct_limit", ui(""
                + "import java.util.*;\n"
                + "List<Integer> xs = new ArrayList<>();\n"
                + "xs.add(3); xs.add(1); xs.add(2); xs.add(3); xs.add(2);\n"
                + "List out = xs.stream().distinct().sorted().limit(2).toList();\n"
                + "root.add(new Label(\"out=\" + out));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "stream_any_all_none_match", ui(""
                + "import java.util.*;\n"
                + "List<Integer> xs = new ArrayList<>();\n"
                + "xs.add(2); xs.add(4); xs.add(6);\n"
                + "boolean any = xs.stream().anyMatch(x -> ((Integer) x) > 3);\n"
                + "boolean all = xs.stream().allMatch(x -> ((Integer) x) % 2 == 0);\n"
                + "boolean none = xs.stream().noneMatch(x -> ((Integer) x) > 10);\n"
                + "root.add(new Label(any + \",\" + all + \",\" + none));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "stream_reduce_binary_operator", ui(""
                + "import java.util.*;\n"
                + "List<Integer> xs = new ArrayList<>();\n"
                + "xs.add(1); xs.add(2); xs.add(3); xs.add(4);\n"
                + "Object sum = xs.stream().reduce(0, (a, b) -> ((Integer) a) + ((Integer) b));\n"
                + "root.add(new Label(\"sum=\" + sum));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "stream_find_first_min_max", ui(""
                + "import java.util.*;\n"
                + "List<Integer> xs = new ArrayList<>();\n"
                + "xs.add(5); xs.add(2); xs.add(9); xs.add(3);\n"
                + "Object first = xs.stream().findFirst();\n"
                + "Object min = xs.stream().min();\n"
                + "Object max = xs.stream().max();\n"
                + "root.add(new Label(first + \",\" + min + \",\" + max));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "stream_flat_map", ui(""
                + "import java.util.*;\n"
                + "List<List<Integer>> groups = new ArrayList<>();\n"
                + "List<Integer> a = new ArrayList<>(); a.add(1); a.add(2);\n"
                + "List<Integer> b = new ArrayList<>(); b.add(3); b.add(4);\n"
                + "groups.add(a); groups.add(b);\n"
                + "List flat = groups.stream().flatMap(g -> ((List) g).stream()).toList();\n"
                + "root.add(new Label(\"flat=\" + flat));"), ExpectedOutcome.SUCCESS, null));
        // Record compact constructor runs validation/normalisation
        // before the implicit field assignments.
        cases.add(new Case(cat, "record_compact_constructor", ui(""
                + "record Range(int lo, int hi) {\n"
                + "    Range {\n"
                + "        if (lo > hi) { int t = lo; lo = hi; hi = t; }\n"
                + "    }\n"
                + "}\n"
                + "Range r = new Range(7, 3);\n"
                + "root.add(new Label(\"lo=\" + r.lo() + \" hi=\" + r.hi()));"), ExpectedOutcome.SUCCESS, null));
        // Pattern-match switch binds the scrutinee into a typed variable
        // inside each arrow branch. Desugars to an if/else chain.
        cases.add(new Case(cat, "pattern_switch_statement", ui(""
                + "Object o = Integer.valueOf(7);\n"
                + "String tag;\n"
                + "switch (o) {\n"
                + "    case Integer i -> tag = \"int:\" + i;\n"
                + "    case String s -> tag = \"str:\" + s;\n"
                + "    default -> tag = \"other\";\n"
                + "}\n"
                + "root.add(new Label(tag));"), ExpectedOutcome.SUCCESS, null));
        // Sealed class with subclass named in the permit list is accepted.
        cases.add(new Case(cat, "sealed_permits_allowed", ui(""
                + "sealed class Shape permits Circle, Square {}\n"
                + "final class Circle extends Shape {}\n"
                + "final class Square extends Shape {}\n"
                + "Circle c = new Circle();\n"
                + "root.add(new Label(\"ok\"));"), ExpectedOutcome.SUCCESS, null));
        // Sealed enforcement: a subclass NOT named in the permit list is
        // replaced at rewrite time with an immediate throw.
        cases.add(new Case(cat, "sealed_permits_rejected", ui(""
                + "sealed class Base permits Allowed {}\n"
                + "final class Allowed extends Base {}\n"
                + "final class Sneaky extends Base {}\n"
                + "root.add(new Label(\"oops\"));"), ExpectedOutcome.EVAL_ERROR, "not permitted"));
        // Diagnostic suggestions: typo'd field name produces a "did you
        // mean" hint pointing at the nearest registry-known field.
        cases.add(new Case(cat, "typo_field_suggests_correction", ui(""
                + "int t = Display.PICKER_TYP_DATE;\n"
                + "root.add(new Label(\"t=\" + t));"), ExpectedOutcome.EVAL_ERROR, "did you mean"));
        // Inherited static field access: Display extends CN1Constants, so
        // PICKER_TYPE_DATE (declared on CN1Constants) is reachable through
        // the Display subclass reference.
        cases.add(new Case(cat, "inherited_static_field_via_subclass", ui(""
                + "int t = Display.PICKER_TYPE_DATE;\n"
                + "root.add(new Label(\"t=\" + t));"), ExpectedOutcome.SUCCESS, null));
        // Nested public static class static-field access: the registry
        // now flattens Outer.Inner nested classes so that static fields
        // like Paint.Align.CENTER dispatch through the generated
        // getStaticField tables.
        cases.add(new Case(cat, "nested_static_class_field", ui(""
                + "import com.codename1.charts.compat.Paint;\n"
                + "int align = Paint.Align.CENTER;\n"
                + "root.add(new Label(\"align=\" + align));"), ExpectedOutcome.SUCCESS, null));
        // End-to-end Picker script — inherited static field via subclass,
        // Runnable lambdas, and nested-class enum reference all in one.
        cases.add(new Case(cat, "picker_lightweight_popup_script", ui(""
                + "import com.codename1.ui.spinner.Picker;\n"
                + "import java.util.Calendar;\n"
                + "import java.util.Date;\n"
                + "Picker picker = new Picker();\n"
                + "picker.setType(Display.PICKER_TYPE_DATE);\n"
                + "picker.setUseLightweightPopup(true);\n"
                + "picker.setDate(new Date());\n"
                + "picker.addLightweightPopupButton(\"Today\", () -> picker.setDate(new Date()));\n"
                + "picker.addLightweightPopupButton(\"+7 Days\", () -> {\n"
                + "    Calendar cal = Calendar.getInstance();\n"
                + "    cal.add(Calendar.DAY_OF_MONTH, 7);\n"
                + "    picker.setDate(cal.getTime());\n"
                + "}, Picker.LightweightPopupButtonPlacement.BELOW_SPINNER);\n"
                + "root.add(picker);"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "method_overloading", ui(""
                + "class M { String fmt(int n) { return \"int:\" + n; } String fmt(String s) { return \"str:\" + s; } }\n"
                + "M m = new M();\n"
                + "root.add(new Label(m.fmt(5) + \" \" + m.fmt(\"hi\")));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "ctor_overloading", ui(""
                + "class P { int v; P() { v = 0; } P(int x) { v = x; } }\n"
                + "P a = new P();\n"
                + "P b = new P(7);\n"
                + "root.add(new Label(\"a=\" + a.v + \" b=\" + b.v));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "iterator_walk", ui(""
                + "import java.util.*;\n"
                + "List<Integer> items = new ArrayList<>();\n"
                + "items.add(1); items.add(2); items.add(3);\n"
                + "Iterator it = items.iterator();\n"
                + "int sum = 0;\n"
                + "while (it.hasNext()) sum += (Integer) it.next();\n"
                + "root.add(new Label(\"sum=\" + sum));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "class_implements_scripted_interface", ui(""
                + "interface Greeter { String greet(); }\n"
                + "class Hello implements Greeter { public String greet() { return \"hi\"; } }\n"
                + "Hello h = new Hello();\n"
                + "root.add(new Label(h.greet()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "comparator_natural_order", ui(""
                + "import java.util.*;\n"
                + "List<String> items = new ArrayList<>();\n"
                + "items.add(\"c\"); items.add(\"a\"); items.add(\"b\");\n"
                + "Collections.sort(items, (a, b) -> a.compareTo(b));\n"
                + "root.add(new Label(items.toString()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "three_level_inheritance", ui(""
                + "class A { String name() { return \"A\"; } }\n"
                + "class B extends A { String name() { return \"B(\" + super.name() + \")\"; } }\n"
                + "class C extends B { String name() { return \"C(\" + super.name() + \")\"; } }\n"
                + "C c = new C();\n"
                + "root.add(new Label(c.name()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "recursion_factorial", ui(""
                + "class F { int fact(int n) { return n <= 1 ? 1 : n * fact(n - 1); } }\n"
                + "F f = new F();\n"
                + "root.add(new Label(\"5!=\" + f.fact(5)));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "enum_implements_interface", ui(""
                + "interface Op { int apply(int a, int b); }\n"
                + "enum Ops implements Op {\n"
                + "  ADD { public int apply(int a, int b) { return a + b; } },\n"
                + "  MUL { public int apply(int a, int b) { return a * b; } };\n"
                + "  public int apply(int a, int b) { return 0; }\n"
                + "}\n"
                + "root.add(new Label(\"add=\" + Ops.ADD.apply(2,3) + \" mul=\" + Ops.MUL.apply(2,3)));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "interface_extends_interface", ui(""
                + "interface Named { String name(); }\n"
                + "interface Greet extends Named { default String greet() { return \"hi \" + name(); } }\n"
                + "Greet g = new Greet() { public String name() { return \"bob\"; } };\n"
                + "root.add(new Label(g.greet()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "builder_pattern", ui(""
                + "class Person {\n"
                + "  String name; int age;\n"
                + "  Person withName(String n) { name = n; return this; }\n"
                + "  Person withAge(int a) { age = a; return this; }\n"
                + "  String show() { return name + \",\" + age; }\n"
                + "}\n"
                + "Person p = new Person().withName(\"Alice\").withAge(30);\n"
                + "root.add(new Label(p.show()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "custom_exception_hierarchy", ui(""
                + "class AppEx extends RuntimeException { AppEx(String m) { super(m); } }\n"
                + "String caught = \"\";\n"
                + "try { throw new AppEx(\"boom\"); }\n"
                + "catch (AppEx e) { caught = e.getMessage(); }\n"
                + "root.add(new Label(\"c=\" + caught));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "nested_switch_expressions", ui(""
                + "int x = 2, y = 3;\n"
                + "String s = switch (x) {\n"
                + "  case 1 -> \"one\";\n"
                + "  case 2 -> switch (y) { case 3 -> \"two-three\"; default -> \"two-?\"; };\n"
                + "  default -> \"?\";\n"
                + "};\n"
                + "root.add(new Label(s));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "record_with_static_factory", ui(""
                + "record Box(int v) { static Box of(int v) { return new Box(v); } }\n"
                + "Box b = Box.of(7);\n"
                + "root.add(new Label(\"v=\" + b.v()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "enum_used_in_switch", ui(""
                + "enum Color { RED, GREEN, BLUE }\n"
                + "Color c = Color.GREEN;\n"
                + "String s = switch (c) { case RED -> \"r\"; case GREEN -> \"g\"; case BLUE -> \"b\"; };\n"
                + "root.add(new Label(s));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "state_machine_via_enum", ui(""
                + "enum S {\n"
                + "  OPEN { public S next() { return CLOSED; } },\n"
                + "  CLOSED { public S next() { return OPEN; } };\n"
                + "  public S next() { return this; }\n"
                + "}\n"
                + "S s = S.OPEN.next();\n"
                + "root.add(new Label(s.name()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "twr_with_var", ui(""
                + "import java.io.*;\n"
                + "try (var in = new StringReader(\"hi\")) {\n"
                + "  root.add(new Label(\"ok\"));\n"
                + "}"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "field_init_at_declaration", ui(""
                + "class C { int x = 42; String s = \"hi\"; }\n"
                + "C c = new C();\n"
                + "root.add(new Label(\"x=\" + c.x + \" s=\" + c.s));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "list_of_records", ui(""
                + "import java.util.*;\n"
                + "record Task(String name, int priority) {}\n"
                + "List<Task> tasks = new ArrayList<>();\n"
                + "tasks.add(new Task(\"a\", 1));\n"
                + "tasks.add(new Task(\"b\", 5));\n"
                + "Collections.sort(tasks, (x, y) -> x.priority() - y.priority());\n"
                + "root.add(new Label(\"first=\" + tasks.get(0).name()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "exception_with_cause_chain", ui(""
                + "class AppEx extends RuntimeException { AppEx(String m, Throwable c) { super(m, c); } }\n"
                + "String chain = \"\";\n"
                + "try {\n"
                + "  try { throw new RuntimeException(\"root\"); }\n"
                + "  catch (RuntimeException e) { throw new AppEx(\"wrapped\", e); }\n"
                + "} catch (AppEx ex) {\n"
                + "  chain = ex.getMessage() + \"/\" + ex.getCause().getMessage();\n"
                + "}\n"
                + "root.add(new Label(chain));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "generic_method_on_class", ui(""
                + "class Box { <T> T identity(T t) { return t; } }\n"
                + "Box b = new Box();\n"
                + "String s = (String) b.identity(\"hi\");\n"
                + "root.add(new Label(s));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "multi_dim_array", ui(""
                + "int[][] grid = new int[][]{{1,2,3},{4,5,6}};\n"
                + "int sum = 0;\n"
                + "for (int[] row : grid) for (int v : row) sum += v;\n"
                + "root.add(new Label(\"sum=\" + sum));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "interface_with_two_methods", ui(""
                + "interface Named { String name(); void setName(String n); }\n"
                + "class Item implements Named {\n"
                + "  String n;\n"
                + "  public String name() { return n; }\n"
                + "  public void setName(String v) { n = v; }\n"
                + "}\n"
                + "Item i = new Item();\n"
                + "i.setName(\"hello\");\n"
                + "root.add(new Label(i.name()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "enum_values_iteration", ui(""
                + "enum Color { RED, GREEN, BLUE }\n"
                + "String s = \"\";\n"
                + "for (Color c : Color.values()) s += c.name() + \" \";\n"
                + "root.add(new Label(s.trim()));"), ExpectedOutcome.SUCCESS, null));
        // Map.Entry now resolves as a type (falling back to the JVM
        // `Outer$Inner` form). Method dispatch on HashMap$Node itself
        // isn't in the registry, so iterate via keySet() instead — that
        // case is covered by map_keyset_iteration.
        cases.add(new Case(cat, "map_entry_type_resolves", ui(""
                + "import java.util.*;\n"
                + "Class c = Map.Entry.class;\n"
                + "root.add(new Label(c.getName()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "map_entryset_iteration", ui(""
                + "import java.util.*;\n"
                + "Map<String, Integer> m = new HashMap<>();\n"
                + "m.put(\"a\", 1); m.put(\"b\", 2);\n"
                + "int total = 0;\n"
                + "for (Map.Entry e : m.entrySet()) total += (Integer) e.getValue();\n"
                + "root.add(new Label(\"total=\" + total));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "map_keyset_iteration", ui(""
                + "import java.util.*;\n"
                + "Map<String, Integer> m = new HashMap<>();\n"
                + "m.put(\"a\", 1); m.put(\"b\", 2);\n"
                + "int total = 0;\n"
                + "for (String k : m.keySet()) total += m.get(k);\n"
                + "root.add(new Label(\"total=\" + total));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "lambda_composition", ui(""
                + "import java.util.function.*;\n"
                + "Function<Integer, Integer> plus1 = x -> x + 1;\n"
                + "Function<Integer, Integer> times2 = x -> x * 2;\n"
                + "int v = times2.apply(plus1.apply(3));\n"
                + "root.add(new Label(\"v=\" + v));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "stringbuilder_no_arg", ui(""
                + "StringBuilder sb = new StringBuilder();\n"
                + "for (int i = 0; i < 3; i++) sb.append(\"x\");\n"
                + "root.add(new Label(sb.toString()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "stringbuilder_with_initial", ui(""
                + "StringBuilder sb = new StringBuilder(\"start-\");\n"
                + "for (int i = 0; i < 3; i++) sb.append(\"x\");\n"
                + "root.add(new Label(sb.toString()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "string_concat_loop_substitute", ui(""
                + "String s = \"\";\n"
                + "for (int i = 0; i < 3; i++) s += \"x\";\n"
                + "root.add(new Label(s));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "arrays_aslist", ui(""
                + "import java.util.*;\n"
                + "List<String> items = Arrays.asList(\"a\",\"b\",\"c\");\n"
                + "String txt = \"\";\n"
                + "for (String s : items) txt += s;\n"
                + "root.add(new Label(txt));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "record_equals_and_tostring", ui(""
                + "record K(int a, int b) {}\n"
                + "K x = new K(1, 2);\n"
                + "K y = new K(1, 2);\n"
                + "root.add(new Label(\"ok=\" + (x != y)));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "class_with_protected_method", ui(""
                + "class A { protected int internal() { return 11; } }\n"
                + "class B extends A { int expose() { return internal() + 1; } }\n"
                + "root.add(new Label(\"v=\" + new B().expose()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "visitor_pattern", ui(""
                + "interface Visitor { int visit(int n); }\n"
                + "class Doubler implements Visitor { public int visit(int n) { return n * 2; } }\n"
                + "class Caller { int run(Visitor v, int n) { return v.visit(n); } }\n"
                + "Caller c = new Caller();\n"
                + "Doubler d = new Doubler();\n"
                + "root.add(new Label(\"v=\" + c.run(d, 7)));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "factory_method_returning_interface", ui(""
                + "interface Shape { int area(); }\n"
                + "class Shapes { static Shape rect(int w, int h) { return new Shape() { public int area() { return w * h; } }; } }\n"
                + "Shape s = Shapes.rect(3, 4);\n"
                + "root.add(new Label(\"a=\" + s.area()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "chained_method_calls_returning_this", ui(""
                + "class Fluent { StringBuilder sb = new StringBuilder(\"\"); Fluent add(String s) { sb.append(s); return this; } String build() { return sb.toString(); } }\n"
                + "String r = new Fluent().add(\"a\").add(\"b\").add(\"c\").build();\n"
                + "root.add(new Label(r));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "static_method_calling_other_static", ui(""
                + "class U { static int add(int a, int b) { return a + b; } static int sum3(int a, int b, int c) { return add(add(a, b), c); } }\n"
                + "root.add(new Label(\"v=\" + U.sum3(1, 2, 3)));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "generic_container", ui(""
                + "class Box<T> { T value; Box(T v) { value = v; } T get() { return value; } }\n"
                + "Box<String> s = new Box<String>(\"hi\");\n"
                + "Box<Integer> i = new Box<Integer>(7);\n"
                + "root.add(new Label(s.get() + \" \" + i.get()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "array_element_increment", ui(""
                + "int[] a = new int[]{0};\n"
                + "int v = (a[0]++ > -1) ? a[0] : -1;\n"
                + "root.add(new Label(\"v=\" + v));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "array_element_postfix_decrement", ui(""
                + "int[] a = new int[]{3};\n"
                + "int before = a[0]--;\n"
                + "root.add(new Label(\"before=\" + before + \" after=\" + a[0]));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "nested_lambdas", ui(""
                + "import java.util.function.*;\n"
                + "Function<Integer, Function<Integer, Integer>> adder = a -> b -> a + b;\n"
                + "root.add(new Label(\"v=\" + adder.apply(3).apply(4)));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "array_access_in_ternary", ui(""
                + "int[] arr = new int[]{1,2,3};\n"
                + "int pick = 2;\n"
                + "int v = pick >= 0 && pick < arr.length ? arr[pick] : -1;\n"
                + "root.add(new Label(\"v=\" + v));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "class_with_static_field_used_via_instance", ui(""
                + "class Counter { static int total = 0; void tick() { total = total + 1; } }\n"
                + "Counter a = new Counter();\n"
                + "a.tick(); a.tick(); a.tick();\n"
                + "root.add(new Label(\"total=\" + Counter.total));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "method_ref_in_sort", ui(""
                + "import java.util.*;\n"
                + "List<String> items = new ArrayList<>();\n"
                + "items.add(\"bbb\"); items.add(\"a\"); items.add(\"cc\");\n"
                + "Collections.sort(items, (a, b) -> Integer.compare(a.length(), b.length()));\n"
                + "root.add(new Label(items.toString()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "long_chain_calls", ui(""
                + "import java.util.*;\n"
                + "String s = new ArrayList<String>().toString();\n"
                + "root.add(new Label(s));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "cast_of_scripted_instance", ui(""
                + "class A { int v() { return 5; } }\n"
                + "Object o = new A();\n"
                + "A a = (A) o;\n"
                + "root.add(new Label(\"v=\" + a.v()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "exception_across_methods", ui(""
                + "class C {\n"
                + "  int inner() { throw new RuntimeException(\"boom\"); }\n"
                + "  String outer() { try { inner(); return \"no\"; } catch (RuntimeException e) { return e.getMessage(); } }\n"
                + "}\n"
                + "root.add(new Label(new C().outer()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "return_inside_lambda_block", ui(""
                + "import java.util.function.*;\n"
                + "Function<Integer, Integer> abs = n -> { if (n < 0) return -n; return n; };\n"
                + "root.add(new Label(\"abs=\" + abs.apply(-7)));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "multiple_anonymous_impls", ui(""
                + "interface Op { int go(); }\n"
                + "Op a = new Op() { public int go() { return 1; } };\n"
                + "Op b = new Op() { public int go() { return 2; } };\n"
                + "root.add(new Label(\"sum=\" + (a.go() + b.go())));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "generic_class_with_generic_method", ui(""
                + "class Box<T> { T val; Box(T v) { val = v; } <U> U swap(U other) { return other; } }\n"
                + "Box<String> b = new Box<String>(\"a\");\n"
                + "String r = (String) b.swap(\"hello\");\n"
                + "root.add(new Label(r));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "enum_switch_with_yield", ui(""
                + "enum D { UP, DOWN }\n"
                + "D d = D.DOWN;\n"
                + "String s = switch (d) { case UP: yield \"up\"; case DOWN: yield \"down\"; };\n"
                + "root.add(new Label(s));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "record_toString_format", ui(""
                + "record P(int x, int y) {}\n"
                + "P p = new P(3, 4);\n"
                + "String s = \"point=\" + p;\n"
                + "root.add(new Label(\"len>0=\" + (s.length() > 0)));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "class_with_private_helper_method", ui(""
                + "class C { private int helper(int n) { return n * 2; } int doit(int n) { return helper(n) + 1; } }\n"
                + "root.add(new Label(\"v=\" + new C().doit(3)));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "loop_accumulating_into_list", ui(""
                + "import java.util.*;\n"
                + "List<Integer> nums = new ArrayList<>();\n"
                + "for (int i = 0; i < 5; i++) nums.add(i * i);\n"
                + "int sum = 0;\n"
                + "for (int n : nums) sum += n;\n"
                + "root.add(new Label(\"sum=\" + sum));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "bounded_generic_type", ui(""
                + "class NumberBox<T extends Number> { T val; NumberBox(T v) { val = v; } double asDouble() { return val.doubleValue(); } }\n"
                + "NumberBox<Integer> b = new NumberBox<Integer>(42);\n"
                + "root.add(new Label(\"v=\" + b.asDouble()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "string_equals", ui(""
                + "String a = \"hello\";\n"
                + "String b = \"hel\" + \"lo\";\n"
                + "root.add(new Label(\"eq=\" + a.equals(b)));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "string_substring_indexof", ui(""
                + "String s = \"hello world\";\n"
                + "int sp = s.indexOf(\" \");\n"
                + "String first = s.substring(0, sp);\n"
                + "root.add(new Label(first));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "string_indexof_char_widens", ui(""
                + "String s = \"hello world\";\n"
                + "int sp = s.indexOf(' ');\n"
                + "root.add(new Label(\"sp=\" + sp));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "integer_parse_and_format", ui(""
                + "int v = Integer.parseInt(\"42\");\n"
                + "String s = Integer.toString(v * 2);\n"
                + "root.add(new Label(s));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "container_with_multiple_buttons", ui(""
                + "for (int i = 0; i < 3; i++) {\n"
                + "  int n = i;\n"
                + "  Button b = new Button(\"b\" + n);\n"
                + "  b.addActionListener(e -> {});\n"
                + "  root.add(b);\n"
                + "}\n"
                + "root.add(new Label(\"count=\" + root.getComponentCount()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "form_layout_switching", ui(""
                + "Container c = new Container(new FlowLayout());\n"
                + "c.add(new Label(\"a\"));\n"
                + "c.add(new Label(\"b\"));\n"
                + "c.setLayout(new BoxLayout(BoxLayout.Y_AXIS));\n"
                + "root.add(c);"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "hashset_basic", ui(""
                + "import java.util.*;\n"
                + "Set<String> s = new HashSet<>();\n"
                + "s.add(\"a\"); s.add(\"b\"); s.add(\"a\");\n"
                + "root.add(new Label(\"n=\" + s.size()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "linked_list_as_queue", ui(""
                + "import java.util.*;\n"
                + "LinkedList<String> q = new LinkedList<>();\n"
                + "q.add(\"a\"); q.add(\"b\"); q.add(\"c\");\n"
                + "String first = q.removeFirst();\n"
                + "root.add(new Label(\"first=\" + first + \" left=\" + q.size()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "instanceof_array_type", ui(""
                + "Object o = new int[]{1,2,3};\n"
                + "boolean b = o instanceof int[];\n"
                + "root.add(new Label(\"b=\" + b));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "final_local_variable", ui(""
                + "final int x = 42;\n"
                + "int y = x + 1;\n"
                + "root.add(new Label(\"y=\" + y));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "long_arithmetic", ui(""
                + "long a = 1_000_000_000L;\n"
                + "long b = a * 7L;\n"
                + "root.add(new Label(\"b=\" + b));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "bitwise_long_ops", ui(""
                + "long mask = 0xFFL << 8;\n"
                + "long v = mask | 0xAAL;\n"
                + "root.add(new Label(\"v=\" + v));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "scripted_class_as_map_key", ui(""
                + "import java.util.*;\n"
                + "class Key { int id; Key(int v) { id = v; } }\n"
                + "Map<Key, String> m = new HashMap<>();\n"
                + "Key k = new Key(1);\n"
                + "m.put(k, \"one\");\n"
                + "root.add(new Label(\"got=\" + m.get(k)));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "multi_return_paths", ui(""
                + "class C { String classify(int n) { if (n < 0) return \"neg\"; if (n == 0) return \"zero\"; return \"pos\"; } }\n"
                + "C c = new C();\n"
                + "root.add(new Label(c.classify(-3) + \"/\" + c.classify(0) + \"/\" + c.classify(5)));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "this_in_listener_captures_field", ui(""
                + "class C {\n"
                + "  String[] tag = new String[]{\"init\"};\n"
                + "  void setup(Button b) { b.addActionListener(e -> tag[0] = \"clicked\"); }\n"
                + "}\n"
                + "C c = new C();\n"
                + "Button b = new Button(\"Go\");\n"
                + "c.setup(b);\n"
                + "root.add(b);\n"
                + "root.add(new Label(c.tag[0]));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "triple_nested_enhanced_for", ui(""
                + "int[][][] cube = new int[][][]{{{1,2},{3,4}},{{5,6},{7,8}}};\n"
                + "int total = 0;\n"
                + "for (int[][] plane : cube) for (int[] row : plane) for (int v : row) total += v;\n"
                + "root.add(new Label(\"total=\" + total));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "labeled_outer_break_with_search", ui(""
                + "int[][] grid = new int[][]{{1,2,3},{4,5,6},{7,8,9}};\n"
                + "int found = -1;\n"
                + "outer: for (int[] row : grid) {\n"
                + "  for (int v : row) { if (v == 5) { found = v; break outer; } }\n"
                + "}\n"
                + "root.add(new Label(\"found=\" + found));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "try_finally_side_effect", ui(""
                + "String[] log = new String[]{\"\"};\n"
                + "try {\n"
                + "  log[0] = log[0] + \"body \";\n"
                + "} finally {\n"
                + "  log[0] = log[0] + \"fin\";\n"
                + "}\n"
                + "root.add(new Label(log[0]));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "null_check_then_use", ui(""
                + "String s = null;\n"
                + "s = \"hello\";\n"
                + "String msg = s != null ? s.toUpperCase() : \"(none)\";\n"
                + "root.add(new Label(msg));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "abstract_template_method", ui(""
                + "abstract class Base { public abstract int getNum(); public int doubled() { return getNum() * 2; } }\n"
                + "class Five extends Base { public int getNum() { return 5; } }\n"
                + "Five f = new Five();\n"
                + "root.add(new Label(\"v=\" + f.doubled()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "chained_ternary", ui(""
                + "int n = 0;\n"
                + "String sign = n < 0 ? \"-\" : n == 0 ? \"0\" : \"+\";\n"
                + "root.add(new Label(sign));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "enum_compare_identity", ui(""
                + "enum C { RED, BLUE }\n"
                + "C a = C.RED;\n"
                + "C b = C.BLUE;\n"
                + "boolean same = (a == C.RED) && (a != b);\n"
                + "root.add(new Label(\"same=\" + same));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "enum_switch_default_fallthrough", ui(""
                + "enum Op { ADD, SUB, MUL }\n"
                + "Op o = Op.MUL;\n"
                + "String tag = switch (o) {\n"
                + "  case ADD -> \"plus\";\n"
                + "  case SUB -> \"minus\";\n"
                + "  default -> \"other\";\n"
                + "};\n"
                + "root.add(new Label(tag));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "math_operations", ui(""
                + "double v = Math.abs(-3.5);\n"
                + "double w = Math.max(v, 7.0);\n"
                + "root.add(new Label(\"v=\" + v + \" w=\" + w));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "integer_comparison", ui(""
                + "Integer a = 5;\n"
                + "Integer b = 10;\n"
                + "int r = Integer.compare(a, b);\n"
                + "String tag;\n"
                + "if (r < 0) tag = \"less\"; else if (r > 0) tag = \"more\"; else tag = \"eq\";\n"
                + "root.add(new Label(\"r=\" + tag));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "rethrow_exception", ui(""
                + "String r = \"\";\n"
                + "try {\n"
                + "  try { throw new RuntimeException(\"inner\"); }\n"
                + "  catch (RuntimeException e) { r += \"caught-\"; throw e; }\n"
                + "} catch (RuntimeException e) { r += e.getMessage(); }\n"
                + "root.add(new Label(r));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "interface_default_calling_abstract", ui(""
                + "interface Named { String name(); default String hello() { return \"hi \" + name(); } }\n"
                + "class Dog implements Named { public String name() { return \"rex\"; } }\n"
                + "Dog d = new Dog();\n"
                + "root.add(new Label(d.hello()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "shared_static_field_two_instances", ui(""
                + "class N { static int counter = 0; void tick() { counter = counter + 1; } }\n"
                + "N a = new N(); N b = new N();\n"
                + "a.tick(); a.tick(); b.tick();\n"
                + "root.add(new Label(\"v=\" + N.counter));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "three_level_polymorphism", ui(""
                + "class A { String who() { return \"A\"; } }\n"
                + "class B extends A { public String who() { return \"B\"; } }\n"
                + "class C extends B { public String who() { return \"C\"; } }\n"
                + "A a1 = new A(); A a2 = new B(); A a3 = new C();\n"
                + "root.add(new Label(a1.who() + a2.who() + a3.who()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "finally_runs_despite_return", ui(""
                + "class C {\n"
                + "  String[] log = new String[]{\"\"};\n"
                + "  String go() { try { return \"body\"; } finally { log[0] = \"fin\"; } }\n"
                + "}\n"
                + "C c = new C();\n"
                + "String r = c.go();\n"
                + "root.add(new Label(r + \":\" + c.log[0]));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "observer_pattern", ui(""
                + "import java.util.*;\n"
                + "interface Listener { void on(String ev); }\n"
                + "class Bus {\n"
                + "  List<Listener> ls = new ArrayList<>();\n"
                + "  void add(Listener l) { ls.add(l); }\n"
                + "  void fire(String ev) { for (Listener l : ls) l.on(ev); }\n"
                + "}\n"
                + "String[] log = new String[]{\"\"};\n"
                + "Bus bus = new Bus();\n"
                + "bus.add(new Listener() { public void on(String ev) { log[0] = log[0] + \"A:\" + ev + \" \"; } });\n"
                + "bus.add(new Listener() { public void on(String ev) { log[0] = log[0] + \"B:\" + ev + \" \"; } });\n"
                + "bus.fire(\"ping\");\n"
                + "root.add(new Label(log[0].trim()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "decorator_pattern", ui(""
                + "interface Greeter { String greet(); }\n"
                + "class Basic implements Greeter { public String greet() { return \"hi\"; } }\n"
                + "class Excited implements Greeter { Greeter inner; Excited(Greeter i) { inner = i; } public String greet() { return inner.greet() + \"!\"; } }\n"
                + "Greeter g = new Excited(new Excited(new Basic()));\n"
                + "root.add(new Label(g.greet()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "map_of_list", ui(""
                + "import java.util.*;\n"
                + "Map<String, List<String>> m = new HashMap<>();\n"
                + "m.put(\"a\", new ArrayList<String>());\n"
                + "m.get(\"a\").add(\"x\"); m.get(\"a\").add(\"y\");\n"
                + "int n = m.get(\"a\").size();\n"
                + "root.add(new Label(\"n=\" + n));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "continue_in_loop", ui(""
                + "int sum = 0;\n"
                + "for (int i = 0; i < 10; i++) {\n"
                + "  if (i % 2 == 0) continue;\n"
                + "  sum += i;\n"
                + "}\n"
                + "root.add(new Label(\"sum=\" + sum));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "string_starts_ends_with", ui(""
                + "String s = \"hello.world\";\n"
                + "boolean starts = s.startsWith(\"hello\");\n"
                + "boolean ends = s.endsWith(\"world\");\n"
                + "root.add(new Label(\"s=\" + starts + \" e=\" + ends));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "string_char_at_and_length", ui(""
                + "String s = \"abc\";\n"
                + "char c = s.charAt(1);\n"
                + "int n = s.length();\n"
                + "root.add(new Label(\"c=\" + c + \" n=\" + n));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "deeply_nested_conditions", ui(""
                + "int x = 3, y = 5;\n"
                + "String tag;\n"
                + "if (x > 0) { if (y > 0) { if (x < y) { tag = \"ok\"; } else { tag = \"ge\"; } } else { tag = \"yneg\"; } } else { tag = \"xneg\"; }\n"
                + "root.add(new Label(tag));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "enum_iter_if_else", ui(""
                + "enum E { A, B, C }\n"
                + "String tag = \"\";\n"
                + "for (E e : E.values()) { if (e == E.B) tag = tag + \"!\" + e.name() + \"!\"; else tag = tag + e.name(); }\n"
                + "root.add(new Label(tag));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "nested_records", ui(""
                + "record Inner(int v) {}\n"
                + "record Outer(Inner left, Inner right) {}\n"
                + "Outer o = new Outer(new Inner(3), new Inner(4));\n"
                + "root.add(new Label(\"sum=\" + (o.left().v() + o.right().v())));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "record_with_primitive_components", ui(""
                + "record Pair(int a, int b) {}\n"
                + "Pair p = new Pair(3, 4);\n"
                + "root.add(new Label(\"sum=\" + (p.a() + p.b())));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "generic_interface_scripted", ui(""
                + "interface Mapper<T, R> { R map(T t); }\n"
                + "Mapper<String, Integer> m = new Mapper<String, Integer>() { public Integer map(String s) { return s.length(); } };\n"
                + "root.add(new Label(\"v=\" + m.map(\"hello\")));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "throwing_scripted_exception", ui(""
                + "class BadInput extends RuntimeException { BadInput(String m) { super(m); } }\n"
                + "String caught = \"\";\n"
                + "try { throw new BadInput(\"oops\"); }\n"
                + "catch (BadInput e) { caught = e.getMessage(); }\n"
                + "root.add(new Label(caught));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "array_literal_in_call", ui(""
                + "import java.util.*;\n"
                + "List<Integer> items = Arrays.asList(new Integer[]{3, 1, 2});\n"
                + "root.add(new Label(\"n=\" + items.size()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "null_coalesce_via_ternary", ui(""
                + "String candidate = null;\n"
                + "String result = candidate != null ? candidate : \"default\";\n"
                + "root.add(new Label(result));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "sequence_of_if_returns", ui(""
                + "class Validator {\n"
                + "  String check(String s) {\n"
                + "    if (s == null) return \"null\";\n"
                + "    if (s.length() == 0) return \"empty\";\n"
                + "    if (s.length() > 10) return \"too long\";\n"
                + "    return \"ok\";\n"
                + "  }\n"
                + "}\n"
                + "Validator v = new Validator();\n"
                + "root.add(new Label(v.check(\"hello\")));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "collections_sort_natural_order", ui(""
                + "import java.util.*;\n"
                + "List<String> items = new ArrayList<>();\n"
                + "items.add(\"c\"); items.add(\"a\"); items.add(\"b\");\n"
                + "Collections.sort(items);\n"
                + "root.add(new Label(items.toString()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "class_shadowing_imported_type", ui(""
                + "class Button { String id; Button(String v) { id = v; } }\n"
                + "Button b = new Button(\"custom\");\n"
                + "root.add(new Label(\"id=\" + b.id));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "empty_switch_expression", ui(""
                + "int x = 1;\n"
                + "String s = switch (x) { default -> \"fallback\"; };\n"
                + "root.add(new Label(s));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "switch_in_lambda_body", ui(""
                + "import java.util.function.*;\n"
                + "Function<Integer, String> f = n -> { String s = switch (n) { case 1 -> \"one\"; default -> \"other\"; }; return s; };\n"
                + "root.add(new Label(f.apply(1) + \"-\" + f.apply(9)));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "two_classes_typed_cross_ref", ui(""
                + "class Pair { Box first; Box second; }\n"
                + "class Box { int v; Box(int n) { v = n; } }\n"
                + "Pair p = new Pair();\n"
                + "p.first = new Box(1);\n"
                + "p.second = new Box(2);\n"
                + "root.add(new Label(\"sum=\" + (p.first.v + p.second.v)));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "external_field_assignment", ui(""
                + "class Pair { Object first; Object second; }\n"
                + "class Box { int v; Box(int n) { v = n; } }\n"
                + "Pair p = new Pair();\n"
                + "p.first = new Box(1);\n"
                + "p.second = new Box(2);\n"
                + "Box f = (Box) p.first;\n"
                + "Box s = (Box) p.second;\n"
                + "root.add(new Label(\"sum=\" + (f.v + s.v)));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "internal_field_assignment_via_setter", ui(""
                + "class Pair { Object first; Object second; void setFirst(Object o) { first = o; } void setSecond(Object o) { second = o; } }\n"
                + "class Box { int v; Box(int n) { v = n; } }\n"
                + "Pair p = new Pair();\n"
                + "p.setFirst(new Box(1));\n"
                + "p.setSecond(new Box(2));\n"
                + "Box f = (Box) p.first;\n"
                + "Box s = (Box) p.second;\n"
                + "root.add(new Label(\"sum=\" + (f.v + s.v)));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "class_accessing_another_class_method", ui(""
                + "class Math1 { int plus(int a, int b) { return a + b; } }\n"
                + "class Client { Math1 m = new Math1(); int run() { return m.plus(2, 3); } }\n"
                + "Client c = new Client();\n"
                + "root.add(new Label(\"v=\" + c.run()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "enum_switch_statement_arrow", ui(""
                + "enum Light { RED, YELLOW, GREEN }\n"
                + "Light l = Light.YELLOW;\n"
                + "String[] out = new String[]{\"\"};\n"
                + "switch (l) {\n"
                + "  case RED -> out[0] = \"stop\";\n"
                + "  case YELLOW -> out[0] = \"slow\";\n"
                + "  case GREEN -> out[0] = \"go\";\n"
                + "}\n"
                + "root.add(new Label(out[0]));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "initial_non_null_field", ui(""
                + "import java.util.*;\n"
                + "class Holder { List<String> items = new ArrayList<>(); }\n"
                + "Holder h = new Holder();\n"
                + "h.items.add(\"a\"); h.items.add(\"b\");\n"
                + "root.add(new Label(\"n=\" + h.items.size()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "scripted_list_of_scripted_instances", ui(""
                + "import java.util.*;\n"
                + "class Item { String n; Item(String v) { n = v; } }\n"
                + "List<Item> items = new ArrayList<>();\n"
                + "items.add(new Item(\"a\")); items.add(new Item(\"b\"));\n"
                + "String joined = \"\";\n"
                + "for (Item it : items) joined = joined + it.n;\n"
                + "root.add(new Label(joined));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "cn1_container_border_layout", ui(""
                + "Container c = new Container(new BorderLayout());\n"
                + "c.add(BorderLayout.NORTH, new Label(\"n\"));\n"
                + "c.add(BorderLayout.CENTER, new Label(\"c\"));\n"
                + "c.add(BorderLayout.SOUTH, new Label(\"s\"));\n"
                + "root.add(c);\n"
                + "root.add(new Label(\"cnt=\" + c.getComponentCount()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "cn1_textfield_and_listener", ui(""
                + "TextField tf = new TextField();\n"
                + "String[] last = new String[]{\"\"};\n"
                + "tf.addActionListener(e -> last[0] = \"changed\");\n"
                + "root.add(tf);\n"
                + "root.add(new Label(\"ok\"));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "cn1_button_chain_setters", ui(""
                + "Button b = new Button(\"Go\");\n"
                + "b.setUIID(\"Custom\");\n"
                + "b.setEnabled(false);\n"
                + "root.add(b);\n"
                + "root.add(new Label(\"uiid=\" + b.getUIID() + \" en=\" + b.isEnabled()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "collections_reverse_then_check", ui(""
                + "import java.util.*;\n"
                + "List<Integer> xs = new ArrayList<>();\n"
                + "for (int i = 1; i <= 3; i++) xs.add(i);\n"
                + "Collections.reverse(xs);\n"
                + "root.add(new Label(xs.toString()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "function_chain_via_scripted_class", ui(""
                + "class Pipeline {\n"
                + "  int run(int start) { return add(mul(start, 3), 4); }\n"
                + "  int add(int a, int b) { return a + b; }\n"
                + "  int mul(int a, int b) { return a * b; }\n"
                + "}\n"
                + "root.add(new Label(\"v=\" + new Pipeline().run(5)));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "varargs_like_user_method", ui(""
                + "class V { int sum(int... xs) { int s = 0; for (int x : xs) s += x; return s; } }\n"
                + "V v = new V();\n"
                + "root.add(new Label(\"s=\" + v.sum(1, 2, 3, 4)));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "interface_constant_unqualified", ui(""
                + "interface Config { String DEFAULT_NAME = \"world\"; String greet(); }\n"
                + "class Greeter implements Config { public String greet() { return \"hi \" + DEFAULT_NAME; } }\n"
                + "root.add(new Label(new Greeter().greet()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "interface_constant_qualified", ui(""
                + "interface Config { String DEFAULT_NAME = \"world\"; String greet(); }\n"
                + "class Greeter implements Config { public String greet() { return \"hi \" + Config.DEFAULT_NAME; } }\n"
                + "root.add(new Label(new Greeter().greet()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "boolean_short_circuit_and", ui(""
                + "String[] log = new String[]{\"\"};\n"
                + "boolean result = false && (log[0] = \"touched\") != null;\n"
                + "root.add(new Label(\"r=\" + result + \" log=\" + log[0]));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "boolean_short_circuit_or", ui(""
                + "boolean r = true || (1/0 > 0);\n"
                + "root.add(new Label(\"r=\" + r));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "scripted_class_returned_from_method", ui(""
                + "class Factory { Holder make(String s) { return new Holder(s); } }\n"
                + "class Holder { String text; Holder(String t) { text = t; } }\n"
                + "Holder h = new Factory().make(\"hello\");\n"
                + "root.add(new Label(h.text));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "nested_enhanced_for_with_continue", ui(""
                + "int[][] g = new int[][]{{1,2,3},{4,5,6}};\n"
                + "int sum = 0;\n"
                + "for (int[] row : g) {\n"
                + "  for (int v : row) { if (v % 2 == 0) continue; sum += v; }\n"
                + "}\n"
                + "root.add(new Label(\"sum=\" + sum));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "complex_predicate_with_and_or", ui(""
                + "import java.util.function.*;\n"
                + "Predicate<Integer> isPositive = n -> n > 0;\n"
                + "Predicate<Integer> isEven = n -> n % 2 == 0;\n"
                + "int count = 0;\n"
                + "for (int n : new int[]{-2,-1,0,1,2,3,4}) {\n"
                + "  if (isPositive.test(n) && isEven.test(n)) count++;\n"
                + "}\n"
                + "root.add(new Label(\"n=\" + count));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "enum_with_multiple_fields", ui(""
                + "enum Axis { X(1,0), Y(0,1); int dx; int dy; Axis(int x, int y) { dx = x; dy = y; } }\n"
                + "Axis a = Axis.X;\n"
                + "root.add(new Label(\"dx=\" + a.dx + \" dy=\" + a.dy));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "enum_method_returning_self", ui(""
                + "enum State { OPEN, CLOSED; public State flip() { return this == OPEN ? CLOSED : OPEN; } }\n"
                + "State s = State.OPEN.flip();\n"
                + "root.add(new Label(s.name()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "method_parameter_shadowing_field", ui(""
                + "class C { int x = 10; int add(int x) { return this.x + x; } }\n"
                + "C c = new C();\n"
                + "root.add(new Label(\"v=\" + c.add(5)));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "lambda_method_ref_combo", ui(""
                + "import java.util.function.*;\n"
                + "Predicate<String> nonEmpty = s -> s.length() > 0;\n"
                + "Function<String, Integer> len = String::length;\n"
                + "boolean ok = nonEmpty.test(\"hi\");\n"
                + "int n = len.apply(\"hello\");\n"
                + "root.add(new Label(\"ok=\" + ok + \" n=\" + n));"), ExpectedOutcome.SUCCESS, null));
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
        cases.add(new Case(cat, "list_explicit_type", ui(""
                + "List<String> items = new ArrayList<String>();\n"
                + "items.add(\"a\"); items.add(\"b\");\n"
                + "root.add(new Label(\"n=\" + items.size()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "list_diamond", ui(""
                + "List<String> items = new ArrayList<>();\n"
                + "items.add(\"a\"); items.add(\"b\");\n"
                + "root.add(new Label(\"n=\" + items.size()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "map_nested_generics", ui(""
                + "Map<String, List<Integer>> m = new HashMap<String, List<Integer>>();\n"
                + "m.put(\"a\", new ArrayList<Integer>());\n"
                + "m.get(\"a\").add(1);\n"
                + "root.add(new Label(\"n=\" + m.get(\"a\").size()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "wildcard_extends", ui(""
                + "List<? extends Number> nums = new ArrayList<Integer>();\n"
                + "root.add(new Label(\"n=\" + nums.size()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "wildcard_super", ui(""
                + "List<? super Integer> sink = new ArrayList<Number>();\n"
                + "sink.add(1);\n"
                + "root.add(new Label(\"ok\"));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "raw_type_mix", ui(""
                + "List items = new ArrayList();\n"
                + "items.add(\"a\");\n"
                + "root.add(new Label(\"n=\" + items.size()));"), ExpectedOutcome.SUCCESS, null));
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
        // Method refs are rewritten to one-arg lambdas before the parser sees
        // them. This handles bound-instance and "instance-looking class"
        // receivers used with known SAM call sites; unbound instance refs
        // and zero-arg targets like Supplier still fail.
        cases.add(new Case(cat, "bound_known_sam_static", ui(""
                + "Button b = new Button(\"Go\");\n"
                + "b.addActionListener(System.out::println);\n"
                + "root.add(b);"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "bound_instance", ui(""
                + "String prefix = \"X:\";\n"
                + "Button b = new Button(\"Go\");\n"
                + "b.addActionListener(prefix::concat);\n"
                + "root.add(b);"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "constructor_ref", ui(""
                + "import java.util.function.*;\n"
                + "Supplier<ArrayList> ctor = ArrayList::new;\n"
                + "root.add(new Label(\"size=\" + ctor.get().size()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "unbound_instance", ui(""
                + "import java.util.function.*;\n"
                + "Function<String, Integer> len = String::length;\n"
                + "root.add(new Label(\"n=\" + len.apply(\"abc\")));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "predicate", ui(""
                + "import java.util.function.*;\n"
                + "Predicate<String> p = s -> s.length() > 2;\n"
                + "root.add(new Label(\"p=\" + p.test(\"abcd\") + \" \" + p.test(\"a\")));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "consumer", ui(""
                + "import java.util.function.*;\n"
                + "String[] sink = new String[]{\"\"};\n"
                + "Consumer<String> c = s -> sink[0] = s;\n"
                + "c.accept(\"hello\");\n"
                + "root.add(new Label(sink[0]));"), ExpectedOutcome.SUCCESS, null));
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
        cases.add(new Case(cat, "list_iteration", ui(""
                + "List<String> items = new ArrayList<String>();\n"
                + "items.add(\"a\"); items.add(\"b\");\n"
                + "String txt = \"\";\n"
                + "for (String v : items) txt += v;\n"
                + "root.add(new Label(txt));"), ExpectedOutcome.SUCCESS, null));
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
                + "root.add(new Label(new Outer.Inner().label()));"), ExpectedOutcome.SUCCESS, null));
        // Non-static inner classes: Inner's methods resolve outer
        // fields through the enclosing instance.
        cases.add(new Case(cat, "non_static_inner_reads_outer_field", ui(""
                + "class Outer {\n"
                + "    int x = 10;\n"
                + "    class Inner {\n"
                + "        int y = 5;\n"
                + "        int sum() { return x + y; }\n"
                + "    }\n"
                + "    Object make() { return new Inner(); }\n"
                + "}\n"
                + "Outer o = new Outer();\n"
                + "Object i = o.make();\n"
                + "int s = (int) i.sum();\n"
                + "root.add(new Label(\"sum=\" + s));"), ExpectedOutcome.SUCCESS, null));
        // Constructing an inner class with no enclosing scope in scope
        // is an error at the call site.
        cases.add(new Case(cat, "non_static_inner_needs_enclosing", ui(""
                + "class Outer {\n"
                + "    class Inner { int x = 1; }\n"
                + "}\n"
                + "Object i = new Outer.Inner();\n"
                + "root.add(new Label(\"oops\"));"), ExpectedOutcome.EVAL_ERROR, "enclosing"));
        // Concrete class implementing a Java SAM interface must
        // provide the SAM method. Declaration fails otherwise.
        cases.add(new Case(cat, "class_missing_iface_method_rejected", ui(""
                + "import com.codename1.ui.events.ActionListener;\n"
                + "class Silent implements ActionListener { }\n"
                + "root.add(new Label(\"unreachable\"));"),
                ExpectedOutcome.EVAL_ERROR, "does not implement"));
        // Concrete class providing the SAM method is accepted.
        cases.add(new Case(cat, "class_with_iface_method_accepted", ui(""
                + "import com.codename1.ui.events.ActionListener;\n"
                + "import com.codename1.ui.events.ActionEvent;\n"
                + "class Loud implements ActionListener {\n"
                + "    public void actionPerformed(ActionEvent e) { }\n"
                + "}\n"
                + "Loud l = new Loud();\n"
                + "root.add(new Label(\"ok\"));"), ExpectedOutcome.SUCCESS, null));
        // Calling an undefined method produces a helpful "did you
        // mean" diagnostic rather than a bare "not found".
        cases.add(new Case(cat, "missing_method_suggests_correction", ui(""
                + "class Thing { String say() { return \"hi\"; } }\n"
                + "Thing t = new Thing();\n"
                + "t.sayz();\n"
                + "root.add(new Label(\"unreachable\"));"), ExpectedOutcome.EVAL_ERROR, "Did you mean"));
        // Scripted interface with an abstract method must be
        // implemented by a concrete class that names it in `implements`.
        cases.add(new Case(cat, "scripted_iface_abstract_method_required", ui(""
                + "interface Shape { int area(); }\n"
                + "class Box implements Shape { }\n"
                + "root.add(new Label(\"unreachable\"));"),
                ExpectedOutcome.EVAL_ERROR, "does not implement"));
        // Default methods on a scripted interface satisfy the
        // requirement without the implementing class redeclaring them.
        cases.add(new Case(cat, "scripted_iface_default_method_inherited", ui(""
                + "interface Greeter { default String greet() { return \"hi\"; } }\n"
                + "class Hi implements Greeter { }\n"
                + "Hi h = new Hi();\n"
                + "root.add(new Label(h.greet()));"), ExpectedOutcome.SUCCESS, null));
        // outer.new Inner(...) explicit-enclosing construction.
        cases.add(new Case(cat, "outer_dot_new_inner", ui(""
                + "class Outer {\n"
                + "    int x = 3;\n"
                + "    class Inner {\n"
                + "        int y = 4;\n"
                + "        int sum() { return x + y; }\n"
                + "    }\n"
                + "}\n"
                + "Outer a = new Outer();\n"
                + "Object i = a.new Inner();\n"
                + "int s = (int) i.sum();\n"
                + "root.add(new Label(\"sum=\" + s));"), ExpectedOutcome.SUCCESS, null));
        // Static nested class rejects outer.new because it doesn't need
        // an enclosing instance.
        cases.add(new Case(cat, "outer_dot_new_static_rejected", ui(""
                + "class Outer { static class Nested { int v = 1; } }\n"
                + "Outer o = new Outer();\n"
                + "Object n = o.new Nested();\n"
                + "root.add(new Label(\"oops\"));"), ExpectedOutcome.EVAL_ERROR, "static"));
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
        cases.add(new Case(cat, "super_constructor_call", ui(""
                + "class Base { int x; Base(int v) { x = v; } }\n"
                + "class Derived extends Base { int y; Derived(int v) { super(v); y = v * 2; } }\n"
                + "Derived d = new Derived(7);\n"
                + "root.add(new Label(\"x=\" + d.x + \" y=\" + d.y));"), ExpectedOutcome.SUCCESS, null));
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
                + "root.add(new Label(\"ok\"));"), ExpectedOutcome.SUCCESS, null));
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
        cases.add(new Case(cat, "anonymous_impl_method", ui(""
                + "interface Greet { String hello(); }\n"
                + "Object g = new Greet(){public String hello(){return \"hi\";}};\n"
                + "root.add(new Label(g.hello()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "anonymous_impl_default_inherited", ui(""
                + "interface Greet { default String hello() { return \"hi\"; } }\n"
                + "Object g = new Greet(){};\n"
                + "root.add(new Label(g.hello()));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "anonymous_impl_override", ui(""
                + "interface Greet { default String hello() { return \"base\"; } }\n"
                + "Object g = new Greet(){public String hello(){return \"override\";}};\n"
                + "root.add(new Label(g.hello()));"), ExpectedOutcome.SUCCESS, null));
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
        cases.add(new Case(cat, "with_fields", ui(""
                + "enum Prio { LOW(1), HIGH(10); int v; Prio(int v){this.v=v;} }\n"
                + "root.add(new Label(\"low=\" + Prio.LOW.v + \" high=\" + Prio.HIGH.v));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "with_method_per_constant", ui(""
                + "enum Op { ADD { public int apply(int a, int b){return a+b;} }; public int apply(int a, int b){return 0;} }\n"
                + "root.add(new Label(\"v=\" + Op.ADD.apply(2,3)));"), ExpectedOutcome.SUCCESS, null));
    }

    // ------------------------------------------------------------------
    // Category: Records (Java 14+)
    // ------------------------------------------------------------------
    private static void addRecords(List<Case> cases) {
        cases.add(new Case("record", "shadow_imported_cn1_type",
                ui("record Point(int x, int y) {} Point p = new Point(1,2); root.add(new Label(\"x=\" + p.x()));"),
                ExpectedOutcome.SUCCESS, null));
        cases.add(new Case("record", "single_component",
                ui("record Named(String n) {} Named o = new Named(\"hello\"); root.add(new Label(o.n()));"),
                ExpectedOutcome.SUCCESS, null));
        cases.add(new Case("record", "use_in_method",
                ui("record Sum(int a, int b) { int total() { return a + b; } } Sum s = new Sum(2, 3); root.add(new Label(\"t=\" + s.total()));"),
                ExpectedOutcome.SUCCESS, null));
        cases.add(new Case("record", "two_components",
                ui("record Pt(int x, int y) {} Pt p = new Pt(3,4); root.add(new Label(\"x=\" + p.x() + \" y=\" + p.y()));"),
                ExpectedOutcome.SUCCESS, null));
    }

    // ------------------------------------------------------------------
    // Category: Sealed classes (Java 17)
    // ------------------------------------------------------------------
    private static void addSealed(List<Case> cases) {
        String cat = "sealed";
        cases.add(new Case(cat, "sealed_with_permits", ui(""
                + "sealed interface Shape permits Circle, Square {}\n"
                + "final class Circle implements Shape {}\n"
                + "final class Square implements Shape {}\n"
                + "Circle c = new Circle();\n"
                + "root.add(new Label(\"created\"));"), ExpectedOutcome.SUCCESS, null));
        cases.add(new Case(cat, "non_sealed", ui(""
                + "sealed class Animal permits Dog {}\n"
                + "non-sealed class Dog extends Animal {}\n"
                + "Dog d = new Dog();\n"
                + "root.add(new Label(\"d=\" + (d != null)));"), ExpectedOutcome.SUCCESS, null));
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
        cases.add(new Case(cat, "yield_form", ui(""
                + "int x = 2;\n"
                + "String s = switch (x) { case 1: yield \"one\"; case 2: yield \"two\"; default: yield \"?\"; };\n"
                + "root.add(new Label(s));"), ExpectedOutcome.SUCCESS, null));
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

    /** Attempt to lay out and paint the given component into an
     * offscreen image. This simulates the playground's actual render
     * path (the JavaSE simulator uses a framebuffer in CI via
     * xvfb-run). Returns an error string when paint fails, or
     * {@code null} when the component paints cleanly. */
    private static String tryPaintHeadless(Component component) {
        try {
            int w = 320;
            int h = 480;
            if (component instanceof Container) {
                component.setWidth(w);
                component.setHeight(h);
                ((Container) component).revalidate();
            }
            Image offscreen = Image.createImage(w, h);
            component.paintComponent(offscreen.getGraphics(), true);
            return null;
        } catch (Throwable t) {
            return t.getClass().getSimpleName() + ": " + t.getMessage();
        }
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
