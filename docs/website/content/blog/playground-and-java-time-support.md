---
title: Playground and java.time Support in Codename One
date: '2026-03-27'
author: Shai Almog
slug: playground-and-java-time-support
url: /blog/playground-and-java-time-support/
description: Codename One now includes a new Playground for rapid UI scripting and iteration, and adds built-in support for the java.time API.
feed_html: '<img src="https://www.codenameone.com/blog/playground-and-java-time-support.jpg" alt="Playground and java.time Support in Codename One" /> Codename One now includes a new Playground for rapid UI scripting and iteration, and adds built-in support for the java.time API.'
---

![Playground and java.time Support in Codename One](/blog/playground-and-java-time-support.jpg)

This week includes two important additions to Codename One.

The first is a new [Playground](/playground/), which gives us a much faster way to experiment with UI code, prototype ideas, and share small runnable examples.

The second is built-in support for the **`java.time` API**, which fills another long-standing gap in modern Java compatibility.

Both are useful on their own, and both remove a bit more friction from day-to-day development.

## A New Playground for Rapid UI Prototyping

The new Playground is an interactive scripting environment for Codename One UI development.

You can try it here:

[Open the Playground](/playground/)

The implementation is built on top of **BeanShell**, and that choice was very intentional.

A playground should feel lightweight. It should encourage experimentation. You should be able to try an idea, tweak it, throw it away, and try the next version within seconds. That is very different from the normal compile-run cycle of a full application build.

The traditional cycle is still exactly what we want for real projects. But for prototyping, especially UI prototyping, it is often just too heavy. If all you want to do is test a layout, check an interaction, verify a component hierarchy, or try a styling idea, waiting for a full build gets in the way of the creative process.

That is why a scripting approach makes sense here.

Instead of treating every experiment like a full application, the Playground lets you write a small piece of code and see the result immediately. That fits the rapid prototype mentality much better.

Here is a simple example:

```java
Container root = new Container(BoxLayout.y());
Button btn = new Button("Click me");
btn.addActionListener(e -> Dialog.show("Hello", "World", "OK", null));
root.add(btn);
root;
```

This is exactly the kind of thing that benefits from an instant feedback loop. You are not building an app here. You are testing an idea.

The Playground also supports more structured approaches when you want them. You can write loose scripts, use lifecycle-style scripts, or return a component from a `build(PlaygroundContext)` method. That gives it enough flexibility to be useful for both quick experiments and slightly more realistic prototypes.

Another important part of this was making the scripting environment feel familiar to modern Java developers. Since BeanShell does not natively support Java 8 lambdas, the Playground adds transformation support so common listener patterns still work naturally in scripts.

That means code in the Playground can still feel close to the way we normally write Codename One code.

Beyond basic script execution, the Playground includes a few features that make it much more useful in practice:

- shareable URLs for sending examples to other people
- an inspector tab for viewing the component hierarchy
- support for common UI and listener patterns
- pre-imported Codename One packages to reduce setup noise

The shareable URL support is especially useful. A playground becomes far more valuable when it is not just a personal scratchpad, but also a communication tool. Being able to send someone a small, runnable UI example is great for demos, bug reports, support, and collaboration.

If you want to dig into the details, including architecture notes, supported script styles, lambda handling, inspector support, and current limitations, the full README is here:

[Codename One Playground README](https://github.com/codenameone/CodenameOne/blob/master/scripts/cn1playground/README.md)

## Built-in Support for `java.time`

The other big addition is support for the `java.time` API.

This is another one of those APIs that modern Java developers expect to have available. The older date and time APIs have always been awkward, and `java.time` gave Java a much better model for representing dates, times, durations, offsets, and zones.

That support is now built into Codename One.

The implementation includes the core types developers are most likely to use in real code, including:

- `Instant`
- `LocalDate`
- `LocalTime`
- `LocalDateTime`
- `OffsetDateTime`
- `ZonedDateTime`
- `Duration`
- `Period`
- `Clock`
- `ZoneId`
- `ZoneOffset`
- `DateTimeFormatter`

This matters for a few reasons.

First, it makes shared Java code much easier to bring into Codename One projects. A lot of code written for server-side logic, validation, scheduling, formatting, or general business rules already uses `java.time`. Supporting that API reduces the amount of adaptation needed.

Second, `java.time` gives us a much clearer way to express time-related logic. It separates concepts that should be separate. A local date is not the same thing as an instant. A duration is not the same thing as a period. A zoned date-time is not the same thing as a local date-time. These distinctions help prevent bugs and make code easier to understand.

Third, this support improves behavior in areas that are notoriously tricky: leap years, daylight saving transitions, zone offsets, parsing, and formatting. The included tests specifically cover edge cases like DST transitions in `America/New_York`, leap-day handling, localized formatting, and translator/runtime consistency.

For example, code like this now becomes natural in Codename One:

```java
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

LocalDate today = LocalDate.now();
LocalDateTime meeting = LocalDateTime.of(2026, 4, 2, 14, 30);
ZonedDateTime localMeeting = ZonedDateTime.of(meeting, ZoneId.of("America/New_York"));
```

[See it in the playground...](https://pr-4636-website-preview.codenameone.pages.dev/playground-app/?code=aW1wb3J0IGphdmEudGltZS5Mb2NhbERhdGU7CmltcG9ydCBqYXZhLnRpbWUuTG9jYWxEYXRlVGltZTsKaW1wb3J0IGphdmEudGltZS5ab25lSWQ7CmltcG9ydCBqYXZhLnRpbWUuWm9uZWREYXRlVGltZTsKCkxvY2FsRGF0ZSB0b2RheSA9IExvY2FsRGF0ZS5ub3coKTsKTG9jYWxEYXRlVGltZSBtZWV0aW5nID0gTG9jYWxEYXRlVGltZS5vZigyMDI2LCA0LCAyLCAxNCwgMzApOwpab25lZERhdGVUaW1lIGxvY2FsTWVldGluZyA9IFpvbmVkRGF0ZVRpbWUub2YobWVldGluZywgWm9uZUlkLm9mKCJBbWVyaWNhL05ld19Zb3JrIikpOwpMYWJlbCB0aW1lID0gbmV3IExhYmVsKGxvY2FsTWVldGluZy50b1N0cmluZygpKTsKdGltZTsK)

And formatting/parsing flows look the way modern Java developers expect:

```java
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

LocalDateTime parsed = LocalDateTime.parse(
        "2026-03-27 09:45:00",
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
);
```

[See it in the playground...](https://pr-4636-website-preview.codenameone.pages.dev/playground-app/?code=aW1wb3J0IGphdmEudGltZS5Mb2NhbERhdGVUaW1lOwppbXBvcnQgamF2YS50aW1lLmZvcm1hdC5EYXRlVGltZUZvcm1hdHRlcjsKCkxvY2FsRGF0ZVRpbWUgcGFyc2VkID0gTG9jYWxEYXRlVGltZS5wYXJzZSgKICAgICAgICAiMjAyNi0wMy0yNyAwOTo0NTowMCIsCiAgICAgICAgRGF0ZVRpbWVGb3JtYXR0ZXIub2ZQYXR0ZXJuKCJ5eXl5LU1NLWRkIEhIOm1tOnNzIikKKTsKCkxhYmVsIHRpbWUgPSBuZXcgTGFiZWwocGFyc2VkLnRvU3RyaW5nKCkpOwp0aW1lOwo)

This is a meaningful compatibility milestone because it removes another obvious gap in the Java API surface available inside Codename One.

## Closing Thoughts

The Playground gives us a faster and more natural way to prototype UI ideas.

The `java.time` support gives us a better and more modern foundation for date and time code.

These are very different additions, but both make everyday development smoother, and both push Codename One further in the right direction.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}