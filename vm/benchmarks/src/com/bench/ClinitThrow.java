package com.bench;

/**
 * Reproducer for the static-initializer monitor leak: a &lt;clinit&gt; that
 * throws used to leave the class monitor locked (plain monitorEnter with no
 * unwind registration), so any other thread touching the class afterwards
 * deadlocked in monitorEnter. With the fix (monitorEnterBlock/monitorExitBlock,
 * the synchronized-method pattern) the throw releases the monitor and the
 * second thread proceeds.
 *
 * NOT part of the byte-identical gauntlet: initialization-failure semantics
 * intentionally differ from the JVM spec (ParparVM marks the class initialized
 * before running clinit, so later users see a partially-initialized class
 * instead of NoClassDefFoundError). This test only asserts liveness.
 */
public class ClinitThrow {
    static class Bomb {
        static int value;
        static {
            value = 41;
            if (System.getProperty("java.version") != null || value == 41) {
                throw new RuntimeException("clinit bomb");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        try {
            int v = Bomb.value;
            System.out.println("unexpected: clinit did not throw, value=" + v);
        } catch (Throwable t) {
            System.out.println("first access threw: " + t.getMessage());
        }

        final int[] result = new int[1];
        Thread second = new Thread(new Runnable() {
            public void run() {
                // Pre-fix: deadlocks here (class monitor leaked locked).
                result[0] = Bomb.value;
            }
        });
        second.start();
        second.join(15000);
        if (second.isAlive()) {
            System.out.println("FAIL: second thread deadlocked in class init");
        } else {
            System.out.println("second thread completed, value=" + result[0]);
        }
    }
}
