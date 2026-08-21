---
title: "One application action can belong to several system surfaces"
slug: 2026-08-27-1200-codenameone-app-intents-siri-spotlight-shortcuts
platform: linkedin
account: codenameone
source_slug: app-intents-siri-spotlight-shortcuts
publish_at: '2026-08-27T12:00:00'
timezone: Asia/Jerusalem
image: /blog/app-intents-siri-spotlight-shortcuts.jpg
---

Siri, Spotlight, Shortcuts, an Android launcher shortcut, and a widget button all need the same thing: a declaration of what an application can do.

Codename One App Intents put that declaration on a public static Java method. The Maven build generates a reflection-free dispatch table plus the native declarations each platform supports.

Entities let the operating system ask which workout, playlist, or document the user meant. `opensRoute` sends a foreground action through the existing route table. `Intents.index()` publishes application objects to device search.

Android is not labeled as Siri parity. It gets launcher shortcuts, donation, indexing, and headless service execution. Voice phrases and system disambiguation remain iOS capabilities.

`Intents.invoke()` calls the same generated command on every port, even when the operating system exposes no intent surface. One application behavior can serve native integrations and the application's own command layer.

{{canonical}}
