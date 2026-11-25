---
name: codename-one-bootstrap
description: Turn a user's description of a mobile app into a Codename One project, a verified working app and a store submission, starting from nothing installed. Use when the user asks to build, make or create a mobile / iPhone / Android / cross-platform app and there is no Codename One project yet, or mentions codenameone.com, Codename One or cloud.codenameone.com. Covers installing a JDK without admin rights, creating the project from the start.codenameone.com generator, writing an app spec with the user, building and visually checking every screen, an app icon, and handing off to the cloud for signing, builds and App Store / Google Play submission.
metadata:
  type: skill
---

# Codename One: from a description to the stores

You are building a native mobile app for a user who may never read its code.
Codename One compiles Java to native iOS, Android, desktop and web apps, and
cloud.codenameone.com builds, signs and submits them, so nothing here needs a
Mac, Xcode or Android Studio. The user's job is to describe the app, look at
what you show them, and do the few account steps only a human can do (listed
in step 7). Everything else is yours.

Map of what exists, with links you can fetch: https://www.codenameone.com/llms.txt

**No shell?** If you can only drive a browser (a browser agent, or a chat
client without a terminal), skip to "The browser IDE path" at the end.

Commands below are given for bash (macOS, Linux) and PowerShell (Windows).
On Windows the project's wrapper is `mvnw.cmd`, paths use `\`, and the home
folder is `%USERPROFILE%` (`$env:USERPROFILE` in PowerShell). `java <file>.java`
works the same everywhere.

## 1. Interview once, write the spec

Before creating anything, ask in ONE message (offer defaults so the user can
just say "yes"):

- The app's name, and what it does in one sentence.
- The screens, and what the user does on each.
- Data: local only, or synced through a server? Sign-in?
- Look: brand colour(s), light or dark, any app it should feel like.
- Stores: iOS, Android or both; a bundle id / package (default
  `com.<their-domain-reversed>.<appname>`).

Write the answers to `APP_SPEC.md` in the project once it exists, and treat it
as the acceptance test. Do not ask again for anything the spec answers.

## 2. Pick the closest starting point

```bash
curl -fsSL https://www.codenameone.com/agents/create-app.sh | bash -s -- --list
```

(Windows: `& ([scriptblock]::Create((irm https://www.codenameone.com/agents/create-app.ps1))) --list`.)

`BAREBONES` is an empty app. The others are complete apps with a Java backend
(ride hailing, food delivery, banking, messaging, streaming, booking, gym,
games). When the user's app is a variation of one, start from it and change
it -- that is far closer to their expectations than writing from scratch.

## 3. Create the project

```bash
curl -fsSL https://www.codenameone.com/agents/create-app.sh | bash -s -- \
    --name HabitTracker --package com.acme.habits --template BAREBONES \
    --brand 1f5eff [--theme dark] [--icon icon.png]
```

Windows (PowerShell; do not use Git Bash, whose paths java cannot read):

```powershell
$c = irm https://www.codenameone.com/agents/create-app.ps1
& ([scriptblock]::Create($c)) --name HabitTracker --package com.acme.habits --template BAREBONES --brand 1f5eff
```

- It finds a JDK 17+ or downloads Temurin 17 into `~/.codenameone/agent/jdk`
  (`%USERPROFILE%\.codenameone\agent\jdk` on Windows; no admin rights), then
  runs the same generator as start.codenameone.com. A complete-app template
  is downloaded only when you name it.
- The last line it prints is `JAVA_HOME=...`. Set it for every later command
  (`export JAVA_HOME=...`; PowerShell: `$env:JAVA_HOME='...'`). Nothing else
  needs installing: the project has its own Maven wrapper (`./mvnw`,
  `mvnw.cmd` on Windows).
- `--build gradle` gives a Gradle project (BAREBONES and KOTLIN only).
- `--jdk-only` just ensures the JDK.

Then `cd` into the project and **read its `AGENTS.md` and
`.agent-skills/codename-one/SKILL.md`**. That project skill is how to write
the code; this skill is only the path around it.

## 4. Build it screen by screen, and look at every screen

For each screen in the spec:

1. Write it (`common/src/main/java`, style in `common/src/main/css/theme.css`).
2. Run it and look:
   ```bash
   ./mvnw cn1:agent &                 # keep it running in the background
   S=.agent-skills/codename-one/tools/Sim.java
   java $S wait && java $S freeze && java $S screenshot target/screen-<name>.png
   ```
   PowerShell:
   ```powershell
   Start-Process -NoNewWindow -FilePath .\mvnw.cmd -ArgumentList 'cn1:agent'
   $S = '.agent-skills\codename-one\tools\Sim.java'
   java $S wait; java $S freeze; java $S screenshot target\screen-<name>.png
   ```
   Open the PNG and compare it with the spec. Drive the flow with
   `java $S tap`, `java $S type`, and `java $S settle` after each tap that
   navigates (never sleep); the project skill's tools
   (`GuiLint`, `AlignmentCheck`, `CompareToMockup`) catch layout mistakes.
3. Fix until it matches, then move on.

When every screen is done, record the main flow
(`java $S record-start target/flow.mp4` ... `record-stop`) and show the user the
screenshots or video. **Get their "yes" before step 6.** Changes now are
cheap; after a store review they are not.

## 5. Icon

Unless the user supplied one, make it with the project skill's tool -- one bold
glyph on the brand colour, no words:

```bash
T=.agent-skills/codename-one/tools/MakeIcon.java
java $T --palettes                       # if the spec named no colour
java $T --list-glyphs <idea>             # e.g. fitness, restaurant, chat, wallet
java $T --glyph <name> --bg <RRGGBB> [--bg2 <RRGGBB>] --out common/icon.png
```

Open the PNG, check it reads at a glance, and show it to the user with the
screenshots. (`icon.png` at the root in a Gradle project.)

## 6. Review it the way the stores will

Before any submission, go through the project skill's
`references/store-review.md` and its checklist: purpose strings, privacy
policy, account deletion, Sign in with Apple, a demo account for the
reviewer, IAP with restore, no placeholder screens, the backend deployed.
Most rejections are visible in the code. Write the store listing (name,
subtitle, description, keywords, category, privacy labels, Data safety,
age rating) from `APP_SPEC.md` and what the code really does, and have the
user approve it with the screenshots. Capture one screenshot per key screen,
then size them for every store slot:

```bash
java .agent-skills/codename-one/tools/StoreScreenshots.java --in shots --out store \
    --captions captions.txt --bg <brand RRGGBB>
```

## 7. Ship through cloud.codenameone.com

The project's `.mcp.json` connects you to the Codename One cloud MCP server
(`https://cloud.codenameone.com/api/v1/mcp`). Follow
`.agent-skills/codename-one/references/cloud-agent.md` in the project: connect,
sign, build, fix from the build log, submit.

What only the human can do -- ask for each once, exactly when it is needed,
with the link:

- Sign in to cloud.codenameone.com in the browser when the MCP connection
  asks, and approve the grants (signing, push, submission).
- iOS: an Apple Developer Program membership, and an App Store Connect API key
  (`.p8`, key id, issuer id) for signing and submission.
- Android: a Google Play developer account, the app created once in Play
  Console, and a service-account key for uploads.
- Approve the listing text and screenshots you propose.

Never tell the user to install Xcode, Android Studio or Maven: the cloud
builds both platforms from any OS.

After release, offer to turn on Crash Protection (Pro and above). Crashes then
arrive as symbolicated GitHub issues you can read with `gh` and fix:
`.agent-skills/codename-one/references/crash-triage.md` in the project.

## The browser IDE path (no shell)

https://cloud.codenameone.com/ide/ is a Codename One IDE in the browser. In a
browser that supports WebMCP it exposes tools to the browser's agent: read,
search and edit files, `run_app`, `check_compile`, `get_problems`,
`get_app_logs`, `screenshot_app`, `tap_app`, `type_text`,
`get_component_tree`, `lookup_api` (the Codename One API with imports),
`set_build_hint`, `get_changes`, and the GUI Builder's tools. Use the same
loop as step 4: edit, `run_app`, `screenshot_app`, compare with the spec.

What it cannot do, so choose the shell path when the app needs any of it:

- **Client apps only, run in the browser.** Full-stack and backend projects
  can be created and edited, but the backend does not run there and the
  preview runs the client only. An app with a server needs the shell path.
- **Starts from the six basic templates** (Maven or Gradle; client,
  full-stack or backend). The complete apps (Savor, Wayline, ...) and
  importing an existing project are not available.
- **Size and dependency limits**: about 2 MiB of Java source in at most 1,000
  files, 2 MiB of dependency classes, public Maven dependencies in compile
  scope only, no cn1libs that need a local build, no native interfaces or
  platform source sets in cloud builds.
- **No language server**: completion, outline and `find_symbol` are lexical;
  `check_compile` is the source of truth.
- **The human does the rest**: creating or switching projects, signing in,
  the Certificate Wizard, and pressing **Cloud build...** stay with the user.
  Tell them exactly which target to pick (for example "iOS App Store") and
  what to enter.
- **Projects live in that browser only.** Remind the user to use
  **Download ZIP** as a backup; that ZIP is also a normal project for the
  shell path later.

Store submission after a browser build goes through cloud.codenameone.com the
same way; steps 6 and 7 apply unchanged.
