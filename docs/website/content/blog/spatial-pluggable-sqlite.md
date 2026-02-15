---
title: Spatial, Pluggable SQLite
slug: spatial-pluggable-sqlite
url: /blog/spatial-pluggable-sqlite/
original_url: https://www.codenameone.com/blog/spatial-pluggable-sqlite.html
aliases:
- /blog/spatial-pluggable-sqlite.html
date: '2018-05-07'
author: Shai Almog
---

![Header Image](/blog/spatial-pluggable-sqlite/maps.jpg)

One of our enterprise accounts is working on a complex GIS application that needs fine grained control over mapping. In this case features such as native maps aren’t useful. For some GIS applications the old `MapComponent` is more useful as it allows working with domain specific data. One of the features he needed was spatial support in the builtin SQL database.

When we initially implemented sqlite support we just delegated all the SQL calls to the OS native equivalents and called it a day. This works well for 98% of the cases but there are two big use cases that are missing by default in Android & iOS: Spatial queries & Security.

[SpatiaLite](https://www.gaia-gis.it/fossil/libspatialite/index) allows you to use geographic locations in SQL. You can effectively query based on physical location which is surprisingly hard to do accurately without such an API. For simple cases like an Uber app you wouldn’t necessarily need something like that but for the more elaborate use cases it might become essential.

The SQL database in iOS/Android isn’t encrypted either, this isn’t a big deal for most applications but for some highly secure implementations that might be a hindrance. There is builtin support for encryption in SQLite that can be turned on as well.

### A Pluggable Solution

We needed to solve the first problem so Steve created the [Spatialite](https://github.com/shannah/cn1-spatialite) cn1lib which is now available in the extension manager. The cn1lib extends the `Database` class & allows you to plug-in a custom version of sqlite to replace the `Database`.

This approach can be extended to support other use cases for security and potentially other capabilities we aren’t aware of.

As a side note this took a lot of effort to build. That’s the type of work we do for enterprise customers!

### Facebook Module Status

With a quick pivot I’ll also provide a quick update on the facebook clone module. We now have 8 lessons in the module and I’ve been adding more on a daily basis. I’m pretty sure we’ll have way more than 30 lessons so I’ll try to pick up the pace!

Here is another sample of the new post `Form`:

![New Post Form in The Facebook Clone App](/blog/spatial-pluggable-sqlite/new-post-form.jpg)

Figure 1. New Post Form in The Facebook Clone App
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Julio Valeriron Ochoa** — November 29, 2022 at 2:29 pm ([permalink](/blog/spatial-pluggable-sqlite/#comment-24548))

> how turn on encryptation mode in sqlite
>



### **Kimotho E** — July 9, 2024 at 4:51 pm ([permalink](/blog/spatial-pluggable-sqlite/#comment-24621))

> Kimotho E says:
>
> How can a custom version of sqlite be plugged in to replace the Database?
>



### **Shai Almog** — July 10, 2024 at 2:31 am ([permalink](/blog/spatial-pluggable-sqlite/#comment-24622))

> Shai Almog says:
>
> See <https://github.com/shannah/cn1-spatialite> create something similar with your version of sqlite.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
