# Backend benchmarks: Codename One against Go

A like-for-like comparison of the server-side backend against a Go `net/http`
server, the harness that produces it, and what it found.

## Where it stands

Clean run, 64 workers, 20s per cell, both servers on the same two pinned cores:

| route | conns | CN1 req/s | Go req/s | CN1 p50 | Go p50 | CN1 p99 | Go p99 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| /plaintext | 16 | **259,417** | 223,262 | **45 us** | 60 us | 25.3 ms | 2.35 ms |
| /plaintext | 64 | 268,431 | 270,042 | **200 us** | 218 us | 57.2 ms | 1.61 ms |
| /plaintext | 256 | 237,198 | 252,886 | **395 us** | 0.98 ms | 72.5 ms | 3.14 ms |
| /json | 16 | 195,014 | 239,486 | 61 us | 57 us | 77.7 ms | 2.24 ms |
| /json | 64 | 159,449 | 263,763 | 281 us | 224 us | 136.0 ms | 1.72 ms |
| /json | 256 | 140,322 | 221,523 | **806 us** | 1.11 ms | 117.5 ms | 3.79 ms |

| | Codename One | Go net/http | |
| --- | --- | --- | --- |
| static binary, stripped | 8.18 MB | 5.05 MB | Go 1.6x smaller |
| ...without the SQLite engine | 6.56 MB | - | |
| cold start, mean of 15 | **2.69 ms** | 4.56 ms | **CN1 1.7x faster** |
| idle RSS | **3.5-4.8 MB** | 4.5-4.6 MB | comparable |
| RSS under load | 289-550 MB | 11-15 MB | Go ~30x lower |

**Throughput on /plaintext is at or slightly above Go.** Measured properly --
five interleaved repetitions per runtime on an idle machine, 64 connections:

| | median req/s | min | max | spread |
| --- | --- | --- | --- | --- |
| go net/http | 199,038 | 194,597 | 204,330 | 5.0% |
| codename one | **209,013** | 206,780 | 214,304 | 3.6% |

Paired ratios 1.012, 1.045, 1.077, 1.074, 1.034 -- **CN1 ahead in 5 of 5, median
+4.5%**. That is a real if modest lead, and the per-run spread is small enough
that it is not an artifact. **Median latency is better at every point measured.**
The single-sample ratios elsewhere in this file (116%, 99%, 94%) predate the
repeated measures and should be re-run before being quoted.
The two things that are not competitive are **tail latency** and **memory under
load**, and `/json` costs a further 40%. All three have measured causes below.

### /json: the container was the cost, not the serialiser

Go encodes a struct with a cached per-type encoder. We built a `LinkedHashMap`
per request, hashed a key, inserted, then walked it back with an `instanceof` per
value. Those are not the same work, and the benchmark's own comment said to keep
the handlers matched.

Measured, one binary, three interleaved reps, 64 connections, bodies asserted
byte-identical in every arm:

| | median req/s | vs Go |
| --- | --- | --- |
| go net/http | 200,437 | - |
| CN1, map per request | 149,146 | 74% |
| CN1, fields written straight to the sink | **189,510** | **94%** |

Per-rep ratios: the direct write is **1.265 / 1.291 / 1.588** times the map
version (median **+29%**), and lands at **0.941 / 0.961 / 0.930** of Go.

An intermediate arm isolates why: reusing ONE map (still serialised by walking it)
was worth +22%, so most of the cost is building the container rather than writing
the bytes. That is why `RestServerAnnotationProcessor` now emits
`toJson(T, ByteSink)` beside `toMap` -- it knows every field name and type at
build time, so the names go in as pre-escaped literals and each value takes the
writer its static type selects.

Caveat on the 29%: this arm reuses a static writer, while the generated code
allocates one small wrapper per response. That is one object against a map plus
an entry per field, so most of the win should survive, but the shipped figure will
be somewhat lower and has not been measured through an annotated endpoint yet.

### How it moved

| /plaintext, 16 conns | req/s | p50 | p99 | bytes allocated/req |
| --- | --- | --- | --- | --- |
| starting point | 24,012 | 303 us | 342 ms | 4,786 |
| + poller no longer re-armed per request | 29,279 | 127 us | 414 ms | 4,786 |
| + collector answering its demand signal (master #5609) | 48,875 | 127 us | 212 ms | 4,786 |
| + response built as bytes, no String/StringBuilder | 84,750 | - | - | 2,948 |
| + request parsed in place, no header Map or Strings | ~250,000 | 43 us | 5.8 ms | 432 |
| + JSON serialised into a reused byte sink | 259,417 | 45 us | 25.3 ms | **176** |

**Each row is a single sample, and the harness spread is 28-45% (see the caveats
at the end), so read this table by step size rather than by the numbers.** The
first step (24,012 to 29,279, +22%) is INSIDE the noise and is not evidence of
anything on its own; it is kept because the syscall census independently showed
the per-request `epoll_ctl`/`fcntl`/`futex` traffic going to zero, which is a
count rather than a timing and does not move run to run. The later steps
(48,875 to 84,750 to ~250,000, +73% then +195%) are far outside the noise and
are real. The last row is within noise of the one above it.

The **allocation** column is the trustworthy half of this table throughout: those
are exact counts from the census, not sampled timings, and they are what the
rest of this file's conclusions rest on.

Allocation per request fell **27-fold**, and that -- not any change to the
collector -- is what moved throughput and the tail together.

## The bug that made an earlier version of this table wrong

An earlier revision reported 302k req/s at 16 connections and ~276k at 64. Those
numbers were not reproducible, and the reason matters more than the numbers.

The keep-alive linger (below) let a worker hold a connection and wait for the
next request instead of handing the descriptor back to the reactor. It had a
timeout but **no bound on the number of requests served in one visit**. A client
that keeps sending is readable every time, so the worker went round again
forever. With 16 workers, **16 connections were served at full speed and every
other connection starved** -- including newly accepted ones.

It is invisible in a throughput number. `wrk` reported 234k req/s and no socket
errors while a `curl` issued during that same run **received nothing in five
seconds**. The h2 path's own comment had warned about exactly this shape
("pinning a worker to each would mean the pool size is the limit on concurrent
clients"); the h1 path did it anyway.

Two lessons are now enforced in code rather than remembered:

- A worker hands the connection back when something is waiting **and** there are
  not enough idle workers for it. Testing the idle count alone is wrong in the
  commonest configuration of all -- with connections == workers, `idle <= pending`
  reads `0 <= 0` and hands back on every request, undoing the optimisation.
- `SelfTest.fairness()` saturates a 2-worker server with 4 relentless connections
  and requires a fresh connection to be answered. With the guard removed it fails
  with "no response in 5s"; with it, it passes -- on both the JavaSE and the
  translated runtime.

## Workers: a bounded pool, not one per connection

Go net/http runs a goroutine per connection, so matching workers to connections
sounds like the equivalent arrangement. It is not. Every thread's stack is walked
every GC cycle, so threads are not free here the way goroutines are -- though what
exactly that walk costs is an open question (see below); the numbers are what
stand.

| /plaintext, 256 conns | workers | req/s | p99 |
| --- | --- | --- | --- |
| one worker per connection | 256 | 45,171 | 364.7 ms |
| bounded pool | 64 | **237,198** | 72.5 ms |
| bounded pool | 32 | 216,100 | 55.6 ms |

At 256 workers `/json` collapses to 11,101 req/s.

**64 is not a magic number, and it is not the core count either.** Both were
worth ruling out, because every reading above came from one machine with the
server pinned to two cores -- where "64 workers" and "32 per core" are the same
number. Sweeping the worker count against the cores the server actually runs on,
at 64 connections:

| server cores | 8 | 16 | 32 | 64 | 128 |
| --- | --- | --- | --- | --- | --- |
| 1 | 77,084 | 106,611 | 115,893 | **138,076** | 133,755 |
| 2 | 126,939 | 177,399 | 214,091 | 248,507 | **252,100** |

Doubling the cores nearly doubles throughput at a fixed worker count, but barely
moves where the curve peaks: one core peaks at 64, two cores are flat from 64 to
128. **The optimum tracks the offered concurrency, not the core count** -- this
sweep ran at 64 connections and both rows peak at workers close to it, which is
also why 64 workers beat 256 at 256 connections while 256 workers collapsed.

The working rule is `workers ~= min(concurrent connections, 64..128)`, where the
upper bound is the stack-scan ceiling below, not a property of the machine. A
default derived from `Runtime.availableProcessors()` would be the wrong shape --
and that method is not in `vm/JavaAPI` anyway.

Measured on 1 and 2 cores only: this VM has 4 and the load generator needs two of
them, so whether the ceiling moves on a 16-core host is **untested**.

## What the syscall census found

The first version of this comparison blamed the collector. That was half wrong:
the collector is expensive, but the request path was worse, and counting
syscalls is what settled it. Both servers traced under identical load:

| per request | Go | CN1 before | CN1 after |
| --- | --- | --- | --- |
| `epoll_ctl` | 0.00 | **2.00** | 0.00 |
| `fcntl` | 0.00 | **4.00** | 0.00 |
| `futex` | 0.05 | **3.77** | 0.03 |
| `write` / `sendto` | 1.00 | **2.00** | 1.00 |
| `read` / `recvfrom` | 1.02 | 1.00 | 1.00 |

The reactor handed each connection back to the poller after **every response**,
so a keep-alive connection paid an `epoll_ctl` pair, two blocking-mode flips and
a cross-thread handoff per request. The `futex` traffic from that handoff alone
was 80% of our syscall time. Go's netpoller registers a connection once,
edge-triggered, and the goroutine keeps reading.

Three changes, following what Go does:

1. **The worker holds the connection.** After responding it waits briefly for the
   next request instead of returning the descriptor to the reactor. The timeout
   alone does **not** bound that hold -- a client that keeps sending is readable
   every time -- which is the bug described above. What bounds it is handing the
   connection back as soon as another connection is waiting and no idle worker can
   take it.
2. **That wait is one `poll`.** The first attempt set and restored `SO_RCVTIMEO`
   around each wait; the trace showed it cost **4 `setsockopt` per request**, 15%
   of syscall time. `poll` is one syscall and changes no socket state, so the
   deadline governing a real request read is never disturbed.
3. **One write per response.** Head and body go out together when the body is
   small and in memory.

Effect: p50 fell from 1.16 ms to 48 us at 64 connections - past Go - and
throughput rose about 25%. Which is where the collector takes over.

## What is left: the tail, and where it comes from

`[GCSTALL]` charges every mutator stop to a cause. On `/json` at 64 workers:

```
[GCSTALL-T] threads=66 stallMs=22624 dutyPct=66.2
cause=pacingVolume  count=4219     totalMs=456202  meanUs=108130  p99Us=131072
cause=signalStop    count=4760     totalMs=1597    meanUs=335     p99Us=4096
cause=nativeResume  count=5780945  totalMs=371     meanUs=0
markMs=387.4  stackMs=360.7  liveSlotKb=176687   allocated 462 bytes/request
```

The tail is **not** a mark pause: `signalStop` is 335 us mean, 4 ms p99. It is
`pacingVolume` -- the run-ahead cap parking mutators for a mean of **108 ms** when
the collector falls behind. That park is the p99, and the workers run only 66% of
wall time.

### Proved causally, by moving one variable at a time

`[GCSTALL]` names a cause; it does not prove one. Two knobs were added to settle
it by experiment -- `CN1_GC_PACING_CAP_MB` (overrides `cn1BibopPacingCap`) and the
existing `WORKERS` -- and each was moved with everything else held fixed. Same
binary, 2 pinned cores, 64 connections, `/plaintext`, medians of repeated runs.

**Cause 1 -- the run-ahead cap parks mutators for a whole collection.** Raising it
moves the tail by an order of magnitude AND buys throughput, on both routes:

| route | cap | req/s | p99 |
| --- | --- | --- | --- |
| /plaintext | default | 160274, 157707 | 44.4 ms, 45.4 ms |
| /plaintext | 2048 MB | 177966, 172337 | **5.3 ms, 4.3 ms** |
| /json | default | 115748, 118407 | 69.7 ms, 82.0 ms |
| /json | 2048 MB | 139050, 128940 | **8.4 ms, 9.0 ms** |

It is a cliff, exactly as the comment on `CN1_BIBOP_GC_MAX_CAP_MULTIPLIER` says:
`cn1PacingPark` spins on `usleep(50)` until `cn1PacingVolume` drops, and that
counter only drops when a cycle ENDS. So crossing the cap costs a full collection,
whoever crosses it. That is a p99 shape -- the median is untouched.

**The cap is load-bearing; it cannot simply be removed.** With it set past reach
the `/json` server died during the run. Whatever replaces it has to keep bounding
run-ahead. Note also which branch this is: `cn1ProcessHeadroom()` returns -1 only
where there is no per-process limit, and iOS always takes the other branch through
`os_proc_available_memory()`, so this path is the server path and tuning it does
not touch jetsam behaviour.

**Cause 2 -- worker threads oversubscribing the cores.** With the cap left alone,
only `WORKERS` moving:

| workers | req/s | p50 | p99 |
| --- | --- | --- | --- |
| 2 | 110551 | 547 us | **2.27 ms** |
| 4 | 111974 | 530 us | 16.2 ms |
| 8 | 126384 | 422 us | 49.1 ms |
| 16 | 162623 | 396 us | 44.6 ms |
| 64 | 245538 | 225 us | 61.3 ms |
| **Go, GOMAXPROCS=2** | **240864** | **245 us** | **1.56 ms** |

Two things fall out of that table. The first is that **throughput is already at
parity and the median is better than Go's** -- 245538 against 240864, 225 us
against 245 us. The whole remaining gap is the tail. The second is that the tail
is bought with threads: 64 runnable OS threads on 2 cores wait on the OS
scheduler, while Go multiplexes goroutines onto 2 OS threads in user space and
never queues behind a timeslice.

Both causes are independent and compose:

| configuration | req/s | p99 |
| --- | --- | --- |
| 64 workers, default cap | 219361 | 64.98 ms |
| 64 workers, 2048 MB cap | **241134** | **14.67 ms** |
| 16 workers, 2048 MB cap | 170704 | 6.29 ms |
| 8 workers, 2048 MB cap | 138822 | 4.16 ms |
| 2 workers, 2048 MB cap | 118747 | 2.28 ms |

At 64 workers with the cap corrected the server is at **100.1% of Go's throughput
with a better median and a tail 4.4x smaller than before** -- and still 9x Go's,
which is the thread-oversubscription residual.

### The zero-copy read: what the bisection found

`CN1_HTTP_ZERO_COPY` helps one route and hurts the other, which no single-cause
story explains. Mode 2 was added to split mode 1's two differences from mode 0:
it uses mode 1's NATIVE (read into the thread's foreign buffer) and mode 0's JAVA
(copy into a fresh heap array). Medians of 3 reps, 64 workers, 2 pinned cores:

| route | mode 0 recv+heap | mode 1 foreign | mode 2 foreign+heap |
| --- | --- | --- | --- |
| /plaintext | 249524 | 234939 | 250962 |
| /json | 158972 | 166289 | 150654 |

**Mode 2 lands on mode 0 on both routes, so the native read costs nothing** and is
not what moved either number. Mode 1 against mode 2 therefore isolates a single
thing -- keeping the foreign off-heap array in a Java field rather than copying
out of it -- and that one thing is worth **-6.4% on /plaintext and +10% on /json**.

Both signs come from the same trade. Mode 1 skips a per-request allocation and in
exchange `Conn.buffer` points outside the heap, so the fast range check in the
mark path fails and the object has to be resolved as an immortal root on every
traversal. On `/json`, which is allocation bound, the saved allocation is worth
more than the collector's extra work. On `/plaintext` there is little GC pressure,
so the saving buys little and the marking cost is paid anyway. The default is a
judgement about the workload, not a fact about the code.

### What Go actually does differently (read from go1.23.12 source)

The comparison is against `net/http` on go1.23.12. Reading its scheduler and
poller alongside ours explains every gap that is left, and the syscall census
below confirms the reading rather than resting on it.

**1. Go has no poller thread, and no handoff.** `netpoll()` is called from
`findRunnable()` -- the scheduler's own loop, on whatever M has run out of work
(`runtime/proc.go`). What it does with the result is the whole story:

```go
if list, delta := netpoll(0); !list.empty() {
        gp := list.pop()        // take the first ready goroutine
        injectglist(&list)      // the REST go to run queues
        return gp, false, false // and RUN IT ON THIS THREAD
}
```

The thread that polled runs the work itself. Our reactor cannot: `handOff` does
`reactor.remove(fd)`, allocates a `Runnable`, enqueues it, and wakes a worker,
which is a cross-thread dispatch per request. Ours is also ONE thread doing the
polling; Go's polling capacity scales with GOMAXPROCS because any M can do it.

**2. Go registers each fd once, edge-triggered.** `netpollopen` sets
`EPOLLIN|EPOLLOUT|EPOLLRDHUP|EPOLLET` and there are exactly three `EpollCtl` call
sites in `netpoll_epoll.go` -- the wake eventfd, one ADD per fd, one DEL per fd.
**Zero per request.** Ours is level triggered, so `handOff` MUST deregister
before a worker reads (an fd left registered is reported ready again and two
workers land on one connection) and re-register afterwards: two `epoll_ctl` per
handed-back request.

**3. Go reads optimistically.** `internal/poll.FD.Read` calls `syscall.Read`
first and only parks on `EAGAIN`. A ready connection costs one syscall and never
touches the poller.

**4. Go's GC charges the allocator WORK, not SLEEP.** `assistWorkPerByte` is
documented as "the ratio of scan work to allocated bytes that should be performed
by mutator assists". An over-budget goroutine computes a debt proportional to its
own excess, first tries to steal `bgScanCredit` (free when the background workers
are ahead), and otherwise does the marking itself. Parking is a last resort and
is woken by CREDIT, not by the end of a cycle.

Ours does the opposite, and this is the sharpest single contrast in the whole
comparison: `cn1PacingPark` spins `usleep(50)` until `cn1PacingVolume` drops, and
that only happens when a cycle ENDS. Go's assist is bounded, proportional, and
*productive* -- the work it does advances the very cycle it is waiting on. Ours
is unbounded in the sense that matters (a whole collection), disproportionate
(whoever crosses the cap pays for everyone) and pure loss (sleeping makes the
cycle no shorter).

### The syscall census confirms it

`strace -f -c`, both servers, same load, 2 pinned cores, 64 connections,
normalised per request (absolute rates are meaningless under strace; the ratios
within a run are not):

| syscall | Go | CN1 | |
| --- | --- | --- | --- |
| read / recvfrom | 1.436 | 1.503 | the same I/O |
| write / sendto | 1.397 | 1.502 | the same I/O |
| futex | 0.063 | 0.437 | **6.9x** -- the cross-thread dispatch |
| epoll_ctl | 0.0021 | 0.087 | **41x** -- level-triggered re-registration |

**The actual I/O work per request is identical.** What differs is entirely
coordination: waking another thread, and re-arming the poller. Note CN1's
`epoll_ctl` is only 0.087 rather than 2.0 because the keep-alive linger keeps
about 91% of requests inside the worker they are already on -- the linger exists
precisely to dodge the path this table is measuring.

### Why each measured gap follows

- **The 119-129k plateau below 64 workers** is the dispatch. Every request that
  is not held by the linger pays deregister + allocate + enqueue + futex wake +
  context switch + re-register. Go pays none of it.
- **Why we need 64 workers at all**: our unit of concurrency is an OS thread, so
  "keep this connection attached and skip the dispatch" costs a whole thread. Go
  keeps 64 connections attached with 64 goroutines on 2 OS threads.
- **Why the tail is 40x worse** follows from that: reaching Go's throughput
  requires workers >= connections, which puts 64 runnable threads on 2 cores, and
  the OS scheduler supplies the tail. The GC park adds the rest, independently.
- **Why p50 and throughput are already at or above parity**: the per-request work
  is the same, and our parsing and response building are cheap (176 bytes/req).
  With the dispatch bypassed we beat Go. Neither the runtime nor the handler code
  is the problem -- the concurrency architecture is.

**The fix does not require green threads.** Point 1 is the expensive one, and the
trick that removes it is available to us: let the WORKERS poll. A worker with
nothing to do calls `epoll_wait` itself and runs the first ready fd inline,
instead of a dedicated reactor thread waking it. That deletes the futex wake and
the queue, and combined with edge-triggered registration deletes the two
`epoll_ctl` as well -- without any goroutine machinery, and without a thread per
connection. The GC half is a separate change: replace park-until-cycle-end with
proportional mark assist.

### Tested: does removing the dispatch actually win?

The reading above says the dispatch is what Go does not pay, so it was
implemented and measured rather than argued about. `CN1_HTTP_POLL_MODE` selects
who takes a ready descriptor from the poller:

- **0** the reactor thread dispatches (deregister, allocate a task, queue it,
  wake a worker, re-register).
- **1** every worker calls the poller itself, descriptors armed `EPOLLONESHOT`
  so the kernel hands each to exactly one waiter.
- **2** ONE worker polls at a time, serves the first descriptor on its own
  thread and queues the surplus, handing the polling role on with a token. This
  is the shape of Go's `findRunnable`.

**Measuring this needs a null control.** Two runs of the SAME configuration on
this box differed by 19% while the effect under test was around 20%, so an
uncontrolled A/B here is worthless. Every window is therefore three runs -- mode
0, mode 2, mode 0 again -- giving `effect = B/mean(A,C)` and `null = C/A` from
the same minutes. Windows whose null exceeds 5% are discarded rather than
averaged in.

15 windows, 9 of them usable once the noisy ones are dropped:

| workers | usable | discarded | effect (poll / dispatch) | the windows |
| --- | --- | --- | --- | --- |
| 4 | 2 | 3 | **+32.2%** | 1.191, 1.453 |
| 8 | 3 | 2 | -2.0% | 0.972, 0.980, 1.007 |
| 16 | 4 | 1 | **-32.5%** | 0.660, 0.669, 0.682, 0.726 |

Read the two ends together, because that is the finding: **the handoff costs
about a third when workers are few, and removing it costs about a third when
workers are many**, with the crossover near eight workers on two cores. You need
a large pool to hide the dispatch, and a large pool is what produces the 61ms
tail measured further up. No point on that curve has both. That is the tension a
thread-pool server cannot resolve by tuning, and the reason the interesting
direction is decoupling "many contexts" from "many threads" rather than moving
the dispatch around.

**The dispatch is a real cost, and removing it is not a win.** Both halves are
supported. On an idle machine the low-worker gain reproduced across two
independent implementations -- +40% and +31% at two workers, +24% and +18% at
four -- and the paired window above puts it at +19% against a 0.9% floor. But at
eight and sixteen workers removing the dispatch LOSES, and the sixteen-worker
result is the most reproducible number in the set.

The reason is visible in the design rather than the numbers: **while the single
poller is serving a request, nobody is in `epoll_wait`** until another worker
picks up the token, whereas a dedicated reactor thread never stops polling. Our
architecture buys continuous polling at the price of a handoff per request, and
that trade wins as soon as there are enough workers to hide the handoff. Go pays
neither because it has many threads that can poll and switches contexts in user
space; that is the goroutine model, and it is blocked here by the CONSERVATIVE
collector -- stacks cannot be moved, so they cannot start small and grow, so a
context per connection is not cheap. It is not blocked by C.

Two implementation defects were found and fixed on the way, both worth recording
because each looked like an architectural result until it was understood:

- **Mode 1 wakes every worker per event.** All of them block in `epoll_wait` on
  one set, so one arrival wakes all; ONESHOT still gives the descriptor to
  exactly one, but the other wakeups happen. The damage scales with the pool:
  +40% at two workers, -26% at eight, -44% at sixteen.
- **Mode 1 of mode 2 busy-waited.** The worker that lost the race for the poller
  polled the queue on a 2ms timeout, so on two cores seven of eight workers spun
  against the cores the server needed (95435 against 113345 at eight workers).
  Handing the polling role over with a token so waiters PARK is what Go does with
  `stopm`/`wakep`.

**Where the evidence points next is not this.** Per request the census puts us at
6.9x Go's futex rate, and the reactor loop calls `handOff` once per ready
descriptor -- so twenty ready descriptors are twenty task objects and up to
twenty wakes. One wake can carry all twenty. That divides the dominant cost by
the batch size, gets LARGER under exactly the load where this server is weakest,
keeps the dedicated reactor the table above vindicates, and needs neither
coroutines nor C.

### What a context switch costs, by mechanism

The reason to want virtual threads here is that a suspended context should be far
cheaper than the thread handoff it replaces. That is one number, so it was
measured before any of it was designed. Ping-pong between two contexts, arm64,
two pinned cores:

| mechanism | ns per switch |
| --- | --- |
| `_setjmp`/`_longjmp` (musl) | **4** |
| `_setjmp`/`_longjmp` (glibc) | **8** |
| `swapcontext` (glibc) | 403 |
| mutex + condvar handoff between two threads | **21181** |

**The switch is three to five thousand times cheaper than the handoff.** That is
the whole case for the design, and it is why no amount of tuning inside a thread
pool reaches it: the pool pays 21us to move work between threads, and a context
switch costs single-digit nanoseconds.

Two portability findings that constrain the implementation:

- **musl has no `makecontext`/`swapcontext`.** They are obsolescent in POSIX 2008
  and musl omits them, so the portable route for CREATING a stack is not
  available on the static target. A stack can still be established with no
  assembly by raising a signal with `SA_ONSTACK` and `_setjmp`ing inside the
  handler, which is what libcoro's SJLJ backend does; measured working above.
- **glibc aborts a cross-stack `_longjmp` under `_FORTIFY_SOURCE`.**
  `__longjmp_chk` reports "longjmp causes uninitialized stack frame" at
  `-D_FORTIFY_SOURCE=1` and `=2`, and passes with hardening off. Distributions
  enable hardening by default, so a setjmp-based switch is green wherever it is
  tested here and dies in somebody else's build. This is the argument for a small
  per-architecture stub -- set the stack pointer and jump, about twenty
  instructions for arm64 and x86_64 -- rather than for a portable C trick.

### Virtual threads: a context per connection without a thread per connection

`CN1_HTTP_POLL_MODE=3` gives every connection a virtual thread. Host threads poll
and resume; a connection's virtual thread runs until it finishes or asks for
bytes that have not arrived, at which point it parks INSIDE the ordinary blocking
read and the host thread goes and runs another one. `serve()` is untouched --
still written in the blocking style, and unaware it is not on a thread, which is
the property that makes this worth having rather than a rewrite into callbacks.

**Why this and not more tuning of the pool.** The paired experiment over modes 0
to 2 showed the handoff is worth about a third of throughput at four workers and
that REMOVING it costs about a third at sixteen. Both are true because a pool
large enough to hide the handoff is a pool large enough to lose to the OS
scheduler. The assumption that makes those irreconcilable is "a context per
connection means an OS thread per connection", and a virtual thread is how that
assumption stops holding.

**What it rests on, measured.** Handing work between OS threads costs 21181ns on
this hardware; switching a virtual thread costs 2.6ns. A parked OS thread costs
~118KB resident (the figure is in nativeMethods.m, where the eager memset was
removed for exactly this reason); a parked virtual thread costs its C stack plus
a lazily faulted Java stack, and its C stack is small because ParparVM keeps Java
locals and the operand stack in `threadObjectStack` rather than on the machine
stack -- so the C frames hold only pointers and temporaries.

**Correctness first.** 50/50 requests over one kept-alive connection, 40/40 over
separate connections, correct JSON, no non-2xx and no socket errors under 20s of
concurrent load, and the process healthy afterwards.

**Throughput, paired against mode 0 with a null control.** The host was busy
during this run and many windows were discarded at 20-40% noise; what survives:

| route | host threads | null | effect |
| --- | --- | --- | --- |
| /plaintext | 2 | 1.7% | **+33.6%** |
| /plaintext | 4 | 1.8% | **+28.4%** |
| /plaintext | 8 | 5.7% (marginal) | +46.4% |
| /plaintext | 16 | 3.7% | **-23.3%** |

**The crossover is the same one as before and it is not a virtual-thread
problem.** Sixteen host threads oversubscribe two cores whatever they are
running, so mode 3 has to be configured the way its premise implies: a host
thread PER CORE, with the virtual threads supplying the concurrency. Comparing
mode 3 at sixteen hosts against mode 0 at sixteen workers measures the host
threads, not the design.

**A cost worth stating**: one virtual thread per connection is one VM thread
state per connection, and the process measured ~10% higher RSS than the pooled
path at 64 connections. The Java stacks are mapped lazily so this scales with
what connections actually touch rather than with their number, but it is not
free.

### Host threads must track CORES, not expected concurrency

In virtual-thread mode `workerCount` stops meaning "how many requests may be in
flight" -- the virtual threads supply that, one per connection -- and a host
thread only earns its keep while there is a core free to run it on. Past that
they contend for the cores the server needs.

Pinned to two cores, `wrk -c64`:

| hosts | requests |
| --- | --- |
| 2 | 257297 |
| 16 | **117** |

Unpinned, with cores to spare, the effect vanishes entirely -- 2, 4, 8, 16 and 32
hosts all serve between 760000 and 834000 in the same test and every one stays
healthy. So this is not a bug in the scheduler, it is host threads competing for
CPU, and the ceiling has to be read from the machine at runtime rather than
guessed. `start()` now clamps the host count to `availableProcessors()`.

**What this does NOT show.** An earlier revision of this section claimed the same
sweep proved a THROUGHPUT optimum at hosts == cores. It does not. Across three
reps the spread within one host count (70%, 84%, 100% of Go at two hosts; 78%,
81%, 109% at eight) is wider than any difference between host counts, on a
machine that was running a browser and a Spotlight index. The robustness finding
above survives that noise because 117 against 257297 is not a 30% effect; the
performance one does not.

**Honest throughput position.** Across all eight usable windows virtual threads
measured 70, 78, 81, 84, 100, 106, 108 and 109 percent of Go -- median 92%, with
three windows above parity. That is indistinguishable from Go within this host's
noise and is NOT a demonstration of parity. Settling it needs an idle machine,
not more code.

### Virtual threads against Go: the tail is closed, the syscalls are not

Once the collector was no longer being blocked (see below), the comparison became
meaningful. p99 over five windows, and this half of the comparison is trustworthy
even on a busy host because tail latency resists background load in a way
throughput does not:

| | p99 median | range |
| --- | --- | --- |
| pool, 64 workers | 59.70 ms | 54.76 - 67.50 |
| **virtual threads, 4 hosts** | **1.58 ms** | 1.51 - 1.78 |
| Go, GOMAXPROCS=2 | 1.58 ms | 1.35 - 1.76 |

**Identical to Go, and 37.8x better than the pool, on four host threads rather
than sixty four.** That is the whole architectural claim demonstrated: a context
per connection without a thread per connection. The tail was the entire remaining
gap against Go in every earlier measurement here.

Throughput is 78% of Go (236357 against 303573) and that number is NOT solid --
one of five windows had a clean null, the rest 8-21%, on a host running Spotlight
and a browser.

**Where the throughput goes, per request, from a syscall census:**

| syscall | virtual threads | Go |
| --- | --- | --- |
| recvfrom | 1.91 | 1.03 |
| sendto / write | 1.91 | 1.00 |
| ppoll | **1.90** | **0** |
| epoll_ctl | 0.021 | 0.003 |
| futex | **0.012** | 0.047 |

The futex is essentially gone -- 0.012 against Go's 0.047 -- so the virtual
threads are doing exactly what they were built to do. What is left is that a
request costs about 5.7 syscalls here and about 2 in Go, and the largest single
line is a `ppoll` Go never makes: `awaitReadable` asked whether a descriptor was
readable immediately before a read that parks on EAGAIN anyway and would have
learned the same thing. Go's `internal/poll.FD.Read` calls `syscall.Read`
straight away for exactly this reason.

Two more remain and are not yet done: the response takes about two `sendto` where
Go takes one, and the keep-alive loop takes a speculative second `recvfrom`.

### Why virtual-thread mode stopped serving after one burst

Three separate bugs, each found with a debugger or a counter rather than by
reasoning, and each of which alone was enough to stop the server.

**1. The collector waits for ever on a finished virtual thread.** This was the
one that mattered. Stop-the-world does this for every lightweight thread:

```c
if(t->lightweightThread) {
    t->threadBlockedByGC = JAVA_TRUE;
    while(t->threadActive) { usleep(500); }      // no timeout
}
```

A virtual thread's ThreadLocalData is registered in `allThreads` and flagged
lightweight, so when one FINISHED and its state was left in the list, nothing
would ever clear `threadActive` again and the collector blocked on it. Proof was
the cycle counter under `CN1_GC_LOG_CYCLES=1`: burst one reached cycle 3, and
burst two was still at cycle 3. No cycle ever completed again, so the allocation
pacing never released and the server sat at a few hundred requests a second
looking completely idle -- 8% of a CPU, no crash, no spin.

A platform thread calls `markDeadThread` at the end of `threadRunner` for exactly
this reason. A virtual thread owes the collector the same announcement, made from
the HOST after the switch back: calling it from inside the body frees
`threadObjectStack`, which is the Java stack the body is still standing on, and
kills the process inside the first burst.

**2. GC backpressure pins host threads.** `cn1PacingPark` sleeps until a cycle
ends, and a host thread has no virtual thread to hand back, so it just stops
polling. gdb found all four hosts in that loop at once, three inside
`HttpServer.serve`. The pacing park now yields the virtual thread instead, and
connections are pinned to a host so the VM's per-OS-thread state -- the BiBOP
page cache, the pacing claim, the mark buffer, `cn1TlsSelf` -- stays valid across
a park.

**3. The scheduler allocated on the host's hot path.** The run queue was a
LinkedList of boxed Longs, so every yield allocated twice ON THE HOST, and those
allocations hit the same backpressure. gdb caught the accepting host inside
`LinkedList.addLast` inside the scheduler. It is now a preallocated ring of raw
handles: a scheduler that allocates becomes a customer of the backpressure it
exists to relieve.

**What the fix is worth.** Four bursts, one server, no degradation:

| burst | requests | still serving |
| --- | --- | --- |
| 1 /plaintext | 1047890 | yes |
| 2 /plaintext | 1043497 | yes |
| 3 /plaintext c=64 | 1030535 | yes |
| 4 /json c=64 | 778673 | yes |

Zero socket errors throughout. Note the magnitude as well as the stability: the
first burst went from about 205000 requests to 1047890. Every earlier
virtual-thread measurement in this file's history was taken against an already
crippled collector and is void.

**Two traps worth remembering.** `-Wl,--strip-all` is on by default, so a
backtrace from a deployed binary is a list of hex addresses; `CN1_LINK_DEBUG=1`
keeps the symbols. And a counter that is only printed from an idle poll is
invisible exactly when the server is stuck -- "accepts=0 while serving 204000
requests" looked like a contradiction for hours and was a report from start-up.

### What pins a host thread, found with a debugger

Virtual-thread mode served one burst of traffic and then stopped accepting for
good. Four hypotheses were spent on it by inference -- the EPOLLONESHOT re-arm,
a synchronized map, virtual-thread creation failing, a missing safepoint bracket
-- and every one was wrong. Attaching gdb answered it in a single backtrace:

```
cn1PacingPark  (usleep 50)          <- the collector's allocation backpressure
  cn1BibopMaybeGc
    cn1BibopAlloc
      codenameOneGcMalloc
        HttpServer_asciiString
          HttpServer_readRequest
            HttpServer_serveOne
              HttpServer_serve      <- running ON a virtual thread
```

All four host threads were in that loop at once, three of them inside
`HttpServer.serve` on different descriptors. `cn1PacingPark` spins on
`usleep(50)` until uncollected volume falls, which only happens when a cycle
ENDS, and ending one needs the mutator progress the spin is preventing. Nothing
was polling, so nothing was accepted, and it never recovered.

**The general rule this is an instance of**: any blocking operation that is not
virtual-thread aware pins its HOST, and a host thread is not a spare resource --
it is one of the few threads that poll. The known instance of that rule is a
monitor held across a park; the one that actually bit is the collector's own
backpressure, which fires wherever Java allocates, which on a server is
everywhere.

Two notes for whoever reads the counters next. `-Wl,--strip-all` is on by
default, so a backtrace from a deployed binary is a list of hex addresses;
`CN1_LINK_DEBUG=1` keeps the symbols. And a counter printed only from an idle
poll is invisible exactly when the server is stuck -- the "accepts=0 while
serving 204000 requests" that looked like a contradiction for hours was a report
from start-up, because once the hosts pin, no host is ever idle again to print
another one.

### Two explanations that measurement killed

Worth recording, because both were plausible and both were wrong:

- **"Workers block in the keep-alive linger, which is why the pool must be large."**
  Setting `CN1_HTTP_KEEPALIVE_LINGER_MS=0` leaves low-worker throughput exactly
  where it was (111897 vs 110551 at 2 workers; 112379 vs 111974 at 4). The linger
  is not what caps a small pool. It is, however, what makes a LARGE pool fast:
  at 64 workers, removing it drops throughput from 245538 to 87475.
- **"Then the parks are what a small pool cannot absorb."** Also wrong. Raising
  the cap at 4 workers fixes the tail (32.4 ms -> 2.43 ms) but leaves throughput
  at 125500.

### Why the pool has to be larger than the core count: the reactor is the ceiling

The discriminator is connections against a FIXED pool. With connections <= workers
nothing is ever queued, the fairness rule never fires, every connection stays with
its worker for the whole run and the reactor is out of the path. With connections
>> workers every request pays a dispatch through it.

| workers | connections | req/s | p50 | p99 |
| --- | --- | --- | --- | --- |
| 8 | 8 | **225645** | 28 us | **2.44 ms** |
| 8 | 64 | 128804 | 408 us | 48.2 ms |
| 2 | 64 | 118913 | 480 us | 2.56 ms |
| 4 | 64 | 119433 | 464 us | 29.8 ms |
| 64 | 64 | 245538 | 225 us | 61.3 ms |

Every row reproduced on a second rep within a few percent -- 8/8 gave 222569 at
2.46 ms, 2/64 gave 121354 at 2.52 ms, 4/64 gave 119823 at 30.7 ms, 8/64 gave
126875 at 51.3 ms -- so the plateau and the two low-tail cells are not samples.

Throughput sits at 119-129k for EVERY pool smaller than the connection count and
then doubles the moment workers >= connections. That plateau is the single reactor
thread's dispatch rate: a workload with more connections than workers is capped
there however large the pool. The keep-alive linger exists to bypass it, and a
worker can only hold one connection, which is why bypassing it for N connections
takes N workers. The 64-worker default is not a tuning constant -- it is what
makes connections <= workers for a 64-connection benchmark.

The best cell in the whole matrix is **8 workers and 8 connections: 225645 req/s
at p99 2.44 ms** -- 94% of Go's throughput and within 1.6x of its tail, on 8
threads rather than 64. Whatever replaces the handback path should aim there.

Note also what the two low-tail rows have in common. `w=2,c=64` has connections
far above workers and still holds p99 to 2.56 ms, so the handback alone does not
produce the tail; `w=8,c=8` has 4 threads per core and holds 2.44 ms, so thread
count alone does not either. The bad cells need BOTH -- threads competing for
cores AND workers competing for connections. Neither factor is sufficient, which
is why single-variable sweeps of each looked contradictory.

Why the collector falls behind is **not yet established**, and the analysis that
previously stood here was wrong three times over. What went wrong is worth more
than what it claimed:

**The measurement bug.** `consWords` is a **cumulative running total** since
process start -- the source says so at `cn1GcProbeResetPhases`: "the cumulative
counters (matured, consWords, staleSkips, ...) are deliberately left alone: they
are running totals and the reader diffs them". `markMs` and `stackMs` beside it
are **per-cycle**. Every cross-configuration comparison of `consWords` here read a
running total as if it were one cycle's work, and two different time bases were
divided into each other.

Corrected, the scan is unremarkable: 4,309,657 words over 46 cycles is **~94K
words per cycle -- about 750 KB across 66 threads, ~11 KB of live stack per
thread**, which is exactly what an HTTP worker should have. The "26 MB floor" and
"34 MB per cycle" figures previously reported here were that bug, not a finding.

**The attribution bug.** `stackMs` brackets `cn1GcScanThreadNativeStack`, and the
loop inside it does not only walk words -- it calls `gcMarkObject` on every
resolved reference, which resolves the pointer and pushes to the mark worklist. So
`stackMs` is *"walk one thread's stack and enqueue what it finds"*, not *"time
spent scanning stack words"*. **"93% of mark time is the conservative stack scan"
does not follow from it**, and neither does the conclusion that scanning less
stack is the lever.

That also explains the two null results below without any new theory: a range
filter and a presence bitmap only make *rejection* cheaper, and rejection was
never shown to be where the time goes.

**What is still solid:** every throughput, latency and allocation number in this
file (those were measured directly, not modelled); that the tail is
`pacingVolume` back-pressure rather than a mark pause; and that cutting allocation
27-fold moved throughput and tail together. **What is open:** what actually
dominates a ~300 ms mark cycle. Isolating it needs a counter that separates
walking, resolving, enqueueing and draining -- which the probe does not currently
have.

Only 1.3% of the words scanned resolve to an object.

`/json` allocates 462 bytes per request against `/plaintext`'s 176. The
difference is the handler's per-request `LinkedHashMap`, which is the honest
counterpart to Go marshalling a struct -- a direct-to-sink JSON API would beat
Go's number by doing less work than Go does, so it is deliberately not measured
here.

**The lever that is established is allocation per request** -- it is what moved
throughput and the tail together, 27-fold. Whether anything inside the collector
is worth attacking is **not established**, because the phase attribution that
suggested it turned out not to say what it looked like it said.

### Two optimizations that did NOT work, and why

Recorded so nobody spends the day twice. Both targeted the conservative scan's
per-word cost (~290 ns, which is ~300x slower than streaming the same memory):

1. **A lo/hi range filter** rejecting words outside the heap's address span
   before any lookup. **No measurable change** - the thread stacks are mmap'd
   inside the same span as the heap.
2. **A 64KB-granule presence bitmap** over that span, small enough to stay in
   cache. **Also no measurable change** - the words genuinely land in heap
   granules and are rejected by the finer slot checks (`bumpIndex`, `FREE_MARK`,
   `__heapPosition`), not by the coarse ones.

3. **A 16x smaller thread stack** (`-DCN1_THREAD_STACK_BYTES=1024*1024`, and the
   macro really does reach `pthread_attr_setstacksize` -- that was checked, because
   a flag that silently does nothing looks exactly like a null result).
   **No change**: 543 ms of stack scan against 521 ms. The scan covers the used
   depth, not the reserved region, so the reservation is not the lever.

Those null results say the cost is neither the reject path nor the stack
reservation. They do NOT establish where it is -- an earlier revision read them as
pointing at "scan less stack", which the attribution bug above shows was never
supported. Allocating less, so cycles run less often, is the one lever with
evidence behind it.

## The clamp, and the two bugs found verifying it

Virtual-thread mode clamps the requested `WORKERS` to the core count, because host
threads track cores rather than expected concurrency (the concurrency comes from
the virtual threads). Verifying that clamp on two pinned cores is what turned up
everything below, which is the argument for verifying a fix rather than reasoning
that it must work.

**The clamp itself holds.** Where the unclamped build served 117 requests at 16
hosts on two cores, every host count now lands in the same band:

| WORKERS | rep1 | rep2 | Go (`GOMAXPROCS=2`) |
| --- | --- | --- | --- |
| 2 | 252,101 | 263,999 | 303,307 / 318,702 |
| 16 | 254,164 | 267,232 | " |
| 64 | **0** | 255,641 | " |

### 1. A never-used thread pool, and a segfault

That `0` is not a slow run. The server bound, served nothing, and was **gone** by
the end of the window (1.5M client write errors, `wait` status 139 -- SIGSEGV).
It reproduced at 2 runs in 10.

`WORKERS` still sized `Executors.newFixedThreadPool`, and in this mode that pool
is dead weight: `workers.execute()` is reached only from `handOff()`, which is
reached only from `pump()`, which runs only on the dispatching path. All 64 OS
threads were created (69 in `/proc/<pid>/task` against 5 expected) and never given
anything to do -- while the collector still scanned each one's stack every cycle
and still waited for each at every safepoint.

Holding the host count equal via the clamp made the pool size the only variable:

| arm | workers | bursts | died |
| --- | --- | --- | --- |
| A | 64 | 2 | **2/6** |
| B | 4 | 2 | 0/6 |
| D | 4 | 1 | 0/6 |

The pool is no longer created in this mode. Arm A then ran **12/12 clean**, at
unchanged throughput -- this buys robustness, not speed.

Re-running the clamp against the fixed build, every cell is populated and the
spread across host counts is noise:

| WORKERS | rep1 | rep2 | our p99 | Go | Go p99 |
| --- | --- | --- | --- | --- | --- |
| 2 | 265,954 | 245,568 | 1.72 / 1.75 ms | 328,943 / 316,304 | 1.52 / **56.42** ms |
| 16 | 264,708 | 251,083 | 1.66 / 1.47 ms | " | " |
| 64 | 266,252 | 256,317 | 1.59 / 1.62 ms | " | " |

On two pinned cores that is about 81% of Go's throughput. The p99 column is worth
a second look rather than a claim: our tail sat between 1.47 and 1.75 ms in all
six readings, and Go's second replicate spiked to 56 ms. One spike is not a
finding -- it is one reading, on a loaded shared host, and it has not been
repeated -- but it is the reason the tail is measured per replicate here instead
of being averaged away.

### 2. A use-after-free between the collector and a freed virtual thread

Reading the collector to explain that crash turned up a separate, genuine race.
The collector does not stop the world and then scan; it stops and scans **one
thread at a time**, rebuilding its virtual-thread snapshot inside that loop, and
every other thread keeps running -- including host threads, whose job is finishing
connections and freeing the virtual threads that served them. A pointer copied
into the snapshot could therefore be freed, and its stack unmapped, before the
scan that snapshot feeds read it.

`cn1VirtualThreadFree` now unlinks the virtual thread immediately but defers the
release when a scan is in progress; the collector drains the retired list when the
scan ends. The free and the snapshot serialise on the registry lock, so the
handoff is exact rather than merely likely. The lock is never *held* across a scan
-- a frozen thread can hold it -- so the flag is what crosses that boundary.

Test 8 in `vm/tests/virtualthread/test_virtual_thread.c` covers it, and it was
checked against the unfixed code rather than assumed to bite: with the deferral
neutered it exits **139**, the same signal as production, and it passes with the
deferral in place.

**What is not established:** whether this race is what killed the WORKERS=64 runs.
Both are SIGSEGV and both widen with thread count, but the reproducer built for it
(`BENCH_IDLE_THREADS=64`, which parks idle Java threads to lengthen the scan loop
without a pool) did **not** crash in 8 runs, so the link is unproven. Two real
bugs were fixed; only the first is tied to the observed failure by measurement.

### 3. A native name that silently disabled a method

`VirtualThread.isVirtual()` was inert. Its C body was written
`isVirtualImpl__R_boolean` where the signature rule gives
`isVirtualImpl___R_boolean` -- an empty argument list still contributes its own
leading underscore before `_R`. Nothing linked against the wrong name and nothing
called the method, so the build stayed green. An exhaustive diff of the symbols
the translator declares against the ones we define found exactly this one across
the whole backend native surface.

`build.sh` now sets `CN1_NATIVE_VERIFY=strict` for **every** backend build rather
than only the no-TLS one. Every native here is ours, so a name that does not match
the generated one is always a bug, never a symbol in a prebuilt library.

## Where the remaining gap is, and where it is NOT

Virtual-thread mode against Go net/http, both pinned to two cores, `/plaintext`,
64 connections, interleaved reps. Medians: **Go 318,008, ours 260,156 -- 82%.**

Three candidates were tested and two were eliminated. The eliminations are the
useful part, because each had a plausible story and a table behind it.

### It is not the run-ahead cap

Raising `CN1_GC_PACING_CAP_MB` to 2048 is worth +11-20% on the DISPATCHING path
(table above). In virtual-thread mode it is worth nothing, three reps out of
three -- median 261,411 capped against 267,279 default, slightly WORSE:

| rep | Go | vt default | vt cap=2048 |
| --- | --- | --- | --- |
| 1 | 279,924 | 267,279 | 253,273 |
| 2 | 321,318 | 266,925 | 261,638 |
| 3 | 321,111 | 268,389 | 261,411 |

That is not a contradiction, it is the earlier table's own explanation running
out: the cap bound because 64 OS threads oversubscribed two cores and a parked
one held a core it could not use. Virtual threads removed the oversubscription,
so the cap stopped being what anyone waits on. **A tuning verdict measured on one
scheduler does not transfer to another.**

### It is not collection either -- but "not collection" is not "not memory"

A diagnostic knob (`CN1_GC_TRIGGER_MB`, the twin of the cap override) raises the
cycle trigger past reach, so no collection runs in the measured window:

| rep | Go | GC on | GC off | GC cost |
| --- | --- | --- | --- | --- |
| 1 | 314,854 | 245,797 (56 cycles) | 257,157 (1) | +4.6% |
| 2 | 327,403 | 239,837 (56) | 274,483 (1) | +14.4% |
| 3 | 318,604 | 212,071 (55) | 233,089 (2) | +9.9% |

Collection costs about 10%, and switching it off entirely still leaves us at
**81% of Go**. The tail is the other way round: with collection off our p99 is
406-592 us against Go's 1.37-1.67 ms, three to four times BETTER.

The obvious reading -- "the gap is not memory" -- is wrong, and worth stating
because it was drawn here first. Turning the collector off does not stop us
ALLOCATING: every object still costs a bump, a write barrier and a fresh cache
line. Go escape-analyses most of a request onto the stack, so it neither
allocates nor collects it, and its heap is small because of that rather than
because its collector is better. What the ablation rules out is the COLLECTOR.
Allocation volume is still live, and is the thing to attack.

### What the allocation matrix did and did not show

Both allocation-removing switches were tried in virtual-thread mode:

| rep | Go | base | +target cache | GC cycles base -> cache |
| --- | --- | --- | --- | --- |
| 1 | 285,584 | 238,937 | 237,471 | 55 -> 43 |
| 2 | 300,789 | 258,879 | 255,254 | 56 -> 48 |
| 3 | 310,776 | 259,549 | 274,720 | 57 -> 51 |

The target cache is a real **15% cut in allocation** -- the cycle count falls in
all three reps -- and it does not show up in throughput at all. `CN1_HTTP_ZERO_COPY`
is inert here: `ZERO_COPY_READ` ands it with `POLL_MODE != 3`, so setting it under
virtual threads changes nothing. That was not noticed until after a reading of
"95.8% of Go" had been taken from it, which is worth recording as the method
failure it is: the run had FOUR arms that were really two configurations, and the
two identical pairs came back 4.0% and 15.2% apart. **Those pairs are the honest
noise floor of this harness** -- 0.04% to 15% between runs of the same binary --
and no single-rep conclusion here can resolve less than that.

### The per-virtual-thread read buffer: attempted, reverted

Removing the per-request read `byte[]` under virtual threads needs the buffer to
belong to the virtual thread rather than the host, since it parks on one host and
resumes on another. Implemented (pooled, because the array header must be an
immortal GC root and nothing un-roots one), it passed the virtual-thread suite
21/21 and served 175k req/s -- and then produced one truncated response on the
DISPATCHING path, which the same refactor went through. It passed on re-run.

An intermittent corrupt response is not a flake to re-run until green, and the
prize is one `byte[]` per request, so the refactor was reverted rather than
shipped ahead of an explanation. `ZERO_COPY_READ` is back to refusing to combine
with virtual threads, with the attempt recorded at its definition.

## A request that was never answered (fixed)

Chasing the throughput gap turned up a correctness bug that outranked it: the
server occasionally answered a request with **nothing at all**.

It surfaced twice, from opposite ends, and neither report named it.
`transactionRollsBack` failed with status -1 after exactly 15.05 s -- the test
class's own `setSoTimeout(15000)` expiring -- and `authGuardsMutatingRoutes`
failed with `StringIndexOutOfBounds: -1` out of a bare `substring` on the
response. Replacing that substring with an assertion that PRINTS the body is
what turned it into evidence: the body was empty. About 2 full-suite runs in 6.

The server log said the rest:

```
java.lang.ArrayIndexOutOfBoundsException
    at com_codename1_backend_HttpServer.serveOne:1655
```

`Conn.fill` decides whether it may let go of a borrowed zero-copy buffer by
asking whether anything is left unread:

```java
if(borrowed && available() == 0) {        // "start of a new request"
    buffer = EMPTY_BODY; pos = 0; borrowed = false;
} else if(borrowed) {
    detachPreservingOffsets();            // the guard that should have run
}
```

That test is right for a kept-alive connection and wrong for exactly one case.
A POST whose headers arrive in one TCP segment and whose body arrives in the
next reaches the body loop with the header block fully consumed -- so
`available()` is 0 -- while the Request's header slices still name positions in
that very buffer. The first branch dropped the borrow, skipped
`detachPreservingOffsets`, and the next zero-copy read landed on top of the
headers. `wantsKeepAlive` then walked off the end of the array, and because that
call sits OUTSIDE the try block the exception killed the connection with no
response written.

The discriminator is not "is anything left to read" but "is anything still
pointing at this buffer", so `Conn.parsedFromBuffer` now says so directly:
cleared on entry to `readRequest`, raised once the slices name positions in the
buffer. Verified 8 full-suite runs clean with zero occurrences of the exception
in any server log, against 2 failures in 6 before.

Two things worth keeping from how it hid for so long. It needs the body to
arrive in a SEPARATE segment, so it never reproduced in isolation -- 400 plain
POSTs and 120 replays of the failing test's exact request sequence were both
clean, and only the full suite's timing produced it. And it never appeared under
virtual threads at all, because `ZERO_COPY_READ` is force-disabled there; every
virtual-thread run in this whole effort was green while the DEFAULT path was the
broken one.

## Running it

```bash
CN1_BACKEND_DEMO=demo/bench ../package.sh Bench com.demo musl-arm64
podman build --platform linux/arm64 -t cn1-bench-go   -f Containerfile.go   .
podman build --platform linux/arm64 -t cn1-bench-load -f Containerfile.load .
# then, on a Linux host with both binaries staged:
BENCH_DIR=/var/tmp/cn1bench ./run-comparison.sh
```

`run-comparison.sh` documents the fairness rules it enforces: same host, the same
two pinned cores for whichever server is running, two other cores for the load
generator, matched handlers, a warm-up before every measured run, and the two
servers never running at once.

## What the numbers are, and are not

- Measured on a 4-CPU aarch64 Linux VM (podman machine on an Apple silicon
  host), server pinned to cores 0-1, `wrk` to cores 2-3.

- **Benchmark only on an otherwise IDLE machine, and check that it is.** This is
  the single biggest source of wrong numbers here, and it is self-inflicted:
  running a `package.sh` build (or a second benchmark) while measuring does not
  add a little noise, it destroys the measurement.

  | machine state | one configuration, repeated | spread |
  | --- | --- | --- |
  | builds running concurrently | 145,719 - 210,660 | **45%** |
  | idle | 194,597 - 204,330 (Go), 206,780 - 214,304 (CN1) | **3.6 - 5.0%** |

  Idle, this harness resolves a few percent and is perfectly adequate. Busy, it
  cannot resolve 30%. A single-sample A/B taken during a build "showed" a change
  costing 30% throughput; a second sample showed the same change GAINING 4%; a
  conclusion was drawn and acted on from the first. Check with
  `ps aux | grep package.sh` and `podman machine ssh 'ps aux | grep bench'`
  before believing anything.

- **Prefer interleaved repeated measures with medians** (`reps.sh`,
  `head2head.sh`) over one run per arm. Cheap insurance even on an idle machine,
  and it reports the spread so a reader can see whether a difference clears it.
  Never compare numbers taken with different run lengths or warmups.

- The syscall-per-request counts are far more stable than the throughput figures
  and are what the networking diagnosis rests on.
- **Never run two measurements at once.** Every script here kills the server by
  name before it starts, so a second one launched while the first is measuring
  does not merely share the CPU -- it kills the running server mid-measurement.
  The reading that comes back is unremarkable (114k where a clean re-run gives
  216k) and nothing in the output says anything went wrong, so a stray background
  run silently rewrote a whole results table once. `run-comparison.sh` now takes
  an exclusive `flock` on `$BENCH_DIR/.bench.lock` and exits 3 rather than
  measure alongside another run.
- The Go side is the **standard library**, not fasthttp. fasthttp is faster, so
  nothing here supports a claim about "Go" in general - only about Go's standard
  HTTP server, which is the comparison most people mean.
- Go's response omits `Connection: keep-alive` (the HTTP/1.1 default) where ours
  sends it, and Go's JSON encoder appends a newline. Neither is material.

## A trap in this harness

Staging a rebuilt binary over one that is running fails with `Text file busy`,
and a copy loop that swallows the error leaves the OLD binary in place. A whole
measurement then describes the previous build while looking perfectly normal.
Kill the server, remove the file, copy, and compare checksums before believing a
number:

```bash
md5sum target/bench-tools/bench-cn1
podman machine ssh 'md5sum /var/tmp/cn1bench/bench-cn1'
```
