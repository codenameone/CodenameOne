# BiBOP slot and page invariants

Why this file exists: three consecutive attempts to give BiBOP pages a single
class each corrupted the heap, and each one found a real invariant by violating
it rather than by reading it. The invariants were all discoverable -- several are
written in comments next to the code that depends on them, and one is already
checked by a build flag nobody turns on -- but they were not in one place, so
each attempt rediscovered a different subset the hard way.

Every rule below is followed by the mechanism that makes it load-bearing. A rule
with no consequence is not a rule, and if you cannot state what breaks, the rule
is probably wrong.

`scripts`-free: the checks live in the VM under `-DCN1_BIBOP_VALIDATE`, and
`run-gc-verify.sh` builds that arm. See "The validator" at the end.

---

## Part 1: what a slot can be

A BiBOP page is a header followed by `slotCount` fixed-size slots. A slot is in
exactly one of these states, and **the state is decided by the MARK WORD, never
by the class pointer**.

| state | how to tell | is the header readable? |
|---|---|---|
| never allocated | index >= `bumpIndex` | NO -- contents undefined |
| live / aging | `mark >= 0` | yes |
| fresh | `mark == -1` | yes |
| freed | `mark == CN1_BIBOP_FREE_MARK` (-7) | **NO** |
| quarantined | `mark == CN1_BIBOP_QUAR_MARK` (-8), `CN1_GC_VERIFY` builds only | **NO** |

### R1. Read `bumpIndex` with acquire, and never look at a slot at or above it.

`bumpIndex` is the publication point for the whole page. The allocator writes the
slot -- class pointer, heap position, zeroed body -- and only then stores
`bumpIndex + 1` with release. A reader that takes a relaxed or stale `bumpIndex`
can see a slot whose header has not been written yet.

### R2. A freed or quarantined slot's first word is NOT a class pointer.

The page free list is intrusive: it stores its next pointer in the slot's first
word, which is exactly where `__codenameOneParentClsReference` lives. Reading
that word as a `struct clazz*` and dereferencing it walks a slot address as if it
were a class.

**This is not hypothetical.** The invariant checker written for the second typed
page attempt skipped `FREE_MARK` and did not know `QUAR_MARK` existed, so it
dereferenced quarantined slots and took SIGSEGV in `FusedTest` -- and only in
`CN1_GC_VERIFY` builds, because quarantine only exists there. It also printed
`slot 1 of a java.lang.String page holds ?` as though it had found a genuine
violation. A checker that crashes and cries wolf is worse than no checker.

### R3. Load the mark word before any other header field, with acquire.

`cn1BibopInitSlot` release-stores the mark LAST, so the mark word is the single
happens-before edge that publishes a freshly allocated object. A relaxed load
here is how the parallel marker once observed an object's mark without observing
its `parentClsReference` store, dereferenced a stale class pointer, and crashed
at a wild PC on arm64 -- x86 masked it because every x86 load is already acquire.

### R4. `heapPosition` says which collector owns the slot, and there are three answers.

* `CN1_BIBOP_HEAP_POS` (-3): ordinary page-resident object, swept by the page sweep.
* `CN1_BIBOP_ADOPTED` (-4): MATURED. The memory is still in its BiBOP slot but
  its lifecycle now belongs to the legacy mark/sweep; the page sweep must skip it
  and its slot comes back only after the legacy sweep flips it to -3.
* `CN1_GC_EMBEDDED_PRIMITIVE` (-5): a fused child living INSIDE another object's
  slot. It has no slot of its own, is never registered anywhere, and the sweep
  can never free it independently -- it dies with its owner.

Consequence for anything that walks slots: an embedded primitive is **not** at a
slot boundary and will never be found by a slot walk, so a slot walk is not an
enumeration of all objects.

---

## Part 2: what a page guarantees

### R5. Exactly one cursor may allocate into a page, and it is the owning thread.

`owned == JAVA_TRUE` means some thread holds this page as its current allocation
cursor. That thread bumps `bumpIndex` without the page lock, which is only sound
because nothing else does.

### R6. Only a RETIRED (non-owned) page may reach the sweep.

Already checked under `CN1_BIBOP_VALIDATE` at the sweep entry, with the comment
"the sweep will reset/recycle it out from under that thread -> the intermittent
cn1BibopFastAlloc crash". The sweep may reformat a page, which resets
`bumpIndex` to zero; doing that while a thread is bumping it hands the same slot
to two objects.

**Corollary, and the one the third attempt broke:** a page that a thread stops
using must actually be retired. Leaving a full page installed and `owned` means
it is never swept, its dead slots never return, and its epoch bookkeeping never
advances.

### R7. Any allocation into a page must set `gcAllocedSinceSweep`.

The grace pass slot-scans exactly the flagged pages. An allocation that does not
flag its page leaves a `mark == -1` object the grace pass never traces, so an
older object reachable only through it is freed while still referenced. The flag
is cleared only by the sweep, never by a concurrent phase.

### R8. `classIndex`, `slotSize`, `slotCount` and `firstSlotOffset` are fixed at format time.

A page is formatted for one size class. The inline fast path computes the slot
address from a COMPILE-TIME size class, so a page reached through a cursor whose
size class differs writes objects at the wrong stride.

### R9. The page-level reclaim decides a whole page is dead without looking at slots.

It uses `gcAllocedSinceSweep`, `gcNeedsReclaim`, `gcHasAdopted`, `freeList`,
`gcLastMarkedEpoch` and `gcGraceEpoch`. Both epoch bounds are needed and neither
implies the other: `gcLastMarkedEpoch` covers slots marked by `gcMarkObject`,
`gcGraceEpoch` covers slots the sweep itself promoted out of grace. Anything that
changes who advances these fields, or how often, changes which pages are
reclaimed wholesale.

---

## Part 3: rules a TYPE-HOMOGENEOUS page would add

Not yet implemented; recorded so the next attempt states them up front.

### R10. A page may be typed only while it is EMPTY.

Typing a partially filled page leaves objects of other classes below the bump
cursor, and anything reading the class off the page is then wrong about them.

### R11. A typed page must stay typed for its whole life, or be untyped only when empty.

Once objects of class A are in a page, the page can never hold class B. This is
why typed pages need per-class partial pools rather than the shared size-class
pools -- returning a partially live typed page to the shared pool offers its free
slots to other classes.

### R12. "One class" does not imply "one size class".

`CN1_FAST_NEW` always asks for `sizeof(struct obj__X)`, which makes the
implication look safe. `cn1AllocFused` does not: it asks for the String header
PLUS its inlined characters, so `class__java_lang_String_i8` allocates at many
sizes. Keyed by class alone, a 200-byte String landed in a page whose slots were
sized for a 40-byte one and ran off the end -- corrupted string payloads,
`LargeArrayLoad` failing, two broken self-tests.

### R13. A class that can be allocated at more than one size cannot be typed by class alone.

Either key such a page by (class, size class), or exclude the class.

### R14. An OWNED page is invisible to the sweep, so pinning one page per class costs a page per class OF UNCOLLECTABLE HEAP.

R6 says only retired pages reach the sweep. The corollary nobody stated until it
was measured: every page a thread holds as a cursor is heap the collector cannot
touch until that thread lets go. With one cursor per size class that is 23 pages
at worst. With one cursor per CLASS it is one page per class the program has
ever allocated.

The cost is NOT one instance per page -- a 64KB page holds 2048 instances of a
32-byte class, and a typed page fills with instances of its class exactly as a
size-classed one does. The cost is one page per CLASS: the page is dedicated, so
a class with three live objects strands the other 2045 slots where no other
class may use them.

Measured against the clean tree, same drivers, same validator build:

                   clean          typed            live objects
    hashMapChurn   7 pages        22 (16 typed)    47-62
    recursion      7 pages        24 (18 typed)    53-61

+15 to +17 pages for ~16-18 live classes -- one page per class, as predicted.
~448KB becomes ~1.4MB to hold about fifty objects, and ALL of those objects
would fit in a single page with room for two thousand more. That is the whole
cost in one sentence.

At that ratio reclaim effectively stops, and it is not subtle: two fault
injections in `run-gc-verify` -- restoring the pre-fix page-reclaim bound, and
tracing only half of every reference block -- both stopped producing ANY damage,
because there was nothing left for the collector to get wrong. The gauntlet
stayed green and the invariant validator reported zero violations over 37
million objects throughout. Correctness was never the problem.

Bounding the pinning to one GC cycle fixes the large case and cannot fix the
small one: `objectAllocation` went from 16 typed pages to 2, while
`hashMapChurn` went from 17 to 16 and `recursion` did not move at all. With ~18
live classes and ~24 pages, one 64KB page per class IS the heap. A class with
three live objects still occupies a full page.

**Consequence for the header plan.** The 8.9% that type-homogeneous pages buy is
measured in ALLOCATED BYTES -- a throughput and bandwidth number. The
page-per-class floor is PEAK FOOTPRINT: `live classes x CN1_BIBOP_PAGE_SIZE`,
which is ~12MB for the 194-class selfhost corpus and dominant for anything
smaller. Those are different axes, and this VM sells the small one -- the 2.4MB
floor. Delivering the header saving this way costs more than it saves wherever
the heap is small.

Making it viable needs a typed granularity far below 64KB (sub-page runs, or
segregated regions with a shared page), which is a much larger change than the
8.9% justifies on its own. The saving is real; this delivery mechanism is not
the way to collect it.

---

## The validator

`-DCN1_BIBOP_VALIDATE` already existed and already encoded R5, R6 and R8 at the
points that depend on them -- and no gate built it, which is why three attempts
ran without it. `run-gc-verify.sh` now builds a `bibop-validate` arm so these
checks actually run.

`cn1BibopValidateHeap()` walks every page and checks R1-R9 from the outside, and
`cn1BibopSlotState()` is the safe slot-state predicate R1-R3 demand -- the one
thing that did not exist in a single place, which is why each caller invented its
own and one of them crashed.
