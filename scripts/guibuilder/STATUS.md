# Modern GUI Builder status

- Last updated: 2026-08-05
- Branch: `feat-guibuilder-rewrite`
- Working state: committed and rebased on `origin/master`

## Executive summary

The old settings-era GUI Builder has been replaced locally with a Maven-first,
standalone Codename One desktop application under `scripts/guibuilder`. The new
builder opens an entire Maven project rather than one form at a time, discovers
all `.gui` files recursively, switches between them, renders a live preview,
edits the XML model, generates Java and binding code, edits `theme.css`, exposes
accessibility semantics, and can be inspected or driven over MCP.

Guided Layout is the default for newly created containers. It is implemented on
top of `LayeredLayout` with persistent name-based relationships, responsive
insets, reference positions, size policies, baseline alignment, live simulated
drag/resize results, dependency visualization, detach behavior, multi-selection,
group movement, and group resizing.

The latest `origin/master` was fetched and fast-forwarded into this working tree
on 2026-07-28. The previous base was 33 commits behind. There were no overlapping
paths between the upstream changes and this GUI Builder work, so the update was a
clean fast-forward.

Current validation after that update and the 2026-07-28 review pass:

- 18 `CodeEditorTest` tests pass.
- 23 `LayeredLayoutTest` tests pass.
- 41 focused core tests pass in total.
- All 66 standalone GUI Builder tests pass (63 previous plus 3 new model tests).
- All 5 `OpenGuiBuilderMojoTest` tests pass (2 previous plus 3 new launcher tests).
- Both `git diff --check` and `git diff --cached --check` pass.

Important: the standalone suite only compiles after the current core is
reinstalled into the local repository it builds against. Running it against a
stale `8.0-SNAPSHOT` fails with `cannot find symbol
setProtectedRegionMarkers/setCursorPosition`, because the editor depends on
core APIs that are part of this same change. See
[Install current snapshots for the standalone build](#install-current-snapshots-for-the-standalone-build).

The implementation is substantially functional and test-backed, but it is still
a large uncommitted change. It should not be treated as shipped until it is
reviewed, staged intentionally, committed on a feature branch, and validated in
CI and through another deliberate hands-on GUI session.

## Original requirements and product direction

The work is based on these requirements:

- Replace the GUI Builder bundled with the old Codename One Settings.
- Package and distribute it like the standalone Maven-based Settings tool.
- Preserve the useful architecture of the old builder without retaining Ant-era
  assumptions or the old downloaded `guibuilder.jar` mechanism.
- Use modern Codename One UI constructs, menus, CSS, accessibility, and MCP.
- Detect all GUI Builder files in a project and switch between forms quickly.
- Prefer CSS and UIIDs over resource-file authoring.
- Provide source and event editing inside the application.
- Make Guided Layout the default and strong enough to construct arbitrary,
  responsive user interfaces.
- Support predictable drag/drop, live placement previews, resizing, alignment,
  baseline relationships, size policies, detach behavior, multi-selection, and
  atomic undo/redo.

The historical design reference is
`codenameone/CodenameOne#3175`. The legacy implementation used for behavioral
reference is the `GUIBuilder` module of the pre-Maven CodenameOne checkout, which
is not part of this repository.

## Repository and cleanup state

### Latest master

The branch is now exactly at:

```text
c1e839071d83ed84b79dae527ad738a8061e0b83
Let the grace passes mark objects the resolve guard was discarding (issue 5425) (#5477)
```

The update from `4c4fc3e327` to `c1e839071d` was a 33-commit fast-forward.
Upstream did not add or modify `scripts/guibuilder` or any other path that
collided with the local GUI Builder changes.

### Cleanup performed

The following unrelated or temporary material was discarded:

- `docs/website/static/social/` — untracked social artwork unrelated to the
  GUI Builder.
- `scripts/hellocodenameone/common/iosCerts/` — untracked local signing
  certificates and provisioning profiles.
- `scripts/hellocodenameone/common/androidCerts/` — ignored local Android
  keystore material.
- `scripts/hellocodenameone/common/target/` — unrelated ignored build output.
- Local signing paths and passwords in
  `scripts/hellocodenameone/common/codenameone_settings.properties` were
  restored to `HEAD`.
- The accidental stash was dropped after confirming that intended changes were
  already recovered. Its only non-recovered changes were the obsolete
  `CodeEditorHtml` and JavaSE Swing-editor path that were deliberately replaced
  by master’s pure Codename One editor.

No stash is required to recover the current GUI Builder work. The working tree
itself is now the authoritative local copy.

### Important staging warning

The worktree contains a mixture of staged, unstaged, and untracked changes
because it was recovered from an accidental stash and then adapted to master.
Most of `scripts/guibuilder` is still untracked. Before committing, inspect
`git status`, stage only the paths listed in this document, and do not use a
broad commit that could pick up unrelated files.

## Module and file map

### Standalone application

- `scripts/guibuilder/pom.xml`
  - Maven reactor root.
  - Java 17 source/target.
  - Modules for common and JavaSE code.
  - `guibuilder-central` release profile with sources, Javadocs, GPG signing,
    and Central publishing.
- `scripts/guibuilder/common/pom.xml`
  - Codename One common application module.
  - Compiles CSS and annotations through the Codename One Maven plugin.
  - Attaches common tests for the JavaSE test module.
- `scripts/guibuilder/javase/pom.xml`
  - Desktop executable artifact:
    `com.codenameone:codenameone-guibuilder`.
  - `executable-jar` profile builds the launcher JAR and copies runtime
    dependencies into `target/libs`.
- `scripts/guibuilder/common/src/main/java/com/codename1/guibuilder/CodenameOneGUIBuilder.java`
  - Main application, workspace, menus, inspector, drag/drop, resize,
    source/CSS editing, generation, undo/redo, accessibility state, and MCP
    domain operations.
- `scripts/guibuilder/common/src/main/java/com/codename1/guibuilder/GuiBuilderMcpController.java`
  - GUI Builder-specific MCP tools and live action journal.
- `scripts/guibuilder/common/src/main/java/com/codename1/guibuilder/model/GuiDocument.java`
  - XML model, normalized attributes, selection, clipboard operations,
    hierarchy changes, toolbar commands, transactions, undo/redo, and
    serialization.
- `scripts/guibuilder/common/src/main/java/com/codename1/guibuilder/project/ProjectBinding.java`
  - Parses the project-binding input file.
- `scripts/guibuilder/common/src/main/java/com/codename1/guibuilder/project/ProjectIO.java`
  - Portable file access, recursive `.gui` discovery, and UTF-8 reads/writes.
- `scripts/guibuilder/common/src/main/java/com/codename1/guibuilder/ui/ComponentPreviewFactory.java`
  - Converts XML elements to live Codename One components.
  - Applies component properties, layout constraints, stable accessibility
    identifiers, design-only event behavior, and safe preferred-size overrides.
- `scripts/guibuilder/common/src/main/java/com/codename1/guibuilder/ui/DragGuideOverlay.java`
  - Selection outlines, visible resize handles, drag ghosts, snap guides,
    dependency arrows, affected-component rectangles, and descriptive tags.
- `scripts/guibuilder/common/src/main/java/com/codename1/guibuilder/ui/GuidedLayoutSupport.java`
  - Persistent name-based Guided Layout constraints and size policies.
- `scripts/guibuilder/common/src/main/css/theme.css`
  - Builder UI theme, including light/dark styles, toolbars, inspector,
    sidebars, editors, selection controls, and canvas skins.
- `scripts/guibuilder/javase/src/desktop/java/com/codename1/guibuilder/CodenameOneGUIBuilderLauncher.java`
  - Desktop entry point.
- `scripts/guibuilder/javase/src/desktop/java/com/codename1/guibuilder/CodenameOneGUIBuilderStub.java`
  - Generated-style JavaSE lifecycle wrapper.
- `scripts/guibuilder/tools/guibuilder-mcp-client.mjs`
  - Small JSON-line MCP client used to inspect and drive the live builder.

### Demo project

`scripts/guibuilder/demo-project` is the reproducible manual-test project. It
contains:

- `GuidedLayoutForm.gui`
- `BorderDropForm.gui`
- `BoxXLayoutForm.gui`
- `GridLayoutForm.gui`
- `TableLayoutForm.gui`
- `NestedLayoutsForm.gui`
- `LoginForm.gui`
- `theme.css`
- `guibuilder.input`

The input file binds:

```text
projectDir=<project root>
guiDir=<project>/src/main/guibuilder
sourceDir=<project>/src/main/java
cssFile=<project>/src/main/css/theme.css
initialForm=com.example.GuidedLayoutForm
```

## Maven distribution and launcher integration

`maven/codenameone-maven-plugin/src/main/java/com/codename1/maven/OpenGuiBuilderMojo.java`
was rewritten to launch the standalone Maven artifact instead of downloading or
opening the old `~/.codenameone/guibuilder.jar`.

Current behavior:

- `mvn cn1:guibuilder` can run from the application aggregator or `common`
  module.
- `-DclassName=com.example.FormName` optionally selects the initial form.
- The mojo creates a unique binding file under
  `~/.codenameoneGUIBuilder/`.
- The binding covers the whole project: project directory, GUI directory,
  source directory, CSS file, and optional initial form.
- The launcher resolves `com.codenameone:codenameone-guibuilder` and its
  transitive runtime dependencies through Maven.
- It normally spawns a detached process and writes output to
  `~/.codenameoneGUIBuilder/guibuilder.log`.
- `-Dguibuilder.spawn=false` or the legacy `-Dspawn=false` path can run it in
  the foreground.
- A JVM property prevents duplicate launches within one Maven invocation.
- Every other `guibuilder.*` system property is forwarded to the editor JVM, so
  `mvn cn1:guibuilder -Dguibuilder.mcp.port=18349` (and `darkMode`,
  `canvasMode`, `initialSelection`, `openEditor`) reach the application. The
  `guibuilder.input` binding and the `guibuilder.spawn` flag are owned by the
  mojo and are never forwarded.
- The process is named for the dock, taskbar, and window manager, and the
  `java.desktop/com.apple.eawt*` packages the JavaSE port needs are exported,
  matching `cn1:settings` and `cn1:certificate-wizard`.
- The goal fails with a friendly message when Maven runs on a JDK older than
  17. The editor is a Java 17 artifact and the spawned process writes only to
  `guibuilder.log`, so without the check an old JDK produces a silent
  `UnsupportedClassVersionError`.
- No Ant GUI Builder protocol, running marker file, resource-file handoff, or
  IDE source-jump callback remains.

Release integration is present in:

- `.github/workflows/release-on-maven-central.yml`
- `maven/update-version.sh`

The release workflow deploys the executable JavaSE artifact after the main
Codename One deployment and polls Maven Central for it. The version update
script updates all three GUI Builder POMs and their Codename One/plugin
properties.

## Workspace and general UX

The builder uses a resizable three-area workspace:

- Left: project forms/hierarchy tabs plus the searchable component palette.
- Center: canvas-mode toolbar and live design surface.
- Right: Properties, Layout, and Events inspector tabs.

Nested `SplitPane` components make both sidebar boundaries resizable. Defensive
null guards were added to `SplitPane` and `Tabs` because the builder rebuilds
parts of the inspector and tab hierarchy during active pointer gestures.

The main toolbar exposes:

- Save
- Undo
- Redo
- Refresh
- CSS editor
- Companion Java editor

The canvas toolbar exposes:

- Phone portrait
- Phone landscape
- Tablet portrait
- Desktop full-canvas mode

Desktop mode has no device skin and fills the available center area. Phone and
tablet modes use predictable fixed design surfaces. Dark mode is persisted in
preferences and can be toggled through the application command path and MCP.

All project forms are discovered recursively beneath `guiDir`, sorted, shown
with simple names and fully qualified tooltips, and opened as independent
`GuiDocument` instances. Unsaved work is never silently discarded when
switching forms.

The component palette currently includes:

- Button
- Label
- SpanLabel
- TextField
- TextArea
- CheckBox
- RadioButton
- Slider
- Container
- Tabs

Palette items can be activated or dragged. Palette search filters immediately.

## Component model and property editing

`GuiDocument` preserves unknown XML while normalizing edited attributes
case-insensitively. It supports:

- Unique component names.
- Add, delete, copy, cut, paste, reorder, and cross-container moves.
- Cycle prevention when moving containers in the hierarchy.
- Compound transactions so one placement/group action is one undo entry.
- Toolbar commands stored separately from visual components.
- BorderLayout constraint normalization so duplicate or missing constraints do
  not hide components.
- Default UIIDs derived from component type when an explicit UIID is absent.
- Guided Layout as the default for new child-bearing components.

The Properties inspector supports type-aware fields, including:

- Name
- Form title
- Text
- Hint
- UIID/CSS selector
- Enabled, visible, RTL
- Alignment, gap, ticker
- Button toggle state
- CheckBox/RadioButton selected state
- Text-area columns, rows, maximum length, editable/grow settings, constraints
- Slider range/progress/editability
- Tabs selected index and placement
- Container horizontal/vertical scrolling

Numeric layout fields use bounded parsing and reject invalid text and overflow
instead of writing malformed values into the document.

Editing a text-bearing component or form title can be started through the
inspector, long press, or double click. The inline `TextField` is an overlay,
not a child inserted into the designed layout. Clicking outside commits and
tears it down; the XML property and visible preview update together.

Form toolbar commands can be added, removed, renamed, assigned to left/right/
overflow/side placement, and connected to generated event handlers.

## Layout adapter architecture

Every supported layout is routed through a placement adapter:

- `LayeredPlacementAdapter`
- `BorderPlacementAdapter`
- `BoxPlacementAdapter`
- `FlowPlacementAdapter`
- `GridPlacementAdapter`
- `TablePlacementAdapter`

This separation is important. A generic component-reorder algorithm cannot
correctly represent BorderLayout slots, TableLayout cells, Guided Layout
relationships, or BoxLayout insertion gaps.

### BorderLayout

- Edge bands are resolved before component hit testing so Center cannot consume
  all reachable East/West/North/South positions.
- The band size scales with the dragged component while remaining usable with a
  mouse.
- Dropping onto an occupied slot swaps components instead of stacking one
  behind the other.
- Missing and duplicate legacy constraints are normalized deterministically.
- The designer caps edge-component preferred dimensions to keep a displaced
  component from consuming the whole preview.
- Tests verify that a West-to-Center move remains visible, serializes, and
  renders after reopening.

### BoxLayout and FlowLayout

- A real temporary spacer opens at the exact insertion point.
- X and Y axes calculate before/after using the correct dimension.
- The spacer is removed on cancellation and completion.
- Reordering updates both the XML and rendered order.
- Scrollable previews disable tensile/bounce behavior.
- Edge dragging can advance a scrollable horizontal container without the
  native tensile animation fighting the drag.

### GridLayout

- Drops reorder children into deterministic grid order.
- Cells remain unique and components are not overlaid.
- Row/column values are validated.

### TableLayout

- Drops assign unique cells.
- Rows expand when necessary rather than hiding overflow.
- Reorder commands reassign cells and visibly change order.
- Row, column, spans, and optional percentage sizes are editable.

### Nested containers and form isolation

- Hit testing finds the deepest valid container.
- Moving across nested containers updates only the selected branch.
- A container cannot be moved into its own descendant.
- Stale elements from a previously opened form are rejected because every drag
  retains and validates its originating `GuiDocument`.

## Guided Layout

Guided Layout is represented by `LayeredLayout` plus builder-owned,
name-based metadata. Names are used because serialized component indexes become
unstable when siblings are reordered.

Per-component data includes:

- `layeredInsets`
- `guidedReferences`
- `guidedReferencePositions`
- horizontal and vertical anchors
- horizontal and vertical size policies
- optional matched-width/matched-height targets
- fixed preferred width/height values when explicitly resized

Supported size policies on each axis:

- Preferred
- Fixed
- Fill parent
- Match reference

Supported snap/alignment relationships:

- Left edge
- Horizontal center
- Right edge
- Top edge
- Text baseline
- Bottom edge
- Same width
- Same height
- Fill width
- Fill height

The `LayeredLayout` baseline implementation was corrected to use actual
component baselines, margins, and padding for components that declare a
baseline. `Component.getBaseline()` has a non-abstract default that returns the
bottom content edge rather than a text baseline, so the layout only trusts it
for components that also describe their baseline resize behavior (`Label` and
its subclasses: `Button`, `CheckBox`, `RadioButton`). Everything else
(`Container`, `TextArea`, `TextField`) keeps the historical font-ascent
approximation, so no existing layout changes behavior. See
[Known limitations](#known-limitations-and-remaining-work) for the follow-up:
`TextArea` should report a real text baseline.

### Drag behavior

- The initial pointer grab offset is preserved, so a component does not jump
  when dragging starts.
- Selection does not modify component styles or layout metrics.
- A live simulation clones the document and lays out the proposed result.
- The overlay shows the dragged ghost, every affected rectangle, snap lines,
  dependency arrows, and concise movement/size deltas.
- The simulated result and committed result are tested to match.
- Dependencies are visualized before commit so cascading effects are visible.
- Moving into free space tears away incoming relationships and persists fixed
  insets.
- Snapping back to the same reference preserves the explicit relationship.
- Cycle-breaking rebases or freezes only the relationship that would create the
  cycle, preventing the rest of the layout from bouncing or disappearing.
- Transitive dependents remain visible when the primary/secondary sample
  components are moved around each other.

### Resizing

- Edge and corner hit zones are larger than the painted handles.
- Cursor shapes change to the relevant horizontal, vertical, or diagonal resize
  cursor.
- The filled handles identify the stable reference component.
- Live resize simulation shows dependent cascade without mutating the model.
- Resizing the reference component can resize all selected components in
  unison.
- Same-width/same-height uses the primary selected component as the reference;
  other components stretch to it instead of all components shrinking to an
  arbitrary minimum.
- Fixed guided dimensions are implemented through `calcPreferredSize()`
  overrides in generated/preview components. Deprecated
  `setPreferredW()`, `setPreferredH()`, or similar APIs are not used.

### Multi-selection

- Shift, Control, or Command click toggles additive selection.
- A normal click selects a new component without requiring an initial drag.
- Every selected component receives its own outline and handles.
- The first/primary selected component remains the reference and uses filled,
  larger handles.
- Group drag preserves relative positions and internal relationships.
- Group drag and group resize commit atomically and undo in one step.
- The floating icon toolbar is shown only for meaningful multi-selection in one
  Guided Layout parent.
- Every icon has a tooltip and accessibility label explaining that the
  filled-handle component is the reference.
- Actions cover left/center/right, top/baseline/bottom, same width, same
  height, and disconnect.

## Undo/redo

Structural and layout changes are stored in `GuiDocument` snapshots. Compound
operations use explicit transactions, making a group move, group resize, slot
swap, or relationship action one undo step.

The embedded pure `CodeEditor` has its own undo/redo stack. CSS and Java editing
remain inside the editor rather than routing Command-Z to close the editor.

Form-model undo/redo and source-editor undo/redo are separate domains. A future
polish pass may expose which domain currently owns the global shortcut more
explicitly.

## CSS and theme editing

The CSS button opens the project `theme.css` in the built-in Codename One
`CodeEditor`, not a native external application or CEF-based editor.

Current behavior:

- Editable pure Codename One source surface.
- CSS syntax highlighting and editor gutter come from the modern `CodeEditor`.
- A short debounced live-edit timer reads the editor text.
- `CSSThemeCompiler` compiles into an in-memory `MutableResource`.
- The compiled project theme is applied to the preview immediately.
- The builder theme is restored after preview styling, preventing the builder
  chrome from inheriting arbitrary project styles.
- Compile failures produce editor diagnostics and leave the last valid preview
  intact.
- Saving writes UTF-8 back to the project CSS file.
- Font and box-unit values from the runtime compiler are normalized for safe
  preview use.

The project preview is CSS-driven; the standalone builder still has a compiled
`theme.res` generated from its own `theme.css` as a normal Codename One build
artifact. This is not a return to resource-file authoring.

## Java source, events, and binding

The Code toolbar opens an editable, live-generated companion source preview
inside the builder.

Generated source contains explicit markers:

```java
// <gui-builder-generated>
// </gui-builder-generated>
// <gui-builder-user-code>
// </gui-builder-user-code>
```

The generated regions are protected in the editor while the user-code region
remains editable. Regeneration merges the existing user-code region instead of
deleting it. The editor can move the caret directly to a generated event
handler.

Generated code includes:

- Form title and layout.
- Component fields and construction.
- Names and UIIDs.
- BorderLayout and TableLayout constraints.
- Guided Layout insets, name-resolved references, reference positions, and
  anchors.
- Toolbar commands.
- Component and command action handlers.

Binding strategy is selectable per form:

- None
- `PropertyBusinessObject`
- `@Bindable` POJO using build-time binding annotations

Generated-source tests compile the form and model together for all three
strategies. No-binding output has no model dependency.

## Accessibility

The builder uses the accessibility APIs added on master:

- Stable identifiers for workspace, forms list, hierarchy tree, palette,
  inspector, status, canvas modes, selection actions, and preview components.
- Roles for lists, tree, search field, and generic container groups.
- Labels, descriptions, hints, selected state, pane titles, and grouping.
- A polite live region for status updates.

Preview component identifiers follow:

```text
guibuilder.preview.<component-name>
```

These identifiers are also published in MCP state, allowing tests and agents to
correlate XML elements, physical bounds, and accessibility nodes.

## MCP support and live inspection

The builder registers the portable UI accessibility MCP tools provided by
Codename One and adds domain-specific tools:

- `guibuilder_state`
  - Active form and path
  - All form names
  - Canvas mode and bounds
  - Dark mode
  - Modified/undo/redo state
  - Selected component(s)
  - Selection paint bounds
  - Component attributes, layouts, bounds, visibility, and accessibility IDs
  - Current drop/resize guide
- `guibuilder_actions`
  - Sequence-numbered journal
  - Optional long poll up to ten seconds
  - Bounded history of 500 actions
- `guibuilder_select`
  - Normal or additive selection by component name
- `guibuilder_open_form`
  - Safe form switching that refuses to discard unsaved work
- `guibuilder_drag`
  - Drives the real pointer drag path by absolute coordinates or semantic
    target/placement
- `guibuilder_command`
  - Save, undo, redo, refresh, dark mode, and canvas-mode commands

The socket is opt-in through:

```text
-Dguibuilder.mcp.port=18349
```

The verified endpoint is loopback:

```text
127.0.0.1:18349
```

The MCP controller crosses an EDT barrier after commands before returning
state, preventing new component bounds from being paired with stale selection
bounds.

## Tests

### Standalone GUI Builder: 63 tests

`DesignerInteractionTest` has 41 tests covering:

- Fixed width without freezing theme-derived height
- Exact BoxLayout spacer targets
- Selection without drag
- modifier-based multi-selection
- reference-based same width
- atomic group movement and undo
- group ghost/commit equivalence
- internal group relationship preservation
- guided rectangle persistence
- pointer grab offsets
- responsive surface-edge docking
- free-space detach
- explicit relationship preservation
- cycle prevention and downstream visibility
- actual baseline alignment
- center anchors
- selection/layout metric stability
- resize snapping
- group resize
- resize simulation
- drag simulation
- BorderLayout swap/serialization
- inline editor teardown
- explicit placement adapter routing
- reachable BorderLayout edges
- numeric validation
- BoxLayout X
- GridLayout
- TableLayout placement/reorder
- nested placement
- cross-form stale-element rejection
- horizontal scrolling and drag autoscroll
- accessibility identifiers
- MCP state, action journal, and additive selection

Additional standalone tests:

- `GuiDocumentTest`: 18, including the three added by the review pass:
  - a pasted container renames its children and keeps its internal
    relationships pointed at the copy,
  - renaming keeps names unique and repoints every relationship in one undo
    step,
  - deleting a referenced component leaves no dangling relationship.
- `GeneratedSourceTest`: 4
- `CodeEditorInteractionTest`: 2
- `ProjectBindingTest`: 1

Latest result:

```text
Tests run: 66, Failures: 0, Errors: 0, Skipped: 0
```

### Core tests: 41

Latest focused result:

```text
CodeEditorTest:    18 passed
LayeredLayoutTest: 23 passed
Total:             41 passed
```

The two new editor tests verify:

- Protected generated regions reject user edits while the user-code region
  remains editable.
- `setCursorPosition()` moves the pure editor caret.

The LayeredLayout regression verifies actual baseline alignment across
different component margins and padding.

### Maven launcher tests: 5

`OpenGuiBuilderMojoTest` verifies:

- Binding files contain project-wide GUI, source, CSS, and initial-form data.
- The goal launches correctly from either the application aggregator or common
  module without duplicate launches.
- `guibuilder.*` properties are forwarded while the binding and spawn flag are
  not.
- The desktop identity arguments export the JDK packages the JavaSE runtime
  needs.
- The running Java feature version is detected (the input to the JDK 17 gate).

Latest result:

```text
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
```

### Non-failing test noise

- The JavaSE interaction suite prints many `EDT violation detected!` diagnostic
  lines. The assertions pass, but this noise should eventually be investigated
  or suppressed by making the tests consistently cross the EDT.
- The plugin test prints an HTML tidy warning:
  `line 23 column 45 - Error: <serial> is not recognized!`
  It is non-fatal and unrelated to the GUI Builder assertions.
- The supplied `-Plocal-dev-javase` core-test profile is not defined at the
  top-level Maven reactor in this checkout; Maven warns and continues. The
  focused tests still compile and pass with Java 8.

## Build and test commands

### Core regression tests

Run with Java 8:

```bash
cd maven
export JAVA_HOME="$(/usr/libexec/java_home -v 1.8)"   # any JDK 8
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl core-unittests -am \
  -DunitTests=true \
  -Dmaven.javadoc.skip=true \
  -Plocal-dev-javase \
  -Dcn1.binaries="$PWD/target/cn1-binaries" \
  -Dtest=CodeEditorTest,LayeredLayoutTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

### Install current snapshots for the standalone build

The isolated repository used during development is
`/tmp/cn1-local-repo`. Reinstall current core, CSS compiler, JavaSE, and Maven
plugin artifacts after changing branches or updating master. Otherwise Maven
can silently test against an older `8.0-SNAPSHOT`.

This is a prerequisite, not an optimization. The editor calls
`CodeEditor.setProtectedRegionMarkers()` and `setCursorPosition()`, which are
added by this change, so against an older snapshot the standalone build fails
with `cannot find symbol` before any test runs. Reinstalling `core` alone is
enough after editing only `CodenameOne/src`.

JavaSE must be built with `cn1.binaries` so `jfxrt.jar` is available:

```bash
cd maven
mvn -Dmaven.repo.local=/tmp/cn1-local-repo \
  -pl factory,core,css-compiler \
  -DskipTests -Dmaven.javadoc.skip=true install

mvn -Dcn1.binaries="$PWD/target/cn1-binaries" \
  -Dmaven.repo.local=/tmp/cn1-local-repo \
  -pl javase \
  -DskipTests -Dmaven.javadoc.skip=true \
  clean install

mvn -Dmaven.repo.local=/tmp/cn1-local-repo \
  -pl codenameone-maven-plugin \
  -DskipTests -Dmaven.javadoc.skip=true install
```

### Full GUI Builder suite

Run with Java 21 because the standalone project targets Java 17 and the
executable build was validated with the installed Azul 21 JDK:

```bash
cd scripts/guibuilder
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"    # JDK 17 or newer
export PATH="$JAVA_HOME/bin:$PATH"
mvn -nsu \
  -Dmaven.repo.local=/tmp/cn1-local-repo \
  -pl javase -am clean test
```

On macOS the CSS compiler may exit with code 134 inside the filesystem/process
sandbox. Run the same Maven command with normal desktop/native process access;
this is a process-environment issue, not a CSS syntax failure.

### Maven launcher test

```bash
cd maven
mvn -nsu \
  -Dmaven.repo.local=/tmp/cn1-local-repo \
  -pl codenameone-maven-plugin \
  -Dtest=OpenGuiBuilderMojoTest \
  test
```

### Package executable

```bash
cd scripts/guibuilder
mvn -nsu \
  -Dmaven.repo.local=/tmp/cn1-local-repo \
  -pl javase -am \
  -Pexecutable-jar \
  -DskipTests package
```

Expected main artifact:

```text
scripts/guibuilder/javase/target/codenameone-guibuilder-8.0-SNAPSHOT.jar
```

### Run the demo with MCP

The binding file holds absolute paths, so it is generated per machine rather
than tracked (see `scripts/guibuilder/.gitignore`). `mvn cn1:guibuilder` writes
one for a real project; for the demo project, write it once:

```bash
cd scripts/guibuilder/demo-project
cat > guibuilder.input <<EOF
projectDir=$PWD
guiDir=$PWD/src/main/guibuilder
sourceDir=$PWD/src/main/java
cssFile=$PWD/src/main/css/theme.css
initialForm=com.example.GuidedLayoutForm
EOF
```

Then launch the packaged editor against it:

```bash
cd scripts/guibuilder
java \
  -Dguibuilder.input="$PWD/demo-project/guibuilder.input" \
  -Dguibuilder.mcp.port=18349 \
  -jar javase/target/codenameone-guibuilder-8.0-SNAPSHOT.jar
```

In another terminal:

```bash
cd scripts/guibuilder
node tools/guibuilder-mcp-client.mjs 18349
```

Client commands:

```text
tools
{"tool":"guibuilder_state"}
{"tool":"guibuilder_select","arguments":{"component":"primary"}}
{"tool":"guibuilder_select","arguments":{"component":"secondary","additive":true}}
{"tool":"guibuilder_drag","arguments":{"component":"primary","target":"secondary","placement":"below"}}
{"tool":"guibuilder_command","arguments":{"command":"undo"}}
```

## Important implementation lessons

1. **Never use `setPreferredW()`, `setPreferredH()`, or other deprecated
   preferred-size setters.** Guided fixed sizes are implemented with
   `calcPreferredSize()` overrides and model metadata.
2. **The new pure Codename One editor is the correct editor backend.** Do not
   restore the old CEF editor, `CodeEditorHtml`, or a private Swing
   `NativeCodeEditorState`.
3. **Clean builds matter when classes are deleted.** An incremental JavaSE JAR
   once retained stale nested Swing-editor classes in `target/classes`, causing
   a reflection test to pass against code that no longer existed in source.
4. **Snapshot provenance matters.** The GUI Builder can compile against old
   `8.0-SNAPSHOT` artifacts unless current branch artifacts are reinstalled in
   the exact local repository used by its Maven build.
5. **JavaSE needs the repository’s `cn1.binaries/jfxrt.jar`.** Building on a JDK
   without JavaFX and without the `cn1.binaries` property produces a large,
   misleading set of missing `javafx.*` errors.
6. **CSS compilation needs native desktop access on macOS.** Exit 134 in the
   sandbox was environmental; the same clean build passed outside it.
7. **Pointer interactions must be tested as complete gestures.** Selection,
   arming, threshold crossing, preview, release, model mutation, relayout, and
   overlay cleanup all need assertions. A test that calls only the model helper
   is insufficient.
8. **The preview rectangle and selection rectangle must come from the same
   layout generation.** MCP intentionally waits across an EDT barrier after a
   command to prevent stale-offset reports.
9. **A drag simulation must use a cloned document.** Mutating the real document
   during hover produces bounce, unpredictable dependency changes, and broken
   undo.
10. **Layout semantics cannot be generalized.** Border slots, box insertion,
    table cells, grids, nested containers, and Guided Layout constraints need
    explicit strategies.
11. **BorderLayout must swap occupied slots.** Allowing two components in
    Center is what caused components to “vanish” behind one another.
12. **Form isolation must use element identity plus document identity.** A stale
    element from another form must never enter the active model.
13. **Selection styling must not affect preferred size.** The overlay paints
    selection; preview components are not given borders/padding that alter
    layout.
14. **Project CSS must never theme the builder chrome.** Apply it temporarily to
    preview components and restore the builder theme in `finally`.
15. **Generated code must clearly separate owned and user regions.** Protect
    generated markers in the editor and merge only the user-code region.
16. **Tensile scrolling is harmful in a designer.** It is disabled on preview
    containers so dragging does not trigger bounce animation.
17. **Inline editing must remain an overlay.** Inserting the temporary editor
    into the designed container corrupts layout and serialization.
18. **The primary multi-selection member is a stable reference.** It must remain
    visually distinct, drive align/size actions, and not change simply because
    another member is dragged.

## Reading the canvas styling

The design canvas resolves its look and feel from a `UIManager` of its own
(`Container#setUIManager`), holding the theme compiled from the project's
`theme.css`. The builder's chrome keeps the global one. Both work; a canvas
component's resolved style is reported over MCP as `style.fgColor` /
`style.bgColor` alongside the `UIManager` that resolved it.

When comparing a resolved colour against the stylesheet, account for
`@media (prefers-color-scheme: dark)`. The builder runs in dark mode by
default, so a dark block overrides the light declarations, and the flattened
theme carries both. A resolved value that does not match the light-mode
declaration is not evidence of a problem -- during this review it was
mistaken for the project stylesheet being ignored, and it was not.

## Review pass 2026-07-28: defects found and fixed

A code review of the whole change found the following defects. All of them are
fixed and covered by tests in this working tree.

### The model let name-based relationships rot

Guided Layout, the simulation clone, `equivalentElement()`, and the generated
Java fields all identify components by name, but nothing kept names unique or
maintained the references:

1. **Renaming silently destroyed relationships.** The Name field wrote the
   `name` attribute on every keystroke and never touched
   `guidedReferences`, `guidedMatchWidth`, `guidedMatchHeight`, or
   `guidedReferenceTarget`. Every relationship pointing at the old name became
   dangling. A duplicate name was also accepted, which breaks name lookups and
   emits two Java fields with the same identifier.
   Fixed by `GuiDocument.renameSelected()`, which forces a unique name and
   repoints every reference in one undo step, plus a dedicated inspector field
   that commits on Enter or focus loss rather than per keystroke and reports the
   name it actually applied.
2. **Deleting a referenced component left dangling references.** Its
   dependents kept an inset measured from a component that no longer existed.
   `deleteSelected()` now clears references to every name in the removed
   subtree, and a `match` size policy whose target disappeared falls back to
   `preferred`.
3. **Paste only renamed the pasted root.** Pasting a container produced
   duplicate names for all of its children and left their internal
   relationships pointing at the original copy. Paste now renames the whole
   subtree and rewrites only the references inside it, so the original keeps
   its own relationships.

### Opening a source editor twice hid the canvas

`openCss()` rebuilt the canvas before installing its split pane, but
`openCompanionSource()` and `openBindingModel()` did not. Pressing Code twice
nested a second `SplitPane` whose right side was the first editor, pushing the
design surface out of reach. Both now rebuild the canvas first.

### The launcher dropped everything except the binding

`cn1:guibuilder` forwarded only `guibuilder.input`, so
`-Dguibuilder.mcp.port`, `-Dguibuilder.darkMode`, `-Dguibuilder.canvasMode`,
`-Dguibuilder.initialSelection`, and `-Dguibuilder.openEditor` did nothing when
the builder was started the documented way; the MCP socket could only be
reached by launching the JAR by hand. It also omitted the desktop identity and
`--add-exports` arguments that `cn1:settings` and `cn1:certificate-wizard` pass,
and it forked the editor with whatever JVM ran Maven, so a JDK 8 or 11 Maven
produced an `UnsupportedClassVersionError` visible only inside
`guibuilder.log`. All three are fixed.

### The baseline change was wider than intended

`Component.getBaseline()` is not abstract: its default returns the bottom
content edge, so it never reports "no baseline". The new `LayeredLayout`
baseline path therefore applied to every component and its documented
font-ascent fallback was unreachable, which changed `UNIT_BASELINE` for plain
containers and text areas. The layout now uses a reported baseline only when
the component also describes its baseline resize behavior, so `Label`, `Button`,
`CheckBox`, and `RadioButton` align exactly and everything else keeps the
previous behavior.

### Nothing in CI compiled the editor

`scripts/guibuilder` is outside the Maven reactor and `pr.yml` ignores
`scripts/**`, so no job built the editor. This is not theoretical: the tree as
reviewed did not compile against the `8.0-SNAPSHOT` in the development
repository, because the editor uses `CodeEditor` APIs added by this same change.
`.github/workflows/guibuilder.yml` now builds Codename One on JDK 8, runs the
standalone suite on JDK 17 under `xvfb`, and packages the executable JAR,
triggered by `scripts/guibuilder/**`, the core editor/layout files it depends
on, and the mojo. That workflow has not run on GitHub yet; the first PR that
includes it will validate it.

### Smaller cleanups

- `ComponentPreviewFactory` set `BuilderPreviewContainer` on preview containers
  and then immediately overwrote it with the element's own UIID. The dead
  assignment and its two unreachable `theme.css` rules were removed; preview
  containers are intentionally styled by the project CSS.
- Renaming through the live-preview path set the component name without the
  `preview.` prefix the rest of the designer relies on.

## Known limitations and remaining work

The following should be treated as open work, even though the automated suite
is green:

1. Perform a fresh hands-on pass on the latest master build, especially:
   - long multi-selection drag sessions,
   - group resize from every handle,
   - repeated detach/resnap cycles,
   - baseline alignment across more UIIDs/fonts,
   - canvas resizing after several relationship edits,
   - nested scrollable containers,
   - rename, delete, and paste against components that other components are
     anchored to, which the review pass fixed at the model level but has not yet
     been exercised by hand.
2. Reduce or eliminate the JavaSE test suite's EDT-violation diagnostics.
3. Add first-class keyboard shortcuts and make the active undo domain
   (document versus embedded editor) explicit in the UI.
4. Expand the palette and property coverage beyond the current ten component
   types.
5. Add richer CSS completion. Syntax highlighting, diagnostics, gutter, and
   live compilation are present, but completion is not yet a full CSS language
   service.
6. Add a more complete source completion/event-navigation experience.
7. Consider direct resize/dock affordances for percent and dual-edge constraints
   instead of exposing advanced insets as text.
8. GridBagLayout and MigLayout are not supported. They were considered
   optional/too large for the current pass.
9. Flow/Grid/Table currently share the ordered-insertion base planner and then
   normalize their model. More specialized visual guides could improve them.
10. Validate Central publishing in the real release workflow. Local packaging
    and launcher resolution are tested; no release was published from this
    worktree.
11. Add a project-level README/user guide for the GUI Builder module.
12. Decide whether the MCP socket should have a UI preference/indicator beyond
    the current system property and status label.
13. Review the large `CodenameOneGUIBuilder.java` class for safe extraction into
    editor, inspector, placement, source-generation, and MCP-facing services
    after behavior is stable. Do not refactor it merely for aesthetics before
    the interaction contract is locked down.
14. `TextArea` and `TextField` do not report a text baseline, so baseline
    alignment between a label and a text field still uses the font-ascent
    approximation. Overriding `getBaseline()`/`getBaselineResizeBehavior()` on
    `TextArea` would make the common login-form case exact, but it also affects
    `FlowLayout` baseline alignment and needs its own tests.
15. Undo history keeps a full XML snapshot per edit with no bound. A long
    session on a large form grows without limit; cap the stack or store deltas.
16. There is no keyboard shortcut path to cut/copy/paste/delete. The operations
    exist and are reachable through the static application hooks and MCP, but
    not from the keyboard. This overlaps item 3.

## Road forward

The work is feature-complete enough to review. The ordering below front-loads
the things that make it reviewable and keeps it from rotting, and defers the
things that are easier to judge once it is on a branch.

### Phase 1: get it onto a branch, unchanged in behavior (done)

1. Branch `feat-guibuilder-rewrite`, rebased on `origin/master`.
2. Staged exactly the paths in
   [Intended change paths](#intended-change-paths) and nothing else. Most of
   `scripts/guibuilder` was untracked, so explicit paths were used, never
   `git add -A`. `scripts/guibuilder/*/target/` and the generated
   `demo-project/guibuilder.input` stay out via the module's `.gitignore`.
3. Re-ran the three suites in
   [Build and test commands](#build-and-test-commands), in that order, after
   reinstalling the current core snapshots. The core install step is not
   optional.
4. Pushed and opened a PR.
5. `guibuilder.yml`, `pr.yml`, and the static-analysis gates run on it. Expect to
   fix workflow details on the first run; that is the point of doing it before
   any further feature work.

### Phase 2: prove the interaction model by hand

6. Package the executable JAR, launch it against `demo-project`, and work
   through item 1 of
   [Known limitations](#known-limitations-and-remaining-work).
7. Drive the same session over MCP with `tools/guibuilder-mcp-client.mjs` and
   keep the action journal, so any regression can be replayed.
8. Turn every defect found into a failing `DesignerInteractionTest` first, then
   fix it. The suite's value is that it tests complete gestures; keep that
   property.

### Phase 3: make it usable by someone who did not write it

9. Add `scripts/guibuilder/README.md`: how to run it from a project, how to run
   it from source, the `guibuilder.*` properties, and the MCP surface.
10. Document `mvn cn1:guibuilder` in the developer guide next to `cn1:settings`,
    including the JDK 17 requirement.
11. Add first-class keyboard shortcuts and make the active undo domain explicit
    (items 3 and 16).

### Phase 4: breadth

12. Expand the palette and the property inspector beyond the current ten
    component types (item 4). This is the most visible gap for real projects.
13. Improve CSS and Java completion (items 5 and 6).
14. Specialised visual guides for Flow/Grid/Table (item 9).
15. Consider extracting services out of `CodenameOneGUIBuilder.java` once the
    interaction contract has stopped moving (item 13).

### Phase 5: release

16. Cut a release and confirm the `guibuilder-central` profile actually publishes
    `com.codenameone:codenameone-guibuilder` and that `cn1:guibuilder` resolves
    it from Central on a clean machine (item 10). Until that round trip is
    proven, the distribution story is untested.

## Intended change paths

The current GUI Builder change should be limited to:

```text
.github/workflows/guibuilder.yml
.github/workflows/release-on-maven-central.yml
CodenameOne/src/com/codename1/components/SplitPane.java
CodenameOne/src/com/codename1/ui/CodeEditor.java
CodenameOne/src/com/codename1/ui/Tabs.java
CodenameOne/src/com/codename1/ui/editor/CodePureEditor.java
CodenameOne/src/com/codename1/ui/editor/CodeView.java
CodenameOne/src/com/codename1/ui/editor/PureEditor.java
CodenameOne/src/com/codename1/ui/layouts/LayeredLayout.java
maven/codenameone-maven-plugin/src/main/java/com/codename1/maven/OpenGuiBuilderMojo.java
maven/codenameone-maven-plugin/src/test/java/com/codename1/maven/OpenGuiBuilderMojoTest.java
maven/core-unittests/src/test/java/com/codename1/ui/CodeEditorTest.java
maven/core-unittests/src/test/java/com/codename1/ui/layouts/LayeredLayoutTest.java
maven/update-version.sh
scripts/guibuilder/**
```

This document lives at `scripts/guibuilder/STATUS.md`, next to the code it
describes.

Anything outside that list should be treated as suspect and reviewed before
staging.
