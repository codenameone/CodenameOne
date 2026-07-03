package com.bench;

import java.util.HashMap;

/**
 * Allocation-heavy driver used only to force MANY garbage collections (hence many
 * parallel mark cycles) quickly, e.g. under ThreadSanitizer. Mixes long linked lists
 * (the deep-reference case the iterative worklist must handle), HashMap churn and
 * StringBuilder churn so the parallel drain walks varied object graphs. Folds work
 * into a checksum so nothing is dead-code eliminated. Reduced iteration counts so it
 * finishes in minutes even with TSan's ~10x slowdown.
 */
public class GcStress {
    static final class Node { int v; Node next; Node(int v, Node n){ this.v=v; this.next=n; } }

    static long allocChurn(int iters, int keep) {
        long checksum = 0;
        Node head = null;
        int len = 0;
        for (int i = 0; i < iters; i++) {
            head = new Node(i, head);
            len++;
            if (len >= keep) {
                // walk the whole live chain (parallel mark just snapshot this graph)
                Node p = head; int steps = 0;
                while (p != null) { checksum += p.v; p = p.next; steps++; }
                checksum += steps;
                head = null; len = 0; // drop -> garbage for the next GC
            }
        }
        return checksum;
    }

    static long mapChurn(int iters) {
        HashMap<Integer,Integer> map = new HashMap<Integer,Integer>();
        long checksum = 0;
        for (int i = 0; i < iters; i++) {
            Integer key = Integer.valueOf(i & 0x3FFF);
            Integer prev = map.get(key);
            map.put(key, Integer.valueOf(prev == null ? i : prev.intValue() + i));
            if (prev != null) checksum += prev.intValue();
            if (map.size() > 20000) map.clear();
        }
        return checksum + map.size();
    }

    static long strChurn(int iters) {
        long checksum = 0;
        for (int i = 0; i < iters; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append("s-").append(i).append('/').append(i * 31 ^ 0x55AA);
            String s = sb.toString();
            checksum += s.hashCode() + s.length();
        }
        return checksum;
    }

    public static void main(String[] args) {
        long c = 0;
        for (int round = 0; round < 6; round++) {
            c += allocChurn(700000, 400);
            c += mapChurn(500000);
            c += strChurn(150000);
            System.out.println("ROUND " + round + " checksum=" + c);
        }
        System.out.println("DONE " + c);
    }
}
