# What the generated C actually costs

Read over the 2,933 files / 3,126,185 lines of C that ParparVM emits for the
`hello` corpus, and the arm64 assembly clang produces from them at
`-O3 -fwrapv -fno-strict-aliasing`. Every figure below is a count over that
corpus or an instruction sequence from that assembly, not an estimate.

The purpose is to decide what C we WANT, verify it by hand-editing the generated
sources, and only then teach the translator to emit it.

---

## 0. The thing that is already free, so nobody spends a week on it

**The operand stack, the locals array and the type tags compile away.**

`ArrayList.size()` is, in C:

```c
JAVA_INT java_util_ArrayList_size___R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject) {
    DEFINE_METHOD_STACK_FRAMELESS(1, 1, 0);
    locals[0].data.o = __cn1ThisObject; locals[0].type = CN1_TYPE_OBJECT;
    PUSH_INT(get_field_java_util_ArrayList_size(__cn1ThisObject));
    return SP[-1].data.i;
}
```

and in assembly, in full:

```
ldr w0, [x1, #20]
ret
```

`ArrayList.add` -- twelve operand-stack operations in the C, including a
`BC_DUP_X1` that is 6 loads and 6 stores as written -- compiles to about twenty
instructions with **no frame traffic at all**.

There are ~232,000 static `.type = CN1_TYPE_*` write sites once macro expansions
are counted (`BC_ALOAD` and `PUSH_OBJ` each write the tag twice, INVALID then
OBJECT, to keep a precise scan from seeing a live tag over stale data). In a
frameless method the frame is a C-local array scanned CONSERVATIVELY, so every
one of those tags is dead -- and clang already deletes them. Removing them from
the emitted C would change nothing.

This was very nearly reported as the headline finding. It is the opposite of one.

**Exception: `volatile SP`.** See item 8.

---

## 1. One allocation costs ~28 instructions, and one of them is a CALL

`CN1_FAST_NEW(java_util_ArrayList)`, compiled standalone:

| what | instructions |
|---|---|
| `class__X` GOT indirection | 2 |
| `ldapur w8,[x20,#40]` -- class initialized? (acquire) | 1 + branch |
| `ldr w8,[x20,#152]` -- `CN1_CLAZZ_REGISTER` flag | 1 + branch |
| `bibopCurrent` TLS: `adrp/ldr`, **`ldr x8,[x0]; blr x8`** | 4 incl. an indirect CALL |
| `p->freeList`, `constantPoolObjects` (GOT + load), `cmp/ccmp/b.eq` | 6 |
| `bumpIndex`, `slotCount`, compare | 3 |
| `firstSlotOffset`, `slotSize`, `smaddl` -> slot address | 4 |
| body zero `stp xzr,xzr` + class ptr + heapPosition + `stlur` mark (release) | 4 |

**~28 instructions, ~13 loads, one indirect call.** HotSpot's TLAB bump is about
six: load top, add, compare against end, store top, store the class word.

Four separable items, in order of how mechanical the fix is:

### 1a. `bibopCurrent` is `__thread`, and on Darwin that is a function call
`extern __thread CN1BibopPage* bibopCurrent[CN1_BIBOP_NUM_CLASSES]` compiles to
the TLS-descriptor sequence: load the descriptor, **`blr`** it, then index. Per
allocation.

`threadStateData` is already the first parameter of every generated function.
Moving the array into `struct ThreadLocalData` turns four instructions and an
indirect call into one `ldr` off `x0`. No semantic change whatever -- it is the
same per-thread storage reached by a cheaper route.

### 1b. Two per-class one-shot flags are tested separately
`class__X.initialized` (acquire load) and the `CN1_CLAZZ_REGISTER` flag are both
one-shot per class and both tested on every allocation. If registration is made
part of initialization -- or the two bits are put in the same word -- one test
covers both.

### 1c. `constantPoolObjects != 0` is a VM-startup test on the hot path
A GOT indirection plus a load plus a compare, per allocation, to ask a question
whose answer is "yes" for the entire life of the process after startup.

### 1d. The body zero is unconditional
`stp xzr, xzr` here, and a real `memset` for larger objects. The comment at the
site says removing it is 2x SLOWER because uninitialized reference fields get
scanned during the mark==-1 grace window -- which is true for a RECYCLED slot off
a free list. A slot that has never been used, on a page freshly carved from the
(zero-filled) window, is already zero. Splitting the two cases removes the clear
from the bump path, which is the common one.

---

## 2. ArrayList is two allocations, and the second one is usually too big

```c
struct obj__java_util_ArrayList {
    ... 16-byte header ...
    JAVA_INT modCount;
    JAVA_INT size;
    _Atomic JAVA_LONG cn1Storage;     /* handle to a SEPARATE native block */
};                                     /* 32 bytes */
```

`new ArrayList()` allocates nothing; the first `add` calls `reserve`, which
allocates `DEFAULT_CAPACITY = 10` references -- 80 bytes of slots plus a 16-byte
block header. So a list holding one element costs **two allocations and 128
bytes for 8 bytes of payload**.

`[CAPHIST]`, cumulative over one translation of the corpus:

| element capacity | reference blocks | MB |
|---|---:|---:|
| <=1 | 694,471 | 26.5 |
| <=2 | 54,920 | 2.5 |
| <=4 | 121,312 | 7.2 |
| <=8 | 160,976 | 11.8 |
| **9..16** | **1,235,497** | **150.9** |
| >1024 | 9,628 | 453.4 |

The 9..16 bucket is the single largest line item in the whole allocation census
and `DEFAULT_CAPACITY = 10` sits in it.

**Proposal: inline slots in the object, exactly as StringBuilder already does.**
`struct obj__java_lang_StringBuilder` carries
`unsigned char __cn1InlineStorage[16]`, and `@Fused` String packs its payload
into the object. ArrayList carries nothing. Four inline references take the
object from 32 to 64 bytes and remove the block entirely for every list that
never exceeds four elements.

The field comment in `ArrayList.java` says the layout is deliberately exactly 32
bytes with no room even for a capacity field, so this is a real trade: 32 more
bytes on every list against one fewer allocation on most of them. **Measure the
final-size distribution of ArrayLists before choosing the inline count** -- that
number does not exist yet and it decides the whole item.

The same shape applies to `Vector` (which carries BOTH an `elementData` object
field and a `cn1Storage` handle) and to the 312,183 hash tables of capacity <=16
(114.3 MB). `HashMap` is already good: three logical tables, one allocation,
sliced by `cn1TablePart`.

---

## 3. The SATB write barrier is not CSE'd, and its tag test is hoisted out of the cold path

`CN1_WRITE_BARRIER` is written to be one predicted-not-taken branch:

```c
if(__builtin_expect(gcSatbActive, 0)) { ... cn1SatbEnqueue(v); }
```

In `ArrayList.add`'s assembly it is not one branch. It is:

```
ldr  w8, [x23]            ; gcSatbActive          <- insertion barrier
cbnz w8, ...
ldr  w8, [x23]            ; gcSatbActive AGAIN    <- deletion barrier
cmp  w8, #0
and  x8, x20, #0x7        ; CN1_IS_TAGGED(value), hoisted OUT of the cold branch
ccmp x20, #0, #4, ne
ccmp x8,  #0, #0, ne
b.eq ...
```

Two loads of the same global, because `cn1SatbEnqueue` on the cold path could
write it and clang will not CSE across that; plus three ALU ops of tag test paid
unconditionally because clang merged the conditions.

13,008 `CN1_WRITE_BARRIER` sites in the corpus. Two fixes worth trying:
read `gcSatbActive` once per region into a local, and keep the tag test inside
the cold branch (an opaque helper, or `__builtin_expect` structured so clang
cannot hoist it).

---

## 4. Collection storage handles are `_Atomic`, so every read is an acquire

`ArrayList.get` compiles its storage read to `ldapur x21, [x19, #24]`. There are
1,846 `_Atomic` field declarations in the generated headers and 12,571
`__ATOMIC_ACQUIRE` uses.

The acquire is there because the collector reads the handle concurrently with a
mutator replacing it on growth. The cost on Apple silicon is not the barrier --
`ldapur` is cheap -- it is that the load cannot be CSE'd or hoisted out of a
loop, so a loop over `list.get(i)` reloads the handle every iteration.

Worth investigating: whether a loop can hoist one acquire load and use relaxed
reads inside, given the handle only changes on growth (which the loop's own
`modCount` check already forbids).

---

## 5. Every frameless method entry pays a stack-overflow guard

```
ldr  x8, [x0, #136]        ; threadStateData->nativeStackLimit
cbz  x8, <lazy init>
sub  x9, x8, #64, lsl #12  ; limit - 256KB guard band
cmp  x8, x29
ccmp x9, x29, #0, gt
b.le <throw>
```

Six instructions, on **19,341** of 21,127 frameless methods. It exists because a
frameless frame does not bump `callStackOffset`, so the 1024-depth call limit
cannot see it, and deep recursion would blow the C stack into a SIGSEGV.

It is already omitted for trivial getters (`ArrayList.size()` has none). The
question is how much further that can go: a method that calls nothing cannot
recurse, so it cannot deepen the stack by more than its own frame. **A leaf
method needs no guard at all** -- the guard belongs on methods that CALL, and
even then only on ones that can participate in a cycle. Closed-world call-graph
reachability can answer that.

---

## 6. 21,016 class-initialization guards

Every static method entry and every static field access emits
`if(!__atomic_load_n(&class__X.initialized, __ATOMIC_ACQUIRE)) __STATIC_INITIALIZER_X(...)`.

In a closed world most of these are provably redundant: a static method reached
only from within its own class, or from a caller that has already forced the
class, cannot observe an uninitialized class. This is ordinary dominator
analysis over the call graph, and it removes an acquire load and a branch from
each site it clears.

---

## 7. `reserve` is not inlined into `add`

`ArrayList.add`'s assembly contains `bl _java_util_ArrayList_reserve___int` and
spills four callee-saved register pairs around it. `reserve`'s hot path is
`if(required <= capacity()) return;` -- three instructions.

ThinLTO is enabled for the shipping builds, so this may already be inlined
there; it is not in a single-file compile. Worth checking in the LTO output,
because the pattern (a tiny guard function called from the hot path of the
function it guards) is everywhere in the collection classes.

---

## 8. `volatile SP` is the one place the abstraction does NOT compile away

1,339 methods emit a `_VSP` frame (353 frameless, 986 framed) because they
contain a try/catch. In the one sampled -- `LanguageIdentifier.identify` -- the
assembly is 317 instructions of which **78 are stack-relative loads and stores**,
25% frame traffic, against essentially zero in the non-volatile methods.

`SP` is `volatile` because C11 7.13.2.1 makes an automatic modified between
`setjmp` and `longjmp` indeterminate afterwards. But **`DEFINE_CATCH_BLOCK`
assigns `SP = &stack[1]` immediately on the `setjmp` return path, before any
use.** An object whose indeterminate value is overwritten before it is read is
not a problem. The locals genuinely do need `volatile` -- a local written in the
try and read in the catch must survive -- but SP appears not to.

The history matters here: the comment says the two were "decided apart, only the
locals got it, and SP stayed undefined behaviour that gcc on musl eventually
refused to compile at all". That reads like gcc's `-Wclobbered`, which is
conservative and fires on any non-volatile local live across `setjmp` regardless
of whether it is reassigned first. So the fix may have been to a diagnostic
rather than to a defect -- which is worth establishing before acting, not after.

---

## Ranking, by measured size and by how mechanical the fix is

| # | item | evidence | risk |
|---|---|---|---|
| 1a | `bibopCurrent` out of `__thread` into `ThreadLocalData` | removes an indirect CALL from every allocation | very low |
| 2 | ArrayList inline slots | 1.2M blocks / 150.9MB in the 9..16 bucket alone | medium -- 32 bytes on every list |
| 1d | body zero only on free-list reuse | one `memset` per allocation | medium -- grace-window invariant |
| 3 | one `gcSatbActive` read, tag test back in the cold path | 13,008 sites, ~5 wasted ops each | low |
| 1b/1c | fold the two per-class flags; drop the startup test | 5 instructions per allocation | low |
| 5 | no SOE guard on leaf methods | 6 instructions on 19,341 entries | low |
| 6 | elide provable class-init guards | 21,016 sites | medium -- needs call-graph proof |
| 8 | non-volatile SP | 25% of instructions in 1,339 methods | HIGH -- setjmp UB |
| 4 | hoist the acquire load out of loops | 12,571 sites | medium |

## How to verify, per the plan

Each of these is checkable by hand-editing the generated C in
`/tmp/genc/dist/...-src`, rebuilding that one binary, and running it against the
corpus -- the translator IS the benchmark, so a hand-hacked VM still produces
byte-comparable output and `verify-selfhost.sh` still applies. Items 1a, 1b, 1c,
3 and 5 are small enough to hack directly into `cn1_globals.h` and measure
without touching the translator at all, which is the right order: prove the C
shape pays, then teach the translator to emit it.

**What must not be skipped:** an instruction count is not a result. Every item
here needs the same interleaved, 8-round, output-verified measurement the
allocator rounds used, because three separate changes this session looked good
at four rounds and evaporated at eight.

---

# Proof pass: what survived contact with the machine

Every item above was re-examined with a standalone microbenchmark
(`cbench/`, interleaved arms, min-of-9, repeated) AND by reading the arm64 that
clang produces. The two disagreed often enough that the method is worth stating
first.

## Instruction count is not a proxy for time

Item 3's arm C removes **22 instructions and 6 loads** from the write barrier and
is **0.003 ns/op** different. The barrier's flag load is a BRANCH condition, not
an input to the store, so it never enters the store's dependency chain and the
issue window absorbs the whole thing.

The corollary cuts the other way: item 1a is worth having not because it removes
three instructions but because one of them is an **indirect call**, which is a
serialization point and forces a stack frame that exists only to spill around it.

So the question to ask of a candidate is never "how many instructions" but:

- is it on a **dependency chain** that feeds an address or a branch that matters;
- does it consume a **load port** on a path that is load-bound;
- is it an **ordering primitive** (`stlr`/`ldapr`) or a **call**, which serialize
  regardless of how they are counted;
- does it force a **frame** in a function that would otherwise need none.

## Taken

| item | what the machine says | measured |
|---|---|---|
| 1a `bibopCurrent` -> ThreadLocalData | loses an indirect `blr`, 4 loads, and the frame that only existed to spill around the call | -0.23 ns/alloc, every run |
| page geometry as constants | `slotCount`/`firstSlotOffset`/`slotSize` were LOADS, two of them feeding `smaddl` on the address chain; now immediates and a shifted `add` | 3 loads and a multiply off the critical path |
| CN1_BIBOP_CIDX extended | 513..2048-byte objects were getting `ci = -1` and falling out of line to `__NEW_X`; the runtime table had the classes, the inline macro did not | the ceiling raise now reaches the fast path |
| SOE guard off leaves | the guard forces a frame-pointer materialisation on entry | 597,522,393 -> 409,951,756 guarded entries, 0.277 ns each = ~52ms of 5.4s |

## Rejected, with the reason

| item | why not |
|---|---|
| 3 SATB barrier restructure | 22 fewer instructions, 0.003 ns. Off the dependency chain. |
| 1d body zero | The zero writes into a line the allocator is already fetching for the header, so it is nearly free. Moving it to page-format measured WORSE (2.02 vs 1.71 ns) -- the bulk pass evicts the working set and every later allocation takes a cold line. |
| 1b / 1c flag and startup tests | Instruction counts drop (63 -> 58 -> 48) but the ops are independent and absorbed; the clean first run had them identical and a later 0.26ns swing reproduced as code layout, not work. |
| 6 class-init guard | 0.018 ns. One `ldapr` that folds into an adjacent compare. Would have cost a whole-program dominator analysis. |

## Open, and it is a proof rather than a measurement

**The allocation rate is set by two store-releases, not by instruction count.**

```
str   x20, [o]        ; class pointer
stlur w11, [o+8]      ; mark = -1          RELEASE
stlur w10, [p+32]     ; bumpIndex = bi+1   RELEASE
```

The second is the loop-carried edge: the next allocation's `bumpIndex` load
cannot issue until it retires, which is what pins the fast path near four cycles
and why everything removed around it was absorbed.

Whether it can be relaxed is a question about the SWEEP, not the mutator. The
mark-word release already publishes the object; the cursor release additionally
guarantees that a collector which sees slot N consumed also sees slot N-1's mark.
Relaxing it is sound only if the sweep's "a slot whose mark is not current is
skipped without dereferencing" invariant already covers a cursor that becomes
visible before the mark it refers to. That is worth establishing -- it removes an
ordering primitive from every allocation in the VM -- and it is not something to
try and measure.

## Method note for whoever runs the harness next

Two arms in `bench_call.c` initially reported **0.000 ns/op** because the
function under test was pure and clang folded twenty million calls into one. The
fix is to make every call depend on the iteration index, and the check is to
count `bl` in the loop body -- which the harness comment now says. A benchmark
arm that reports zero is not a result, it is a bug.

---

# The biggest item, measured and then declined

## The cursor release is half of every allocation

| arm | ns/op |
|---|---:|
| H today: mark release + cursor release | 1.950 |
| I cursor store RELAXED | **0.980** |
| J both relaxed | 0.985 |
| K release kept, cursor read from the thread state | 1.833 |

**The `stlr` on `p->bumpIndex` costs ~0.97ns -- about half the fast path.** The
mark release next to it costs nothing measurable (I against J).

Arm K refutes the obvious fix. The theory was that the cost is the loop-carried
edge -- the next allocation's load of `bumpIndex` being a dependent load on an
`stlr` to the same address -- so K keeps the release exactly as it is and takes
the READ off it, allocating from a plain thread-local cursor. It recovers 0.117ns
of the 0.97. **The cost is the ordering primitive itself**: on this core a
store-release drains the store buffer whether or not anything downstream depends
on it. Worth knowing generally, and it is why every instruction removed around
this store was absorbed -- they were never the constraint.

## Why it is not being relaxed

The sweep's contract is a single acquire on the cursor publishing every slot
header below it, which is what lets its per-slot reads be plain loads:

> ACQUIRE pairs with the allocator's RELEASE store of bumpIndex: for every slot
> i < n the header stores (parentCls / heapPosition / mark) that preceded that
> release are visible to this walk. Relaxed could observe a freshly-bumped slot
> with a garbage header.

The sweep alone could be re-proved -- it never touches an OWNED page (there is an
assert for exactly that), so a thread could publish its cursor once at retire.
The readers that block it are the ones that DO run on owned pages concurrently:
the conservative resolver (`idx >= pg->bumpIndex` rejects the slot) and the grace
pass. Against a stale cursor, a just-allocated object is rejected as a root and
is not traced; an OLD object reachable only through it is then covered by nothing
except the SATB insertion barrier.

That is the allocate-black invariant, and this branch has already been here. From
the grace-pass note: the barrier was audited, two real holes were found and fixed
(`arraycopy` on an object array, `cloneArray`), and then -- decisively -- **two
purpose-built drivers reported `violations=0` with the barrier deliberately
compiled out**. The window is real by inspection and too narrow for any gate here
to open.

So the same conclusion applies, for the same reason: this would trade a measured
0.97ns per allocation (25.8M allocations, ~26ms of a 5.4s run, 0.48%) for a
correctness risk that surfaces as silent heap corruption in a customer app with
no reproducer. **If it is revisited, the thing to build FIRST is the same thing
the grace-pass note asks for: a way to drive an allocation into the residual
window on purpose.** Without that, no version of this change can be validated,
and a green gate would mean nothing.

## What that leaves

The allocation fast path is now ~1.0ns of ordering primitive plus ~1.0ns of
everything else, and the everything-else half has had its dependency chain
shortened as far as it goes without touching the collector's contract. Further
work on allocation THROUGHPUT is blocked on a verification tool, not on ideas.
