package com.bench;

import java.util.HashMap;

/**
 * Multi-threaded allocate-during-GC stress. Several mutator threads allocate
 * heavily (linked-list churn + HashMap churn + StringBuilder churn) in parallel
 * while the concurrent GC fires repeatedly, exercising hard points #1 (objects
 * allocated during a GC must survive) and #2 (concurrent sweep vs mutator alloc
 * on BiBOP pages). Each thread folds its work into an independent checksum over
 * a fixed deterministic range; main sums them (order-independent) so the total
 * is identical on HotSpot and ParparVM regardless of scheduling -- any lost live
 * object would corrupt the sum.
 */
public class MtStress {
    static final int THREADS = 4;
    static final long[] results = new long[THREADS];

    static final class Node { int v; Node next; Node(int v, Node n){ this.v=v; this.next=n; } }

    static long work(int seed) {
        long checksum = 0;
        // linked-list churn: build chains of up to 300, walk them, drop them
        Node head = null; int len = 0;
        for (int i = 0; i < 600000; i++) {
            head = new Node(i ^ seed, head);
            len++;
            if (len >= 300) {
                Node p = head; int steps = 0;
                while (p != null) { checksum += p.v; p = p.next; steps++; }
                checksum += steps;
                head = null; len = 0;
            }
        }
        // HashMap churn
        HashMap<Integer,Integer> map = new HashMap<Integer,Integer>();
        for (int i = 0; i < 300000; i++) {
            Integer key = Integer.valueOf((i ^ seed) & 0x3FFF);
            Integer prev = map.get(key);
            map.put(key, Integer.valueOf(prev == null ? i : prev.intValue() + i));
            if (prev != null) checksum += prev.intValue();
            if (map.size() > 12000) map.clear();
        }
        checksum += map.size();
        // StringBuilder churn
        for (int i = 0; i < 120000; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append("t").append(seed).append('-').append(i).append('/').append(i * 31 ^ 0x55AA);
            String s = sb.toString();
            checksum += s.hashCode() + s.length();
        }
        return checksum;
    }

    public static void main(String[] args) throws Exception {
        Thread[] ts = new Thread[THREADS];
        for (int t = 0; t < THREADS; t++) {
            final int idx = t;
            ts[t] = new Thread(new Runnable() {
                public void run() { results[idx] = work(idx * 0x9E3779B1); }
            });
        }
        for (int t = 0; t < THREADS; t++) ts[t].start();
        for (int t = 0; t < THREADS; t++) ts[t].join();
        long total = 0;
        for (int t = 0; t < THREADS; t++) total += results[t];
        System.out.println("DONE " + total);
    }
}
