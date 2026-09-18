package com.bench;

/**
 * Lambda invocation shapes, and which of them the translator can prove.
 *
 * The interesting axis is not what these print -- every arm must agree with the JDK,
 * which is what the gauntlet checks -- but where the closure's class is still known
 * when the call is emitted:
 *
 *   local()        the lambda is built and invoked in one method. Its class is decided
 *                  at the invokedynamic, so the interface call is emitted direct.
 *   throughField() the lambda is stored and invoked later. Nothing local proves the
 *                  receiver, and it dispatches through the interface thunk's switch.
 *   throughCall()  the lambda is passed to a method that invokes it. The proof would
 *                  have to cross a call boundary, which the local analysis does not do.
 *
 * Kept as a gauntlet test so the shapes stay exercised and the answers stay pinned; a
 * regression in either mechanism shows up as a divergence, not as a silent slowdown.
 */
public class LambdaDevirtT {
    interface IntOp { int apply(int value); }
    interface Sink { void accept(int value); }

    private static IntOp stored;
    private static int total;

    private static int runIt(IntOp op, int value) {
        return op.apply(value);
    }

    private static int local(int seed) {
        IntOp doubler = v -> v * 2;
        int sum = 0;
        for (int i = 0; i < 1000; i++) {
            sum += doubler.apply(seed + i);
        }
        return sum;
    }

    private static int throughField(int seed) {
        stored = v -> v * 3 + 1;
        int sum = 0;
        for (int i = 0; i < 1000; i++) {
            sum += stored.apply(seed + i);
        }
        return sum;
    }

    private static int throughCall(int seed) {
        int sum = 0;
        for (int i = 0; i < 1000; i++) {
            sum += runIt(v -> v ^ 0x5a5a, seed + i);
        }
        return sum;
    }

    private static int capturing(int seed) {
        int captured = seed * 7;
        IntOp op = v -> v + captured;
        int sum = 0;
        for (int i = 0; i < 1000; i++) {
            sum += op.apply(i);
        }
        return sum;
    }

    private static int sideEffects(int seed) {
        total = 0;
        Sink sink = v -> total += v;
        for (int i = 0; i < 1000; i++) {
            sink.accept(seed + i);
        }
        return total;
    }

    public static void main(String[] args) {
        int acc = 0;
        for (int round = 1; round <= 5; round++) {
            acc += local(round);
            acc += throughField(round);
            acc += throughCall(round);
            acc += capturing(round);
            acc += sideEffects(round);
            System.out.println("R" + round + " " + acc);
        }
        System.out.println("LOCAL " + local(7));
        System.out.println("FIELD " + throughField(7));
        System.out.println("CALL " + throughCall(7));
        System.out.println("CAPTURE " + capturing(7));
        System.out.println("SINK " + sideEffects(7));
        System.out.println("DONE");
    }
}
