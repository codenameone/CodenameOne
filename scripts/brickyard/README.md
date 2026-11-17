# Brickyard

A complete user-generated-content game platform as a Codename One project.
Players discover and play experiences. Creators build them on the phone, or
have an AI agent build them, then publish them and earn from them. Admins keep
the place safe. The experiences are Luau scripts in a 3D world of parts, and
the platform is compatible with Luau and with Roblox place files: a creator can
bring their own `.rbxlx` or `.rbxl` export, and it runs.

Everything here is ours and written clean room: the interpreter, the engine
API, the file readers, the renderer and the templates. Nothing is fetched from
Roblox, and a place's assets come from the file or are replaced by
placeholders, which the compatibility report lists.

## Why it exists

A game platform tests a cross-platform framework harder than any other kind of
app. It needs a real scripting VM that cannot freeze the device, 3D rendering
and physics at frame rate, an authoritative multiplayer server, an economy with
store purchases and payouts, social features with child-safety rules,
moderation, and analytics, all from one codebase on iOS, Android, desktop and
the web. Brickyard does all of it in Java, on Codename One's own APIs: `gpu`
for the renderer, `payment` and `paywall` for the store, `analytics.invite`
for referrals, `gaming.GameServices` for presence and achievements, `ads` for
rewarded currency and the built-in MCP server for agentic creation.

## What is in the box

**The engine**, in two framework modules that any app can use:

- `maven/luau-runtime` is a pure-Java Luau interpreter. It covers the full
  syntax, coroutines as interpreter frames, and the standard libraries. Every
  script resume runs under an instruction budget, so an infinite loop in a
  creator's script is a reported error, never a hung app.
- `maven/rbx-compat` is the engine API over Codename One: datatypes, the
  instance tree, signals, services, GUI, tweens, pathfinding, data stores, the
  marketplace and badges. It also holds the place-file reader and writer, a
  compatibility report, a GPU part renderer with materials, lighting and
  effects, rigid-body physics, humanoid characters and multiplayer
  replication. Nine starter templates are included: an empty baseplate,
  two obbies, tycoon, arena, simulator, racing, survival and a roleplay town.

**The app** (`common`):

- **Players** browse home rows and experience pages, play in 3D with touch,
  keyboard or gamepad, and climb leaderboards. They level up, keep daily
  streaks and claim quests. They can edit a block avatar and buy Bricks or the
  Plus subscription. They can also add friends and invite them, and both sides
  of an invite get Bricks.
- **Creators** have an in-app studio with an explorer, a properties editor, a
  script editor with syntax checks, templates, undo, playtest, `.rbxlx`
  export and publishing. A dashboard shows visits, retention, revenue by
  product and error reports grouped by version, and lets them roll back
  versions, sell game passes and products, and request payouts.
- **Admins, moderators and support** use a console that holds the report
  queue, sanctions and appeals, fraud flags, payout approval, home-row
  curation, currency grants, payout jobs and an audit log of every staff
  action.

**The backend** (`backend`, with contracts in `shared`) covers accounts,
experiences with draft, publish and rollback, data stores, leaderboards
written only by the engine, a ledger-based economy, progression with a season
pass, and the social graph. Chat filtering depends on the reader's age, and
referral fraud checks write fraud flags. Moderation keeps an audit log, and
analytics is stored per experience.

## Agentic creation

Open an experience in the studio and choose **AI agent mode**. The studio's
tools are then registered with the app's built-in MCP server under the
`brickyard_` prefix, and an agent connected to the device or simulator can
build the experience itself:

| Tool | What it does |
|---|---|
| `place_tree`, `get_instance` | read the place |
| `create_instance`, `set_properties`, `delete_instance` | edit it |
| `write_script`, `read_script` | edit scripts; a write answers with the syntax check |
| `run_luau` | run a snippet against the place |
| `playtest` | play a saved copy with N players for N seconds; answers output, errors, leaderstats and unsupported API |
| `list_templates`, `apply_template` | start from a template |
| `add_effect`, `set_lighting` | fire, smoke, sparkles, bloom, sun rays, atmosphere, color grading, presets |
| `compat_report`, `import_place`, `export_place` | file I/O |
| `screenshot`, `publish` | look at it, ship it |

Every button in the studio goes through the same tools, so an agent can do
whatever a creator can, and its edits appear live in the explorer.

## Multiplayer

An experience runs on an authoritative server. Server Scripts never leave it,
and neither does anything in ServerScriptService or ServerStorage. Each client
receives a snapshot of the replicated services, followed by property deltas,
remote events and remote function calls. Each client drives its own character,
and the server accepts a move only if the humanoid could make it at its own
speed, correcting the client when it could not. Offline, the same place runs
with the server and client in one process.

## Running it

```bash
# From the repository root, JDK 8 for the framework (per-checkout repository):
cd maven
mvn -pl core,backend,backend-test,codenameone-maven-plugin,luau-runtime,rbx-compat -am install \
  -DskipTests -Plocal-dev-javase -Dmaven.repo.local=$PWD/../.m2-repo

# JDK 17 for the project:
cd ../scripts/brickyard
mvn -Dcodename1.platform=backend -pl shared,backend test -Dmaven.repo.local=$PWD/../../.m2-repo
mvn -pl common -am compile -Dmaven.repo.local=$PWD/../../.m2-repo
```

`.github/workflows/brickyard.yml` runs the engine tests, the server tests on
SQLite, PostgreSQL and MySQL, and the client compile.

## Limits worth knowing

- Store receipts are accepted only on the dev and test profiles until a store
  validator is configured; elsewhere the receipt endpoint answers 503.
- The server runs on the JVM backend runtime; rbx-compat needs the client core,
  so a ParparVM-compiled server is not supported yet.
- Assets a place file references by URL are replaced by placeholders, and the
  compatibility report names them.
