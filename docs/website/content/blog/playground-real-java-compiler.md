---
title: "Try Real Java in Your Browser, Compiler Errors Included"
slug: playground-real-java-compiler
url: /blog/playground-real-java-compiler/
date: '2026-10-13'
author: Shai Almog
description: "The Playground compiles Java to bytecode and translates it inside the browser. Try records and lambdas, inspect diagnostics and understand the supported runtime API."
feed_html: '<img src="https://www.codenameone.com/blog/playground-real-java-compiler.jpg" alt="Java source becomes bytecode and a live browser preview" /> The Playground compiles Java to bytecode and translates it inside the browser. Try records and lambdas, inspect diagnostics and understand the supported runtime API.'
series: ["release-2026-10-09"]
---

![Java source becomes bytecode and a live browser preview](/blog/playground-real-java-compiler.jpg)

A playground should tell you whether your Java works. A Java-like interpreter can instead teach you which Java it happens to understand. That difference becomes annoying as soon as you paste a record, a lambda or an example with a tricky generic type.

We rebuilt the [Codename One Playground](/playground/) around our in-tree Java compiler. Your code becomes class files, then the ParparVM translator turns those classes into JavaScript inside the page. The running app loads them and updates the preview. Compilation does not send your source to a server.

## A compiler running inside its own output

Both the compiler and translator are written in Java. ParparVM translates them into the JavaScript bundle you load when you open the Playground. That bundle can then compile and translate more Java: the code you type.

{{< mermaid >}}
flowchart TD
    Edit[Edit Java source] --> Wait[300 ms after typing stops]
    Wait --> Compiler[In-page Java compiler]
    API[API signature stubs] --> Compiler
    Compiler --> Bytecode[Class files]
    Bytecode --> Translator[Incremental ParparVM translator]
    Translator --> Loader[Load JavaScript class definitions]
    Loader --> Preview[Run and update preview]
    Compiler --> Errors[Line and column diagnostics]
{{< /mermaid >}}

This is our `com.codename1.tools.javac` implementation, not the OpenJDK compiler copied into the page. It targets javac's language rules and diagnostics. The [compiler conformance tests](https://github.com/codenameone/CodenameOne/tree/ebe0e64be3/vm/JavaCompiler/tests) compare program output and negative cases with the reference compiler.

That distinction matters when reporting a bug. A familiar error message is useful, but agreement on a test corpus does not prove every possible Java program behaves identically.

## Paste a record and change it

Open the Playground and replace the Java pane with this script:

```java
record Item(String name, int count) {}

Item item = new Item("Orders", 3);
Form form = new Form("Java in the browser", new BoxLayout(BoxLayout.Y_AXIS));
Label label = new Label(item.name() + ": " + item.count());
Button button = new Button("Show the next count");
button.addActionListener(event ->
        label.setText(item.name() + ": " + (item.count() + 1)));
form.addAll(label, button);
form.show();
```

The Playground supplies the usual UI imports and supports script-shaped input. Change the constructor's `3` to `"three"`. That is a type error, and the editor can point to it before the app runs. Restore the number and click the button to exercise a listener compiled from your code.

Records, switch expressions, pattern matching and lambdas go through the compiler's type checking and lowering. A record gets explicit methods in bytecode. The translator handles the resulting classes against the API the page actually contains.

[![The article record example running in the browser Playground after the button updates Orders to four](/blog/release-2026-10-09/playground-record.png)](/blog/release-2026-10-09/playground-record.png)

*Captured from the public Playground on October 8. Clicking the compiled listener changed the label to `Orders: 4`.*

[![The Playground reports that the Item constructor requires an int after three is supplied as a string](/blog/release-2026-10-09/playground-type-error.png)](/blog/release-2026-10-09/playground-type-error.png)

*The deliberate mistake reports the constructor mismatch at line 3, column 13. The preview does not run the invalid program.*

## Keep the API open without keeping everything

A normal app has a known set of reachable classes. The translator removes unused code and can turn some virtual calls into direct calls. The Playground cannot assume that tomorrow's source will only call methods today's bundle already uses.

Its host bundle therefore keeps selected API packages open. Those methods retain predictable names and support subclasses arriving later. A small stub library supplies signatures, generic information and constants to the compiler without supplying a second copy of the implementations.

The compiler, translator and platform internals stay closed to user code, so they can still be reduced and renamed. Incremental translation only emits the new user classes. A later run replaces those definitions without rebuilding the whole application bundle.

## Removing the interpreter removed other machinery

BeanShell needed access bridges to call framework code in an environment without its usual reflection support. The old generated access adapters occupied 23,827,851 bytes across 161 Java files, before counting their central registry. The new pipeline removes that registry, BeanShell and its listener and lambda bridges.

That source-file saving is not a browser download measurement. The compiler and translator also add code, and compression changes the transfer size. The repository includes `compare-javascript-bundles.sh` to compare actual artifacts; use the same build configuration and compression for both. The Playground console reports compile, load and run times so a smaller bundle and a faster edit cycle can be measured separately.

For the example above, one Chromium run on the local Mac reported **1,247 ms compile, 505 ms load and 8 ms run**. The invalid version reported **841 ms compile** and no load or run. Those are single observations from the deployed Playground, not a warmed benchmark or a before/after speedup. They make the phases visible without attributing an unmeasured gain to the rewrite.

## An unfinished edit must be valid input to the tool

A compiler in an editor sees broken programs most of the time: half a string literal, an incomplete method and a generic type with its closing bracket still missing. The conformance suite includes program prefixes so these produce diagnostics rather than exceptions or hangs.

Other gates compare the self-hosted translator's output with the JVM translator byte for byte. Browser tests run samples, edit code, rerun it, navigate forms and provoke listener exceptions. That last case matters: an exception swallowed after a successful compile makes the preview look frozen while the actual failure disappears.

There are limits. The page exposes a supported API, not the whole JDK. Annotation processing and Java modules are absent; native method declarations are rejected. Some library surfaces exist but remain unsupported at runtime, including the browser regex implementation documented in the guide. BeanShell-only share links can now fail compilation.

The [Java in the browser chapter](/developer-guide/java-in-the-browser/) explains the pipeline and gates. [PR #5938](https://github.com/codenameone/CodenameOne/pull/5938) began this rebuild. Try a small part of your next screen in the [live Playground](/playground/), including a deliberate mistake, and see whether the feedback tells you what you need.

## Discussion

_Which Java construct do you reach for first when checking whether a playground runs the language you use?_

{{< giscus >}}
