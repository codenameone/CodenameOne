---
title: "Expose a Java method to Siri, Spotlight, and Shortcuts"
slug: 2026-08-27-1200-codenameone-app-intents-siri-spotlight-shortcuts
platform: linkedin
account: codenameone
source_slug: app-intents-siri-spotlight-shortcuts
publish_at: '2026-08-27T12:00:00'
timezone: Asia/Jerusalem
image: /blog/app-intents-siri-spotlight-shortcuts.jpg
---

`@AppIntent` can expose a public static Java method to Siri, Spotlight, and Shortcuts. The same declaration can also drive an Android launcher shortcut or an internal application command.

Widget actions use the separate surfaces API. A surface action handler can call `Intents.invoke()` when a widget should reuse the same application behavior; the annotation does not generate that bridge automatically.

Codename One App Intents put that declaration on a public static Java method. The Maven build generates a reflection-free dispatch table plus the native declarations each platform supports.

Entities let the operating system ask which workout, playlist, or document the user meant. `opensRoute` sends a foreground action through the existing route table. `Intents.index()` publishes application objects to device search.

Android is not labeled as Siri parity. It gets launcher shortcuts, donation, indexing, and headless service execution. Voice phrases and system disambiguation remain iOS capabilities.

`Intents.invoke()` calls the same generated command on every port, even when the operating system exposes no intent surface. The platform integrations stay native while the handler remains ordinary Java code.

{{canonical}}
