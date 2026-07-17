---
title: "Shai: pixel perfect needs a test"
slug: 2026-07-27-0300-shai-pixel-perfect-is-a-test
platform: linkedin
account: shai
source_slug: pixel-perfect-is-a-test
publish_at: '2026-07-27T03:00:00'
timezone: Asia/Jerusalem
review_by: '2026-07-24'
status: draft
image: /blog/pixel-perfect-is-a-test.jpg
---

We do not use UIKit or Material widgets for the Codename One UI. We paint the UI ourselves.

That gives us control over every pixel. It also means “pixel perfect” cannot be a slogan.

This week we built native iOS and Android reference apps, captured real controls, and made CI compare our themes against them. Pixels, geometry, and fixed animation frames all get separate checks. A one-way gate stops a known result from quietly getting worse.

The current iOS tab bar still has the lowest score. Full-app visual review is still missing from the automated layer. I would rather publish those limits than hide them behind a 95% median.

Full write-up: {{canonical}}
