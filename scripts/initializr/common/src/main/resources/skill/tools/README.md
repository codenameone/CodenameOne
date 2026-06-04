# Agent Tools

These are small Java 17+ utilities meant to be invoked by an AI agent (or a developer) to answer a single yes/no question about a Codename One project. They use Java's single-file source-mode (Java 11+), so they need no compile step:

```
java tools/<ToolName>.java <args>
```

Each tool prints a one-line result on stdout and exits with status `0` for success, `1` for negative answer, `2` for a usage / discovery error. Diagnostic messages go to stderr.

The tools require **a Java 17+ JDK on `PATH`**. They auto-discover the Codename One jars under `~/.m2/repository/` — run `mvn -pl common compile` once in the consuming app to make sure that cache is populated.

## Available tools

### `IsApiSupported.java`

Checks whether a fully-qualified class name (or class#method) is part of the Codename One supported Java API subset.

```bash
java tools/IsApiSupported.java java.util.HashMap
# YES

java tools/IsApiSupported.java java.nio.file.Files
# NO

java tools/IsApiSupported.java java.util.HashMap#put
# YES (class present at java-runtime-7.0.242.jar!java/util/HashMap.class — for method-level confirmation run `javap -p -classpath …` and grep for `put`)
```

Useful when porting code from desktop Java and you want a quick "is this safe to use" check before discovering it at `mvn cn1:bytecode-compliance` time.

For the full picture of what's supported and what isn't, see `references/java-api-subset.md`.

### `IsCssValid.java`

Validates a `theme.css` file by running it through the Codename One CSS compiler. Reports `VALID` if the compiler produced a theme, `INVALID` plus the compiler's error message otherwise.

```bash
java tools/IsCssValid.java common/src/main/css/theme.css
# VALID
```

The tool loads the latest `codenameone-core` jar from `~/.m2` and invokes the compiler via reflection — no manual `-cp` setup needed.

This is **not** a substitute for running the simulator and looking at the result; it catches CSS *syntax* errors and unsupported properties, but a syntactically-valid file that styles the wrong UIID will still pass. Use it as a fast pre-flight before `mvn -pl common cn1:run`.

### `CompareToMockup.java`

Scores how closely a rendered screen matches a designer mockup and prints a similarity percentage.
Reports two numbers: `STRUCTURAL` (an SSIM-style perceptual score — the headline) and `PIXEL` (the
fraction of pixels within the framework's channel-delta tolerance). The render is auto-resized to
the mockup's dimensions first. Pure JDK — it does **not** need the CN1 jars in `~/.m2`.

```bash
java tools/CompareToMockup.java render.png mockup.png
# STRUCTURAL 0.912  PIXEL 0.874   (compared ... )

# Partial mode — mask device chrome the mockup includes but you don't render:
java tools/CompareToMockup.java render.png mockup.png --ignore-top 8% --diff diff.png
java tools/CompareToMockup.java render.png mockup.png --region 24,120,327,180
```

Options: `--region X,Y,W,H`, `--ignore X,Y,W,H` (repeatable), `--ignore-top N[%]`,
`--ignore-bottom N[%]`, `--resize fit|none`, `--diff out.png`, `--min 0..1` (exit 1 if structural
score is below it), `--json`. Exit codes: `0` compared (and above `--min`), `1` below `--min`,
`2` usage/IO error. See `references/mockup-comparison.md` for the full loop.

### `DesignImport.java`

Parses a Figma / Sketch / Adobe XD design — or an HTML/React design's CSS tokens (`tokens.css` /
`styles.css`, e.g. a Claude-generated mockup) — into a **starter** CN1 style: `theme.css`,
`tokens.json`, and `layout.md`. Fully self-contained (embedded JSON parser, JDK only); Figma mode
needs a token + network, the local formats need neither.

```bash
# Local ZIP+JSON formats
java tools/DesignImport.java design.sketch --out target/design-import
java tools/DesignImport.java design.xd     --out target/design-import
# HTML/React design tokens (a tokens.css/styles.css, or a dir containing one)
java tools/DesignImport.java tokens.css    --out target/design-import   # defaults to --px-per-mm 3.78
java tools/DesignImport.java design-dir/   --out target/design-import
# Figma over its REST API
java tools/DesignImport.java figma --token "$FIGMA_TOKEN" --file FILE_KEY --out target/design-import
```

The output is a starting point — refine it, validate the CSS with `java tools/IsCssValid.java
target/design-import/theme.css`, and measure convergence with `CompareToMockup.java`. Exit codes:
`0` imported, `1` nothing recognisable found, `2` usage/IO/network error.

## Adding more tools

The pattern is intentionally minimal — drop a new `.java` file into this directory and document it in this README. Constraints:

- Single Java source file. No external dependencies beyond what's already on the JDK plus the CN1 jars in `~/.m2`.
- One question per tool. Print one line of result on stdout. Send diagnostics to stderr.
- Exit codes: `0` success, `1` negative answer / validation failed, `2` discovery or usage error.
- Auto-discover the CN1 jars from `~/.m2/repository/com/codenameone/`. Tell the user when discovery fails and what to fix.
