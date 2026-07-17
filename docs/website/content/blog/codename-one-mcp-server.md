---
title: "Your Codename One App Can Be an MCP Server"
slug: codename-one-mcp-server
url: /blog/codename-one-mcp-server/
date: '2026-07-21'
author: Shai Almog
description: "The Codename One JavaSE port can expose a running simulator or desktop tool over the Model Context Protocol. An agent reads the accessibility semantics tree, performs UI actions on the EDT, and can call application-defined tools."
feed_html: '<img src="https://www.codenameone.com/blog/codename-one-mcp-server.jpg" alt="Codename One application exposed as an MCP server" /> A running Codename One simulator or JavaSE tool can expose its semantic UI and application tools to coding agents over MCP.'
series: ["release-2026-07-17"]
---

![Your Codename One App Can Be an MCP Server](/blog/codename-one-mcp-server.jpg)

[Yesterday's accessibility work](/blog/accessibility-semantics/) created an immutable tree that describes what is on screen, what each item means, and which actions it supports. VoiceOver and TalkBack consume that tree for people.

[PR #5377](https://github.com/codenameone/CodenameOne/pull/5377) lets an agent consume it too.

The JavaSE port can expose a running simulator or Codename One desktop tool as a local [Model Context Protocol](https://modelcontextprotocol.io/) server. Codex, Claude Code, Claude Desktop, opencode, or another MCP host can inspect the current form, find a field by label, enter text, activate a button, and call tools published by the application.

## The agent reads meaning, not coordinates

Screenshot automation sees colored rectangles and guesses where to click. MCP exposes the same resolved semantics that accessibility technology receives:

```json
{
  "id": "profile-save",
  "role": "button",
  "label": "Save",
  "enabled": true,
  "actions": ["activate"]
}
```

The agent can ask for `ui_snapshot`, find `profile-save`, then invoke `activate`. It does not need to assume the button stayed at yesterday's x and y coordinates.

{{< mermaid >}}
sequenceDiagram
    participant Agent as MCP host
    participant Server as Codename One MCP server
    participant Tree as Accessibility snapshot
    participant EDT as Codename One EDT
    Agent->>Server: ui_snapshot
    Server->>Tree: build immutable semantics tree
    Tree-->>Agent: roles, labels, values, actions
    Agent->>Server: ui_set_text(profile-name, "Ada")
    Server->>EDT: dispatch action
    EDT-->>Server: success + fresh snapshot
    Server-->>Agent: updated UI state
{{< /mermaid >}}

The built-in tools are small on purpose:

| Tool | Purpose |
|---|---|
| `ui_snapshot` | Return the current semantic UI tree as JSON |
| `ui_find` | Find nodes by identifier, label, or screen coordinate |
| `ui_perform_action` | Run a semantic action with an optional argument |
| `ui_activate` | Activate a node |
| `ui_set_text` | Set editable text through the UI action model |

Every action runs on the Codename One event dispatch thread. The agent never mutates the live component tree from the MCP transport thread. Each action returns a fresh snapshot so the next decision uses current state.

## Starting a server is explicit

There is no build hint that quietly exposes an application. Calling the API is the switch:

```java
MCP.startSocketServer(8765);
```

The socket mode is useful for a running simulator session a person can watch. A tool launched directly by an MCP host can use standard input and output instead:

```java
MCP.startStdioServer();
```

The JavaSE port owns the stdio transport because process standard input is not available on every Codename One target. While the transport is active, normal application logging is redirected away from standard output so it cannot corrupt the newline-delimited JSON-RPC stream.

## Desktop tools get a menu without application code

The JavaSE port adds an MCP menu to the simulator and Codename One desktop tools, including the new Settings editor. The menu can expose the running tool, detect installed MCP hosts, install or remove the local host registration, and control debug logging.

Registration uses a small bridge. Most local MCP hosts launch servers over stdio. The visible Codename One tool is already running and listens on a loopback socket. `MCPStdioLauncher` relays between the host's stdio connection and that socket.

```text
Coding agent <-- stdio --> MCPStdioLauncher <-- loopback --> running tool
```

This is how an agent can drive the actual Certificate Wizard or Settings window in front of you instead of launching a hidden copy with different state.

## Your app can publish domain tools

UI actions are useful, but some operations should not be simulated as clicks. An application can expose a typed `Tool` with a JSON schema and handler:

```java
MCP.addTool(new Tool(
        "current_user",
        "Returns the signed in user",
        "{\"type\":\"object\",\"properties\":{}}",
        argumentsJson -> "{\"name\":\"" + signedInUser + "\"}"
));
```

The server merges application tools with the built-in UI tools. Codename One already uses the same `com.codename1.ai.Tool` contract for model tool calls inside an app, so one definition can serve an in-app model and an external MCP host.

Treat these tools as a privileged API. Do not publish a tool that returns signing passwords, API tokens, or unrestricted file contents because the handler happens to be local. The current server is local, but the agent still receives whatever the tool returns.

## Screenshots remain available

The semantic tree means a vision model does not need a screenshot for routine navigation. Some UI facts remain visual, such as a chart shape or a rendering defect. The server therefore exposes the current form as an optional PNG resource too.

The two sources complement each other. The tree says “this is an enabled Save button with an activate action.” The PNG says “the button overlaps the footer.” A screenshot alone cannot reliably provide the first fact. A semantic tree cannot provide the second.

## The scope is JavaSE today

This release supports the JavaSE port, which covers the simulator and JavaSE-hosted desktop tools. It does not make every packaged Codename One application an MCP server. Packaged executable jars, cloud desktop builds, mobile targets, the JavaScript port, and the native macOS, Linux, and Windows ports do not yet have the launcher and transport plumbing.

If you need MCP in one of those native targets, let us know which port and deployment model you need. The protocol engine and semantic tools are portable. The missing work is the transport, startup, registration, and security boundary for that target.

The PR includes 13 protocol and UI-driving tests plus an end-to-end run against the reference MCP Inspector client. It also builds the core into an iOS application to verify that unused MCP code is pruned and that referenced core classes stay within the ParparVM API surface.

Tomorrow's post covers another machine-readable view of Codename One. The new port status page turns the test suite into a dated support matrix instead of asking you to trust a manually maintained table.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
