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


## ToCharArrayScan

Answers "how many `String.toCharArray()` sites never needed the array".

```
javac -cp <asm> -d . ToCharArrayScan.java
java  -cp .:<asm> ToCharArrayScan <classes-dir> [<classes-dir> ...]
```

`toCharArray()` is **22.8% of all char[] allocations** on the self-hosting corpus --
293,175 of 1,285,250 -- and most of those arrays exist only to be scanned. The worst
single offender is the translator's own `Parser.encodeStringSlashU`, which materialises
the array, reads it in a loop, and in the common case returns the original String and
throws the copy away.

**Returning the backing array uncopied is NOT the fix.** That makes a String mutable
through its own accessor. `com.codename1.io.Util.toCharArray` exists precisely because
some JVMs did exactly that, calls it "a serious security hole in the JVM", and DETECTS
it at runtime with `s.toCharArray() == s.toCharArray()` -- an expression whose result
the change would also flip.

The safe transformation is to not build the array at all: where the result is stored to
a local whose every use is `CALOAD` or `ARRAYLENGTH`, each `a[i]` is `s.charAt(i)` and
each `a.length` is `s.length()`. No array exists, so nothing can alias or mutate one,
and on a compact string `charAt` is a byte load and a mask.

### Measured

| corpus | sites | elidable | escapes | mutated | not stored to a local |
|---|---|---|---|---|---|
| ByteCodeTranslator + ASM | 2 | 1 | 0 | 1 | 0 |
| CN1 framework core | 36 | 5 | 7 | 0 | 24 |
| vm/JavaAPI runtime | 11 | 3 | 1 | 1 | 6 |

**Nine elidable sites in total**, so this is a small, cheap pass rather than a large
one -- and the honest caveat is that static site counts do not predict dynamic volume:
ONE of those nine (`Parser.encodeStringSlashU`) accounts for all 293,175 calls on the
self-hosting corpus. A hot scan loop is invisible until it is not.

### Validated

The classifier is checked against a hand-written class with one case per bucket
(read-only for-each, indexed scan, returned, `CASTORE`d, passed straight to a call) and
reports exactly 5 sites / 2 elidable / 1 escape / 1 mutated / 1 not-stored. A scanner
whose buckets nobody has watched fill is not a measurement.
