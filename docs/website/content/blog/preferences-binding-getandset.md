---
title: Preferences Binding and getAndSet()
slug: preferences-binding-getandset
url: /blog/preferences-binding-getandset/
original_url: https://www.codenameone.com/blog/preferences-binding-getandset.html
aliases:
- /blog/preferences-binding-getandset.html
date: '2018-02-21'
author: Shai Almog
---

![Header Image](/blog/preferences-binding-getandset/new-features-4.jpg)

I added support for binding a property object to `Preferences` a while back and just didn’t have the time to blog about it. I didn’t consider it too crucial as the functionality is very simple to figure out, the only difficult part is knowledge of the features existence.

Some objects make sense as global objects, we can just use the `Preferences` API to store that data directly but then we don’t have the type safety that property objects bring to the table. That’s where the binding of property objects to preferences makes sense. E.g. say we have a global `Settings` property object we can just bind it to preferences using:
    
    
    PreferencesObject.create(settingsInstance).bind();

So if settings has a property called `companyName` it would bind into `Preferences` under the `Settings.companyName` entry.

We can do some more elaborate bindings such as:
    
    
    PreferencesObject.create(settingsInstance).
        setPrefix("MySettings-").
        setName(settingsInstance.companyName, "company").
        bind();

This would customize all entry keys to start with `MySettings-` instead of `Settings.`. This would also set the company name entry to `company` so in this case instead of `Settings.companyName` we’d have `MySettings-company`.

### getAndSet

A part of getting this to work seamlessly is the `getAndSet` API added to the `Preferences` API. This is a bit of a weird API but it’s pretty useful so lets explain it by example.

Say we have a user setting for refresh as an integer in minutes:
    
    
    int refresh = Preferences.get("refresh", 60);

That will return the refresh value as 60 if it doesn’t exist. The problem is: “how do we know we got back the default?”.

Normally that isn’t a big deal but if we have one path that invokes `Preferences.get("refresh", 60)` and another one that does a `Preferences.get("refresh", 100);` how can we tell which one is correct?

I could use `Preferences.get("refresh", 60);` and if 60 is returned I can invoke `set` to explicitly set the 60 value just to make sure that 60 will be used from now on. But that would mean changing preferences with every invocation even if the data didn’t change.

`getAndSet()` essentially solves that problem. You get the data one and if the default is used that default is then stored into the `Preferences` so you can’t get inconsistent results for a specific entry.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
