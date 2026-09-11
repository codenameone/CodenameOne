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

Our API reference used to bring its own website along with it. We copied standard Javadoc output into the site, scoped its stylesheet under a wrapper, and used JavaScript to fetch pages into that wrapper. Dark mode made the mismatch particularly obvious.

[PR #5743](https://github.com/codenameone/CodenameOne/pull/5743) changes the output boundary. Javadoc now supplies the API model and documentation; Hugo owns the pages. The resulting doclet is in the repository for other Java projects to inspect and adapt.

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

## Java 25 runs the tools; Markdown comments began in Java 23

The module targets Java 25 and is built outside the Java 8 reactor. The documentation feature itself arrived in JDK 23: adjacent `///` comments can contain Markdown, and the doclet receives Markdown nodes in the documentation tree. [Oracle's Markdown documentation guide](https://docs.oracle.com/en/java/javase/24/javadoc/using-markdown-documentation-comments.html).

That distinction makes the reuse story clearer. This implementation needs Java 25 as currently packaged, but Markdown Javadoc is not a proprietary Codename One format or a Java 25 language invention.

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

`MarkdownSections` recognizes six conventional headings and extracts their structure. Other headings, such as Threading and Example, remain in the prose. That limit matters for reuse: this parser implements our documented conventions, not every heading a Java project might invent.

The new API search indexes identifiers and camel-case segments, grouped by type. The PR reports a 1.9 MB index, 341 KB gzipped, compared with 9.7 MB for the larger representation considered during development. Search can now return API members alongside indexed site content. The full developer guide remains outside this index; use the browser's find command within the guide.

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

The relevant pieces are [the doclet module](https://github.com/codenameone/CodenameOne/tree/561dab8e05/maven/javadoc-hugo-doclet), [the Javadoc layouts](https://github.com/codenameone/CodenameOne/tree/561dab8e05/docs/website/layouts/javadoc), and [the website build script](https://github.com/codenameone/CodenameOne/blob/561dab8e05/scripts/website/build.sh). Review the source license, URL conventions, recognized Markdown sections, and site-specific templates when adapting it. This is reusable source, not a claim of a drop-in plugin for every Hugo theme.

## Existing member links are part of the API

Changing the renderer can silently break years of links into methods. Generics, erased signatures, varargs, and inherited members all affect the anchors the standard doclet publishes.

The parity check compares the two renderings. The PR records 2,272 pages compared and 29,583 fragments checked. It found defects in erased varargs anchors and inherited members from undocumented superclasses. A case collision between the `List` class and the `list` package also appeared on macOS while looking correct on Linux.

This makes a useful test for another documentation migration: preserve the addresses users already cite, and prove that deleting a page or changing an anchor fails the check.

## Documentation is where the safe path becomes discoverable

This week's {{< post-link path="/blog/performance-work-between-benchmarks" text="release" >}} includes APIs whose boundaries matter: soft references can miss, continuity must respect logout, and task removal is not credential revocation. Those caveats belong beside the method a developer finds through search.

The guide teaches the workflow. The API reference states the contract. Putting both in the same site makes it easier to move between them, including when reviewing code suggested by an assistant. Search now reaches the API members, while the full guide retains its own navigation. The source remains the authority; the site should help readers find and check it.

---

## Discussion

_Which part of your API documentation still behaves like a separate website?_

{{< giscus >}}
