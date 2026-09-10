# Heap experiments

Small programs that answer one question each about the collector, read through the
census (`-DCN1_ALLOC_CENSUS` + `CN1_HEAP_REPORT=1`, see ../README.md).

## PinProbe

**Question: does the conservative stack scan pin objects that are provably dead?**

Three arms, three distinct classes so one run compares them in one census:
`PinShallow` allocated and dropped in a shallow frame, `PinDeep` allocated at the
bottom of a 400-deep recursion, `PinScrub` the same but with the stack overwritten
before collecting. Nothing holds a reference to any of them, so a precise collector
reclaims all three and any survivor is a stale stack word mistaken for a pointer.

Build and run:

```bash
javac -bootclasspath <javaapi-classes> -d /tmp/exp/classes src/com/exp/PinProbe.java
java -cp <translator>:<asm> com.codename1.tools.translator.ByteCodeTranslator \
     clean "<javaapi-classes>;/tmp/exp/classes" /tmp/exp/out PinProbe com.exp PinProbe 1.0 clean none
clang -O3 -flto=thin -w -fwrapv -fno-strict-aliasing -fno-builtin-fmod -fno-builtin-fmodf \
      -DCN1_ALLOC_CENSUS -I<src> <src>/*.c <src>/*.S -lm -lpthread -o /tmp/exp/pinprobe
CN1_HEAP_REPORT=1 /tmp/exp/pinprobe 2>&1 | grep -E 'PROBE|Pin(Shallow|Deep|Scrub)'
```

**Answer: no.** All three arms behave identically, and the marks are precise -- a
batch reads 100% live on the cycle after it is allocated and 0% on the next, with
no difference between the shallow, deep and scrubbed arms. What the probe found
instead is the reclamation LATENCY: a dead object needs **three cycles** to have
its slot returned.

```
cycle 1: PinShallow 200,000 occupied, 100% kept   <- grace: fresh objects are stamped live
cycle 2: PinShallow 200,000 occupied,   0% kept   <- known dead, still occupying
cycle 3: PinShallow 196,320 occupied              <- reclaimed
```

That is the sweep's own rule: `m == -1` (fresh) gets one cycle of grace, `m == V-1`
is kept for another, and only `m < V-1` is reclaimed. It is why a short program
retains nearly everything it allocates -- the ParparVM translator completes three
or four cycles in 1.4s, so most of what it allocates is never eligible.

This probe is also the reason the census reports its four buckets **pre-sweep**:
read post-sweep, the grace stamp makes "traced live" and "kept because it is fresh"
indistinguishable, and the first version of the census reported the second as the
first.
