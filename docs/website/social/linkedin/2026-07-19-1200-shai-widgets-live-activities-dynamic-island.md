---
title: "Shai: the widget request we called impossible"
slug: 2026-07-19-1200-shai-widgets-live-activities-dynamic-island
platform: linkedin
account: shai
source_slug: widgets-live-activities-dynamic-island
publish_at: '2026-07-19T12:00:00'
timezone: Asia/Jerusalem
image: /blog/widgets-live-activities-dynamic-island.jpg
---

We dismissed widget support as impossible for years.

A home-screen widget has to render when the Codename One UI is not running. The answer was to stop treating it as UI. The app publishes a serializable layout plus dated state. WidgetKit, RemoteViews, or the desktop renderer owns the pixels after that.

Steve's decade-old background-process work supplies fresh data. Timelines carry future updates without waking the app. The same model turned out to fit Live Activities and Dynamic Island too.

The first version has real limits, especially around Android RemoteViews and desktop process lifetime. The model finally fits the operating systems instead of fighting them.

Full write-up: https://www.codenameone.com/blog/widgets-live-activities-dynamic-island/
