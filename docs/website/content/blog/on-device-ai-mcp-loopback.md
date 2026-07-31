---
title: "On-Device AI and MCP on Every Port"
slug: on-device-ai-mcp-loopback
url: /blog/on-device-ai-mcp-loopback/
date: '2026-08-02'
author: Shai Almog
description: "Codename One now exposes on-device vision, language, and LiteRT inference in the core, while a guarded loopback MCP transport lets an LLM inspect and drive real applications."
feed_html: '<img src="https://www.codenameone.com/blog/on-device-ai-mcp-loopback.jpg" alt="An AI agent inspects a mobile application through a guarded local connection" /> Codename One now exposes on-device vision, language, and LiteRT inference in the core, while a guarded loopback MCP transport lets an LLM inspect and drive real applications.'
series: ["release-2026-07-31"]
---

![An AI agent inspects a mobile application through a guarded local connection](/blog/on-device-ai-mcp-loopback.jpg)

There are two very different ways to connect AI to an application.

One puts the model inside the feature: OCR a receipt, identify a language, segment a person, or run an application-owned model.

The other lets an agent stand outside the application, inspect its semantic UI, and operate it while a developer watches. That second path is extraordinarily useful for debugging. It is also a control channel, so where it listens matters.

This week Codename One merged both parts: [built-in on-device AI](https://github.com/codenameone/CodenameOne/pull/5467) and [MCP over a loopback socket on mobile and desktop ports](https://github.com/codenameone/CodenameOne/pull/5472).

This is part of the [July 31 weekly release series](/blog/push-v3-new-cloud/).

## The AI API is selective, local, and portable

The new core surface is split by job:

| Package | Capabilities |
| --- | --- |
| `com.codename1.ai.vision` | OCR, barcode recognition, face detection, image labels, pose detection, selfie segmentation, and document correction |
| `com.codename1.ai.language` | Language identification, translation, and smart reply |
| `com.codename1.ai.inference` | Reusable application-owned `.tflite` inference sessions and verified model downloads |

The public API exists on every target. A port that cannot perform an operation reports it as unsupported instead of sending data to a surprise cloud fallback.

Android uses ML Kit for the higher-level vision and language operations. Apple platforms use Vision, Core Image, and Natural Language by default where those frameworks provide the feature. Optional ML Kit support fills selected gaps on iOS. LiteRT and TensorFlow Lite execute application-owned models, with platform acceleration where available and CPU fallback.

{{< mermaid >}}
flowchart TB
    A["Codename One AI API"] --> B["Vision"]
    A --> C["Language"]
    A --> D["Inference"]
    B --> E["Android ML Kit"]
    B --> F["Apple Vision and Core Image"]
    C --> G["ML Kit or Apple Natural Language"]
    D --> H["LiteRT / TensorFlow Lite"]
    H --> I["NNAPI, Core ML delegate, or CPU"]
{{< /mermaid >}}

Only referenced feature families bring their native dependencies into a build. An application using language identification does not need the document scanner, pose detector, and model runtime by association.

## OCR should look like an asynchronous operation

The API owns native image conversion, lifecycle, and EDT delivery:

```java
TextRecognizer recognizer = new TextRecognizer();
recognizer.process(VisionImage.encoded(jpegBytes))
        .ready(result -> textArea.setText(result.getText()))
        .except(error -> Log.e(error));
```

The result is structured, so an application can work with blocks and geometry instead of immediately flattening everything into one string.

Model inference uses a session because loading and compiling a model for every frame is expensive:

```java
InferenceSession.open(
        ModelSource.file(modelFile),
        new InferenceOptions()
).ready(session -> {
    session.run(inputs)
            .ready(outputs -> renderPrediction(outputs))
            .except(error -> Log.e(error));
});
```

Close a session when the screen or feature that owns it is done. A model downloaded at runtime can go through `ModelCache`, which requires HTTPS and verifies the model's SHA-256 digest before the file becomes active. Model bytes are executable behavior in a different costume. Treating an unverified download as “just data” is a software supply chain mistake.

## Local inference improves privacy, but does not solve it

These APIs do not upload images, text, or tensors to Codename One. That is a useful privacy property, especially for camera frames and documents.

It is not a complete privacy policy. A recognized receipt can still be logged. A translated medical note can still be copied into analytics. An application can still send an inference result to its own backend. On-device processing narrows the data path; the application remains responsible for what happens before and after the operation.

The backend capabilities also differ. Smart reply is not available through every native framework. Apple Vision support extends to Mac Catalyst for vision tasks, while unsupported desktop and browser operations fail rather than simulating an answer. Check capabilities at the feature boundary and design a real fallback.

## MCP makes the running application legible to an agent

We introduced the [Codename One MCP server](/blog/codename-one-mcp-server/) on JavaSE first. It exposes the semantic UI tree instead of screenshots and guessed coordinates.

An agent can:

- Read `ui_snapshot` output
- Find a component by role, text, or semantic identifier
- Activate an action
- Set text through the component model
- Scroll the current viewport
- Observe the resulting UI state

The operations run on the Codename One EDT. A button press is a component action, not a coordinate that happens to land where the button was during one recording.

```java
if (Display.getInstance().isDebuggableBuild()) {
    MCP.startSocketServer(8642);
}
```

With the new loopback transport, the same portable MCP server can run on Android, iOS, and other ports that implement loopback listening. A developer can connect through the platform's normal device or simulator forwarding path and let an agent reproduce a bug in the actual application.

{{< mermaid >}}
sequenceDiagram
    participant Dev as Developer and LLM client
    participant Port as Device port forward
    participant MCP as Loopback MCP server
    participant UI as Codename One EDT
    Dev->>Port: Connect to debug device
    Port->>MCP: Forward to 127.0.0.1:8642
    Dev->>MCP: ui_snapshot
    MCP->>UI: Read semantic tree
    UI-->>MCP: Roles, labels, state, actions
    MCP-->>Dev: Structured snapshot
    Dev->>MCP: ui_activate semantic target
    MCP->>UI: Perform action
    UI-->>Dev: Updated state
{{< /mermaid >}}

This shortens a debugging conversation. Instead of “tap near the upper-right corner, unless the font is larger,” the agent can say “activate the button whose semantic text is Submit,” then inspect the error label that appeared.

## Why wildcard listening was unacceptable

The existing general server socket API could listen on every network interface. On a phone connected to office Wi-Fi, that can expose a debugging control channel to the local network.

The new `Socket.listenLoopback(...)` contract is different:

- It binds only to the loopback interface.
- It never falls back to wildcard listening.
- An unsupported port fails on the calling thread.
- The server does not appear reachable merely because a start method returned.

That prevents accidental LAN exposure. It does not make the channel private.

Loopback is a host boundary, not an application identity boundary. Another process on the same device can try to connect. A compromised developer workstation can use an established port forward. Once connected, the client can read application UI state and invoke the actions the MCP server exposes.

The safe assumption is simple:

> Starting MCP grants a local agent the ability to inspect and drive this application.

That is excellent in a debug session and a terrible surprise in a production build.

## Release builds are blocked by default

`MCP.startSocketServer(port)` checks `Display.isDebuggableBuild()`. It refuses to start in release builds unless the application explicitly overrides the guard:

```java
MCP.setAllowOnReleaseBuilds(true);
MCP.startSocketServer(8642);
```

That override exists for controlled test labs, managed fleets, and specialized internal deployments. Do not use it as a convenience flag in a consumer release.

JavaSE is a special case because the desktop port reports a development environment. If you package a desktop application for end users, add your own product-level gate and omit the MCP startup path from normal launch.

A defensible test-lab gate combines several conditions:

```java
if (isInternalTestAccount()
        && isManagedTestDevice()
        && wasMcpEnabledForThisSession()) {
    MCP.setAllowOnReleaseBuilds(true);
    MCP.startSocketServer(8642);
}
```

The exact checks belong to the application. A hard-coded secret inside the binary is not an authentication system.

## This is debugging access, not autonomous product behavior

On-device AI and MCP sit near each other in the release, but they should not be blurred together.

The AI API runs bounded application features with explicit inputs and results. MCP exposes a developer tool channel into the running UI. Neither automatically turns an application into an autonomous agent, and neither should silently send user data to a model provider.

The compelling workflow is smaller and more practical:

1. A developer launches a debuggable build.
2. MCP starts on loopback.
3. The development machine forwards the port.
4. An LLM reads the semantic UI and reproduces a reported path.
5. The developer sees the exact state transition and keeps control of the session.
6. The server disappears when the debug process ends.

That is enough to make mobile debugging dramatically more observable without pretending a control channel has no security cost.

Next in the series: [road-following routes in the maps API](/blog/road-following-map-routing/).

---

## Discussion

_What is the first real application bug you would hand to an MCP-enabled debug build?_

{{< giscus >}}
