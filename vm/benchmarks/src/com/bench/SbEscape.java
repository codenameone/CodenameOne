package com.bench;

/**
 * Publishes a stack-allocated StringBuilder into a heap object's field.
 *
 * This is the negative control for the verifier's escaped-stack-object check, and it
 * is NOT correct code. It only escapes when the translator is told to skip the escape
 * validation (-Dcn1.sbSkipEscapeValidation=true); built normally the analysis refuses
 * a builder stored into a field, nothing is stack-allocated here, and the run is
 * clean. That difference is the whole test: the check has to be the thing that
 * notices, and a check that has never been seen firing should be assumed not to.
 *
 * The escape is published from MAIN's frame on purpose. A builder escaping a frame
 * that then RETURNS is the real-world shape, but it does not survive to be reported:
 * later calls overwrite the dead frame, the collector reads a garbage class word out
 * of it and the process dies with SIGBUS before any verify pass runs -- measured,
 * exit 138 with no output at all. Keeping the frame alive leaves the object
 * well-formed and the violated rule unchanged: a heap object's reference field must
 * never point into a thread's C stack.
 */
public class SbEscape {
    static class Holder {
        Object ref;
        int tag;
    }

    private static Holder holder = new Holder();
    private static int sink;

    public static void main(String[] args) {
        StringBuilder escaped = new StringBuilder();
        escaped.append("escaped-builder");
        holder.ref = escaped;
        holder.tag = escaped.length();

        // Enough churn to complete several GC cycles: the verify pass runs after a
        // SWEEP, so a driver that never collects reports clean for the same reason an
        // empty one does. run-gc-verify.sh calls that vacuous and checks for it.
        for (int round = 0; round < 400; round++) {
            for (int rep = 0; rep < 60; rep++) {
                StringBuilder churn = new StringBuilder();
                for (int i = 0; i < 300; i++) {
                    churn.append(i).append(',');
                }
                sink += churn.length();
                byte[] block = new byte[4096];
                block[round & 0xff] = (byte) rep;
                sink += block[round & 0xff];
            }
        }
        System.out.println("SBESCAPE_DONE tag=" + holder.tag + " sink=" + (sink > 0));
    }
}
