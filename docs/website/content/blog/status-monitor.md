---
title: Status Monitor
slug: status-monitor
url: /blog/status-monitor/
original_url: https://www.codenameone.com/blog/status-monitor.html
aliases:
- /blog/status-monitor.html
date: '2018-08-29'
author: Shai Almog
---

![Header Image](/blog/status-monitor/generic-java-1.jpg)

I wrote before about [Crisp](/blog/moving-away-from-intercom.html) and how pleased we are over the migration to their service. Recently they started offering a new service of status page. This service runs on their servers and essentially monitors whether our service is down.

Due to the complexity of our service not all of the pieces are monitored but a few of the more important features are already mapped. Hopefully, if you experience service issues you can look in the status page and you’d know if something is going on. Notice that when it goes red we get alerts and notice that something is broken

You can see the status page at <https://status.codenameone.com/>

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
