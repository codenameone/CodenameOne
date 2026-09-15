package com.bench;

import java.util.ArrayList;
import java.util.List;

/**
 * Iteration-dominated driver for the ArrayListIterator.next() frameless split.
 * Deliberately narrow: the whole-program ratio cannot resolve a change to one
 * method, so this makes next() the overwhelming majority of the work.
 */
public class ForEachBench {
    public static void main(String[] args) {
        List<Integer> l = new ArrayList<Integer>();
        for (int i = 0; i < 20000; i++) {
            l.add(Integer.valueOf(i));
        }
        long best = Long.MAX_VALUE;
        for (int rep = 0; rep < 12; rep++) {
            long t0 = System.nanoTime();
            long sum = 0;
            for (int outer = 0; outer < 400; outer++) {
                for (Integer v : l) {
                    sum += v.intValue();
                }
            }
            long dt = System.nanoTime() - t0;
            if (dt < best) {
                best = dt;
            }
            if (sum != 400L * 19999L * 20000L / 2L) {
                System.out.println("BAD " + sum);
                return;
            }
        }
        System.out.println("bestMs=" + (best / 1000000L));
    }
}
