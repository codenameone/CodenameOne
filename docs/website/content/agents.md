---
title: "Build and Ship an App with an AI Agent"
slug: "agents"
url: "/agents/"
description: "Point Claude Code, Codex or another coding agent at Codename One: it creates the project, builds and checks every screen, makes the icon, and signs and submits the app to the App Store and Google Play through cloud.codenameone.com."
---

# Build and ship an app with an AI agent

Tell your coding agent what app you want and to use Codename One. It creates
the project, writes the app, runs it and checks every screen, makes the icon,
and submits it to the App Store and Google Play through
[cloud.codenameone.com](https://cloud.codenameone.com/). You review
screenshots and approve; you don't write code, and you don't need a Mac, Xcode
or Android Studio.

A prompt that works in Claude Code or Codex:

```text
Use Codename One (read https://www.codenameone.com/llms.txt) to build me an
iPhone and Android app that tracks daily habits with streaks and reminders.
Show me screenshots before you submit it to TestFlight and Google Play testing.
```

## For agents: the path

1. **Read** [llms.txt](/llms.txt) for the map, then install the
   [bootstrap skill](/agents/codenameone-bootstrap.zip): unzip it into
   `~/.claude/skills/` for Claude Code (`%USERPROFILE%\.claude\skills\` on
   Windows), or read its `SKILL.md` directly.
2. **Interview the user once** and write `APP_SPEC.md`: screens, data, sign-in,
   brand colour, name, bundle id, target stores.
3. **Create the project** with nothing installed first. The script finds a
   JDK 17 or downloads Temurin 17 without admin rights, then runs the same
   generator as [start.codenameone.com](https://start.codenameone.com/):

   ```bash
   curl -fsSL https://www.codenameone.com/agents/create-app.sh | bash -s -- --list
   curl -fsSL https://www.codenameone.com/agents/create-app.sh | bash -s -- \
       --name HabitTracker --package com.acme.habits --template BAREBONES --brand 1f5eff
   ```

   On Windows, in PowerShell:

   ```powershell
   $c = irm https://www.codenameone.com/agents/create-app.ps1
   & ([scriptblock]::Create($c)) --name HabitTracker --package com.acme.habits
   ```

   The generator is about 1 MB. A complete-app template (`--template SAVOR`)
   is downloaded only when it is asked for.

4. **Read the project's `AGENTS.md`** and `.agent-skills/codename-one/SKILL.md`,
   then build screen by screen. Run the app with `./mvnw cn1:agent`
   (`mvnw.cmd cn1:agent` on Windows) in the background and look at each screen
   with `java .agent-skills/codename-one/tools/Sim.java`
   (`screenshot`, `tap`, `type`, `record-start`).
5. **Make the icon** with `.agent-skills/codename-one/tools/MakeIcon.java`
   (one bold Material glyph or initials on the brand colour, 1024px, opaque).
6. **Show the user** the screenshots, a video of the main flow, and the icon.
   Wait for their approval. Then review the app the way the stores will, with
   the project skill's `references/store-review.md`, and have the user approve
   the listing.
7. **Ship** through the cloud MCP server, `https://cloud.codenameone.com/api/v1/mcp`
   (already in the project's `.mcp.json`): sign the app, build it, fix it from
   the build log if needed, and submit it. The project skill's
   `references/cloud-agent.md` has the details.

## No terminal? The browser IDE

An agent that can only drive a browser can work in
[cloud.codenameone.com/ide/](https://cloud.codenameone.com/ide/). In a browser
with [WebMCP](https://webmachinelearning.github.io/webmcp/), the IDE gives the
browser's agent tools to edit files, compile, run the app, take screenshots,
tap and type in it, read its logs and look up the Codename One API.

It fits client apps. Use the terminal path above when the app needs any of
these:

- A server: backend projects can be edited but do not run in the browser.
- A complete-app template (Savor, Wayline, ...) or an existing project.
  The browser IDE starts from the basic templates.
- More than about 2 MiB of source or of dependency classes, non-public or
  non-compile-scope dependencies, cn1libs that need a local build, or native
  interfaces.

In the browser IDE you create the project, sign in, run the Certificate
Wizard and press **Cloud build...** yourself; the agent tells you which
target to pick. Projects stay in that browser, so use **Download ZIP** as a
backup.

## What only you can do

The agent asks for each of these once, when it's needed:

- Sign in to cloud.codenameone.com when your agent connects, and approve what
  it may do: signing, push setup, store submission.
- **iOS:** an [Apple Developer Program](https://developer.apple.com/programs/)
  membership, and an App Store Connect API key for signing and uploads.
- **Android:** a [Google Play developer account](https://play.google.com/console/),
  the app created once in Play Console, and a service account for uploads.
- Approve the store listing text and screenshots the agent proposes.

## Downloads

| File | What it is |
|---|---|
| [llms.txt](/llms.txt) | What Codename One can do, the templates, the docs |
| [codenameone-bootstrap.zip](/agents/codenameone-bootstrap.zip) | The skill for getting from a description to the stores |
| [codenameone-skill.zip](/agents/codenameone-skill.zip) | The project skill: code, UI, CSS, tests, backend (already in every project) |
| [create-app.sh](/agents/create-app.sh) / [create-app.ps1](/agents/create-app.ps1) | Create a project with nothing installed |
| [cn1-create.jar](/agents/cn1-create.jar) | The project generator, used by the scripts (complete-app templates are separate files under `/agents/templates/`) |

## Start from a complete app

When your app is a variation of one of these, ask the agent to start from it.
Each is a complete app with a Java backend, MIT-0 licensed:
[Wayline](/wayline/) (ride hailing), [Savor](/savor/) (food delivery),
[Bastion](/bastion/) (banking), [Parley](/parley/) (messaging),
[Pulse](/pulse/) (gym and studio), [Slate](/slate/) (appointments), and the
games [Skirmish](/skirmish/), [Dash](/dash/), [Splat](/splat/) and
[Papercut](/papercut/).
