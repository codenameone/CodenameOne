/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.functions;

import com.codenameone.examples.bazaar.domain.Discount;
import com.codenameone.examples.bazaar.pricing.DiscountEngine;
import com.codenameone.examples.bazaar.pricing.PriceLine;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The function language on its own: what the compiler and the verifier
/// refuse, what a run does, its fuel and output limits, and the cost of an
/// interpreted discount against the built-in engine.
class FunctionLanguageTest {
    private static final String BULK = "# 10% off lines of three or more\n"
            + "let off = 0;\n"
            + "for line in lines {\n"
            + "  if line.quantity >= 3 && starts_with(line.sku, \"MUG\") {\n"
            + "    emit line_discount(line.index, line.total / 10, \"Bulk\");\n"
            + "    off = off + line.total / 10;\n"
            + "  } else if line.title == \"Free\" { emit line_discount(line.index, line.total, \"Gift\"); }\n"
            + "}\n"
            + "if off > 500 && cart.currency != \"JPY\" { emit discount(100, \"Big bulk bonus\"); }\n";

    private static FunctionInput lines(Target t, int count, int quantity) {
        FunctionInput in = t.listCount() > 1 ? t.input(count, 0) : t.input(count);
        for (int iter = 0; iter < count; iter++) {
            in.cell(Target.LINES, iter, Target.LINE_QUANTITY, quantity)
                    .cell(Target.LINES, iter, Target.LINE_PRICE, 1000)
                    .cell(Target.LINES, iter, Target.LINE_TOTAL, 1000L * quantity)
                    .cell(Target.LINES, iter, Target.LINE_SKU, "MUG-" + iter)
                    .cell(Target.LINES, iter, Target.LINE_TITLE, "Mug");
        }
        return in.scalar(Target.CURRENCY, "USD");
    }

    private static FunctionOutput run(Program p, FunctionInput in) throws FunctionException {
        FunctionOutput out = new FunctionOutput();
        Interpreter.run(p, in, Interpreter.DEFAULT_FUEL, out);
        return out;
    }

    private static String refused(String source, Target t) {
        return assertThrows(FunctionException.class, () -> FunctionCompiler.compile(source, t))
                .getMessage();
    }

    @Test
    void aDiscountScriptRunsAndSurvivesStorage() throws Exception {
        Program p = FunctionCompiler.compile(BULK, Target.DISCOUNT);
        FunctionInput in = lines(Target.DISCOUNT, 1, 3);
        FunctionOutput out = run(p, in);
        assertEquals(1, out.size());
        assertEquals(Target.EMIT_LINE_DISCOUNT, out.op(0));
        assertEquals(0, out.number(0, 0));
        assertEquals(300, out.number(0, 1));
        assertEquals("Bulk", out.text(0, 2));
        // 600 off in all: the bonus follows.
        FunctionOutput more = run(p, lines(Target.DISCOUNT, 2, 4));
        assertEquals(3, more.size());
        assertEquals(Target.EMIT_DISCOUNT, more.op(2));
        assertEquals("Big bulk bonus", more.text(2, 1));
        Program back = Program.decode(p.encode());
        assertEquals(3, run(back, lines(Target.DISCOUNT, 2, 4)).size());
    }

    @Test
    void theCompilerRefusesBadProgramsNamingTheLine() {
        assertTrue(refused("for line in lines {\n emit line_discount(line.index, line.colour, \"x\");\n}",
                Target.DISCOUNT).startsWith("line 2:"));
        assertTrue(refused("let a = cart.weather;", Target.DISCOUNT).contains("no field"));
        assertTrue(refused("let a = 1; a = \"x\";", Target.DISCOUNT).contains("holds a number"));
        assertTrue(refused("emit hide(\"std\");", Target.DISCOUNT).contains("cannot emit"));
        assertTrue(refused("emit discount(\"ten\", \"x\");", Target.DISCOUNT).contains("should be"));
        assertTrue(refused("b = 2;", Target.DISCOUNT).contains("not declared"));
        assertTrue(refused("let a = (1 + 2;", Target.DISCOUNT).contains("Expected ')'"));
        assertTrue(refused("let s = \"open;", Target.DISCOUNT).contains("not closed"));
        assertTrue(refused("let n = lines;", Target.DISCOUNT).contains("for x in lines"));
        assertTrue(refused("for r in rates { }", Target.DISCOUNT).contains("no list 'rates'"));
        assertTrue(refused("for l in lines { l = 1; }", Target.DISCOUNT).contains("cannot be assigned"));
        assertTrue(refused("if \"a\" < \"b\" { }", Target.DISCOUNT).contains("== and !="));
        assertTrue(refused("if 1 { }", Target.DISCOUNT).contains("true or false"));
        assertTrue(refused("let x = 1; let x = 2;", Target.DISCOUNT).contains("already declared"));
        assertTrue(refused("let x = 99999999999999999999;", Target.DISCOUNT).contains("larger"));
        assertTrue(refused("let x = 1 @ 2;", Target.DISCOUNT).contains("Unexpected"));
        assertTrue(refused("", Target.DISCOUNT).contains("no source"));
        assertTrue(refused("emit error(\"x\");", null).contains("target"));
        // A let is gone once its block ends.
        assertTrue(refused("if true { let y = 1; }\nemit discount(y, \"x\");", Target.DISCOUNT)
                .startsWith("line 2:"));
    }

    private static Program tampered(int[] code, long[] longs, String[] strings, String locals) {
        return new Program(Target.DISCOUNT, code, longs, strings, locals);
    }

    private static String unverified(int[] code, String locals) {
        Program p = tampered(code, new long[] {1}, new String[] {"x"}, locals);
        return assertThrows(FunctionException.class, () -> Verifier.verify(p)).getMessage();
    }

    @Test
    void theVerifierRefusesProgramsTheCompilerWouldNeverMake() {
        assertTrue(unverified(new int[] {Program.JMP, 7, Program.HALT}, "")
                .contains("outside the code"));
        assertTrue(unverified(new int[] {Program.JMP, 1, Program.HALT}, "")
                .contains("outside the code"));
        assertTrue(unverified(new int[] {Program.ADD, Program.HALT}, "").contains("empty"));
        int[] deep = new int[2 * 33 + 1];
        for (int iter = 0; iter < 33; iter++) {
            deep[2 * iter] = Program.PUSH_L;
            deep[2 * iter + 1] = 0;
        }
        deep[deep.length - 1] = Program.HALT;
        assertTrue(unverified(deep, "").contains("too deep"));
        assertTrue(unverified(new int[] {Program.FIELD, 0, 42, 0, Program.HALT}, "L")
                .contains("No such field"));
        assertTrue(unverified(new int[] {Program.FIELD, 3, 0, 0, Program.HALT}, "L")
                .contains("No such field"));
        assertTrue(unverified(new int[] {Program.SCALAR, 99, Program.HALT}, "")
                .contains("No such scalar"));
        assertTrue(unverified(new int[] {Program.PUSH_L, 0, Program.PUSH_S, 0, Program.EMIT, 9,
            Program.HALT}, "").contains("No such emit"));
        assertTrue(unverified(new int[] {Program.PUSH_S, 0, Program.PUSH_L, 0, Program.EMIT, 0,
            Program.HALT}, "").contains("wrong type"));
        assertTrue(unverified(new int[] {Program.PUSH_L, 5, Program.HALT}, "")
                .contains("No such constant"));
        assertTrue(unverified(new int[] {Program.LOAD, 3, Program.HALT}, "L")
                .contains("No such local"));
        assertTrue(unverified(new int[] {Program.PUSH_L, 0}, "").contains("off its end"));
        assertTrue(unverified(new int[] {77, Program.HALT}, "").contains("Unknown opcode"));
        assertTrue(unverified(new int[] {Program.PUSH_L}, "").contains("cut short"));
        // Two paths into one instruction, one with a value pushed.
        assertTrue(unverified(new int[] {Program.PUSH_L, 0, Program.PUSH_L, 0, Program.LT,
            Program.JZ, 9, Program.PUSH_L, 0, Program.HALT}, "").contains("different stacks"));
        assertThrows(FunctionException.class, () -> Program.decode(
                "{\"target\":\"discount\",\"code\":[28,99],\"longs\":[],\"strings\":[],"
                + "\"locals\":\"\"}"));
        assertThrows(FunctionException.class, () -> Program.decode("{\"target\":\"nope\"}"));
    }

    @Test
    void fuelOutputAndArithmeticFailTheRun() throws Exception {
        Program nested = FunctionCompiler.compile("let n = 0;\nfor a in lines { for b in lines {"
                + " for c in lines { n = n + 1; } } }", Target.DISCOUNT);
        FunctionInput big = lines(Target.DISCOUNT, 60, 1);
        String fuel = assertThrows(FunctionException.class, () -> run(nested, big)).getMessage();
        assertTrue(fuel.contains("fuel"), fuel);
        // The same program within budget on a small cart.
        run(nested, lines(Target.DISCOUNT, 3, 1));

        Program chatty = FunctionCompiler.compile("for l in lines { emit discount(1, \"x\"); }",
                Target.DISCOUNT);
        assertTrue(assertThrows(FunctionException.class,
                () -> run(chatty, lines(Target.DISCOUNT, 65, 1))).getMessage().contains("more than"));
        assertEquals(64, run(chatty, lines(Target.DISCOUNT, 64, 1)).size());

        Program divide = FunctionCompiler.compile("emit discount(10 / cart.quantity, \"x\");",
                Target.DISCOUNT);
        assertTrue(assertThrows(FunctionException.class,
                () -> run(divide, lines(Target.DISCOUNT, 0, 0))).getMessage().contains("zero"));
        Program grow = FunctionCompiler.compile("let x = 1000000000000000;\nx = x * x;",
                Target.DISCOUNT);
        assertTrue(assertThrows(FunctionException.class,
                () -> run(grow, lines(Target.DISCOUNT, 0, 0))).getMessage().contains("range"));
        Program min = FunctionCompiler.compile("let x = -1000000000000000 * 9223;\n"
                + "x = x - 1000000000000000;", Target.DISCOUNT);
        assertThrows(FunctionException.class, () -> run(min, lines(Target.DISCOUNT, 0, 0)));
    }

    @Test
    void otherTargetsReadTheirOwnLists() throws Exception {
        Program ship = FunctionCompiler.compile("for r in rates {\n"
                + "  if r.kind == \"PICKUP\" { emit hide(r.id); }\n"
                + "  else if r.price > 1000 { emit reprice(r.id, max(r.price - 200, 0)); }\n"
                + "  else { emit rename(r.id, \"Saver\"); emit move(r.id, 0); }\n"
                + "}", Target.SHIPPING);
        FunctionInput in = Target.SHIPPING.input(0, 3);
        String[] kinds = {"PICKUP", "FLAT", "FLAT"};
        long[] prices = {0, 1500, 500};
        for (int r = 0; r < 3; r++) {
            in.cell(1, r, Target.RATE_ID, "r" + r).cell(1, r, Target.RATE_KIND, kinds[r])
                    .cell(1, r, Target.RATE_PRICE, prices[r]);
        }
        FunctionOutput out = run(ship, in);
        assertEquals(4, out.size());
        assertEquals(Target.EMIT_HIDE, out.op(0));
        assertEquals(1300, out.number(1, 1));
        assertEquals(Target.EMIT_SHIPPING_MOVE, out.op(3));

        assertTrue(refused("for m in methods { emit rename(m.type, \"x\"); }", Target.PAYMENT)
                .contains("cannot emit"));
        Program pay = FunctionCompiler.compile("for m in methods { if m.type == \"card\" && "
                + "cart.subtotal > 100000 { emit hide(m.type); } }", Target.PAYMENT);
        FunctionInput p = Target.PAYMENT.input(0, 2).scalar(Target.SUBTOTAL, 200000);
        p.cell(1, 0, Target.METHOD_TYPE, "card").cell(1, 1, Target.METHOD_TYPE, "ideal");
        assertEquals("card", run(pay, p).text(0, 0));

        Program check = FunctionCompiler.compile("if cart.quantity > 5 { emit error(\"Five at most\"); "
                + "return; }\nemit error(\"never\");", Target.VALIDATION);
        FunctionInput v = lines(Target.VALIDATION, 1, 1).scalar(Target.QUANTITY, 6);
        FunctionOutput errors = run(check, v);
        assertEquals(1, errors.size());
        assertEquals("Five at most", errors.text(0, 0));
    }

    @Test
    void aRemoteReplyIsHeldToTheTargetLikeAScript() throws Exception {
        FunctionOutput ok = CheckoutFunctions.parseReply(Target.SHIPPING,
                "{\"ops\":[{\"op\":\"reprice\",\"args\":[\"r1\",250]}]}");
        assertEquals(250, ok.number(0, 1));
        String[] bad = {"[]", "{\"ops\":[{\"op\":\"discount\",\"args\":[1,\"x\"]}]}",
            "{\"ops\":[{\"op\":\"hide\",\"args\":[1]}]}",
            "{\"ops\":[{\"op\":\"reprice\",\"args\":[\"r1\",2.5]}]}",
            "{\"ops\":[{\"op\":\"hide\"}]}", "not json"};
        for (int iter = 0; iter < bad.length; iter++) {
            String reply = bad[iter];
            assertThrows(FunctionException.class,
                    () -> CheckoutFunctions.parseReply(Target.SHIPPING, reply), reply);
        }
    }

    // ------------------------------------------------------------- benchmark

    private static List<PriceLine> priceLines(int count) {
        List<PriceLine> out = new ArrayList<PriceLine>();
        for (int iter = 0; iter < count; iter++) {
            out.add(new PriceLine("k" + iter, "p" + iter, "v" + iter, 1000 + iter, 1 + iter % 4,
                    300, true));
        }
        return out;
    }

    /// The interpreted side does everything a quote does with a function:
    /// builds the record, runs the program and takes the emitted cents off
    /// the lines.
    private static long interpreted(Program p, List<PriceLine> lines) throws FunctionException {
        for (int iter = 0; iter < lines.size(); iter++) {
            lines.get(iter).discountCents = 0;
        }
        FunctionInput in = Target.DISCOUNT.input(lines.size());
        for (int iter = 0; iter < lines.size(); iter++) {
            PriceLine pl = lines.get(iter);
            in.cell(Target.LINES, iter, Target.LINE_QUANTITY, pl.quantity)
                    .cell(Target.LINES, iter, Target.LINE_TOTAL, pl.netCents())
                    .cell(Target.LINES, iter, Target.LINE_SKU, pl.variantId);
        }
        FunctionOutput out = new FunctionOutput();
        Interpreter.run(p, in, Interpreter.DEFAULT_FUEL, out);
        long total = 0;
        for (int iter = 0; iter < out.size(); iter++) {
            PriceLine pl = lines.get((int) out.number(iter, 0));
            long take = Math.min(out.number(iter, 1), pl.netCents());
            pl.discountCents += take;
            total += take;
        }
        return total;
    }

    private static long builtIn(List<PriceLine> lines, Discount d) {
        return DiscountEngine.apply(lines, Arrays.asList(d), null, null, Instant.EPOCH,
                new HashSet<String>(), 0).totalCents;
    }

    /// A ratio within one interleaved series, never an absolute time: each
    /// round times both arms back to back, and the median of the per-round
    /// ratios is held to a generous bound.
    @Test
    void anInterpretedDiscountCostsAFewTimesTheBuiltInOne() throws Exception {
        Program p = FunctionCompiler.compile("for line in lines {\n"
                + "  emit line_discount(line.index, line.total / 10, \"Ten\");\n}",
                Target.DISCOUNT);
        Discount d = new Discount();
        d.setId("ten");
        d.setActive(true);
        d.setAutomatic(true);
        d.setKind(DiscountEngine.PERCENT);
        d.setAmount(1000);
        d.setAppliesTo(DiscountEngine.ALL);
        List<PriceLine> lines = priceLines(20);
        // Both arms agree before they are timed.
        long expect = builtIn(lines, d);
        assertTrue(expect > 0);
        long got = interpreted(p, lines);
        assertTrue(Math.abs(got - expect) <= lines.size(), got + " vs " + expect);
        int rounds = 41;
        int reps = 300;
        double[] ratios = new double[rounds];
        long sink = 0;
        for (int warm = 0; warm < 2000; warm++) {
            sink += builtIn(lines, d) + interpreted(p, lines);
        }
        for (int r = 0; r < rounds; r++) {
            long a = System.nanoTime();
            for (int i = 0; i < reps; i++) {
                sink += builtIn(lines, d);
            }
            long b = System.nanoTime();
            for (int i = 0; i < reps; i++) {
                sink += interpreted(p, lines);
            }
            long c = System.nanoTime();
            ratios[r] = (double) Math.max(1, c - b) / Math.max(1, b - a);
        }
        Arrays.sort(ratios);
        double median = ratios[rounds / 2];
        System.out.println("[functions] interpreted/built-in discount ratio, median of "
                + rounds + ": " + median + " (" + sink % 7 + ")");
        assertTrue(median < 25, "Interpreted discount is " + median + "x the built-in one");
    }
}
