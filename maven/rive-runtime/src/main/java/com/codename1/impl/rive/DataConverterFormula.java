/*
 * MIT License
 *
 * Copyright (c) 2020 Rive
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Java rewrite of rive-runtime for Codename One.
 * Codename One modifications Copyright (c) 2026 Codename One.
 */
package com.codename1.impl.rive;

import java.util.ArrayList;
import java.util.Random;

/// Evaluates a formula over the input: the tokens are put in postfix order
/// once (shunting yard) and evaluated on a float stack, as upstream does.
public class DataConverterFormula extends DataConverterFormulaBase {
    private final ArrayList<FormulaToken> tokens = new ArrayList<FormulaToken>();
    private final ArrayList<FormulaToken> queue = new ArrayList<FormulaToken>();
    private final ArrayList<Integer> argCounts = new ArrayList<Integer>();
    private boolean built;

    /// Upstream's `DataConverterFormula::clone`: the tokens are copied.
    @Override
    protected void copyFrom(Core source) {
        super.copyFrom(source);
        ArrayList<FormulaToken> from = ((DataConverterFormula) source).tokens;
        for (int i = 0; i < from.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated
            Core copy = from.get(i).makeCopy();
            tokens.add(copy instanceof FormulaToken ? (FormulaToken) copy : from.get(i));
        }
    }
    private float[] stack = new float[8];
    // Upstream calls rand(). A fixed seed per converter keeps the sequence
    // pseudo random but makes every run, and so every screenshot capture,
    // produce the same frames.
    private final Random random = new Random(0x5249564CL);

    public void addToken(FormulaToken t) {
        tokens.add(t);
        built = false;
    }

    @Override
    public int outputType() {
        return DataValue.NUMBER;
    }

    @Override
    public void bind(ViewModelInstance root) {
        super.bind(root);
        for (int i = 0; i < tokens.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated
            DataBind b = tokens.get(i).getDataBind();
            if (b != null) {
                b.bind(root);
            }
        }
    }

    @Override
    public boolean refreshBinds() {
        boolean changed = super.refreshBinds();
        for (int i = 0; i < tokens.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed, no iterator allocated
            DataBind b = tokens.get(i).getDataBind();
            if (b != null && b.getSource() != null) {
                int before = b.getSource().getChangeCount();
                b.updateSourceToTarget();
                // lastCount records the count, so it must run for every token
                // even once an earlier one has already reported a change.
                int was = lastCount(i, before);
                changed = changed || before != was;
            }
        }
        return changed;
    }

    private int[] counts = new int[0];

    private int lastCount(int i, int now) {
        if (counts.length != tokens.size()) {
            counts = new int[tokens.size()];
            for (int k = 0; k < counts.length; k++) {
                counts[k] = -1;
            }
        }
        int was = counts[i];
        counts[i] = now;
        return was;
    }

    private static int precedence(FormulaToken t) {
        if (t instanceof FormulaTokenParenthesis) {
            return 1;
        }
        if (t instanceof FormulaTokenOperation) {
            int op = ((FormulaTokenOperation) t).getOperationType();
            return op == 0 || op == 1 ? 2 : op == 2 || op == 3 || op == 4 ? 3 : 0;
        }
        return 0;
    }

    private void build() {
        built = true;
        queue.clear();
        argCounts.clear();
        ArrayList<FormulaToken> ops = new ArrayList<FormulaToken>();
        ArrayList<Integer> args = new ArrayList<Integer>();
        for (int i = 0; i < tokens.size(); i++) {
            FormulaToken t = tokens.get(i);
            if (t instanceof FormulaTokenValue || t instanceof FormulaTokenInput) {
                queue.add(t);
                argCounts.add(Integer.valueOf(0));
            } else if (t instanceof FormulaTokenOperation) {
                while (!ops.isEmpty() && !(top(ops) instanceof FormulaTokenParenthesisOpen)
                        && !(top(ops) instanceof FormulaTokenFunction)
                        && precedence(top(ops)) >= precedence(t)) {
                    queue.add(ops.remove(ops.size() - 1));
                    argCounts.add(Integer.valueOf(2));
                }
                ops.add(t);
            } else if (t instanceof FormulaTokenParenthesisOpen || t instanceof FormulaTokenFunction) {
                ops.add(t);
                boolean empty = i + 1 < tokens.size() && tokens.get(i + 1) instanceof FormulaTokenParenthesisClose;
                args.add(Integer.valueOf(empty ? 0 : 1));
            } else if (t instanceof FormulaTokenParenthesisClose) {
                while (!ops.isEmpty() && !(top(ops) instanceof FormulaTokenParenthesisOpen)
                        && !(top(ops) instanceof FormulaTokenFunction)) {
                    queue.add(ops.remove(ops.size() - 1));
                    argCounts.add(Integer.valueOf(2));
                }
                if (!ops.isEmpty()) {
                    FormulaToken open = ops.remove(ops.size() - 1);
                    int count = args.isEmpty() ? 0 : args.remove(args.size() - 1).intValue();
                    if (open instanceof FormulaTokenFunction) {
                        queue.add(open);
                        argCounts.add(Integer.valueOf(count));
                    }
                }
            } else if (t instanceof FormulaTokenArgumentSeparator) {
                while (!ops.isEmpty() && !(top(ops) instanceof FormulaTokenParenthesisOpen)
                        && !(top(ops) instanceof FormulaTokenFunction)) {
                    queue.add(ops.remove(ops.size() - 1));
                    argCounts.add(Integer.valueOf(2));
                }
                if (!args.isEmpty()) {
                    args.set(args.size() - 1, Integer.valueOf(args.get(args.size() - 1).intValue() + 1));
                }
            }
        }
        while (!ops.isEmpty()) {
            FormulaToken t = ops.remove(ops.size() - 1);
            if (!(t instanceof FormulaTokenParenthesisOpen)) {
                queue.add(t);
                argCounts.add(Integer.valueOf(t instanceof FormulaTokenFunction ? 1 : 2));
            }
        }
        if (stack.length < queue.size() + 1) {
            stack = new float[queue.size() + 1];
        }
    }

    private static FormulaToken top(ArrayList<FormulaToken> l) {
        return l.get(l.size() - 1);
    }

    @Override
    public void convert(DataValue v, DataBind bind) {
        if (v.type != DataValue.NUMBER) {
            v.setNumber(0);
            return;
        }
        if (!built) {
            build();
        }
        float input = v.number;
        int sp = 0;
        for (int i = 0; i < queue.size(); i++) {
            FormulaToken t = queue.get(i);
            if (t instanceof FormulaTokenInput) {
                stack[sp++] = input;
            } else if (t instanceof FormulaTokenValue) {
                stack[sp++] = ((FormulaTokenValue) t).getOperationValue();
            } else if (t instanceof FormulaTokenOperation) {
                if (sp < 2) {
                    v.setNumber(0);
                    return;
                }
                float right = stack[--sp];
                float left = stack[--sp];
                int op = ((FormulaTokenOperation) t).getOperationType();
                stack[sp++] = op <= 4 ? DataConverterOperation.operate(op, left, right) : 0;
            } else if (t instanceof FormulaTokenFunction) {
                int n = argCounts.get(i).intValue();
                if (n > sp) {
                    n = sp;
                }
                sp -= n;
                stack[sp] = function(((FormulaTokenFunction) t).getFunctionType(), sp, n);
                sp++;
            }
        }
        v.setNumber(sp == 1 ? stack[0] : 0);
    }

    private float function(int type, int base, int n) {
        float a = n > 0 ? stack[base + n - 1] : 0;
        switch (type) {
            case 0:
            case 1: {
                if (n == 0) {
                    return 0;
                }
                float r = stack[base];
                for (int i = 1; i < n; i++) {
                    float x = stack[base + i];
                    r = type == 0 ? Math.min(r, x) : Math.max(r, x);
                }
                return r;
            }
            case 2:
                return (float) DataConverterOperation.roundHalfAway(a);
            case 3:
                return (float) Math.ceil(a);
            case 4:
                return (float) Math.floor(a);
            case 5:
                return (float) Math.sqrt(a);
            case 6:
                return n >= 2 ? (float) com.codename1.util.MathUtil.pow(stack[base], stack[base + 1]) : 0;
            case 15:
                return n >= 2 ? (float) com.codename1.util.MathUtil.atan2(stack[base], stack[base + 1]) : 0;
            case 16:
                return random.nextFloat();
            default:
                return type >= 7 && type <= 14 ? DataConverterOperation.operate(type, a, 0) : 0;
        }
    }
}
