# Microbenchmarks for the generated-C cost items

Whole-program timing on this host cannot resolve anything under about 5%, so the
items in `../GENERATED-C-REPORT.md` are settled here instead: the mechanism in
isolation, with the arms interleaved inside one process.

```sh
source tools/env.sh
clang -O3 -w -fwrapv -fno-strict-aliasing -o bench_alloc   bench_alloc.c   && ./bench_alloc
clang -O3 -w -fwrapv -fno-strict-aliasing -o bench_call    bench_call.c    && ./bench_call
clang -O3 -w -fwrapv -fno-strict-aliasing -o bench_barrier bench_barrier.c && ./bench_barrier
```

Run each binary several times: a single run of the harness resolves about
0.25ns, which is the size of the smaller effects here, and code layout alone
moves numbers by that much. Aggregate the minimum across runs.

**Read the assembly too, and not as an instruction count.** `bench_barrier`'s
arm C removes 22 instructions and 6 loads and is 0.003ns different, because the
work it removes is off the dependency chain. What shows up in the clock is
ordering primitives, calls, loads on an address chain, and anything that forces a
frame. Dump it with `-S` and look at what feeds what.

**An arm that reports 0.000 ns/op is a bug, not a result.** It means the
optimizer folded the calls away -- a pure function called with identical
arguments. Every arm here takes the iteration index for that reason; verify with
`grep -c '^\s*bl' ` over the loop body in the `-S` output.
