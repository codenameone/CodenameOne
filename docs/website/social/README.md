# LinkedIn social queue

LinkedIn posts live in `docs/website/social/linkedin/`. One Markdown file
represents one post. Its body is the exact LinkedIn text; the frontmatter tells
the browser syndication queue when to publish it, which account to use, and
which blog image to attach.

Every valid artifact merged into the default branch is added to
`scripts/website/syndication-queue.json`. There is no review or approval-status
filter. The local browser runner leaves future tasks in the queue until
`scheduled_at`, then publishes through the requested LinkedIn account with the
declared image.

`publish_at` must use the source blog post's publication date. LinkedIn is
same-day distribution; the delayed full-article syndication cadence does not
apply to it.

## File contract

```yaml
---
title: "Internal label"
slug: 2026-07-20-1200-shai-source-slug
platform: linkedin
account: shai # shai or codenameone
source_slug: source-slug
publish_at: '2026-07-20T12:00:00'
timezone: Asia/Jerusalem
image: /blog/source-slug.jpg
---

The LinkedIn post body goes here.

Full write-up: {{canonical}}
```

The IANA timezone keeps the schedule readable and lets the queue convert it to
an unambiguous UTC instant. Choose a time after the blog is live on that same
date. `{{canonical}}` is required exactly once and is replaced with the source
blog post's canonical URL.

Validate artifacts without changing the queue:

```bash
python3 scripts/website/queue_social_posts.py --validate-only
```
