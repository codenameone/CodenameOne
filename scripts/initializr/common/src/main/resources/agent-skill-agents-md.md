# AGENTS.md

This project is a Codename One cross-platform mobile app (Java 17 / Maven /
ParparVM-iOS / Android / JavaScript / desktop). A vendor-neutral authoring skill
is bundled in this repository for any AI agent:

- **Start here:** `.agent-skills/codename-one/SKILL.md`
- **Topical references:** `.agent-skills/codename-one/references/`
- **Runnable utilities (Java 17 single-file source mode):** `.agent-skills/codename-one/tools/`

Tool integrations (Claude Code, Cursor, etc.) may also pick this skill up via
their own conventions; the canonical source of truth is `.agent-skills/`.

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
- Native cloud builds use `mvn -pl <ios|android|javascript|javase> package -Dcodename1.platform=... -Dcodename1.buildTarget=...`.
- The server side lives in `backend/` (Spring-style `@RestController` / `@Service`,
  resolved at build time). Run it with
  `CN1_PROFILE=dev mvn -pl backend -Dcodename1.platform=backend cn1:backend`; it then
  serves MCP tools at `http://127.0.0.1:8080/mcp` for inspecting and exercising it.
  See `.agent-skills/codename-one/references/backend.md`, and
  `references/full-stack-loop.md` for changes that span the app and the server.

When in doubt, open `.agent-skills/codename-one/SKILL.md` and follow the
reference table at the bottom.
