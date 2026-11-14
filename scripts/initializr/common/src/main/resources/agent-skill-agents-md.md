# AGENTS.md

This project is a Codename One cross-platform mobile app (Java 17 / Maven /
ParparVM-iOS / Android / JavaScript / desktop). A vendor-neutral authoring skill
is bundled in this repository for any AI agent:

- **Start here:** `.agent-skills/codename-one/SKILL.md`
- **Topical references:** `.agent-skills/codename-one/references/`
- **Runnable utilities (Java 17 single-file source mode):** `.agent-skills/codename-one/tools/`

Tool integrations (Claude Code, Cursor, etc.) may also pick this skill up via
their own conventions; the canonical source of truth is `.agent-skills/`.

## Verify your work yourself -- you can run, see and drive the app

You do not need a human, a display or an MCP host restart to check a UI change.
Do this before reporting any UI work done:

```bash
./mvnw cn1:agent &                                   # simulator in the background, MCP from the first frame
S=.agent-skills/codename-one/tools/Sim.java
java $S wait                                      # blocks until the app answers (first build takes a while)
java $S snapshot                                  # the screen as JSON: every node, label, value, action
java $S tap "Sign In"                             # by label, or by node id from the snapshot
java $S type "Email" alice@example.com
java $S screenshot target/after.png               # then LOOK at the PNG
java $S record-start target/flow.mp4              # video of a flow (needs ffmpeg) ... record-stop
kill %1                                           # stop the simulator when done
```

- On Linux with no display `cn1:agent` starts Xvfb by itself; on macOS the window
  opens in the background and never takes the focus.
- If port 8765 is busy it picks another and writes it to `common/target/cn1-agent.port`;
  run `Sim.java` from the project root and it reads that file.
- Tests run headless too: `mvn -pl common cn1:test` / `mvn test` (Linux without a
  display: `xvfb-run -a mvn ...`).
- Native: the same `Sim.java` commands drive a development build on the iOS simulator
  or an Android emulator; `java $S device-screenshot ios|android out.png` and
  `device-record` capture them. See `references/mcp-agent-control.md`.
- Only if one of these steps actually fails, say so and quote the error -- never
  skip verification because "the environment is headless".

## Quick orientation for an agent

- App source lives in `common/src/main/java/`.
- Theme/styling lives in `common/src/main/css/theme.css` (Codename One CSS — a
  deliberate subset, see `.agent-skills/codename-one/references/css.md`).
- Run the simulator with `mvn -pl common cn1:run`.
- Run tests with `mvn -pl common cn1:test` (on Linux CI use `xvfb-run -a`).
- You can drive the RUNNING simulator yourself over MCP (read the screen, type,
  tap) - see `.agent-skills/codename-one/references/mcp-agent-control.md`.
- A bug that only reproduces on an Android phone or an iPhone is still debuggable:
  attach a Java debugger to the device build and drive it over MCP the same way;
  see `.agent-skills/codename-one/references/on-device-debugging.md`.
- Native cloud builds use `mvn package -Dcodename1.platform=<ios|android|javascript|javase> -Dcodename1.buildTarget=...`
  from the project root. A platform with no module of its own is built from `common/`.
- Local native-project builds (`-Dcodename1.buildTarget=ios-source`, `mac-source`,
  `android-source`) open Xcode / Android Studio when they finish. Always add
  `-Dopen=false` so you generate the sources without launching an IDE window.
- The server side, when the project has one, lives in `backend/` (Spring-style
  `@RestController` / `@Component`, resolved at build time). Run it with
  `CN1_PROFILE=dev mvn -pl backend -Dcodename1.platform=backend cn1:backend`; it then
  serves MCP tools at `http://127.0.0.1:8080/mcp` for inspecting and exercising it.
  See `.agent-skills/codename-one/references/backend.md`, and
  `references/full-stack-loop.md` for changes that span the app and the server.

When in doubt, open `.agent-skills/codename-one/SKILL.md` and follow the
reference table at the bottom.
