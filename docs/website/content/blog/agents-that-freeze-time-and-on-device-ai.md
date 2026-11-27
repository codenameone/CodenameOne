---
title: "Let Your Agent Freeze Time in the Simulator, and Run the Phone's Own AI Model"
slug: agents-that-freeze-time-and-on-device-ai
url: /blog/agents-that-freeze-time-and-on-device-ai/
date: '2026-12-03'
author: Shai Almog
description: "Coding agents can now freeze the simulator's clock, step animations frame by frame and settle them before a screenshot, and cn1:verify checks a form in light, dark, landscape and the largest text for unlabeled controls, truncated text and poor contrast. LlmClient.system() calls Apple Foundation Models on iOS and macOS and Gemini Nano on Android, with no download and no key."
feed_html: "<img src=\"https://www.codenameone.com/blog/agents-that-freeze-time-and-on-device-ai.jpg\" alt=\"Agents that freeze time, and on-device AI\" /> Agents that stop the clock to inspect any animation frame, a verify goal that fails the build on accessibility errors, and the operating system's own language model from Java."
series: ["release-2026-11-27"]
---

![Agents that freeze time, and on-device AI](/blog/agents-that-freeze-time-and-on-device-ai.jpg)

Watch a coding agent test a UI and you'll see the same failure over and over. It taps a button, takes a screenshot, and gets the frame halfway through the transition. Then it "fixes" a bug that doesn't exist, or misses one that does. Humans see the animation finish. Agents see a single frozen instant at a random point in it.

So we gave them the clock.

## Freeze, step, settle

The simulator's MCP server, which every new project's agent already talks to, has four new clock tools:

| Tool | What it does |
| --- | --- |
| `ui_clock_freeze` | Stops time for every animation, transition and timer |
| `ui_clock_advance` | Moves time forward, `{"ms":300}`, painting each 16 ms frame |
| `ui_clock_settle` | Runs time until nothing is animating, up to `maxMs` |
| `ui_clock_release` | Hands time back to the wall clock |

The pattern an agent follows is simple: freeze, tap, settle, look.

{{< mermaid >}}
sequenceDiagram
    participant Agent
    participant Sim as Simulator (MCP)
    Agent->>Sim: ui_clock_freeze
    Agent->>Sim: tap "Checkout"
    Agent->>Sim: ui_clock_advance 150 ms
    Agent->>Sim: screenshot (mid transition, on purpose)
    Agent->>Sim: ui_clock_settle
    Sim-->>Agent: settled: true
    Agent->>Sim: screenshot (the final frame, every time)
{{< /mermaid >}}

An animation that never ends, like a spinner, answers `settled: false` instead of hanging the agent. Input timing (long press, double tap, fling) keeps the wall clock, so gestures still register as gestures. And `sim_record_film` writes a contact sheet of an animation, one frame every 50 ms, so an agent can look at a whole transition in a single image and say where it stutters.

The same tools wrap as shell commands for agents that prefer a terminal:

```bash
java .agent-skills/codename-one/tools/Sim.java freeze
java .agent-skills/codename-one/tools/Sim.java settle
```

## cn1:verify fails the build on accessibility errors

Agents are bad at noticing what a screen doesn't say. A button with an icon and no label looks fine in a screenshot and is invisible to a screen reader. `cn1:verify` checks for it:

```bash
./mvnw cn1:verify
```

It starts the simulator hidden and renders your first form four ways: light, dark, landscape and the largest text size. Then it reports:

- **Errors:** `unlabeled-control`, `zero-size-control`, `truncated-text`, `low-contrast` (under 3:1).
- **Warnings:** `overflows-parent`, `no-dark-theme`.

It writes `report.html` with screenshots of every condition and fails the build on any error. `-Dcn1.verify.failOnError=false` writes the report without failing, for when you're getting an existing app to zero. Agents call the same check as `sim_verify` on any form.

## Errors an agent can act on

When a build fails, the message now carries a stable ID, like `CN1-CSS-PROPERTY`, `CN1-HINT-UNKNOWN` or `CN1-API-JDK`, and the project skill maps each ID to its fix. A failed cloud build log maps the same way, to `CN1-SIGN-CERT`, `CN1-SIGN-PROFILE` or `CN1-ANDROID-KEYSTORE`. The agent doesn't have to guess what "unmappable character" meant this time. When Codename One itself gets in the way, the agent asks whether to report it and, if you agree, files a GitHub issue with the error ID and versions, never your source code.

## From prompt to production

The developer guide gained an [end to end deployment chapter](/developer-guide/#deployment): build the server for Linux, rent a machine, set up the database, systemd and HTTPS, ship the web app inside the server, build and sign the mobile apps, submit them, and wire up CI. It's written for people, and it's precisely what the agent skill follows.

The [BuildCloud MCP chapter](/developer-guide/#buildcloud-mcp) documents the cloud tools an agent uses to build, read logs, sign and submit, with the three approval checkboxes that keep you in charge. And [Crash Protection](/developer-guide/#crash-protection) now explains how an agent triages crashes: they arrive as GitHub issues with symbolicated traces, and the agent ranks them by count and last seen and starts with the one hurting the most users.

## The phone already has a language model

Apple ships a language model with Apple Intelligence. Google ships Gemini Nano on Pixel and Galaxy devices through AICore. Both are free to call, need no API key, add nothing to your download, and keep the prompt on the device. Now one Java call reaches either:

```java {class="cn1-no-try"}
if (SystemLlmClient.getStatus(SystemLlmFeature.CHAT).isAvailable()) {
    SystemLlmClient llm = LlmClient.system();
    llm.chat(ChatRequest.builder()
            .addMessage(ChatMessage.system("Answer in one sentence."))
            .addMessage(ChatMessage.user("What is a monad?")).build())
        .ready(r -> answer.setText(r.getText()))
        .except(err -> Log.e(err));
}
```

It works on iOS, macOS, and Android. Besides chat there are task methods for the things on-device models are good at: `summarize`, `proofread`, `rewrite` and, on Android, `describeImage`. On Android they map to the ML Kit GenAI task APIs; on Apple they're prompts to the same model.

The status tells you what the device can do right now: available, downloadable, downloading, disabled because the user turned Apple Intelligence off, or unsupported. On Android, `download(feature)` fetches the model when it's missing. The simulator reports unsupported, so develop against `LlmClient.ollama()` with a local model and switch to `system()` on the device; both are the same `LlmClient` interface.

Only apps that call `LlmClient.system()` pay for it. Apple builds link Foundation Models weakly, so the app still launches on older OS versions and simply reports unsupported. No build hints, no extra dependency.

{{< mermaid >}}
flowchart LR
    C[LlmClient.system] --> I[iOS and macOS:<br/>Apple Foundation Models]
    C --> A[Android:<br/>Gemini Nano via AICore]
    I --> P[Prompt never leaves the device]
    A --> P
{{< /mermaid >}}

It sits next to the rest of `com.codename1.ai`: cloud providers and local llama models behind the same `LlmClient` interface, with Whisper speech recognition beside them. Start on the device for free, fall back to the cloud when a task needs a bigger model, and change one line to switch.

That's the week. If you missed it, start with {{< post-link path="/blog/full-shopify-clone-in-three-days" text="the Shopify clone we built in three days" >}}, or point your agent at [llms.txt](/llms.txt) and let it freeze some time of its own.
