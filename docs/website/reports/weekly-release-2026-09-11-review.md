# September 11 release series: editorial and evidence review

**Verdict: ready for PR review.** One parent and six follow-ups publish once per day from September 11 through September 17, 2026. The changes remain on a feature branch; this task has not published the articles or queued social posts. This is an editorial assessment, not a prediction of traffic.

Baseline: last week's `voip-vpn-builders` parent, dated follow-ups, matching `series` front matter, future-aware links, concrete examples, and discussion prompts. The marketing-context, blog-writer, engineering-narrative, positioning-redteam, and copy-polish skills informed the work.

## Reading order

| Date | Article | Narrative |
| --- | --- | --- |
| 2026-09-11 | [Lies, Damn Lies and Benchmarks](../content/blog/performance-work-between-benchmarks.md) | Benchmark blind spots and the weekly overview |
| 2026-09-12 | [What Go Taught Us About Java Garbage Collection](../content/blog/parparvm-gc-small-heaps.md) | GC pacing, weak references, and useful caches |
| 2026-09-13 | [Faster Maps: Chasing Swiss Speed](../content/blog/hashmap-misses-probe-sequence.md) | Map probing and the boxed values inside collections |
| 2026-09-14 | [Faster Starts, Less JavaScript Overhead](../content/blog/startup-cost-before-first-paint.md) | Unnecessary native waiting and JavaScript suspension |
| 2026-09-15 | [Native Drag and Drop Meets Cross-Device Continuity](../content/blog/continuity-restoring-work.md) | State restoration, cross-device continuity, and native drag/drop |
| 2026-09-16 | [Javadoc That Feels Like Your Website](../content/blog/javadoc-hugo-markdown-doclet.md) | Reusable Javadoc-to-Hugo generation and searchable API docs |
| 2026-09-17 | [Android 17 Without the Last-Minute Scramble](../content/blog/android-37-readiness-location-button.md) | API 37 preparation, location consent, PEM keys, and task removal |

Each follow-up has one opening, connected technical sections, and one conclusion. Five superseded articles and their headers were removed. Their technical material, examples, and relevant diagrams live in the retained narratives, and links now point to those articles. Each narrative has substantial coverage in the parent. Native drag and drop and cross-device continuity each have a major section and direct introductory links; neither is hidden under the other feature. The opening schedule table is replaced by reader-focused previews of the six follow-ups.

## Parent red team

Chosen title: **Lies, Damn Lies and Benchmarks**.

The opening follows the author's suggested benchmark joke, fitted-sheet analogy, and candid assessment: pretty fast, with more work ahead. The joke is linked to SPEC's “Lies, Damn Lies, and Benchmarks” article. A reliable John C. Dvorak attribution was not established, so the copy does not assign it to him.

The title uses the author's requested “Lies, Damn Lies and Benchmarks.” It challenges the evidence behind a good score without narrowing the weekly overview to one feature. The opening grounds the joke and fitted-sheet analogy in a map workload, resident memory, and unnecessary switch artwork. The parent then teaches the mechanisms directly: the incomplete GC counters, concurrent reference reads, perturbed probing, tagged wrapper dispatch, screen-state publication, and receiver-aware suspension. Ten Java examples, a C configuration excerpt, four diagrams, a benchmark chart, and a live search screenshot support its explanations before the follow-up links.

Likely skeptical responses:

1. **“An amusing analogy is not evidence that your runtime is fast.”** The next paragraph gives three concrete failures, and the sections attribute results to specific workloads. “Pretty fast” is the author's assessment, not a cross-runtime ranking. The parent does not claim that the fixes multiply into one app-wide speedup.
2. **“Go still wins your worst pause.”** The GC deep dive shows the full comparison and unchanged serial default. The copy does not turn a matching median/p99 into universal Go parity.
3. **“This is still a release-note dump.”** Six narratives follow six reader problems. Maps and boxing concern the same data structures; GC and references concern reclaiming memory without losing useful cached work; native and JavaScript changes concern unnecessary waiting. The parent now includes the mechanics and application code for each narrative, following last week's substantial call/VPN/builder explanations. Follow-ups extend that material rather than holding back its explanation.
4. **“Security leadership is just marketing.”** The conclusion names reference safety, key validation, account boundaries, and OS consent. It makes no comparative security certification claim.

Boldness ruling: keep the author's humor and personal confidence. The evidence earns detailed claims about these workloads, not a “best runtime” headline. Traction comes from recognizable engineering mistakes and reusable fixes; no traffic result is assumed.

## Narrative and voice review

The series uses the author's investigation and concrete workflows to carry the technical material. The GC opening follows the author-supplied account of comparing HotSpot and Go, suspecting stack allocation, and exploring the choices available to ParparVM's AOT runtime. The map story continues into compact tables and the wider tagged-value implementation; Valhalla supplies the Java ecosystem connection without being described as stack allocation.

The other openings follow a startup stall inside a margin calculation, a draft moving between devices and applications, Javadoc bringing a second website into the site, and an Android SDK version parsed as `372`. Code and diagrams remain central. Measurements sit beside the change they explain. Regressions and incomplete integration work remain in the narrative as engineering decisions and next steps.

Aggressive copy polish removed detached source-label sentences, repeated certification disclaimers, and defenses against claims the article never made. Links now belong to the sentence explaining the code or idea. Chart captions describe the plotted quantities. The supplied feature PRs remain the source of the benchmark numbers; this content task did not rerun them.

The Go runtime terminology and allocation discussion were checked against the official Go FAQ. Valhalla's identity and representation discussion was checked against OpenJDK's value-object design notes. The prose makes no release-date promise for Valhalla and keeps the concrete limits of tagged Long and Double values.

## Six narrative reviews

### What Go taught us about Java garbage collection

The author starts with HotSpot as the familiar reference, suspects stack allocation after comparing with Go, and then investigates collection policy. AOT and the closed-world runtime explain why ParparVM can run the experiment. The allocation question connects to Valhalla and returns in the map/boxing follow-up. The deep dive preserves the failed live-set experiment, worst-pause comparison, concurrent-read barrier, and simulated-budget cache results.

Skeptical checks: the 38 MB result is a controlled runtime measurement; the stock floor did not change; the framework image-cache migration remains separate; recency was not compared with matched-rate random eviction. None is hidden by the combined narrative. The conclusion connects lower garbage, useful cache retention, and the runtime's responsibility to prevent dangling references.

### Chasing Swiss speed

The Swiss-map comparison is the hook, with the missing-key workload showing why the improvement matters. “Faster Maps: Chasing Swiss Speed” describes the investigation, not measured parity with Go. Wider tagged values then answer the next allocation question: what does each key/value slot point to? The probe helper, map regressions, tag diagram, coverage table, and allocation census remain in one article.

Skeptical checks: no Swiss-style SIMD group-probing claim, no equivalent-workload Go parity claim, no universal Long/Double encoding, and no general stack-allocation promise. The “poor man's Valhalla” callback keeps its precise limits. The two optimizations do not become a multiplied speedup.

### Faster starts, less JavaScript overhead

The margin calculation waiting on AppKit leads into publication of screen state, monitor entry, style construction, and artwork generation. Receiver-aware JavaScript analysis follows as a second example of treating too many operations as potentially waiting.

Skeptical checks: native Mac timings are not universal startup numbers; Node results are compiler/runtime measurements; the 13.7% iterator regression remains unexplained and visible. The conclusion preserves conservative resolution and collector coordination while removing unnecessary work.

### Native drag and drop meets cross-device continuity

Continuity preserves a description of a task; native drag/drop transfers a representation to another application. That payload contract ties the APIs together without pretending they use the same transport. Code covers restoration, logout, and incoming files.

Skeptical checks: no automatic universal relay service, no assumption that a checkpoint authorizes access, no unsupported native desktop ports, and no claim of a physical two-device or native-drag test. The linked review's provider timing issue is corrected in prose, table, diagram, and parent. The conclusion treats incoming files as external input and restores routes only after account checks.

### Javadoc that feels like your website

The useful result is a reusable doclet that emits content, letting Hugo own layout and search. Actual reference/search screenshots, the minimal example, source paths, and anchor parity give other Java developers something they can use.

Skeptical checks: Markdown comments arrived in JDK 23; Java 25 is the tooling requirement. The guide remains outside the site search index. The doclet example was verified with a local JDK 26 tool compiling with `--release 25`, not represented as a fresh JDK 25 run.

### Android 17 without the last-minute scramble

API 37 compilation and packaging preparation leads into a system-owned location permission flow, then key-format validation and explicit task removal. These are places where the framework can replace security-sensitive platform and format code in individual applications.

Skeptical checks: compile readiness is not runtime certification; the location-policy date is not a general target-SDK deadline; the separate cloud-builder rollout is not certified here. PEM parsing does not establish trust, and task removal is not credential revocation. The final section closes the entire week with concrete runtime, documentation, platform, and security responsibilities.

## Claims checked against implementation

| Severity | Draft claim at risk | Evidence and final treatment |
| --- | --- | --- |
| Blocking | A live-set-proportional GC floor shipped as the default | PR #5717's opening description is stale relative to its merged source. `cn1_globals.m` explains why incomplete sweep counters made that policy unsound. The articles state that the floor is deployment-selected and the stock floor remains 24 MB. |
| Blocking | GC now matches Go and keeps memory flat through everything | The recorded median and p99 matched in one loop; the worst pause did not. Four-marker ParparVM pauses were 0.3–0.9 seconds versus Go's 20 ms. The lower-memory test configuration is distinguished from a stock mobile build. No invented time-series curve is shown. |
| Blocking | PR #5722 adds Swiss-style SIMD group probing and proves Go map parity | The merged table loop uses scalar perturbed probing. Compact metadata and the native string comparison path are explained separately. No comparable Go timing table was found in the supplied PR, so no equality/superiority claim is published. |
| Blocking | Every image cache now uses ranked soft references | PR #5732 explicitly leaves the iOS strong-reference table and framework call sites for separate integration. The article distinguishes VM semantics, benchmark evidence, and prospective cache migration. |
| Blocking | All boxed primitives are stack allocated | Tagged words are not general stack allocation. Long and Double have explicit encoding limits and heap fallback. Byte and Boolean already have bounded caches. |
| Blocking | API 37 support proves all Android 17 behavior and cloud rollout | Compile gates, generated-app assembly, manual emulator location testing, regular API 36 CI, and the separately managed BuildDaemon changes are reported distinctly. No general target-SDK deadline is invented. |
| Blocking | Site search includes the full developer guide | The live search page explicitly excludes that document. Copy now says that API types/members are indexed and the guide retains its own navigation/browser search. |
| High | Security superiority is established by the release | No comparative security assessment exists here. The close names validated boundaries: key container checks, explicit restoration shutdown, permission injection, and OS-owned consent. It does not certify an app or imply task removal revokes credentials. |
| High | Java 25 introduced Markdown documentation comments | Tooling uses Java 25; Markdown comments arrived in JDK 23. The article links Oracle's documentation and explains the distinction. |
| High | Native drag support covers every desktop port and was exercised by hand | The matrix distinguishes JavaSE and Catalyst from native AppKit/Windows/Linux. The supplied PR lacks a physically driven drag test. This report records that evidence limit; the article gives practical integration tests without claiming they were run. |
| High | The JavaScript optimization improved every workload | The 13.7% iterator regression is included beside the wins, with its unresolved cause. The source-size reduction is not used as a proxy for throughput. |
| High | Every drag data provider waits for a receiver | Review comment [3982187867](https://github.com/codenameone/CodenameOne/pull/5767#discussion_r3982187867) correctly identifies eager paths. Android resolves every provider before starting the drag; iOS resolves file lists to count items. The parent and merged continuity article state these exceptions, and the new diagram shows their timing. Providers must be cheap enough for drag start; expensive exports need preparation or caching. |

## Evidence and media provenance

All thirteen user-supplied PRs were read through authenticated GitHub CLI and were merged when checked. Local baseline: `9384fe3fad76` on September 10, 2026. Implementation commits consulted include `e4dc53f55b` (GC), `963764b5e7` (maps), `157c454bcb` (startup), `0204081e19` (JavaScript), `b84362c66d` (references), `17112e450a` (boxing), `a3c56579cf` (continuity), `8b001a846b` (drag/drop), `561dab8e05` (doclet), `22a820a2a3` (PEM), `76ea973294` (exit), `70451525ab` (API 37), and `7988c73023` (location button).

Performance numbers are attributed to those PRs and their source comments. The benchmark suites were not rerun for this content task. No new comparable Go map result or flatter-memory time series was supplied; the final copy stays within the available evidence.

- Seven JPEG headers were rendered by Playwright through `~/Downloads/Blog Post Generic Header Framework/Blog Hero Generator (offline).html`, at exactly 1024×512 pixels. All were inspected in a contact sheet.
- `gc-trigger-rss.svg` is a Matplotlib bar chart of the reported trigger sweep, with a zero baseline and explicit configuration axis. It is not a time series. The subsequent 38 MB configuration is distinguished from the 30 MB sweep point in the captions.
- `soft-reference-policy.svg` shows reported hit rate and footprint at a simulated 160 MB ceiling. It does not portray device scrolling or confidence intervals unavailable from the source table.
- `javadoc-hugo-reference.png` and `javadoc-hugo-search.png` are live public-site screenshots from September 10. The former shows PublicKey; the latter searches for `fromPem`. Chat/analytics remained disabled for capture.
- All diagrams describe mechanisms. None are passed off as measured curves or screenshots of a native device workflow.


The drag-timing correction was checked directly against `AndroidNativeDragAndDrop.toClipData()` and `IOSImplementation.nativeDragSessionStartedCallback()` in the local source. The Android path needs complete `ClipData`; the iOS file path calls `content.getFiles()` to establish item count. Other iOS representations must not be assumed universally deferred.

## Validation

- Full blog prose gate with Vale and LanguageTool: zero net-new findings across the seven retained articles. No lint rules were weakened.
- Hugo future preview passes. Simulated production builds for every date from September 10 through September 17 expose exactly 0, 1, 2, 3, 4, 5, 6, and 7 series posts. None links to an unpublished or removed article.
- All article-local Markdown links and media paths resolve. Each article has one discussion section; no en/em dashes, placeholder markers, or obsolete story links remain.
- All seven pages pass Chromium checks at 1440 px and 390 px widths, including 15 rendered Mermaid diagrams and 13 loaded image uses. No document-level horizontal overflow was found. The revised parent title, opening, and hero were visually inspected on desktop and mobile.
- Seven JPEG headers were generated at 1024×512 and inspected. Two benchmark SVGs and two live documentation screenshots remain in the package.
- All 26 Java excerpts compile against the local core artifact under Java 8, with imports and enclosing application context supplied where needed. This validates names and signatures, not platform runtime behavior.
- The unchanged standalone Temperature/doclet example retains the prior verification: real doclet output and search index generated with JDK 26 compiling the doclet with `--release 25`.
- Whitespace checks pass. No native implementation changed; benchmark and device results remain attributed to the feature PRs. No fresh native benchmark or full Maven suite was needed for this content revision.

The public GC copy and chart label describe a controlled runtime experiment. Unannounced product details, endpoint names, and connection counts are omitted; the PR attribution, measured RSS, and distinction from a mobile-device benchmark remain.
