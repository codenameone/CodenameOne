---
title: "Javadoc That Feels Like Your Website"
slug: javadoc-hugo-markdown-doclet
url: /blog/javadoc-hugo-markdown-doclet/
date: '2026-09-16'
author: Shai Almog
description: "A Java 25 doclet turns the Codename One API model and Markdown comments into Hugo content. The site gains integrated search and theming while preserving member links and the offline Javadoc archive."
feed_html: '<img src="https://www.codenameone.com/blog/javadoc-hugo-markdown-doclet.jpg" alt="Your Javadoc Your Website" /> A Java 25 doclet turns the Codename One API model and Markdown comments into Hugo content. The site gains integrated search and theming while preserving member links and the offline Javadoc archive.'
series: ["release-2026-09-11"]
---

![Your Javadoc Your Website](/blog/javadoc-hugo-markdown-doclet.jpg)

Follow a link from the developer guide into an API method and you used to arrive in what felt like another website. The navigation changed. The styling changed. Dark mode made the join harder to hide.

We had wrapped standard Javadoc output, scoped its CSS, and loaded its pages through JavaScript. Every improvement to the surrounding site left us with another detail to reconcile inside that wrapper.

The fix was to stop asking Javadoc to build our website. It already knows the Java types, signatures, and documentation trees. Hugo already knows how our pages should look. [The new doclet](https://github.com/codenameone/CodenameOne/pull/5743) passes that API model to Hugo as content, and lets each tool do its own job.

## Let the site render its own content

The new `maven/javadoc-hugo-doclet` module emits a Hugo content file per type. Front matter carries structured API data, including signatures and members. Site templates render that structure with the same theme and typography as the guide and blog.

The standard doclet still runs to create `javadocs.zip` for offline use. Both outputs read the same staged source tree. We get an integrated website without giving up the archive a developer can keep beside an IDE.

{{< mermaid >}}
flowchart LR
    S[Java sources] --> J[Javadoc API model and comment trees]
    J --> D[Hugo doclet]
    D --> C[Type pages and structured front matter]
    D --> I[API search index]
    C --> H[Hugo templates and Goldmark]
    I --> W[Website search]
    H --> W
    J --> A[Standard doclet and offline archive]
{{< /mermaid >}}

## Markdown comments made this a much better fit

Java's [Markdown documentation comments](https://docs.oracle.com/en/java/javase/24/javadoc/using-markdown-documentation-comments.html), introduced in JDK 23, let us write documentation in the same format we use across the site. The doclet receives those Markdown nodes directly. We build our standalone module with Java 25, outside the Java 8 reactor, and pass the prose to Hugo's Goldmark renderer.

That gives other Java projects a useful starting point: keep documentation beside the code, but let the site's own templates render the API.

Here is a complete small input for experimenting with the doclet:

```java
package demo;

/// A temperature conversion used by the sample application.
public final class Temperature {
    private Temperature() {}

    /// Converts Celsius to Fahrenheit.
    ///
    /// #### Parameters
    /// - `celsius`: temperature in degrees Celsius.
    ///
    /// #### Returns
    /// Temperature in degrees Fahrenheit.
    public static double fahrenheit(double celsius) {
        return celsius * 9.0 / 5.0 + 32.0;
    }
}
```

The comment body goes to Hugo's Goldmark renderer. Adding another Markdown renderer inside the doclet would introduce a second interpretation of the same prose.

## Recover structure without eating the author's headings

Codename One documentation often expresses parameters and return values as Markdown sections. Standard Javadoc renders those sections inside the description but does not turn them into parameter tables.

`MarkdownSections` recognizes six conventional headings and extracts their structure. Other headings, such as Threading and Example, remain in the prose. When adapting the doclet, extend those recognized sections to match your own documentation conventions.

The new API search indexes identifiers and camel-case segments, grouped by type. Grouping the API data brought the index to 1.9 MB, or 341 KB gzipped, from the 9.7 MB representation we first considered. Search can now return API members alongside indexed site content. The full developer guide remains outside this index; use the browser's find command within the guide.

![API reference rendered using the Codename One website theme](/blog/javadoc-hugo-reference.png)

*The [PublicKey reference](https://www.codenameone.com/javadoc/com/codename1/security/PublicKey.html), captured from the live site on September 10. The page uses the site's own layout.*

![Website search returns PublicKey and PrivateKey fromPem methods](/blog/javadoc-hugo-search.png)

*A live search for `fromPem` returns the method overloads, grouped under their API types.*

## Try the doclet on another Java project

From a checkout of the repository, build the standalone module with Java 25 selected in `JAVA_HOME` and on `PATH`:

```bash
mvn -f maven/javadoc-hugo-doclet/pom.xml package
javadoc \
  -doclet com.codename1.doclet.hugo.HugoDoclet \
  -docletpath maven/javadoc-hugo-doclet/target/classes \
  -d /tmp/demo-api-content \
  --search-index /tmp/demo-api-search.json \
  /tmp/demo-src/demo/Temperature.java
```

Save the example as `/tmp/demo-src/demo/Temperature.java` first. The command emits content and an index; a bare Hugo site still needs the corresponding rendering templates and search integration.

The relevant pieces are [the doclet module](https://github.com/codenameone/CodenameOne/tree/561dab8e05/maven/javadoc-hugo-doclet), [the Javadoc layouts](https://github.com/codenameone/CodenameOne/tree/561dab8e05/docs/website/layouts/javadoc), and [the website build script](https://github.com/codenameone/CodenameOne/blob/561dab8e05/scripts/website/build.sh). Start with the templates and URL conventions, then adapt the recognized Markdown sections to your API.

## Existing member links are part of the API

Changing the renderer can silently break years of links into methods. Generics, erased signatures, varargs, and inherited members all affect the anchors the standard doclet publishes.

We compared 2,272 pages and checked 29,583 fragments against the old rendering. It found defects in erased varargs anchors and inherited members from undocumented superclasses. A case collision between the `List` class and the `list` package also appeared on macOS while looking correct on Linux.

This makes a useful test for another documentation migration: preserve the addresses users already cite, and prove that deleting a page or changing an anchor fails the check.

## The next method you search for

A search for `fromPem` now finds the overloads under `PublicKey` and `PrivateKey`, on pages that belong to the site. You can move from the workflow in the guide to the method contract without entering a second navigation system.

That matters for the APIs in this week's {{< post-link path="/blog/performance-work-between-benchmarks" text="release" >}}. When a developer or an assistant suggests a cache, a continuity callback, or a sign-out sequence, the reference should make the details easy to check. We have put the API members into site search and kept the old member links working. The guide continues to provide the longer examples and its own navigation.

The same approach is available to your Java project. Javadoc can give you the model without dictating the website.

---

## Discussion

_Which part of your API documentation still behaves like a separate website?_

{{< giscus >}}
