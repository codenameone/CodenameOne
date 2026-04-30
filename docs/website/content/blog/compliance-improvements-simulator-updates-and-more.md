---
title: Compliance Improvements, Simulator Updates, and More
date: '2026-04-03'
author: Shai Almog
slug: compliance-improvements-simulator-updates-and-more
url: /blog/compliance-improvements-simulator-updates-and-more/
description: In today's update, we're finally removing Proguard from the build process with many implications for all of us.

feed_html: '<img src="https://www.codenameone.com/blog/compliance-improvements-simulator-updates-and-more.jpg" alt="Compliance Improvements, Simulator Updates, and More in Codename One" /> In todays update were finally removing Proguard from the build process with many implications for all of us.'
---

![Compliance Improvements, Simulator Updates, and More](/blog/compliance-improvements-simulator-updates-and-more.jpg)

In today's update, we're finally removing Proguard from the build process with many implications for all of us.

## Compliance Checks without Proguard

Codename One only supports a subset of JavaSE's API. While we're constantly trying to expand the supported set of APIs, it would always be a partial list. Initially, this created a lot of problems where developers would send a build and find out that API X wasn't supported.

To solve this problem, Steve came up with an ingenious trick. Maven builds run proguard on the finished jar, it implicitly fails if an API is missing and thus fails "compliance" before reaching the build servers. 

This works well but has several problems:

* Error messages are very obtuse
* It's a bit slow
* We could do so much more 

With this update, we added a new check that replaces proguard with our own custom validator.

The amazing thing is that we can now support `String.split()`!

### The Problem of String's `split()` 

Don't use `String.split()`. It's a terrible API. I use it myself occasionally, but the feature is deeply problematic, which is why we didn't add it. 

The crux of the issue is that split() takes up a regex expression. Regex is deeply nuanced, one misplaced character can bring down an application. The parsing of regex is also challenging and fragile.  

The real problem is that on JavaSE/Android split would have used the Java native version of `split()`, but on JavaScript/iOS it would use our implementation. That would mean a big difference in performance but also in functionality as a regex might work in the simulator then fail on the device. 

Our new compliance code now rewrites the `split()` calls in the code and converts them to our API. That means the behavior will be identical in the simulator and the device.

As a sidenote we also added broader support for `String.format()` across runtimes and included `StandardCharsets` constants, which helps close another gap between standard Java code and what developers expect to work inside Codename One.

### New Java Versions on Android

This also means we will be able to adopt newer versions of Java past 17. Android stopped updating JVMs after JDK 17, which means newer Java language features aren't supported.

However, we can now detect if a class uses newer bytecode and compile it down to JDK 17 levels. This means we can use JDK 25 features while maintaining Android compatibility. 

## Bug Fixes of Note

This week we fixed some interesting bugs in the issue tracker. 

### Simulator Location Support

`LocationSimulation` was updated to replace the old JavaFX-based mapping with a JCEF-based implementation, along with follow-up fixes for JCEF detection and graceful fallback behavior.


### Incorporated Text Scale by Default

The Initializr now defaults `useLargerTextScaleBool` to `true` in the generated `theme.css`.

This is a small change, but it improves the out-of-the-box experience in a very practical way.

### Multiline `TextArea` Supports Vertical Centering

Vertical alignment wasn't for multiline rendering in `TextArea` wasn't supported for a long time. We now support centering both horizontally and vertically of multiline texts.

### Android Video Thumbnail Workaround

On Android, a thumbnail sometimes produces a black-frame. This is an Android OS/VM bug unrelated to Codename One.

A new opt-in workaround was added to improve seek preview behavior. You can enable it using:

```java
video.setVariable(Media.VARIABLE_ANDROID_SEEK_PREVIEW_WORKAROUND, Boolean.TRUE);
```

This is one of my favorite things in Codename One. We can fix device/OS bugs for you.

## Javadoc Playground Links Were Tightened Up

Now that the Playground is part of the normal workflow, it made sense to improve the links generated from Javadocs.

The “Open in playground” links were updated to use a base64-encoded payload, which makes them more robust and less prone to breaking on larger or more awkward examples.

Almost all Java code snippets in the JavaDoc now have an "open in playground" link next to them. A lot of the code might not work since the playground is still under heavy development. But we'll get there...

## JavaScript Support in ParparVM

This is probably the most important feature this week, but it isn't something most of you would "play with." We added support for JavaScript as a target destination in our native VM. 

Right now this has no impact on you. When you send a build or use Playground/Initializr, you would still use TeaVM and its port. So why are we doing this?

TeaVM is a fantastic project. But it’s heading in its own direction, which is different from ours. We decided to consolidate the different ports to enable a singular portable consistent experience for Codename One developers.

The value of maintaining a single VM that supports all the non-Java ports is powerful. By using ParparVM for our JavaScript port, we can move our support for Java language features at our own pace.

## Closing Thoughts

I spent a lot of time this week on the issue tracker going through old issues. Assigning, closing and classifying stuff. If you opened issues in the past, we would appreciate your help in closing or classifying these.

It also stands for issues that are important to you and got buried under the mountain of issues. If there are any of those, then let us know. We won't fix a lot of those. Some issues are just out of scope, and our scope is already huge. A lot are just too old by now, and it's hard to tell what's going on. But we're doing our best to go through that list. 

Over this past week we closed over 30 issues, but there are still over 500 open issues. We'd appreciate your help in reducing these.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}