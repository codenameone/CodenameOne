---
title: "HealthKit cannot tell you whether read access was denied"
slug: 2026-08-01-1200-codenameone-health-api-false-certainty
platform: linkedin
account: codenameone
source_slug: health-api-false-certainty
publish_at: '2026-08-01T12:00:00'
timezone: Asia/Jerusalem
image: /blog/health-api-false-certainty.jpg
---

HealthKit cannot tell an application whether the user denied read access.

A denied read looks exactly like an empty store. That is a privacy guarantee: the application cannot infer that someone chose to hide a sensitive category.

It also means a cross-platform `hasReadPermission()` method would lie.

The new Codename One health API keeps that asymmetry visible. On iOS, a completed permission sheet means the user was asked. An empty query means “no data available,” not “access denied.”

That same refusal to invent certainty shapes the rest of the API:

• An aggregate with no samples is null, not zero.
• Calendar buckets require a time zone.
• Phone and watch sources are not silently deduplicated.
• Change subscriptions say when they can miss backdated data.
• Unsupported workout, sleep, nutrition, and sensor paths fail explicitly.

The deep dive covers HealthKit, Health Connect, recorded workouts, eight Bluetooth health sensor profiles, deterministic simulation, build hints, and why no framework switch can make an application HIPAA compliant.

{{canonical}}
