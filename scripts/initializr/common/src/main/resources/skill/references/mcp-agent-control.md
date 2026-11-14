# Driving the Running App — MCP

You can attach to the **running simulator** and drive the app yourself: read the screen, find a field by its label, type into it, tap a button, and call tools the app publishes. Codename One serves this over the [Model Context Protocol](https://modelcontextprotocol.io/), built on the same accessibility semantics tree that describes the screen to VoiceOver and TalkBack — so every screen is drivable with no extra code in the app.

This is the loop to reach for when a screen "looks right" in a screenshot but you need to know whether it *behaves* right: fill the form, press submit, read what the next screen says. It complements the other two loops in this skill — `tools/DumpForm.java` (a static model of one screen, no interaction) and `references/debugging.md` (`jdb`, for stepping through code).

## The agent loop: start it yourself, drive it from the shell

You do not need the menu, an MCP host, or a host restart. Start the simulator for
yourself in the background and talk to it with the bundled command-line client:

```bash
./mvnw cn1:agent &                 # Gradle: ./gradlew cn1Agent &   (Linux, no display: xvfb-run -a ./gradlew cn1Agent &)
S=.agent-skills/codename-one/tools/Sim.java
java $S wait                    # blocks until the first form answers
java $S snapshot                # ui_snapshot
java $S find "Email"            # ui_find by label
java $S tap "Sign In"           # ui_activate, by label or node id
java $S type "Email" alice@example.com   # ui_set_text
java $S screenshot target/s.png # the current form as a PNG -- open it and look
java $S record-start target/flow.mp4 ; java $S caption "Sign in" ; java $S record-stop
java $S tools                   # every tool the app publishes, including your own
java $S call sim_rotate '{"orientation":"landscape"}'    # any tool, raw JSON arguments
kill %1                         # stop the simulator
```

What `cn1:agent` does for you: the MCP server listens from the first frame (it is
`cn1:run` plus `-Dcn1.mcp.port`), the window never takes the focus or a Dock icon, and
on a Linux machine with no display the Maven goal starts Xvfb itself. When 8765 is
taken it picks a free port and writes it to `common/target/cn1-agent.port`
(`build/cn1-agent.port` under Gradle); run `Sim.java` from the project root and it
reads the file. Pass `-Dcn1.mcp.port=N` (`-Pcn1.mcp.port=N`) to insist on a port.

Every action returns a fresh snapshot, so one call tells you what the screen became.

## Native: the iOS simulator and an Android emulator

The same commands drive a development build running natively, because the app serves
the same MCP server. Start it in your app, gated to development builds:

```java
if (Display.getInstance().isDebuggableBuild()) {
    MCP.startSocketServer(8765);
}
```

**iOS simulator** (macOS with Xcode):

```bash
mvn package -Dcodename1.platform=ios -Dcodename1.buildTarget=ios-source -Dopen=false
#   Gradle: ./gradlew buildIosXcodeProject -Pcodename1.open=false
xcodebuild -workspace <the .xcworkspace the build logged> -scheme <App> \
    -sdk iphonesimulator -configuration Debug -derivedDataPath target/xcode build
xcrun simctl boot "iPhone 16" 2>/dev/null; xcrun simctl install booted target/xcode/Build/Products/Debug-iphonesimulator/<App>.app
xcrun simctl launch booted <your.package.name>
java $S wait                     # the simulator shares your loopback: no forward
java $S device-screenshot ios target/ios.png
java $S device-record ios target/ios.mp4 10
```

**Android emulator or device:**

```bash
mvn package -Dcodename1.platform=android -Dcodename1.buildTarget=android-source -Dopen=false
#   Gradle: ./gradlew buildAndroidGradleProject -Pcodename1.open=false
(cd <the generated Gradle project the build logged> && ./gradlew installDebug)
adb shell monkey -p <your.package.name> 1
adb forward tcp:8765 tcp:8765
java $S --port 8765 wait
java $S device-screenshot android target/android.png
java $S device-record android target/android.mp4 10
adb forward --remove tcp:8765
```

`sim_*` tools (recording, keyboard, rotate) exist only on the desktop simulator; on a
device use `device-screenshot` / `device-record`, which call `simctl` and `adb`.
`references/on-device-debugging.md` adds a Java debugger to the same session.

## For a human at the keyboard: the MCP menu

### Turn it on

Two halves, both one-time:

1. **Serve.** Run the simulator (`./gradlew run`, or `mvn -pl common cn1:run` in a Maven project), then in its menu bar choose **MCP -> Expose This Tool To Agents**. The simulator starts an MCP server on `127.0.0.1:8765`.
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

## Attaching to a build on a device

The socket server is not simulator-only. It binds wherever the platform can bind a loopback port, which includes an app running on an Android phone or an iPhone, so the same five tools drive the real build — semantic identifiers instead of coordinates guessed off a screenshot. The port is on the *device's* loopback rather than yours, so it needs a forward: `adb forward tcp:8765 tcp:8765` on Android, nothing at all on the native iOS simulator (it shares your machine's network stack), and a usbmux relay on a physical iPhone.

`references/on-device-debugging.md` has the whole sequence for both platforms, alongside the JDWP attach that usually goes with it.

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
- **Say when you could not check -- after trying.** A headless environment is not a reason: `cn1:agent` runs there. Report "could not verify" only when a step failed, and quote the failure.
