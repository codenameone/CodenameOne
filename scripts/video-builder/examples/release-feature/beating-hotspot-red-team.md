# Beating HotSpot Video Red Team

## Verdict

REWRITE, resolved before the replacement render.

The technical thesis survives, but the original publication package did not. It duplicated the
full landscape story in portrait, ended without a designed next step, and had no machine-checkable
plan for end screens, cards, comment prompts, or publication order.

## Thesis

ParparVM closed a 4.21x starting geomean gap against warmed Java 25 to 1.00x across the ten measured
benchmarks by combining closed-world code generation and runtime changes, while retaining explicit
client-memory tradeoffs and measurable remaining weaknesses.

## Ranked failures and resolutions

1. **The portrait upload offered no new value.** It repeated the full landscape edit and treated
   vertical framing as syndication. Resolved with a separate, shorter script built around the result,
   three mechanisms, one limitation, and a related-video handoff to the complete benchmark.
2. **The ending stopped instead of converting attention into intent.** Resolved with an
   `outro.show` action that names the source, asks one bounded technical question, and reserves
   uncluttered space for native YouTube end-screen elements.
3. **Engagement settings lived outside the package contract.** Resolved with schema version 2 of
   `youtube.json`: per-orientation metadata, comment prompt, cards, end-screen timing and elements,
   and the Short-to-landscape related-video target are validated before upload.
4. **“After one pull request” overstated the unit of change.** The performance work and follow-up
   wrapper-lock safeguard are not one literal change. Resolved to “after one optimization series.”
5. **The memory result was vulnerable to a false equivalence.** The narration now preserves the
   distinction between a 2.4 MB steady floor, 290–390 MB native churn, and a 508 MB fixed JVM heap.

## Hostile comments the video must survive

### “Geomean parity hides the workloads where ParparVM still loses.”

It survives. The limitations scene names recursive Fibonacci at 1.6x and tight arithmetic at
1.07–1.12x instead of implying universal dominance.

### “This is a macOS benchmark, so it proves nothing about iOS.”

It survives only with the scope stated precisely. HotSpot does not run on iOS; the benchmark tests
generated C on Apple Silicon macOS. The portable claim is about the code-generation mechanisms, not
that every macOS timing or memory number transfers unchanged to every Apple target.

### “Tagged integers violate Java identity and locking semantics.”

It survives. The video presents tagged integers as a tradeoff, not a free optimization, and names
the build-time rejection of synchronization on wrapper objects.

## Boldness ruling

The title may say ParparVM reached HotSpot performance because the measured ten-benchmark geomean
is 1.00x after optimization. It must not say ParparVM is universally faster than HotSpot or imply
that the result covers arbitrary applications.

## Highest-leverage change

Make the Short a focused discovery asset whose payoff is the mechanism and whose next action is the
full benchmark. That creates a real two-video journey instead of publishing the same video twice.
