# Driving the Running App — MCP

You can attach to the **running simulator** and drive the app yourself: read the screen, find a field by its label, type into it, tap a button, and call tools the app publishes. Codename One serves this over the [Model Context Protocol](https://modelcontextprotocol.io/), built on the same accessibility semantics tree that describes the screen to VoiceOver and TalkBack — so every screen is drivable with no extra code in the app.

This is the loop to reach for when a screen "looks right" in a screenshot but you need to know whether it *behaves* right: fill the form, press submit, read what the next screen says. It complements the other two loops in this skill — `tools/DumpForm.java` (a static model of one screen, no interaction) and `references/debugging.md` (`jdb`, for stepping through code).

## Turn it on

Two halves, both one-time:

1. **Serve.** Run the simulator (`mvn -pl common cn1:run`), then in its menu bar choose **MCP -> Expose This Tool To Agents**. The simulator starts an MCP server on `127.0.0.1:8765`.
2. **Register.** In the same menu choose **MCP -> Install in MCP Hosts...**. This writes a server entry into the configuration of every MCP host it finds on the machine:

   | Host | Configuration it writes |
   | --- | --- |
   | Claude Desktop | `claude_desktop_config.json` (`mcpServers`) |
   | Claude Code | `~/.claude.json` (`mcpServers`) |
   | Codex — the ChatGPT desktop app, the Codex CLI, and the Codex IDE extension all share one file | `~/.codex/config.toml` (`[mcp_servers.<name>]`) |

   Your other servers and settings are left exactly as they were. **Restart the host afterwards**: every one of them reads its configuration at startup, so the new server does not appear until it does. `MCP -> Detect MCP Hosts...` lists what was found and where each configuration lives, and `MCP -> Remove From MCP Hosts...` takes the entry out again.

The entry launches a small bridge that relays the host's stdin/stdout to the running simulator's socket. That means **the simulator has to be running and serving** when you call a tool — you are driving the live window a human can watch, not a second headless copy.

If you are running the app yourself rather than through the menu, the same switch is one line of code:

```java
// In MyAppName.start(), for development builds only.
MCP.startSocketServer(8765);
```

## What you can call

Every server publishes these, with no work from the app:

| Tool | What it does |
| --- | --- |
| `ui_snapshot` | The whole screen as JSON: every node with its `id`, `role`, `label`, `value`, `state`, and the ids of the actions it supports. Start here. |
| `ui_find` | Nodes by application identifier, by a case-insensitive substring of their label, or by screen coordinate. Cheaper than a full snapshot when you know what you want. |
| `ui_activate` | Tap or click a node (`nodeId` from a snapshot or a find). |
| `ui_set_text` | Type into an editable node (`nodeId`, `text`). |
| `ui_perform_action` | Any other action a node advertises: `longPress`, `increment`, `decrement`, `focus`, `expand`, `scrollForward`, and so on. |

Every action runs on the Codename One EDT and returns whether it succeeded **plus a fresh snapshot**, so one call tells you what the screen became. The server also exposes a screenshot of the current form as an MCP image resource, which is worth reading when the semantic tree looks right and you suspect a layout or styling problem instead.

A typical loop: `ui_snapshot` to see the screen -> `ui_find` the field by label -> `ui_set_text` -> `ui_activate` the submit button -> read the returned snapshot to confirm what happened.

## Publishing the app's own tools

Anything the agent should be able to ask the app directly — not by driving its UI — is a `com.codename1.ai.Tool`, the same type the Codename One AI client uses, so a tool defined once serves both:

```java
MCP.addTool(new Tool(
        "current_user",
        "Returns the signed in user",
        "{\"type\":\"object\",\"properties\":{}}",
        new ToolHandler() {
            @Override
            public String invoke(String argumentsJson) {
                return "{\"name\":\"" + currentUser + "\"}";
            }
        }));
```

The server merges these with the built-in UI tools when a host lists what is available.

## Rules to respect

- **Development builds only.** The socket server refuses to bind on a release build and throws `IllegalStateException` instead: the loopback interface is shared by everything on a device, so any other installed app could otherwise drive yours. `MCP.setAllowOnReleaseBuilds(true)` lifts that, and is for a controlled fleet (a kiosk, a test lab, a managed deployment) — not for an app that ships to users.
- **Do not leave a starter in shipping code** on the assumption the gate will catch it. The JavaSE port cannot tell a packaged desktop app from the simulator, so it reports a development build in every case and the gate does not protect a desktop release.
- **Watch what you are doing.** `MCP -> Debug Logging` echoes the conversation to the Codename One log; `SUMMARY` gives one line per call, `FULL` gives every request and response.
- **Say when you could not check.** If the simulator is not running, or the environment is headless, say so instead of reporting that a flow works.
