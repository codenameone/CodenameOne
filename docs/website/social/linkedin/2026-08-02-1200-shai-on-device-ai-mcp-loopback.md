---
title: "Loopback is local, not private"
slug: 2026-08-02-1200-shai-on-device-ai-mcp-loopback
platform: linkedin
account: shai
source_slug: on-device-ai-mcp-loopback
publish_at: '2026-08-02T12:00:00'
timezone: Asia/Jerusalem
image: /blog/on-device-ai-mcp-loopback.jpg
---

Loopback is local. It is not private.

We added a loopback socket transport so an MCP client can inspect and drive a Codename One application on mobile and desktop ports.

This is a powerful debugging loop. An LLM can read the semantic UI tree, find a button by role and text, activate it, set text, and inspect the state that follows. No screenshot coordinates are required.

Binding to loopback prevents the control channel from appearing on office Wi-Fi. It does not stop another local process from trying to connect.

That is why `MCP.startSocketServer(...)` refuses to run in a release build by default. There is an explicit override for controlled test labs, but it should feel serious because it is serious.

The same release also moves on-device vision, language, and LiteRT inference into the core. OCR, barcode recognition, pose detection, translation, and application-owned models now have a selective cross-platform API with no surprise cloud fallback.

The article separates those two AI stories and explains the threat boundary.

{{canonical}}
