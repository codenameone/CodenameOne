package com.bench;

/** StackOverflowError recovery torture: an SOE must be CATCHABLE and the
 *  program must keep working afterwards (the trace-building recursion this
 *  guards against turned every deep recursion into a hard SIGSEGV). */
public class SoeTest {
    static int depth;

    static int recurse(int n) {
        depth = n;
        // enough locals to consume stack per frame
        long a = n * 31L, b = a ^ n, c = b + 7;
        int r = recurse(n + 1);
        return (int) (r + a - b + c) & 0xFFFF;
    }

    public static void main(String[] args) {
        long ck = 0;
        for (int round = 0; round < 3; round++) {
            depth = 0;
            try {
                recurse(0);
                ck += 999999; // must not get here
            } catch (StackOverflowError e) {
                ck += 17;
                // the error must be usable: message/trace access must not crash
                String s = String.valueOf(e);
                ck += s.length() > 0 ? 1 : 1000;
            }
            // the VM must still work after recovery
            StringBuilder sb = new StringBuilder();
            sb.append("post-soe-").append(round);
            ck = ck * 31 + sb.toString().hashCode();
        }
        System.out.println("depth reached > 100: " + (depth > 100));
        System.out.println("CK " + ck);
        System.out.println("DONE");
    }
}
