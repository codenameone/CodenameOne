package com.bench;

/** Does `1L << n` keep long semantics for n >= 31 on the clean target? */
public class LongShift {
    public static void main(String[] args) {
        for(int b = 29 ; b <= 34 ; b++) {
            long viaLiteral = 1L << b;
            long one = 1L;
            long viaVariable = one << b;
            System.out.println("SHIFT b=" + b
                    + " literal=" + viaLiteral
                    + " variable=" + viaVariable
                    + " expected=" + expected(b));
        }
    }
    /** Built by doubling, so it cannot share a shift bug with what it checks. */
    static long expected(int b) {
        long v = 1;
        for(int i = 0 ; i < b ; i++) {
            v = v + v;
        }
        return v;
    }
}
