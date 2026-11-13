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

## Verify your work yourself -- you can run, see and drive the app

You do not need a human, a display or an MCP host restart to check a UI change.
Do this before reporting any UI work done:

```bash
./gradlew cn1Agent &                              # simulator in the background, MCP from the first frame
S=.agent-skills/codename-one/tools/Sim.java
java $S wait                                      # blocks until the app answers (first build takes a while)
java $S snapshot                                  # the screen as JSON: every node, label, value, action
java $S tap "Sign In"                             # by label, or by node id from the snapshot
java $S type "Email" alice@example.com
java $S screenshot build/after.png               # then LOOK at the PNG
java $S record-start build/flow.mp4              # video of a flow (needs ffmpeg) ... record-stop
kill %1                                           # stop the simulator when done
```

- On Linux with no display run `xvfb-run -a ./gradlew cn1Agent &`; on macOS the window
  opens in the background and never takes the focus.
- If port 8765 is busy it picks another and writes it to `build/cn1-agent.port`;
  run `Sim.java` from the project root and it reads that file.
- Tests run headless too: `./gradlew cn1Test` / `./gradlew test` (Linux without a
  display: `xvfb-run -a ./gradlew ...`).
- Native: the same `Sim.java` commands drive a development build on the iOS simulator
  or an Android emulator; `java $S device-screenshot ios|android out.png` and
  `device-record` capture them. See `references/mcp-agent-control.md`.
- Only if one of these steps actually fails, say so and quote the error -- never
  skip verification because "the environment is headless".

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
- Local native-project builds (`./gradlew buildIosXcodeProject`,
  `buildAndroidGradleProject`) open Xcode / Android Studio when they finish. Always
  add `-Pcodename1.open=false` so you generate the sources without launching an IDE.
- cn1libs are declared by Maven coordinates in `build.gradle.kts`:
  `dependencies { cn1lib("group:artifact-lib:version") }`.
- A `backend/` directory, when present, is the app's server:
  `./gradlew :backend:runBackend`. `./gradlew addBackend` creates one.

When in doubt, open `.agent-skills/codename-one/SKILL.md` and follow the
reference table at the bottom.
