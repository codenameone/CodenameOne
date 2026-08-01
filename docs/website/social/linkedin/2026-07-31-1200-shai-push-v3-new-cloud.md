---
title: "Please test the new Codename One push server"
slug: 2026-07-31-1200-shai-push-v3-new-cloud
platform: linkedin
account: shai
source_slug: push-v3-new-cloud
publish_at: '2026-07-31T12:00:00'
timezone: Asia/Jerusalem
image: /blog/push-v3-new-cloud.jpg
---

If your Codename One application uses push, I have one request:

Change `push.codenameone.com` to `cloud.codenameone.com` on your server and send a real notification through your existing code.

Next week we plan to bring the old service down and direct its traffic to a completely new implementation. The compatibility endpoint accepts the classic request format, so it should be seamless.

“Should” is why I want people to test now.

Push V3 is also in the core. It adds typed messages, managed APNs and FCM credentials, durable queues, server-side segments, campaigns, operational analytics, and commands for widgets and Live Activities.

Sending and managed credentials are available on every plan, including Free. Higher plans increase volume and add persistent campaign tooling.

The article includes the exact test, client code, plan limits, privacy boundaries, and the rest of this week's release series.

{{canonical}}
