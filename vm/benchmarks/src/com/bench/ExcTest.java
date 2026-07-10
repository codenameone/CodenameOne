package com.bench;

/**
 * Exercises the setjmp/longjmp exception path with the conditional-volatile change.
 * A try/catch method (keeps volatile) modifies locals, calls a throwing method
 * (no try/catch -> unwound past, non-volatile), and reads the locals in the catch
 * block and after. If volatile handling were wrong, the post-catch local values
 * would differ from Java SE.
 */
public class ExcTest {
    static int thrower(int x) {
        if (x % 7 == 3) {
            throw new RuntimeException("boom");
        }
        return x * 2;
    }

    // has try/catch -> setjmp frame -> locals stay volatile
    static long run() {
        long sum = 0;
        int caught = 0;
        for (int i = 0; i < 1000000; i++) {
            int a = i;          // local live across the call/throw
            int b = i + 1;
            try {
                int r = thrower(i);   // may longjmp back here
                sum += r + a + b;     // a,b must be intact on the normal path
            } catch (RuntimeException e) {
                // a and b must have their pre-call values after longjmp
                sum += a - b;
                caught++;
            }
        }
        return sum * 31 + caught;
    }

    public static void main(String[] args) {
        long r = run();
        System.out.println("result=" + r);
    }
}
