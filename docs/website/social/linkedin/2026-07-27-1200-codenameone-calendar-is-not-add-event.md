---
title: "Calendar API with local calendars, cloud sync, and conflict handling"
slug: 2026-07-27-1200-codenameone-calendar-is-not-add-event
platform: linkedin
account: codenameone
source_slug: calendar-is-not-add-event
publish_at: '2026-07-27T12:00:00'
timezone: Asia/Jerusalem
review_by: '2026-07-24'
status: draft
image: /blog/calendar-is-not-add-event.jpg
---

The new `com.codename1.calendar` API supports local calendars, cloud providers, recurrence, tasks, offline changes, and conflict handling through one model:

• Android Calendar Provider and Apple EventKit
• Google Calendar and Tasks
• Microsoft Graph and To Do
• CalDAV and RFC 5545 import/export
• version tokens, conflicts, delta sync, and offline mutation queues

Applications query capabilities instead of platform names. The simulator uses an isolated in-memory calendar, so a test never edits the developer's real schedule.

Version tokens prevent a stale edit from silently overwriting a newer provider copy. Incremental sync uses provider delta tokens, while an optional local cache queues offline mutations for the next sync.

Provider webhooks and OAuth credentials remain application and backend responsibilities.

{{canonical}}
