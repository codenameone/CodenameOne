/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */

/**
 * A program whose collector must keep reclaiming after a thread the VM did not start
 * has called into it.
 *
 * <p>Such a thread registers itself the first time it touches Java, and from then on
 * the collector has to capture its native-stack roots every cycle -- and a cycle that
 * cannot capture a registered thread's roots skips its sweep. Two ordinary kinds of
 * thread defeated that capture: one that EXITED (it stayed registered, and the stop
 * went to a thread that no longer existed) and, on Apple, a libdispatch worker, which
 * pthread_kill cannot signal at all. Either one was enough to stop the whole program
 * reclaiming anything; see GcForeignThreadIntegrationTest.</p>
 *
 * <p>The native half registers both kinds before the churn starts, and the churn is
 * objectAllocation's shape: pure garbage, many cycles.</p>
 */
public class GcForeignThreadApp {
    private static final int ROUNDS = 6;
    private static final int PER_ROUND = 8000000;

    private static native int registerForeignThreads();

    static final class Node {
        final int v;
        final Node next;

        Node(int v, Node next) {
            this.v = v;
            this.next = next;
        }
    }

    public static void main(String[] args) {
        System.out.println("FOREIGN_THREADS=" + registerForeignThreads());
        long checksum = 0;
        for (int round = 0; round < ROUNDS; round++) {
            Node head = null;
            for (int i = 0; i < PER_ROUND; i++) {
                head = new Node(i, head);
                if ((i & 511) == 0) {
                    Node p = head;
                    int steps = 0;
                    while (p != null && steps < 48) {
                        checksum += p.v;
                        p = p.next;
                        steps++;
                    }
                    head = null;
                }
            }
        }
        System.out.println("RESULT=" + checksum);
        System.out.println("GC_FOREIGN_THREAD_DONE");
    }
}
