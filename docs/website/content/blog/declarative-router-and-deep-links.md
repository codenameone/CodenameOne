---
title: A Declarative Router, Deep Links, And A Bytecode Annotation Framework
slug: declarative-router-and-deep-links
url: /blog/declarative-router-and-deep-links/
date: '2026-06-04'
author: Shai Almog
description: An optional declarative router (@Route("/path"), route guards, redirects, per-tab navigation shell, location listeners) on top of the existing Form infrastructure. Cold and warm deep links unified into a single LinkHandler. iOS Universal Links and Android App Links JSON generators. Plus the bytecode annotation framework the rest of this week's posts build on top of.
feed_html: '<img src="https://www.codenameone.com/blog/declarative-router-and-deep-links.jpg" alt="A Declarative Router, Deep Links, And A Bytecode Annotation Framework" /> Declarative router with @Route("/path"), unified cold/warm deep links, iOS Universal Links and Android App Links JSON generators, and the bytecode annotation framework the rest of this week''s posts build on.'
---

![A Declarative Router, Deep Links, And A Bytecode Annotation Framework](/blog/declarative-router-and-deep-links.jpg)

[PR #5037](https://github.com/codenameone/CodenameOne/pull/5037) is the architectural one in this batch. It lands three things that interact: an optional declarative router for URL-shaped navigation, a unified deep-link API that works for cold launches and warm launches alike, and a reusable bytecode annotation framework in the Maven plugin that several other PRs this week (the ORM, the binder, the JSON mapper) all sit on top of.

That last part is the part I want to start with, because once it is in place the rest is straightforward.

## A bytecode annotation framework, not a source-text one

The Maven plugin now has an `AnnotationProcessor` SPI and two new Mojos: `cn1:generate-annotation-stubs` (in `generate-sources`) and `cn1:process-annotations` (in `process-classes`). The orchestrator ASM-scans `target/classes`, dispatches to every registered processor, and fails the build with a combined error list if anything is off. Adding a new annotation framework later is a matter of dropping a new processor into `META-INF/services`; no orchestrator changes.

The reason this runs against bytecode rather than against source text is that the source-regex prototype was scrapped early. The bytecode pass sees the JVM's view of the project (`extends Form` is a thing the JVM actually knows, not a pattern we have to hope the user wrote a specific way), the rule violations come back with class names and reasons, and the build fails fast before any generated `.class` lands on disk. The infrastructure shares the ASM passes that the `BytecodeComplianceMojo`'s existing String rewrites already use, which is the right shape for this kind of work.

A small stub source is emitted under `target/generated-sources/cn1-annotations/` during `generate-sources` so application code that references the generated registry resolves at compile time. The real `.class` overwrites the stub later in `process-classes`. Standard "compile against a stub, link against the real thing" pattern; it just works inside a single Maven build instead of needing a multi-module split.

The first concrete processor is `RouteAnnotationProcessor`, which is the one the router needs. The ORM, binder, JSON mapper, and XML mapper from [PR #5047](https://github.com/codenameone/CodenameOne/pull/5047) all build on the same SPI; that is the subject of a later post.

## The router

The router itself lives in `com.codename1.router`. The API is opt-in; the existing `Form.show()` / `Form.showBack()` flow keeps working unchanged for apps that do not want a router. For apps that do, the basic pattern is:

```java
@Route("/")
public class HomeForm extends Form { /* ... */ }

@Route("/users/:id")
public class UserDetailForm extends Form {
    public UserDetailForm(RouteMatch match) {
        String id = match.param("id");
        // build UI for user `id`
    }
}

@Route("/about")
public class AboutForm extends Form { /* ... */ }
```

`Router.navigate("/users/42")` resolves the path, instantiates `UserDetailForm`, and shows it. The build-time processor validates that the annotated class extends `Form`, that the path starts with `/`, that the constructor is accessible, that there are no duplicate patterns. Anything off the rails fails the build with a list, not at runtime with a stack trace.

The router also supports the kinds of things that have become table stakes in modern client routing: route guards (run before navigation completes and can cancel or redirect), redirects, per-tab navigation stacks (`TabsForm`, where each tab keeps its own back stack), location listeners (anything in the app can subscribe to "the route changed"), and a `Form.setPopGuard(PopGuard)` analogue to Flutter's `PopScope` (intercept hardware back, toolbar back, or `Router.pop()` with a chance to ask "are you sure?").

`Sheet.showForResult()` returns an `AsyncResource<T>` that auto-cancels with `null` if the user dismisses the sheet. Small thing; it makes "show a picker sheet, wait for the result, react" a four-line piece of code instead of a state-machine.

## Deep links: cold and warm in one place

The deep-link API is the part most apps will end up touching first. `Display.setDeepLinkHandler(LinkHandler)` registers a handler that receives a normalised `DeepLink` (scheme, host, path, segments, query map, fragment). The same handler is invoked for cold launches (the app was not running; the OS started it because of a link) and warm launches (the app was already running and got the URL via app-resume).

```java
Display.getInstance().setDeepLinkHandler(link -> {
    if ("/users".equals(link.path()) && link.segments().size() == 2) {
        Router.navigate("/users/" + link.segments().get(1));
        return true;
    }
    return false;
});
```

The reason both surfaces unify cleanly is that under the hood iOS and Android already deliver these URLs by writing into `Display.setProperty("AppArg", url)`. The new handler intercepts URL-shaped `AppArg` values and routes them through the typed `DeepLink`. iOS and Android need no port changes for this to work; the existing platform plumbing is already correct.

For the link-publishing side, the PR also adds an `AasaBuilder` that emits the iOS `apple-app-site-association` JSON and an `AssetLinksBuilder` that emits the Android `assetlinks.json`. Both are the JSON your website needs to publish at `https://yoursite.com/.well-known/apple-app-site-association` and `https://yoursite.com/.well-known/assetlinks.json` to register your app as the handler for `https://yoursite.com/some/path` URLs. The dev guide walks through the rest of the iOS Universal Links / Android App Links setup, including the entitlement and `intent-filter` work that has to happen on the project side.

## The JavaScript port: window.history

The third piece worth pulling out is that the JavaScript port now bridges into `window.history`. A `JsRouterBootstrap` plus the standalone `cn1-router-history.js` shim mean that navigating the in-app router pushes a real entry into the browser history. Back and forward buttons in the browser drive the router; reloading the page lands at the deep-link URL; sharing a URL out of the address bar takes you to the same in-app location. The Initializr, the Playground, the Skin Designer, and the new Build Cloud console (all four Codename One apps shipping as web tooling, as the [last NFC post called out](/blog/nfc-crypto-biometrics-and-build-cloud/#a-new-build-cloud-ui--preview)) are the obvious beneficiaries. For mobile-first projects that also publish a JS build, "URL is a first-class identifier for an app screen" is now true on all four targets.

## A note on the surefire bump

Hidden away in the diff is a small but boring fix: `surefire` in the plugin POM moves from 2.22.1 to 3.2.5. The reason is that the older version silently skipped every JUnit test in the module under JDK 8 (`Tests run: 0` even though there was an `AppTest` present). The bump matches the version already pinned by the parent reactor POM, and the vintage engine is included so the existing JUnit 4 tests in the module keep working. I am mentioning this because the symptom (a test module that thinks it has zero tests) is the kind of thing you might bump into in your own multi-module build, and `surefire` 3.x is the answer.

## Wrapping up

The router is optional. The deep-link API works whether or not you use the router. The annotation framework is the foundation for the ORM and binder posts later this week, and is open for additional processors via `META-INF/services`. Two new chapters in the dev guide: [Routing-And-Deep-Links.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/Routing-And-Deep-Links.asciidoc) for the reference and [Tutorial-Routing-And-Deep-Links.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/Tutorial-Routing-And-Deep-Links.asciidoc) for an end-to-end walkthrough.

Tomorrow: the new AI / LLM package and the ChatView component.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
