---
title: Threadsafe SQLite
slug: threadsafe-sqlite
url: /blog/threadsafe-sqlite/
original_url: https://www.codenameone.com/blog/threadsafe-sqlite.html
aliases:
- /blog/threadsafe-sqlite.html
date: '2017-05-09'
author: Shai Almog
---

![Header Image](/blog/threadsafe-sqlite/generic-java-1.jpg)

One of the main reasons for the thread API I discussed yesterday is a new threadsafe `Database` API. This new API allows you to wrap your `Database` instance with a thread that will hide all access to the database and implicitly make it threadsafe by serializing all requests to a single database thread.

This also has the side effect of removing the hack of working with sqlite on the EDT which could sometimes cause a stutter in the UI when performing SQL. In fact, that was the chief motivation for this particular enhancement!

You can use the new threadsafe database API with a single line:
    
    
    db = new ThreadSafeDatabase(db);

It’s crucial that once you do this you never use the old db object again!

The reason we need this whole thing is that iOS has a builtin version of sqlite that is thread unsafe at a problematic level. E.g. the garbage collector runs on a separate thread and can cause issues if an object is left dangling.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
