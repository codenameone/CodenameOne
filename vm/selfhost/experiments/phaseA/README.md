# Phase A: type-homogeneous BiBOP pages -- ATTEMPTED, REVERTED, NOT CORRECT

`type-homogeneous-pages.patch` is the working tree as of the second gate failure.
It compiles and passes an 8/8 smoke run. It FAILS `run-gc-verify.sh`. Do not
apply it expecting a working tree; apply it to continue the work.

## What it is for

The slot histogram (see REGISTRY Rounds 34-36) prices the object header at 16
bytes -- `clazz*` (8) + `gcMark` (4) + `heapPosition` (4) -- and shows that
removing the class pointer is worth 8.9% of allocated bytes on the selfhost
corpus, while being the ONLY header field whose removal is free: per-PAGE
metadata amortises over ~2048 slots, per-SLOT metadata does not. Phase A builds
the homogeneity (one class per page, `page->pageClazz`) with the header
UNCHANGED, so it must be exactly behaviour-neutral. Phase B would then delete
the field and make `CN1_CLASS_OF` read `pageOf(o)->pageClazz`.

## Two real bugs found, one fixed, one not

### 1. FIXED -- a scalar class does not imply one size class

`CN1_FAST_NEW` always asks for `sizeof(struct obj__X)`, so it looked safe to key
a page by class alone. `cn1AllocFused` does not: it asks for the String header
PLUS the inlined characters, so `class__java_lang_String_i8` allocates at many
sizes. Keyed by class, a 40-byte and a 200-byte String shared one page whose
slots were sized by whichever came first, and the longer ones ran off the end of
their slot.

Symptom: corrupted string payloads in ordinary output (`CK3 -4875186271144687632`,
`java.lang.NullPo<garbage>Hashtable.get:560`), `LargeArrayLoad FAILED`,
self-test4 and self-test5 BROKEN.

Fix in the patch: `clazz.cn1AllocSize` records the size of the first typed
allocation; a later allocation at a different size calls `cn1BibopDemoteClass`,
which returns the class's typed pages to the shared size-class pools and sets
`cn1AllocIndex = -1` permanently. A variable-size class is simply never typed.

NOTE FOR PHASE B: a demoted class still needs its header class pointer, so the
String twins cannot take their class from the page. Phase B needs pages keyed by
(class, size class) for the fused twins rather than an exemption.

### 2. NOT FIXED -- live objects above a reset bump cursor

With the size guard in, `run-gc-verify` reports 452 violations, all
`recycledSlot`:

    DANGLING REFERENCE after sweep at epoch 2
      holder = java.util.Hashtable mark=2 heapPos=-3
      field -> java.lang.String mark=-1 heapPos=-3
      victim = RECYCLED page slot (above bump cursor) (page-resident)

A live, FRESH object sitting above its page's bump cursor means a page was
formatted (`cn1BibopFormatPage`, bumpIndex = 0) while it still held live
objects. The gauntlet ran clean as far as it got; the corruption is
GC-visible rather than mutator-visible, which is why only the verifier catches
it.

Ruled out by inspection: the adopted-died scan and the major-sweep splice, both
of which were extended to walk `bibopPartialByClass` and are structurally
correct. NOT ruled out: the demotion path handing partial typed pages back to
`bibopPartialPool[classIndex]` while they are live, and the interaction between
`bibopCurrentByClass` and `bibopCurrent` when a class is demoted mid-flight.

## What the next attempt should do differently

Land it in smaller verifiable pieces than "make pages typed". Suggested order:

1. Add `pageClazz` and SET it, changing nothing else. Add a `CN1_GC_VERIFY`
   assertion that every object in a page with `pageClazz != 0` has a matching
   `__codenameOneParentClsReference`. Gate. This proves the field is maintained
   before anything depends on it.
2. Route ONLY the `CN1_FAST_NEW` path through typed pages -- fixed size by
   construction, no fused path, no demotion needed. Gate.
3. Extend to `cn1BibopAlloc`, with the size guard. Gate.
4. Only then the pool/sweep/teardown plumbing.

Each step keeps the header intact, so every failure is a gate failure and not a
wrong answer.

---

# Attempt 2: the minimal step also fails, and that is the useful part

`step1-fastpath-only.patch` is the second attempt, and it is much smaller than
the first. It changes NO pool code, NO sweep code and NO demotion logic. All it
does is give `CN1_FAST_NEW` its own page per class, taken fresh from
`bibopFreePool`, typed at format time, untyped and pushed to the sweep stack on
retire. Everything else in the VM allocates exactly as before.

It still breaks. `HtTorture: DIVERGE` on the gauntlet, first with wrong string
sums and then -- after the full-page fix below -- with a NullPointerException.

**That result is worth more than the patch.** Bug 2 of the first attempt (452
`recycledSlot` violations) was blamed on the per-class pool plumbing. This
attempt contains none of that plumbing and fails anyway, so the fault is NOT in
the pools. It is in the basic act of giving the fast path a page that is not
`bibopCurrent[ci]`.

The hypothesis that follows, and the thing to establish before any further code:
**something depends on there being exactly ONE owned page per (thread, size
class).** Candidates, none yet checked:

  * the sweep's treatment of `owned` pages (they are skipped outright);
  * the O(1) page-level reclaim, which decides a whole page is dead from
    `gcAllocedSinceSweep` + `gcLastMarkedEpoch` + `gcGraceEpoch`, all of which
    are per page and were previously advanced by exactly one allocating cursor;
  * `CN1_BIBOP_ACCOUNT_BYTES` and the GC trigger, which my typed acquire never
    consults -- the ordinary path calls `cn1BibopMaybeGc` on every page
    acquisition and this one does not;
  * the free-list path in `cn1BibopAlloc`, which can hand out a slot from a page
    the fast path also considers current.

## Two further bugs found and fixed in this patch, worth keeping

1. A FULL typed page was never retired. `bi >= CN1_BIBOP_SLOT_COUNT(ci)` simply
   fell through to `__NEW_X`, leaving the page installed and `owned = TRUE`
   forever -- and an owned page is skipped by the sweep, so its dead slots are
   never reclaimed and its epoch bookkeeping never advances. Exhaustion now
   counts as a miss and the page is retired and replaced.

2. THE ASSERTION ITSELF WAS UNSAFE, and this is the more embarrassing one. It
   reads `o->__codenameOneParentClsReference->clsName` for every slot below the
   bump cursor, skipping only `CN1_BIBOP_FREE_MARK`. That does not cover every
   non-object slot state -- a slot on the page free list holds an intrusive next
   pointer over offset 0 -- so the check dereferenced a free-list pointer as a
   clazz and took SIGSEGV (`FusedTest FAILED (exit 139)`), while reporting
   `slot 1 of a java.lang.String page holds ?` as though it had found a real
   violation. An invariant checker that crashes and cries wolf is worse than
   none. It needs a slot-liveness predicate that is right for every state a slot
   can be in, and that predicate does not currently exist in one place.

## What this line of work actually needs next

Not another patch. An enumeration, written down and checked against the code:

  * every state a BiBOP slot can be in (live / fresh / freed onto the page free
    list / quarantined / adopted / above the bump cursor) and how to tell them
    apart safely from outside the allocator;
  * every invariant the sweep and the page-level reclaim assume about `owned`,
    about how many cursors advance a page, and about `bibopCurrent[ci]` being
    the only one;
  * only then, which of those a typed page changes.

Three attempts have each found a real invariant by violating it. The cheapest
way to find the fourth is to read for it rather than to run for it.
