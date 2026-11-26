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

/// The operations a function emitted, in order: an emit index into its
/// target and up to three arguments, longs and strings in parallel arrays.
/// Sized once, at the cap; emitting past the cap is an error.
public final class FunctionOutput {
    public static final int MAX_ARGS = 3;
    /// The most operations one run may emit.
    public static final int DEFAULT_CAP = 64;

    private final int cap;
    private int count;
    private final int[] ops;
    private final long[] numbers;
    private final String[] texts;

    public FunctionOutput() {
        this(DEFAULT_CAP);
    }

    public FunctionOutput(int cap) {
        this.cap = cap;
        ops = new int[cap];
        numbers = new long[cap * MAX_ARGS];
        texts = new String[cap * MAX_ARGS];
    }

    /// Starts operation `op`; answers its slot, whose arguments are then set.
    int add(int op) throws FunctionException {
        if (count >= cap) {
            throw new FunctionException("The function emitted more than " + cap + " operations");
        }
        ops[count] = op;
        return count++;
    }

    void number(int slot, int arg, long value) {
        numbers[slot * MAX_ARGS + arg] = value;
    }

    void text(int slot, int arg, String value) {
        texts[slot * MAX_ARGS + arg] = value;
    }

    public int size() {
        return count;
    }

    public int op(int index) {
        return ops[index];
    }

    public long number(int index, int arg) {
        return numbers[index * MAX_ARGS + arg];
    }

    public String text(int index, int arg) {
        return texts[index * MAX_ARGS + arg];
    }

    /// Forgets every operation, keeping the arrays.
    public void clear() {
        count = 0;
    }
}
