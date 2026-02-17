---
title: Moving from WordPress to Hugo + Cloudflare Pages
date: '2026-02-17'
author: Shai Almog
slug: moving-from-wordpress-to-hugo-cloudflare-pages
url: /blog/moving-from-wordpress-to-hugo-cloudflare-pages/
description: Why we moved the Codename One website from WordPress to Hugo and Cloudflare Pages, what improved, and how to use Giscus for discussions.
---

![WordPress to Hugo migration hero](/blog/moving-from-wordpress-to-hugo-cloudflare-pages/moving-from-wordpress-to-hugo-cloudflare-pages.jpg)

For a long time, the Codename One website ran on WordPress. That was a mistake.

WordPress was a nightmare for us: clumsy to work with, slow, painful, and constantly getting in the way of basic maintenance. It repeatedly messed things up and made even simple updates harder than they should have been.

We have now moved to a static site setup based on **Hugo** and **Cloudflare Pages**.

This gives us a cleaner publishing pipeline, better performance, and a much easier way to keep content current.

## Why We Made This Change

The biggest goal was to make website updates fast, predictable, and automated.

With Hugo + Pages, content updates are now part of our normal Git workflow:

- Edit content in Markdown.
- Open a pull request.
- Review changes.
- Merge and deploy automatically.

This removes a lot of manual overhead and lowers the risk of site breakage from plugin/theme drift.

## Faster Site and Faster Iteration

Static rendering means less runtime complexity and fewer moving parts.

In practice this gives us:

- Faster page loads.
- Better caching behavior.
- Simpler, safer deployments.
- Easier rollbacks when needed.

It also makes it practical for us to iterate on the website much more frequently.

## JavaDocs Are Better Now

One specific improvement I want to call out is the JavaDocs.

They are now significantly better looking, easier to navigate, and much more searchable than before. This has been a long-standing pain point for many users (myself included), and the new setup makes this experience much better.

## New Light/Dark Mode Toggle

We also added a new light/dark mode toggle in the site menu.

Use it to switch to the theme that is most comfortable for you while reading docs, tutorials, and blog posts.

## Comments and Discussion: How Giscus Works

We now use **Giscus** for post discussions.

Giscus is GitHub Discussions-powered comments embedded directly in each blog post. It keeps conversations in one place and gives us moderation and threading tools that fit our development workflow.

### How to use it

1. Scroll to the **Discussion** section at the end of a blog post.
2. Click **Sign in with GitHub** if prompted.
3. Write your comment and submit.
4. You can reply, edit, react, and follow discussions directly from GitHub as well.

If you already use GitHub for Codename One issues or PRs, this should feel very natural.

## We Need Your Feedback

Please tell us what works and what doesn’t on the new website:

- Broken links.
- Missing pages.
- Search/navigation problems.
- Mobile layout issues.
- Anything unclear in docs/tutorial flow.

You can leave feedback in the comments below.

## Migration Timing

Right now the new site is available on **beta.codenameone.com**.

Over the weekend of **February 21-22, 2026**, we will flip the switch so the new site becomes the default at **www.codenameone.com**.

During that window, you might see minor link adjustments and occasional content reshuffling as we finalize the move.

Thanks for your patience, and thanks in advance for helping us polish the new site.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
