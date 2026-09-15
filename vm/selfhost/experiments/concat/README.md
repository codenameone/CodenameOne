# Fusible string-concatenation detector

Answers "how much of the StringBuilder churn is mechanically removable?" for a
given corpus, without changing anything.

```
javac -cp <asm> -d . ConcatScan.java
java  -cp .:<asm> ConcatScan <classes-dir> [<classes-dir> ...]
```

## Why this exists

The emit phase of a ParparVM translation is StringBuilder-bound, and the census
of a self-hosted run showed 1.07M live `StringBuilder` and 3.98M `char[]`.
Two fixes already landed against SPECIFIC call sites (a reused emit buffer in
`ByteCodeClass.generateCCode`, and `Util.mangle` memoizing 49 hand-written
copies of `x.replace('/','_').replace('$','_')`). Neither GENERALIZES: they help
the translator and no translated app.

This scanner measures the generic version of the same waste -- the javac
`a + b` idiom -- so a translator pass can be justified and sized before it is
written.

## Measured

| corpus | sites | fusible | escapes | branch | appends |
|--------|-------|---------|---------|--------|---------|
| hellocodenameone app + macPort (5782 classes) | 3058 | 2573 (84%) | 417 | 68 | 8103 |
| ByteCodeTranslator's own classes | 563 | 386 (69%) | 167 | 10 | 1286 |

Fusible chain lengths (hellocodenameone): 2 appends 1289, 3 616, 4 226, 5+ 420.

## What a fusion pass would have to preserve

The scanner deliberately reports escape/branch cases rather than assuming them
away, because a naive fusion is wrong in each:

- `append(null)` renders "null"; a fused helper must too.
- `append(char)` and `append(int)` are DIFFERENT renderings of the same JVM
  int-shaped value, so the fused signature has to carry the static type.
- `append(Object)` goes through `String.valueOf` -> `toString()`, which is
  arbitrary user code: it can throw and can have side effects, so argument
  evaluation ORDER must be preserved exactly.
- A builder stored to a local/field/array or returned is live past the chain
  (414 sites here) and must be left alone.
- A branch inside the chain (68 sites) means the appends are conditional and the
  chain is not a straight line.

## Status

Detector only. No translator pass is wired to it yet.
