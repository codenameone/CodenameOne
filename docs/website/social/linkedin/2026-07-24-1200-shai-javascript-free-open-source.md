---
title: "The Codename One JavaScript port is now free and open source"
slug: 2026-07-24-1200-shai-javascript-free-open-source
platform: linkedin
account: shai
source_slug: javascript-free-open-source
publish_at: '2026-07-24T12:00:00'
timezone: Asia/Jerusalem
image: /blog/javascript-free-open-source.jpg
---

The Codename One JavaScript port is now open source and available on every plan, including Free.

You can also build the web target locally without a Codename One account. ParparVM is now the default compiler, with TeaVM still available through `javascript.port=teavm`.

ParparVM runs Java in a Web Worker and keeps browser integration in the port layer. Because we control the translator, runtime, port, and tests, shared framework changes now go through browser testing in our regular CI.

The local builder can also generate deployment proxies for Jakarta Servlet, `javax.servlet`, Node, PHP, AWS Lambda, Google Cloud Functions, and Cloudflare Workers.

JavaScript builds were previously an Enterprise feature and a meaningful source of revenue. We expect this change to reduce that revenue, which made it a difficult decision for a small company.

We chose broader access because the port will improve faster when more applications build and test against it. I wrote about the architecture, local builds, deployment proxies, and how the community can help.

{{canonical}}
