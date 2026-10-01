# AGENTS.md

This project is a Codename One cross-platform mobile app (Java 17 / Gradle /
ParparVM-iOS / Android / JavaScript / desktop). It is a single Gradle project
built by the `com.codenameone` plugin declared in `settings.gradle.kts`. A
vendor-neutral authoring skill is bundled in this repository for any AI agent:

- **Start here:** `.agent-skills/codename-one/SKILL.md`
- **Topical references:** `.agent-skills/codename-one/references/`
- **Runnable utilities (Java 17 single-file source mode):** `.agent-skills/codename-one/tools/`

Tool integrations (Claude Code, Cursor, etc.) may also pick this skill up via
their own conventions; the canonical source of truth is `.agent-skills/`.

This is the **Gradle** layout. Wherever the skill shows a Maven command or a
`common/` path, use the Gradle column of its tables: there is no `common/`
module, the app lives at the project root, and every command is a `./gradlew`
task.

## Quick orientation for an agent

- App source lives in `src/main/java/` (Kotlin in `src/main/kotlin/`).
- Theme/styling lives in `src/main/css/theme.css` (Codename One CSS - a
  deliberate subset, see `.agent-skills/codename-one/references/css.md`).
- Build hints and app metadata live in `codenameone_settings.properties` at the root.
- Run the simulator with `./gradlew run`.
- Run tests with `./gradlew cn1Test` (on Linux CI use `xvfb-run -a`).
- You can drive the RUNNING simulator yourself over MCP (read the screen, type,
  tap) - see `.agent-skills/codename-one/references/mcp-agent-control.md`.
- A bug that only reproduces on an Android phone or an iPhone is still debuggable:
  attach a Java debugger to the device build and drive it over MCP the same way;
  see `.agent-skills/codename-one/references/on-device-debugging.md`.
- Native cloud builds are tasks: `./gradlew buildAndroid`, `./gradlew buildIos`,
  `./gradlew buildJavascript`, `./gradlew buildMacDesktop`; `./gradlew tasks` lists them all.
- cn1libs are declared by Maven coordinates in `build.gradle.kts`:
  `dependencies { cn1lib("group:artifact-lib:version") }`.
- A `backend/` directory, when present, is the app's server:
  `./gradlew :backend:runBackend`. `./gradlew addBackend` creates one.

When in doubt, open `.agent-skills/codename-one/SKILL.md` and follow the
reference table at the bottom.
