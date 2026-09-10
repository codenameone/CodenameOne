# September 11 release series: editorial and evidence review

**Verdict: SHIP for editorial review.** The package contains one parent article and eleven deep dives dated September 11–17, 2026. It is written in the repository; publication has not been triggered. This review assesses the copy's evidence and usefulness, not predicted traffic.

The parent thesis: **benchmarks that omit misses, cold paths, and memory after allocation bursts can hide the costs users actually encounter.** Codename One's shared runtime and builders give the team a place to fix those costs without multiplying application-specific implementations.

Baseline: last week's `voip-vpn-builders` parent, dated follow-ups, matching `series` front matter, future-aware `post-link` navigation, concrete code and tradeoffs, and discussion prompts. The marketing-context, blog-writer, engineering-narrative, positioning-redteam, and copy-polish skills informed the work. Copy polish used the standard level. Social content was outside the requested series.

## Reading order

| Date | Article source |
| --- | --- |
| 2026-09-11 | [The Performance Bugs Our Benchmarks Missed](../content/blog/performance-work-between-benchmarks.md) |
| 2026-09-12 | [A Small Heap Does Not Need a Large Garbage Budget](../content/blog/parparvm-gc-small-heaps.md) |
| 2026-09-12 | [Our HashMap Was Fast Until the Key Was Missing](../content/blog/hashmap-misses-probe-sequence.md) |
| 2026-09-13 | [A Cache Should Forget Cold Images, Not Every Image](../content/blog/parparvm-ranked-soft-references.md) |
| 2026-09-13 | [Three Pointer Bits Make Boxed Numbers Cheaper](../content/blog/parparvm-tagged-boxed-values.md) |
| 2026-09-14 | [Why Was a Margin Calculation Waiting on AppKit?](../content/blog/startup-cost-before-first-paint.md) |
| 2026-09-14 | [One Blocking run() Made Unrelated JavaScript Methods Suspend](../content/blog/javascript-suspension-receiver-types.md) |
| 2026-09-15 | [The Process Disappeared. The User’s Work Should Not.](../content/blog/continuity-restoring-work.md) |
| 2026-09-15 | [A Drag Is a Clipboard Payload With a Destination](../content/blog/native-drag-drop-clipboard.md) |
| 2026-09-16 | [We Stopped Embedding a Second Website Inside Our API Docs](../content/blog/javadoc-hugo-markdown-doclet.md) |
| 2026-09-16 | [Read the Key File You Have. End the Task You Meant to End.](../content/blog/pem-keys-and-clearing-recents.md) |
| 2026-09-17 | [API 37 Readiness Starts Before the Target SDK Switch](../content/blog/android-37-readiness-location-button.md) |

## Blocking claims removed or corrected

| Severity | Draft claim at risk | Evidence and final treatment |
| --- | --- | --- |
| Blocking | A live-set-proportional GC floor shipped as the default | PR #5717's opening description is stale relative to its merged source. `cn1_globals.m` explains why incomplete sweep counters made that policy unsound. The articles state that the floor is deployment-selected and the stock floor remains 24 MB. |
| Blocking | GC now matches Go and keeps memory flat through everything | The recorded median and p99 matched in one loop; the worst pause did not. Four-marker ParparVM pauses were 0.3–0.9 seconds versus Go's 20 ms. The lower-memory backend configuration is distinguished from a stock mobile build. No invented time-series curve is shown. |
| Blocking | PR #5722 adds Swiss-style SIMD group probing and proves Go map parity | The merged table loop uses scalar perturbed probing. Compact metadata and the native string comparison path are explained separately. No comparable Go timing table was found in the supplied PR, so no equality/superiority claim is published. |
| Blocking | Every image cache now uses ranked soft references | PR #5732 explicitly leaves the iOS strong-reference table and framework call sites for separate integration. The article distinguishes VM semantics, benchmark evidence, and prospective cache migration. |
| Blocking | All boxed primitives are stack allocated | Tagged words are not general stack allocation. Long and Double have explicit encoding limits and heap fallback. Byte and Boolean already have bounded caches. |
| Blocking | API 37 support proves all Android 17 behavior and cloud rollout | Compile gates, generated-app assembly, manual emulator location testing, regular API 36 CI, and the separately managed BuildDaemon changes are reported distinctly. No general target-SDK deadline is invented. |
| Blocking | Site search includes the full developer guide | The live search page explicitly excludes that document. Copy now says that API types/members are indexed and the guide retains its own navigation/browser search. |
| High | Security superiority is established by the release | No comparative security assessment exists here. The close names validated boundaries: key container checks, explicit restoration shutdown, permission injection, and OS-owned consent. It does not certify an app or imply task removal revokes credentials. |
| High | Java 25 introduced Markdown documentation comments | Tooling uses Java 25; Markdown comments arrived in JDK 23. The article links Oracle's documentation and explains the distinction. |
| High | Native drag support covers every desktop port and was exercised by hand | The matrix distinguishes JavaSE and Catalyst from native AppKit/Windows/Linux. The supplied PR explicitly lacks a physically driven drag test; that limit is included. |
| High | The JavaScript optimization improved every workload | The 13.7% iterator regression is included beside the wins, with its unresolved cause. The source-size reduction is not used as a proxy for throughput. |

## Parent red team

Chosen title: **The Performance Bugs Our Benchmarks Missed**.

Alternatives considered: “A Faster App Starts With the Work You Shouldn't Do” and “Why Our Fast Map Took 32 Seconds to Miss.” The first is too general; the second is a strong individual story but misrepresents the scope of the weekly parent.

Three likely hostile comments:

1. **“You fixed your own bugs. Why should anyone else care?”** The opener and follow-ups teach transferable failures: hit-only map benchmarks, incomplete live-set counters, size queries that render artwork, and signature-wide suspension propagation. The fixes are useful case studies even without adopting CN1.
2. **“You say Go parity, but your worst pause is hundreds of milliseconds.”** The published copy gives the actual limits and labels the experimental parallel configuration. Go is a design reference and a specific comparison, not a universal victory claim.
3. **“This is a release-note dump dressed as an engineering article.”** The opening thesis unifies the performance work, with evidence first. The table lets existing users find individual features; the parent gives every follow-up a substantive section and finishes on shared implementation ownership and concrete security boundaries.

Boldness ruling: lead with the benchmark blind spots and the measured failures. Do not lead with “better than native,” “best GC,” “competitive with Go” without workload qualification, or a fabricated memory curve. The strongest single change was replacing the promised universal GC result with the failed live-set experiment and measured deployment choice.

## Per-post adversarial decisions

Every item below is **SHIP** after the corrections described. These are editorial judgments, not posting authorization.

### GC: A Small Heap Does Not Need a Large Garbage Budget

Thesis: a small process should not pay a large fixed garbage allowance when the measured workload gains no throughput from it.

Objections: “You cannot measure the live set” is answered with the failed attempt and merged floor. “38 MB is not a phone benchmark” is answered with backend/workload scope. “Go still wins the tail” is answered with the full pause table. Boldness: claim the measured memory result and explain the limitation. Highest-value change: source truth takes precedence over the stale PR summary.

Alternative titles: “Why We Backed Out a Live-Set GC Floor”; “The 24 MB Garbage Allowance Our Backend Did Not Need.”

### Maps: Our HashMap Was Fast Until the Key Was Missing

Thesis: hit-only measurements can completely miss pathological open-addressed lookup costs.

Objections: “728x is a cherry-picked speedup” is answered with the workload and large-table regression. “This is not a Swiss table” is answered with the actual probe code. “Why not copy the JDK?” is answered with measured allocator and identity-hash differences. Boldness: the 32.7-second miss result is defensible; global speed claims are not. Highest-value change: make the missing-key distribution the story.

Alternative titles: “The Lookup Our Map Benchmark Never Tried”; “Preserve the First Probe, Fix the Rest.”

### References: A Cache Should Forget Cold Images, Not Every Image

Thesis: collector-aware reference semantics can preserve disposable cached work without pinning it indefinitely.

Objections: “The iOS cache table is still there” is answered explicitly. “Recency did not beat a matched random policy” is acknowledged. “A read can race collection” is answered with the load barrier and termination diagram. Boldness: show reclaimability and the pressure-policy comparison; do not claim universal optimal eviction. Highest-value change: separate VM delivery from framework cache migration.

Alternative titles: “When a Weak Reference Was Actually Strong”; “A Concurrent Collector Learns When a Cache Can Let Go.”

### Boxing: Three Pointer Bits Make Boxed Numbers Cheaper

Thesis: representable wrapper values can avoid a separate allocation without replacing application data structures.

Objections: “This is not Valhalla” is answered with a precise boundary. “Long and Double do not all fit” is answered with coverage and fallback. “Wrong hashes can still benchmark fast” is answered with type-specific dispatch and fault-injected validation. Boldness: use the allocation census, not an all-primitives claim. Highest-value change: show one fitting and one non-fitting example for each partial encoding.

Alternative titles: “The Number Fits Where Its Pointer Would Have Been”; “Boxing Without an Allocation, When the Bits Fit.”

### Startup: Why Was a Margin Calculation Waiting on AppKit?

Thesis: repeated platform synchronization and work inside layout queries can dominate costs before a first paint.

Objections: “Those timings cannot be added” is answered explicitly. “Mac is not every mobile target” is scoped. “Your timing example does not measure first paint” is explained beside the code. Boldness: use the concrete main-thread waits and style lookup numbers. Highest-value change: identify the call that should not have waited.

Alternative titles: “The Work We Did Before Anyone Painted a Switch”; “A Startup Profile Hidden Inside Padding and Margins.”

### JavaScript: One Blocking run() Made Unrelated JavaScript Methods Suspend

Thesis: receiver-aware suspension analysis removes unnecessary generator dispatch while keeping unresolved calls conservative.

Objections: “A smaller bundle proves little” is central to the article. “The iterator workload got worse” is given its own table row. “Dynamic bridges can invalidate static analysis” is answered with the shared dispatch model, conservative fallback, and screenshot-test boundary. Boldness: the call-graph error and measured Node results are sufficient. Highest-value change: retain the unexplained regression.

Alternative titles: “The JavaScript Generator That Should Have Been a Function”; “A Method Signature Was Too Broad a Blocking Contract.”

### Continuity: The Process Disappeared. The User’s Work Should Not.

Thesis: portable state describes work independently of a process, while restoration stays behind the application's account boundary.

Objections: “There is no universal built-in sync server” is answered by the relay contract. “Restoration can resurrect a signed-out account” is answered with clear plus disable. “Generated Apple builds are not two-device testing” is stated. Boldness: explain the delivered API and explicit capabilities; avoid “seamless everywhere.” Highest-value change: show the logout boundary alongside the happy path.

Alternative titles: “Restore the Task After the Process Is Gone”; “A Route Stack Can Travel Further Than a Form.”

### Drag/drop: A Drag Is a Clipboard Payload With a Destination

Thesis: reusable clipboard representations and lazy data providers are a useful basis for native cross-application drag/drop.

Objections: “Which ports actually support it?” gets a matrix. “That callback can deadlock AWT” gets the thread diagram and rule. “You didn't drive a native drag” is stated. Boldness: the payload model is the transferable idea, with no unsupported desktop claim. Highest-value change: explain canceled-drag cost and completion ownership.

Alternative titles: “Generate the Export Only When Someone Drops It”; “The Clipboard Already Knew What We Wanted to Drag.”

### Javadoc: We Stopped Embedding a Second Website Inside Our API Docs

Thesis: a doclet can emit the API model as site content while the existing site renderer owns presentation.

Objections: “This is only useful inside CN1” is answered by source paths and the executable example. “You broke years of method links” is answered by parity checks and the defects they caught. “Site search still excludes the guide” is acknowledged and visible in the screenshot. Boldness: a reusable engineering pattern, not a universal plugin. Highest-value change: test the tiny example and show real search output.

Alternative titles: “Let Hugo Render the Java API Model”; “A Javadoc Doclet That Produces Content Instead of a Website.”

### Security: Read the Key File You Have. End the Task You Meant to End.

Thesis: precise format and lifecycle APIs reduce ambiguous security-sensitive application glue.

Objections: “Parsing a key does not establish trust” is answered. “Task removal is not logout” is answered with application responsibilities. “Encrypted keys and EC provider support?” are scoped. Boldness: concrete validation and exit semantics, no security ranking. Highest-value change: put non-guarantees beside each API rather than in a distant disclaimer.

Alternative titles: “PEM Parsing and an Explicit End to an Android Task”; “Two Security APIs With More Precise Boundaries.”

### Android: API 37 Readiness Starts Before the Target SDK Switch

Thesis: source checks, generated-app assembly, and runtime permission tests should prepare a migration without conflating their guarantees.

Objections: “Compile success is not runtime readiness” structures the article. “The cloud builder is a different producer” is explicit. “A fallback button does not prove policy compliance” is explained. Boldness: early preparation and the verified location session, not universal Android 17 certification. Highest-value change: separate the location-policy date from any target-SDK deadline.

Alternative titles: “Make the Next Android Removal Fail in CI First”; “The Location Button Without a Forced Toolchain Upgrade.”

## Evidence and media provenance

All thirteen user-supplied PRs were read through authenticated GitHub CLI and were merged when checked. Local baseline: `9384fe3fad76` on September 10, 2026. Implementation commits consulted include `e4dc53f55b` (GC), `963764b5e7` (maps), `157c454bcb` (startup), `0204081e19` (JavaScript), `b84362c66d` (references), `17112e450a` (boxing), `a3c56579cf` (continuity), `8b001a846b` (drag/drop), `561dab8e05` (doclet), `22a820a2a3` (PEM), `76ea973294` (exit), `70451525ab` (API 37), and `7988c73023` (location button).

Performance numbers are attributed to those PRs and their source comments. The benchmark suites were not rerun for this content task. No new comparable Go map result or flatter-memory time series was supplied; the final copy stays within the available evidence.

- Twelve JPEG headers were rendered by Playwright through `~/Downloads/Blog Post Generic Header Framework/Blog Hero Generator (offline).html`, at exactly 1024×512 pixels. All were inspected in a contact sheet.
- `gc-trigger-rss.svg` is a Matplotlib bar chart of the reported trigger sweep, with a zero baseline and explicit configuration axis. It is not a time series. The subsequent 38 MB configuration is distinguished from the 30 MB sweep point in the captions.
- `soft-reference-policy.svg` shows reported hit rate and footprint at a simulated 160 MB ceiling. It does not portray device scrolling or confidence intervals unavailable from the source table.
- `javadoc-hugo-reference.png` and `javadoc-hugo-search.png` are live public-site screenshots from September 10. The former shows PublicKey; the latter searches for `fromPem`. Chat/analytics remained disabled for capture.
- All diagrams describe mechanisms. None are passed off as measured curves or screenshots of a native device workflow.

## Validation

- Full `blog_prose_gate.py` run with Vale and LanguageTool: all 12 posts pass, zero net-new findings. Eight technical terms were added to `languagetool-accept-blog.txt`; no lint rules were weakened.
- Mermaid: all 12 diagrams render in the browser. The standalone parser check also passed before the parent overview diagram was added; browser validation covers the final complete set.
- Hugo preview (`--buildFuture`), ordinary production, and a simulated September 11 production build pass. Ordinary production exposes zero new posts, Friday exposes only the parent, and the preview exposes all 12. Friday's parent has no links to unpublished children; all eleven child links exist in the future-enabled preview.
- All article-local Markdown links and media paths resolve in the preview. The 12 headers are JPEG files at exactly 1024×512. The package also contains two SVG charts and two PNG screenshots.
- All 12 posts were checked in Chromium at 1440 px and 390 px viewport widths. Seventeen article-image uses load successfully, and there is no document-level horizontal overflow. Headers, charts, both public-site screenshots, and representative article views were visually inspected.
- Seventeen Java snippets compile against the local core artifact under Java 8 with imports and enclosing application context supplied where the article shows an excerpt. This checks names and signatures, not platform runtime behavior.
- The independent Temperature example generated Hugo content and a search index through the real doclet. The local tool was JDK 26, compiling the doclet with `--release 25`; this does not claim a new JDK 25 test run or change the module's documented Java 25 requirement.
- Existing validation tests: 12 prose-gate tests and 8 social-queue tests pass. The existing social artifacts pass validation-only; this task added no social artifacts or queued posts.
- Whitespace/control-character and placeholder checks pass. The new articles contain no en/em dashes or unresolved author/verification markers.

No native implementation was changed. Benchmark results and native test reports in the articles remain attributed to the feature PRs. The full Maven core suite and fresh device benchmarks were not needed or run for this content-only change.
