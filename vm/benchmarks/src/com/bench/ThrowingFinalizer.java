package com.bench;

import java.util.ArrayList;
import java.util.HashMap;

/** Finalizer exceptions must not skip native ownership cleanup or kill the collector. */
public class ThrowingFinalizer {
    private static volatile int finalized;
    private static volatile int unrequestedSuperCalls;
    private static class ListBase extends ArrayList<Object> {
        protected void finalize() { unrequestedSuperCalls++; }
    }
    private static final class ListOwner extends ListBase {
        protected void finalize() { finalized++; throw new IllegalStateException("list finalizer"); }
    }
    private static final class MapOwner extends HashMap<Object, Object> {
        protected void finalize() { finalized++; throw new IllegalStateException("map finalizer"); }
    }
    private static void discard() {
        for (int i = 0; i < 32; i++) {
            ListOwner list = new ListOwner();
            MapOwner map = new MapOwner();
            for (int j = 0; j < 64; j++) { list.add(new Object()); map.put(new Object(), new Object()); }
        }
    }
    public static void main(String[] args) throws Exception {
        // Conservative stack scanning may retain the last few dead references.
        discard();
        for (int round = 0; round < 200 && finalized < 60; round++) {
            // Retire the active allocation pages too; sweeping only closed pages
            // cannot finalize a tiny batch while its allocation page stays active.
            discard();
            System.gc();
            Thread.sleep(50);
        }
        if (finalized < 60) { System.out.println("finalized=" + finalized); throw new AssertionError("Finalizers stopped"); }
        int firstGeneration = finalized;
        // A second generation proves the collector survived the first exceptions.
        discard();
        for (int round = 0; round < 200 && finalized < firstGeneration + 60; round++) {
            // Retire the active allocation pages too; sweeping only closed pages
            // cannot finalize a tiny batch while its allocation page stays active.
            discard();
            System.gc();
            Thread.sleep(50);
        }
        if (finalized < firstGeneration + 60) { System.out.println("finalized=" + finalized); throw new AssertionError("Collector stopped"); }
        if (unrequestedSuperCalls != 0) throw new AssertionError("Implicit super.finalize call");
        System.out.println("THROWING_FINALIZERS_OK");
    }
}
