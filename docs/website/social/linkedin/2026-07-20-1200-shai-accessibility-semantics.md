---
title: "Shai: accessibility became personal"
slug: 2026-07-20-1200-shai-accessibility-semantics
platform: linkedin
account: shai
source_slug: accessibility-semantics
publish_at: '2026-07-20T12:00:00'
timezone: Asia/Jerusalem
image: /blog/accessibility-semantics.jpg
---

Accessibility has become personal for me. I am getting older, and large type is how I read a phone comfortably now.

I also remember working with accessibility experts at Sun and seeing the real complexity. A label is easy. Roles, actions, virtual children, collection position, focus recovery, live announcements, and platform behavior are not.

Our old `setAccessibilityText()` API was the poor man's version. The new semantics tree is a parallel, testable hierarchy with mappings for every current Codename One UI port. Automated audits help, but the final pass still belongs to a person using the actual screen reader.

Full write-up: https://www.codenameone.com/blog/accessibility-semantics/
