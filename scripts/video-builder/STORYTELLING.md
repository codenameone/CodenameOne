# Video storytelling contract

The builder supplies a shared visual language and a shared dramatic spine. It must not supply
interchangeable scenes or article-shaped narration.

## The fixed three-act journey

Every landscape video and Short follows the same three-act logic. The scenes and visual treatment
remain article-specific, but the causal order does not change.

1. **Identity splash (`identity`):** say who Codename One is and what the video is about. Use the
   real logo as a small signature, the current website promise in plain language, and a topic-specific
   welcome graphic. This is orientation, not a product victory lap.
2. **The independent real-world problem (`problem`, `difficulty`):** explain what practitioners
   struggle with before Codename One enters the story. Build the picture from first principles and
   make the cost concrete. Use independent evidence where possible: community questions, public
   videos, official platform documentation, issue reports, measurements, or authentic screenshots.
   Do not mention the Codename One solution, show its API, or show its demo in this act.
3. **Resolution (`intervention`, `proof`, `victory`):** introduce the relevant Codename One feature
   naturally, then revisit every problem from act two in the same order. For each one, show the
   mechanism that changes it and the proof: exact code, a compiled running demo, an authentic
   capture, or a measured artifact. End on what the developer can now build, test, or control.

The developer is the protagonist. Codename One is the tool that lets the developer change the
outcome. Never announce that the product “comes to the rescue”; make the resolution visible.

## What stays uniform

- Codename One typography, palette, caption treatment, and a small in-scene logo signature.
- Proof labels that distinguish a running demo, an actual capture, illustrative code, and abbreviated code.
- Pointer, focus, replay, caption, and end-screen behavior.
- Audio loudness, pronunciation QA, subtitle delivery, and technical export settings.

## What must be unique

Every video declares five editorial choices before scripting:

1. `humanBeat`: the concrete frustration, decision, surprise, or recovery a developer experiences.
2. `visualIdentity`: the recurring visual motif for this story, such as a room becoming test data,
   a benchmark gap physically closing, or a build moving through guarded gates.
3. `bespokeVisualization`: one image, diagram, animation, or physical metaphor authored for this
   topic rather than selected from a generic slide template.
4. `problemDimensions`: the complete list of independent difficulties established in act two.
5. `resolutionMap`: one problem, one Codename One mechanism, and one proof artifact for every
   difficulty. Unmapped problems fail the render gate.

At least three visual modes must carry the story: for example an editorial illustration, real
product capture, source code, live UI, animated diagram, or annotated artifact. Bullet slides are
supporting punctuation, not the default scene.

## Story archetypes

- `live-demo`: establish the independent problem, reveal the behavior and code, interact with the
  compiled application, optionally replay a moment worth seeing twice, then show the outcome.
- `code-and-capture`: begin with a human consequence, show an authentic capture, connect it to the
  exact code, and annotate what changes.
- `code-deep-dive`: make a surprising result visible, trace two or more source mechanisms, then
  show the tradeoff or counterexample. It does not pretend illustrative code is running.
- `visual-explainer`: use a bespoke diagram or visual metaphor to expose a mechanism that has no
  useful UI. Replace generic three-node flows with domain objects and real state changes.

## Review gates

- No production notes in narration. Never say that source is “shown as evidence.” Explain it.
- The splash identifies Codename One and the topic within eight seconds. The logo is a signature,
  not the composition.
- Act two is understandable without knowing Codename One exists. It names the APIs, tools,
  languages, debugging environments, hardware constraints, and simulation difficulty when those
  are part of the subject.
- Keep the solution quarantined until act two has established the problem and independent proof.
- Act three resolves every declared problem in the same order and names the proof for each.
- No more than two slide-only scenes may be adjacent.
- A visible interaction must change application state. Moving, dragging, zooming, or clicking a
  frozen picture is not interaction and fails a `live-demo` review.
- A scripted `pointer.move` must declare its `semanticIntent` and drive a nearby compiled
  `demo.action`. Decorative cursor travel fails the quality gate.
- A `live-demo` contains at least two distinct state-changing `demo.action` events against compiled
  Codename One code. Replay only a moment worth seeing twice.
- Captions use normal technical spelling (`ARKit`, `ARCore`, `JavaSE`). Phonetic substitutions are
  speech-only.
- Remove empty qualifiers such as “real” when no fake alternative is being contrasted.
- Generated illustrations may dramatize the human problem; claims still require real code,
  captures, measurements, or a compiled demo.
- Do not narrate roadmap negatives. Say what the feature does and what the developer can ship.
- Remove “the boundary is,” “not yet,” “does not ship,” and similar release-note phrasing.
- Portrait is separately composed. It may share assets, but not merely crop the landscape edit.
- Red-team verdict is exactly `SHIP` only after the corrections are present in the rendered script.
