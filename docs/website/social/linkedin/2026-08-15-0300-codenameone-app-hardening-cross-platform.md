---
title: "One hardening policy before the platform split"
slug: 2026-08-15-0300-codenameone-app-hardening-cross-platform
platform: linkedin
account: codenameone
source_slug: app-hardening-cross-platform
publish_at: '2026-08-15T03:00:00'
timezone: Asia/Jerusalem
image: /blog/app-hardening-cross-platform.jpg
---

A cross-platform application should not protect only its Android artifact.

Codename One App Hardening runs on the merged application before the build splits into Android, iOS, JavaScript, Windows, Linux, and JavaSE output.

One Enterprise build hint enables the policy:

`codename1.arg.harden.level=standard`

The engine renames classes and members, encrypts eligible application strings, and applies control-flow transforms where the target can support them. Android keeps R8 as its renaming tool. JavaScript skips string encryption. ParparVM skips control-flow changes that would fight its optimizer.

The mapping stays connected to Crash Protection, so a hardened production crash still returns a readable stack trace. A build fails if the required mapping upload fails.

The post covers the full port matrix, string-encryption exclusions, local-build boundary, and how App Hardening combines with App Shield and the database-encryption work now under review.

{{canonical}}
