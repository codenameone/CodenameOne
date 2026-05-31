---
name: blog-release-series
description: Author and ship a weekly release blog post plus its daily follow-ups as a single PR. Covers the front-matter conventions, the Hugo preview loop, the cross-post linking pattern, screenshot reuse, the Mermaid shortcode, and the Shai-style content rules that came out of the 2026-05-29 release.
---

# Blog release series

Use this skill when authoring the weekly Codename One release blog post plus the daily follow-ups that walk through specific PRs in the same release. The previous release (2026-05-29) tried to ship one PR per follow-up; the merge-conflict cascade between sibling PRs (each follow-up edited the previous post's wrap-up to add a forward link) made the cadence painful. Going forward, **the entire week's posts ship as one PR** so the whole arc is review-able, preview-able, and conflict-free.

## When to use

- Friday of a release week, when the weekly release post is going up.
- Any time a release introduces several features that each deserve their own deep-dive follow-up.

## Recommended PR structure: one batch PR per week

A single PR contains:

- The Friday weekly index post (short TL;DR + upcoming attractions).
- One follow-up `.md` per day, dated for its publish day in front matter.
- Per-post static assets under `docs/website/static/blog/<slug>/`.
- A series taxonomy front-matter field if scoped navigation is wanted.

**Why this works.** Hugo hides future-dated posts from production builds by default. `HUGO_BUILD_FUTURE=true` is already wired into the PR-preview workflow (`.github/workflows/website-docs.yml`), so the Cloudflare preview renders every post as if its date had arrived. The live site keeps the production behaviour, revealing each post on its actual date as Hugo rebuilds.

## Layout in the PR

```
docs/website/content/blog/
    weekly-index-<topic>.md            # date: <Friday>
    follow-up-1-<topic>.md             # date: <Saturday>
    follow-up-2-<topic>.md             # date: <Sunday>
    follow-up-3-<topic>.md             # date: <Monday>
docs/website/static/blog/
    weekly-index-<topic>.jpg           # hero
    follow-up-1-<topic>.jpg            # hero
    follow-up-1-<topic>/
        screenshot-1.png
        diagram.svg
    follow-up-2-<topic>.jpg
    follow-up-2-<topic>/
        chatview.png
        ...
```

Each post lives in one file. Each per-post asset folder is named for the slug (no spaces, lower-case-kebab).

## Front matter that every post needs

```yaml
---
title: "Headline About The Biggest Items In The Post"
slug: same-as-the-md-filename-without-extension
url: /blog/same-as-slug/
date: '2026-06-05'                   # publish day, ISO-8601
author: Shai Almog
description: One or two sentences. Lands in <meta description> and the
  RSS feed; avoid a comma-separated feature list (it reads as marketing
  copy). Lead with the why.
feed_html: '<img src="https://www.codenameone.com/blog/<slug>.jpg" alt="<title>" /> Same sentence as `description`, rendered with the hero image for the RSS feed.'
---
```

Rules learned the hard way:

- **Quote any title that contains a colon.** Hugo's YAML parser fails on `title: Foo: Bar` because the second colon reads as a map-value separator. `title: "Foo: Bar"` parses cleanly. Symptom in CI: `mapping value is not allowed in this context`.
- **Slug, URL, and filename must match.** Mismatched slugs make `/blog/<slug>/` 404 even when the rendered HTML is in `public/`.
- **`feed_html` mirrors `description`** but starts with the `<img>` tag for the RSS payload. RSS readers render that markup directly.
- **Author is `Shai Almog`** consistently. The blog index groups posts by author.

## Cross-post links: prefer relative-time prose over weekday names

In the previous release we wrote forward-link prose like:

> Monday's post covers...

That worked until the schedule shifted (Monday's post moved to Sunday) and every weekday reference had to be edited. The resilient pattern is:

> [Tomorrow's post](/blog/...) covers...
> ...
> Yesterday's was about the new platform APIs in the core.

`tomorrow`, `yesterday`, `Friday` (always the weekly index), and `this Friday's release post` (always the next weekly index) survive date shifts.

When the surrounding paragraph really wants a weekday (the Friday weekly index listing the upcoming arc), keep it factual: `Saturday`, `Sunday`, `Monday` etc. without "the next post is on...". State the day the post will land and link to it.

### Better still: stop hand-writing forward links

The cleanest fix is a Hugo partial. None of the previous releases shipped this, but the next release week is a fine time to add it:

```
docs/website/layouts/partials/blog-post-nav.html
```

Rendered from `docs/website/layouts/_default/single.html` for posts that have a `series:` taxonomy entry. The partial uses `where` + the `series` taxonomy to pick the previous and next post in the same series and renders "Read next" / "Previously" links. Authors stop hand-editing wrap-up prose.

If the partial exists, every post in a batch gets `series: ["release-<YYYY-MM-DD>"]` in front matter and the partial handles the rest. If the partial does not exist yet, write the partial first, then author the posts.

## Hugo preview workflow

### Local

```bash
cd docs/website
hugo server --buildFuture --bind 127.0.0.1
```

Open <http://localhost:1313/blog/>. Live reload picks up edits in seconds. Every future-dated post in the PR appears as if its date had arrived.

### Cloudflare PR preview

The workflow at `.github/workflows/website-docs.yml` runs on `pull_request` with `HUGO_BUILD_FUTURE=true`, deploys to Cloudflare Pages, and posts the URL as a comment on the PR. Format: `https://pr-<NNNN>-website-preview.codenameone.pages.dev/blog/`.

Use the preview to:

- Click through every post in the batch. Read each in browser, not in the markdown editor.
- Verify every screenshot loads.
- Verify cross-post links resolve.
- Check the blog index for ordering and excerpts.
- Check the RSS feed at `/blog/feed.xml`.

### Production deploy

The merge to `master` triggers the production deploy with no `HUGO_BUILD_FUTURE` set. Posts appear on the live site on or after their actual `date:`. No manual gating needed.

## Hero images and per-post assets

Every post needs a hero image at `docs/website/static/blog/<slug>.jpg`. The convention is a 1500-2000px-wide JPG, around 30-100 KB after compression. Either drop a file in directly or generate one (the previous release used an LLM-generated hero for each post).

Per-post assets (screenshots, diagrams, animated GIFs) live in `docs/website/static/blog/<slug>/`. Reference them from the post body as `/blog/<slug>/<filename>`.

### Reuse the screenshot test fixtures

Real screenshots beat ASCII mockups. Always. Before asking the user to take a screenshot, look for an existing test fixture:

```
scripts/ios/screenshots-metal/<TestName>.png       # iOS Metal (preferred for SVG content)
scripts/ios/screenshots/<TestName>.png             # iOS GL fallback
scripts/android/screenshots/<TestName>.png         # Android
scripts/javase/screenshots/<TestName>.png          # JavaSE simulator
```

These already render real UIs against the framework, often against the same `theme.css` the post is describing. To use one:

1. Crop the test-runner caption (usually the top ~280 px) with `PIL.Image.crop`.
2. Downscale to ~480 px wide for web payload.
3. Save under `docs/website/static/blog/<slug>/<descriptive-filename>.png`.

Example (from the 2026-05-31 platform-APIs post):

```python
from PIL import Image
src = Image.open('scripts/ios/screenshots-metal/ChatView_light.png')
W, H = src.size
out = src.crop((0, 360, W, H))   # drop caption
target_w = 480
ratio = target_w / out.size[0]
out = out.resize((target_w, int(out.size[1] * ratio)), Image.LANCZOS)
out.save('docs/website/static/blog/<slug>/chatview.png', optimize=True)
```

### Animated GIFs from frame-grid screenshots

Several screenshot tests render N animation frames as a 2×3 grid in one PNG. To turn that into an animated GIF:

1. Crop the grid into individual frames.
2. Mask out the per-frame label in the corner of each frame.
3. Interpolate ~3 blended frames between each pair with `PIL.Image.blend` so the playback is smooth.
4. Quantize against a shared palette so colour identity stays stable across frames.
5. Save as an animated GIF with `duration=60` ms per frame, `loop=0`, `disposal=2`.

The complete script from the 2026-06-01 codegen post is preserved at the end of this SKILL as a reference.

## Mermaid shortcode

The blog supports Mermaid diagrams via `{{< mermaid >}}...{{< /mermaid >}}`. The shortcode at `docs/website/layouts/shortcodes/mermaid.html` emits the diagram div plus an inline ESM loader for Mermaid 10, gated on a `window.__cn1MermaidLoaded` flag so multiple diagrams on one page only initialise once.

```text
{{< mermaid >}}
flowchart LR
    IDE["IntelliJ IDEA"] -- "JDWP" --> Proxy["CN1 Debug Proxy"]
    Proxy -- "wire protocol" --> App["iOS app"]
{{< /mermaid >}}
```

Do not try to gate the Mermaid loader from `extend_footer.html`; that partial is invoked by PaperMod via `partialCached` keyed on layout + kind, so per-page conditional content is shared across every page in the same cache bucket. Inlining the loader in the shortcode dodges that. (Pages without diagrams never see the loader.)

## Shai-style content rules

These are the rules that came out of editorial review across the 2026-05-29 release. Apply them on the first pass instead of waiting for review.

### Voice and pronouns

- **No "I" or "personally".** Use "we" or rephrase impersonally even when the writer did the work themselves. Example: "I have personally wanted this for a long time" → "we have wanted this for a long time".
- **No "what surprised me" / "the thing I noticed"** style anecdote sections. Reviewer rejected this as "hallucinated narrator content".
- **No "ridiculous", "stupid", "halucination"** describing the framework's own decisions. Be respectful of the work.

### Style

- **No em-dashes (—) or en-dashes (–).** Use commas, semicolons, parentheses, periods, hyphens. Em-dashes are an AI tell.
- **Avoid the word "knob"** in prose ("the sizing knob" reads as jargon; just say "sizing").
- **Avoid "deep dive", "dive in", "let me", "let us dive into"**, AI-essay openings, and "in conclusion".
- **Build hints in prose use the in-settings name**, not the `codename1.arg.` prefix. So `ios.metal=false`, not `codename1.arg.ios.metal=false`.

### Marketing

- **Do not disparage the build cloud.** It is a paid product; lines like "the local Xcode build is the fastest iteration loop" implicitly downgrade it. State both paths as equivalent.
- **Apps and screenshots referenced from external sources** (Initializr, Playground, Skin Designer, Build Cloud console) should only be mentioned when they really are working examples of the feature under discussion. Reviewer caught a line claiming they were examples of the routing framework, when none of them used it.
- **The build cloud handles the iOS build.** Do not write "you need a Mac" without qualifying with "for local builds" or "for the iOS Simulator". You do not need a local Mac to ship an iOS app; the cloud has one.

### Accuracy

- **Do not write about half-shipped features.** If a section is about an API whose native bridges are tracked as follow-ups in the PR description, cut the section. When the bridges land, write the post then.
- **Verify the feature actually does what the post says.** Before claiming that SVG `text` is unsupported, check the screenshot in the post: if text is visible in the rendered output, the claim is wrong.
- **Sample code uses the actual facade signatures.** Pull them from the PR or from the dev guide; do not invent plausible-looking method names.
- **Never invent URLs.** Apple's App Store link IDs, third-party demo URLs, blog-post URLs that don't exist; all real, all reviewable.

### Privacy and security

- **API keys must never live in code or in the binary.** If a section discusses storing or using credentials, explicitly state the rule (fetch from your own backend, store in keychain via SecureStorage, never check in, never embed). Don't say "set your API key" without the warning.

### Structure

- **Big features at the top.** The first heading and the first 200 words tell the reader what the most important thing in the post is. Architectural details and implementation notes go to the end.
- **Show, do not just describe.** Code samples + screenshots beat prose every time. If a post is about a UI component, the screenshot is non-negotiable.
- **For SVG / Lottie / any vector graphics post:** real renderings from the screenshot fixtures, plus a millimeter-sizing example. Highlight that an SVG handled by the transcoder is still an `Image` and works everywhere a raster `Image` does.
- **For UI binding examples:** show both sides. The model class AND the form with the matching components. Reviewer caught a `@Bindable` example that showed only the model.
- **For multi-cn1lib coverage:** one subsection per cn1lib, each with TL;DR, platform-specific behaviour, use cases, code sample. The "why these stay cn1libs and not core" answer follows the concrete list; it does not introduce one.

### Asking for feedback

- For SVG / Lottie transcoders and other content-import paths, **explicitly ask readers to file an issue with the failing source file attached** when their content doesn't transcode the way they expect. The transcoder grows one shape family at a time from those reports.
- Link to `github.com/codenameone/CodenameOne/issues` for the issue tracker; do not invent a separate intake form.

## Cloud build URL convention

The new console at `https://cloud.codenameone.com/console/index.html` is the default; the [legacy console](https://cloud.codenameone.com/secure/index.html) at `/secure/` stays online for the time being. Use the new URL in new posts. Do not retroactively rewrite historical posts; that is lying about what we said at the time.

## Pre-merge checklist

Before requesting review on a batch PR:

- [ ] Every post has a hero image at `docs/website/static/blog/<slug>.jpg`.
- [ ] Every screenshot in the post body resolves on the preview URL.
- [ ] No em-dashes anywhere: `grep -P "[\x{2014}\x{2013}]" docs/website/content/blog/<files>` is empty.
- [ ] No first-person singular in prose: `grep -nE "\b(I|my|me|myself|personally)\b" docs/website/content/blog/<files>` is empty (matches inside code blocks and example chat messages are fine).
- [ ] No `codename1.arg.` prefix in build-hint snippets.
- [ ] Every title containing a colon is quoted in YAML.
- [ ] Every slug matches its filename and URL.
- [ ] Cross-post links use `tomorrow's post` / `yesterday's post` / `Friday's release post`, not weekday names that decay if the schedule shifts.
- [ ] If the post discusses credentials, the "never check in, never embed" rule is stated.
- [ ] If the post discusses an iOS-only or Metal-only feature, the caveat says what users will see (bad artifacts, missing gradients) not just "placeholder".
- [ ] Hugo builds clean locally: `hugo --buildFuture` reports zero errors.
- [ ] The PR preview comment has appeared and every post renders on it.

## Appendix: animated-GIF script from the 2026-06-01 codegen post

```python
from PIL import Image, ImageDraw

src = Image.open('scripts/ios/screenshots-metal/LottieAnimatedScreenshotTest.png')
W, H = src.size
cw, ch = 589, 852          # cells in a 2x3 grid
order = [(0,0),(1,0),(0,1),(1,1),(0,2),(1,2)]

key_frames = []
for (cx, cy) in order:
    f = src.crop((cx*cw, cy*ch, (cx+1)*cw, (cy+1)*ch))
    d = ImageDraw.Draw(f)
    d.rectangle((0, 0, 220, 75), fill=(255, 255, 255))   # mask "F1 0%" label
    f = f.crop((0, 0, cw-2, ch-2))                       # trim grid lines
    f = f.resize((f.size[0]//2, f.size[1]//2), Image.LANCZOS)
    key_frames.append(f.convert('RGB'))

# Interpolate between each consecutive pair so playback is smooth.
loop = key_frames + [key_frames[0]]
INTERPOLATE = 3
all_frames = []
for i in range(len(loop) - 1):
    a, b = loop[i], loop[i+1]
    all_frames.append(a)
    for k in range(1, INTERPOLATE + 1):
        alpha = k / (INTERPOLATE + 1)
        all_frames.append(Image.blend(a, b, alpha))

# Shared adaptive palette so colours stay true across frames.
combined = Image.new('RGB',
                     (all_frames[0].size[0]*len(all_frames),
                      all_frames[0].size[1]))
for i, fr in enumerate(all_frames):
    combined.paste(fr, (i*all_frames[0].size[0], 0))
palette_img = combined.quantize(colors=128, method=Image.MEDIANCUT)
p_frames = [fr.quantize(palette=palette_img, dither=Image.NONE)
            for fr in all_frames]

p_frames[0].save(
    'docs/website/static/blog/<slug>/animation.gif',
    save_all=True, append_images=p_frames[1:],
    duration=60, loop=0, optimize=True, disposal=2,
)
```

## Related infrastructure already in place

- `scripts/website/build.sh` reads `HUGO_BUILD_FUTURE` and `HUGO_BUILD_DRAFTS` and passes the matching Hugo flags.
- `.github/workflows/website-docs.yml` sets `HUGO_BUILD_FUTURE=true` on `pull_request` events.
- `docs/website/layouts/shortcodes/mermaid.html` carries the Mermaid loader inline.
- `docs/website/layouts/partials/extend_footer.html` is intentionally empty; the Mermaid loader is in the shortcode for the partialCached reason.

If a future release wants to keep building on these, add to them rather than re-inventing.
