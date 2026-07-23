# Codename One Video Builder

`video-builder` is a JavaSE-only Codename One application for generating scripted product videos in CI or from an LLM. A versioned JSON timeline composes text, transitions, syntax-highlighted `CodeEditor` listings, local narration, and compiled Codename One demos. Media capability checks and audio decoding use the Codename One `VideoIO` API. Export renders sparse visual changes to a temporary image sequence and mixes PCM with `AudioMixer`. The default `staged` pipeline performs one bulk ffmpeg encode and mux; set `output.encodingPipeline` to `videoio` when the deliverable itself must be produced through `VideoWriter`.

[`STORYTELLING.md`](STORYTELLING.md) defines the editorial contract. The renderer keeps branding
uniform, while every production script follows a fixed three-act journey: identify Codename One
and the topic, establish the independent real-world problem without the product, then resolve
every problem with a mapped mechanism and proof. Each story declares its own human beat, visual
identity, bespoke visualization, problem dimensions, and resolution map. The past-year quality
gate blocks unreviewed generic templates.

## Build and run

The project depends on the Codename One `8.0-SNAPSHOT` artifacts from this repository. Install the current core, JavaSE port, and Maven plugin first when they are not already in your local Maven repository, then build the executable:

```bash
cd scripts/video-builder
mvn clean -Pexecutable-jar package
```

Run the resulting JavaSE jar with its adjacent `target/libs` directory:

```bash
java -jar javase/target/codenameone-video-builder-8.0-SNAPSHOT.jar validate examples/release-feature/video.json
java -jar javase/target/codenameone-video-builder-8.0-SNAPSHOT.jar preview examples/release-feature/video.json --orientation landscape
java -jar javase/target/codenameone-video-builder-8.0-SNAPSHOT.jar render examples/release-feature/video.json --orientation both --output output
```

`prepare` emits measured cue timings as JSON (`cues[].id`, `atMs`, and
`durationMs`) after local narration is cached. CI-oriented generators can use
those measurements to fit scene durations before rendering frames, which keeps
natural speech pacing without long gaps or narration overlap.

The past-year syndication batch wraps that two-phase workflow in:

```bash
python3 tools/run_past_year_render.py <blog-slug>
```

It generates the package, measures Kokoro narration, writes `timings.json`,
regenerates accurate chapters and metadata, renders both orientations, validates
the YouTube package, probes the media streams, and records resumable state.
Set `JAVA17_HOME` or `VIDEO_BUILDER_JAVA` when Java 17 is not the default
runtime. `CN1_VIDEO_OUTPUT` changes the batch output root, and
`CN1_YOUTUBE_PACKAGE_VALIDATOR` can point at the validator supplied by the local
syndication skill. Upload automation reads its dedicated browser profile from
`CN1_YOUTUBE_PROFILE`.

`projects/past-year` retains the generated scripts, diagrams, captures,
thumbnails, evidence maps, and red-team notes as reference implementations for
future automated stories. Machine-local render state, narration caches, model
files, measurement scripts, encoded media, and build outputs are intentionally
ignored.

The render command writes `<id>-landscape.mp4`, `<id>-portrait.mp4`, and a JSON report containing dimensions, timing, and SHA-256 hashes. Progress goes to stderr and the final machine-readable result goes to stdout. Set `-Dffmpeg.dir=/path/to/bin` when `ffmpeg` and `ffprobe` are not on `PATH`.

On Apple Silicon, export automatically selects `h264_videotoolbox` and requires hardware encoding (`-allow_sw 0`), with real-time and speed-priority hints. Other systems fall back to `libx264`. Override selection with `-Dvideo.encoder=<ffmpeg-encoder>` or disable automatic hardware selection with `-Dvideo.hardwareEncoding=false`. The staged sequence uses high-quality JPEG assets plus hard links (or symbolic links) for unchanged frames, so long static holds consume only one image while retaining the requested constant frame rate. Staging is removed after success or failure; use `-Dvideo.keepFrames=true` only for diagnostics.

The `videoio` pipeline retains sparse EDT capture, then delivers the completed frame plan and mixed audio through the public `VideoWriter` API. On Apple Silicon the JavaSE VideoIO backend also prefers `h264_videotoolbox` with hardware-only, real-time, speed-priority flags. This mode is useful for end-to-end API demonstrations and integration tests; `staged` remains the faster default for ordinary batch production.

`examples/release-feature/smoke.json` is intentionally a low-resolution, low-frame-count fixture for fast renderer checks. Production examples use 1920×1080 landscape and 1080×1920 portrait output at 30 fps.
`examples/release-feature/visual-smoke.json` adds fast coverage for SVG fallback rendering, animated diagrams, and both replay phases.
`examples/release-feature/beating-hotspot-video.json` is a longer architecture explainer based on the `beating-hotspot-performance` article. It pairs deliberately abbreviated, non-compiling C excerpts with bullets and orientation-specific diagrams; it does not mount or run a demo application.

## Script contract

The canonical schema is [`common/src/main/resources/video-script.schema.json`](common/src/main/resources/video-script.schema.json). All referenced files are resolved relative to the JSON file and may not escape that directory. A scene has an explicit duration and timestamped actions. Supported v1 actions are:

- `text.show`, `text.hide`, and `layer.hide`
- `code.show` and `code.type`
- `demo.mount` and `demo.action`
- `transition` with fade, directional slides/wipes, zoom, and morph effects plus configurable easing
- `layer.animate` for sustained pan, zoom, translation, and opacity changes on an existing visual
- `focus.show` and `focus.hide` for a labeled code or UI region
- `pointer.show`, `pointer.move`, `pointer.click`, and `pointer.hide` with `mouse` or `touch` styling
- `narration.cue` for narration synchronized to an action rather than the start of a scene
- `replay` for a visible fast rewind followed by a configurable slow-motion replay
- `bullets.show` for progressively revealed presentation slides
- `svg.show` for resolution-independent diagrams and artwork
- `image.show` for real screenshots and other source-grounded evidence; use `role: "evidence"`,
  `sourceTitle`, and `sourceUrl` for independent problem proof
- `diagram.show` for animated Mermaid-style flowcharts
- `intro.show` for legacy title-card scripts; new stories identify the product with a small in-scene brand layer
- `outro.show` for an end-screen-safe close with a specific next step and comment prompt

Bounds are normalized `{x,y,width,height}` values. An action or scene may supply `orientation.landscape` and `orientation.portrait` bounds. Landscape defaults to 1920×1080 and portrait to 1080×1920 at 30 fps.

A scene can define orientation-specific composition presets. `code-over-demo` keeps code and the live UI visible together vertically, while `code-left-demo-right` reserves the left side for code and the right side for the compiled demo. Give visual actions a `role` of `title`, `code`, `demo`, `brand`, or `evidence` to use the preset or identify its editorial purpose; explicit bounds still take precedence. A transition may use `effects.landscape` and `effects.portrait`, which supports a demo sliding in from the right only in landscape.

`focus.show` accepts a `target` layer and normalized `relativeBounds`. Pointer coordinates can be canvas-relative or relative to an `area` layer. A replay declares `fromMs`, `toMs`, and its later `atMs`. `rewindDurationMs` controls the fast reverse phase, `rewindFps` controls its sampled visual cadence, and `playbackRate` controls the forward replay (for example, `0.6` for slow motion). The rewind overlay appears at the top left and the optional replay `label` appears during the slow pass.

`text.show` automatically uses responsive wrapping for `VideoTitle` text; set `responsive` explicitly for other UIIDs and use `maxLines` to constrain fitting. Bullet-slide titles and items are also dynamically wrapped and font-fitted against their actual orientation bounds.

For new stories, start with a brief identity splash that says who Codename One is and names the
topic. Add the real logo through a small `image.show` action with `role: "brand"`; the logo remains
a signature while a topic-specific welcome graphic carries the frame. Keep Codename One code,
captures, and solution narration out of the following problem act. `intro.show` remains available
for legacy scripts.

`layer.animate` targets an existing layer. Use `fromX`/`toX` and `fromY`/`toY` as normalized canvas
offsets, `fromScale`/`toScale` for zoom, and `fromAlpha`/`toAlpha` for opacity. The renderer samples
every frame during the action and reuses frames during the surrounding static holds.

`outro.show` accepts `eyebrow`, `title`, `subtitle`, and `prompt`. Landscape leaves a quiet region on the right for YouTube's video element while keeping the message on the left. Portrait keeps the lower player controls clear and presents a single related-video CTA. The renderer never draws fake Subscribe or video controls; those remain real YouTube elements configured from the validated upload package.

`bullets.show` accepts a `title` and `items` array and reveals the items over `durationMs`. `svg.show` loads a `path` relative to the script. Use `paths.landscape` and `paths.portrait` when each orientation needs a separately authored diagram. `diagram.show` accepts `text` or `path`, with equivalent `texts` and `paths` orientation maps, using the CI-safe Mermaid subset below; nodes and arrows reveal progressively over `durationMs`:

```text
flowchart LR
A[Script] --> B[Compile]
B[Compile] --> C[Live UI]
C[Live UI] --> D[VideoIO]
```

On JavaSE, `svg.show` uses a deterministic CN1 vector fallback when the port has no native SVG decoder. The fallback supports the diagram-oriented SVG elements `g`, `rect`, `line`, `polyline`, `polygon`, `circle`, and `text`, including common fill, stroke, font, and text-anchor attributes. Unsupported SVG elements are ignored rather than rasterized through an external browser.

Transitions support `none`, `fade`, `slide-left`, `slide-right`, `slide-up`, `slide-down`, `wipe-left`, `wipe-right`, `wipe-up`, `wipe-down`, `zoom-in`, `zoom-out`, and `morph`. Choose `linear`, `ease-in`, `ease-out`, `ease-in-out`, or `spring` with `easing`; `effects` and `easings` can override the choice per orientation.

See [`examples/release-feature/video.json`](examples/release-feature/video.json) for all of these capabilities together, including slide transitions between explanatory material and a compiled live demo.

Compiled live demos implement `DemoScene` in the `demos` module. The JSON references the fully qualified class name. Rebuild after changing demo source so the exact code shown in the video compiles and runs against the snapshot APIs. `examples/release-feature/ar-demo-smoke.json` is a low-resolution regression fixture for the compiled AR session. A `live-demo` production script must drive at least two distinct `demo.action` state changes; pointer motion or a pan over a frozen capture never counts as interaction.
When a real demo genuinely uses a drag or swipe, `pointer.move` must include a `semanticIntent`
and align with the `demo.action` it drives. Cursor choreography without a state change fails QA.
Set `animated: false` on `demo.mount` when the component changes only at discrete `demo.action`
boundaries. The frame planner then reuses static images between actions while replay windows still
render at full frame rate. Leave it true or omit it for continuously animated application UI.

## Local narration

Narration is offline and opt-in per scene or timed `narration.cue`. No model is downloaded automatically.

- `kokoro`: configure/install [`kokoro-tts`](https://github.com/nazdridoy/kokoro-tts), and set a non-interactive voice.
- `piper`: configure the [Piper](https://github.com/OHF-Voice/piper1-gpl/blob/main/docs/CLI.md) executable and `narration.model`.
- `command`: provide an argument array containing placeholders such as `{input}`, `{output}`, `{text}`, `{voice}`, `{language}`, `{speed}`, and `{model}`.
- `auto`: prefer available Kokoro, then Piper, then the configured command.

One local Kokoro installation path is:

```bash
brew install uv
uv tool install kokoro-tts --python 3.12
mkdir -p examples/release-feature/.video-tools/kokoro
curl -fL https://github.com/nazdridoy/kokoro-tts/releases/download/v1.0.0/kokoro-v1.0.onnx -o examples/release-feature/.video-tools/kokoro/kokoro-v1.0.onnx
curl -fL https://github.com/nazdridoy/kokoro-tts/releases/download/v1.0.0/voices-v1.0.bin -o examples/release-feature/.video-tools/kokoro/voices-v1.0.bin
```

Generated WAV files are cached under the video project’s `.video-cache/narration`. Narration, music, and effects are normalized to 48 kHz stereo and combined with `AudioMixer`. By default, narration longer than its scene fails preparation; set the scene narration’s `overflow` to `extend` to let it continue and lengthen the final output when necessary.

Narration is serialized from measured WAV durations, so a later scene or cue waits until the prior voice has finished plus `narration.minimumGapMs` (180 ms by default). A cue can explicitly opt into layering with `allowOverlap: true`. Use `narration.pronunciations` for deterministic speech-only substitutions without changing on-screen copy. Generated CN1 videos load the shared `pronunciations/technical-en-us.json` index. It preserves terms that eSpeak NG already handles correctly and overrides only problematic spellings. Do not insert periods between acronym letters; Kokoro treats that punctuation as sentence-level timing.

```json
"narration": {
  "minimumGapMs": 250,
  "pronunciations": {
    "ARKit": "A-R kit",
    "ARCore": "A-R core",
    "AOT": "A-O-T"
  }
}
```

If a cue needs speech-directed spelling that differs from the transcript, put the phonetic form in
`text` and the normal written form in `caption`. Subtitle files use `caption`; synthesis uses
`text`. Prefer global `narration.pronunciations` when the same term recurs.

Every narrated render writes `<id>.srt` and `<id>.vtt` beside the MP4 files. Cue boundaries come from the measured generated WAV files rather than estimated reading speed, and the CLI JSON report includes paths and SHA-256 hashes for both subtitle formats. MCP render jobs return the subtitle paths as well.

For Kokoro, `narration.model` points to `kokoro-v1.0.onnx`; the provider loads `voices-v1.0.bin` from the same directory. Relative model paths are resolved against the video JSON file. The executable lookup also includes the standard uv tool directory at `~/.local/bin`.

Audit the index and create a short listening sample before rendering a video that introduces new technical terms:

```bash
python3 tools/audit_pronunciations.py --audition /tmp/cn1-pronunciation-audition.wav
```

Kokoro voices may be blended, for example `"voice": "af_heart:60,af_bella:40"`. This is useful for a warmer, less synthetic female delivery while keeping synthesis completely local and deterministic in CI.

## MCP

Start the built-in Codename One MCP server over stdio or a loopback socket:

```bash
java -jar javase/target/codenameone-video-builder-8.0-SNAPSHOT.jar mcp --stdio
java -jar javase/target/codenameone-video-builder-8.0-SNAPSHOT.jar mcp --port 8642
```

Domain tools expose the schema, validation, audio preparation, preview, asynchronous rendering, status, and cancellation. Standard Codename One UI inspection tools remain available for the preview window.
