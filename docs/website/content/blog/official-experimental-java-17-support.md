---
title: Official Experimental Java 17 Support for Codename One Projects
date: '2026-03-14'
author: Shai Almog
slug: official-experimental-java-17-support
url: /blog/official-experimental-java-17-support/
description: Codename One projects generated with the Initializr can now use official experimental Java 17 support, with record support arriving in Codename One 7.0.229 on March 20, 2026, and more JDK support planned next.
feed_html: '<img src="https://www.codenameone.com/blog/official-experimental-java-17-support.jpg" alt="Official Experimental Java 17 Support for Codename One Projects" /> Codename One projects generated with the Initializr can now use official experimental Java 17 support, with record support arriving in Codename One 7.0.229 on March 20, 2026, and more JDK support planned next.'
---

![Official Experimental Java 17 Support for Codename One Projects](/blog/official-experimental-java-17-support.jpg)

We now have official experimental support for Java 17 in Codename One projects.

This is available through the [Initializr](/initializr/). To use it you should generate a new project and select **Java 17** during project creation.

Support for Java 17 required work across the toolchain, the generated projects and several targets. This wasn't just a matter of changing a version number and hoping for the best.

## How This Works

You can use these new Java 17 projects with practically any JDK for the build itself. We tested this with JDK 21 and even JDK 25. In other words, you select Java 17 for the generated project, but your local machine does not need to run on JDK 17 just to build it.

## Caveats

There are a few caveats you should know before jumping in:

- The desktop target is not currently supported for Java 17 projects. If there is demand we can add that. You can still use the `jar` target to build desktop applications with Codename One, and that works fine with Java 17.
- UWP will not be supported. That target was already deprecated, so there is no point in doing the extra work there.
- Other targets should just work.

## What Java 17 Syntax Works?

This support includes modern language syntax that makes day to day code much nicer to write.

At the moment this includes features like `var`, switch expressions and text blocks. Record support is planned for Codename One `7.0.229`, slated for release on March 20, 2026.

For example, `var` lets us remove obvious type boilerplate from local variables:

```java
var greeting = "Hello";
var target = "Codename One";
```

Switch expressions are more concise and make it easier to return a value directly from the switch:

```java
var message = switch (greeting.length()) {
    case 5 -> greeting + " " + target;
    default -> "unexpected";
};
```

Text blocks are also supported, so multi-line strings are much more readable:

```java
var textBlock = """
        Java 17 language features
        should compile in tests.
        """;
```

Record syntax is also queued for Codename One `7.0.229`, slated for release on March 20, 2026, so Java 17 projects will be able to use compact immutable data carriers too.

For example, this record will compile once `7.0.229` is available on March 20, 2026:

```java
record Person(String name, int age) {}

var person = new Person("Duke", 29);
System.out.println(person.name());
```

That means you can start using language improvements that make code cleaner and easier to read, and records will join that list in `7.0.229` on March 20, 2026.

This still does not include support for newer JDK APIs such as streams. As mentioned in the previous post, if you need streams today there is already a cn1lib solution for that, and we hope to improve the built-in story over time.

## What About Java 21 and 25?

Since we mentioned JDK 21 and 25 above, it is worth clarifying the roadmap.

We do plan to add support for newer language levels in a coming update. That is a different challenge, because Android only supports up to Java 17 today.

We would still like to introduce that support, but at the moment Java 17 is the more important milestone.

## Other Updates Worth Mentioning

A few other useful things landed over the past week.

The [Initializr](/initializr/) now includes more advanced CSS theme editing with live preview, and there were several follow-up fixes to make theming behave more reliably. This is relevant if you are starting a fresh project anyway, because the [Initializr](/initializr/) has become a much stronger starting point than it was even a couple of weeks ago.

## Try Java 17 on a Real Project

If you have been waiting for a more modern Java syntax level in Codename One, this is the time to give it a try.

Create a new project with the [Initializr](/initializr/), select Java 17 and see how it feels in a real application. If you run into issues on Android, iOS, JavaScript or any other target, let us know. That feedback is what will help us polish this support and move it forward.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
