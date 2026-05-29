---
title: Build-Time Codegen: Router, ORM, Mappers, Binder, SVG / Lottie
slug: build-time-codegen
url: /blog/build-time-codegen/
date: '2026-06-03'
author: Shai Almog
description: A reusable bytecode AnnotationProcessor SPI in the Maven plugin, the declarative router that is its first consumer, a SQLite ORM, JSON / XML mappers, a component binder with validation, the baseline additions surfaced by porting a substantial mobile client app onto Codename One, and a build-time SVG / Lottie transcoder that emits Codename One Image subclasses for every asset.
feed_html: '<img src="https://www.codenameone.com/blog/build-time-codegen.jpg" alt="Build-Time Codegen: Router, ORM, Mappers, Binder, SVG / Lottie" /> A reusable bytecode AnnotationProcessor SPI, the declarative router that is its first consumer, ORM + mappers + binder + validation, the porting-exercise baseline additions, and a build-time SVG / Lottie transcoder.'
---

![Build-Time Codegen: Router, ORM, Mappers, Binder, SVG / Lottie](/blog/build-time-codegen.jpg)

This is the architectural post for the week. The Saturday post was about how you iterate; Monday's post was about new platform APIs; today's post is about a shape of code that several PRs in this release share, that explains why a lot of the new APIs work the way they do, and that should shape how Codename One projects look over the next few years.

The shape is **build-time codegen**. A reusable bytecode `AnnotationProcessor` SPI in the Maven plugin, the declarative router that is its first concrete consumer, then a SQLite ORM, JSON / XML mappers, and a component binder (all built on the same SPI), plus the build-time SVG / Lottie transcoders that ship in the same release for related reasons. The grab-bag PR from a recent porting exercise (a substantial mobile client app ported onto Codename One) lives here too because the ORM and mapping work share the porting exercise that drove it.

Six PRs make up this post: [#5037](https://github.com/codenameone/CodenameOne/pull/5037) (router + annotation SPI), [#5047](https://github.com/codenameone/CodenameOne/pull/5047) (ORM + mappers + binder), [#5062](https://github.com/codenameone/CodenameOne/pull/5062) (validation), [#5055](https://github.com/codenameone/CodenameOne/pull/5055) (porting-exercise baseline additions), [#5042](https://github.com/codenameone/CodenameOne/pull/5042) and [#5066](https://github.com/codenameone/CodenameOne/pull/5066) (SVG / Lottie), plus [#5049](https://github.com/codenameone/CodenameOne/pull/5049) (Metal / Android rendering fixes that fell out of the SVG screenshot tests).

## Bytecode codegen, not source-text codegen

The Maven plugin now has an `AnnotationProcessor` SPI and two new Mojos: `cn1:generate-annotation-stubs` (in `generate-sources`) and `cn1:process-annotations` (in `process-classes`). The orchestrator ASM-scans `target/classes`, dispatches to every registered processor, validates the annotated classes, and emits a typed runtime artifact next to each one plus a tiny `Index` class that registers everything with a public runtime registry. Adding a new processor later is a matter of dropping it into `META-INF/services` with no orchestrator changes.

The reason this runs against bytecode rather than against source text is that the source-regex prototype was scrapped early. The bytecode pass sees the JVM's view of the project (`extends Form` is a thing the JVM actually knows, not a pattern we have to hope the user wrote a specific way), rule violations come back with class names and reasons, and the build fails fast before any generated `.class` lands on disk. The infrastructure shares the ASM passes that the `BytecodeComplianceMojo`'s existing String rewrites already use.

A small stub source is emitted under `target/generated-sources/cn1-annotations/` during `generate-sources` so application code that references the generated registry resolves at compile time. The real `.class` overwrites the stub later in `process-classes`. Standard "compile against a stub, link against the real thing" pattern; it just works inside a single Maven build instead of needing a multi-module split.

Three non-negotiable rules across every processor in this batch: **no `Class.forName`, no service loader, no field reflection**. Every read and write in the generated code is a direct symbol reference that ParparVM rename and R8 obfuscation rewrite together with the class they target. Anything that worked in the simulator and broke in production because R8 renamed a class or a field; that whole shape of bug is structurally absent.

cn1-core ships a no-op stub of each generated index (`RoutesIndex`, `MappersIndex`, `BindersIndex`, `DaosIndex`) so application code compiles even when the project has no annotated classes. The build-time processor shadows each stub with the real implementation before packaging.

## The router

The first concrete consumer is the declarative router in `com.codename1.router`. The API is opt-in; the existing `Form.show()` / `Form.showBack()` flow keeps working unchanged.

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
```

`Router.navigate("/users/42")` resolves the path, instantiates `UserDetailForm`, and shows it. The build-time processor validates that the annotated class extends `Form`, that the path starts with `/`, that the constructor is accessible, that there are no duplicate patterns. Anything off the rails fails the build with a class name and a reason, not at runtime with a stack trace.

The rest of the router surface is the kind of thing that has become table stakes in modern client routing: route guards (run before navigation completes; can cancel or redirect), redirects, per-tab navigation stacks (`TabsForm`, where each tab keeps its own back stack), location listeners (anything in the app can subscribe to "the route changed"), and a `Form.setPopGuard(PopGuard)` hook to intercept hardware back, toolbar back, or `Router.pop()` with a chance to ask "are you sure?". `Sheet.showForResult()` returns an `AsyncResource<T>` that auto-cancels with `null` if the user dismisses the sheet.

### Deep links

`Display.setDeepLinkHandler(LinkHandler)` registers a handler that receives a normalised `DeepLink` (scheme, host, path, segments, query, fragment). The same handler is invoked for cold launches (the app was not running; the OS started it because of a link) and warm launches (the app was already running and got the URL via app-resume). iOS and Android need no port changes for this to work; the existing platform plumbing already writes URL-shaped values into `Display.setProperty("AppArg", url)` and the new handler intercepts those.

For the link-publishing side, an `AasaBuilder` emits the iOS `apple-app-site-association` JSON, and an `AssetLinksBuilder` emits the Android `assetlinks.json`. The full setup walk-through (entitlements, `intent-filter`, the `.well-known/` upload) is at [Routing-And-Deep-Links.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/Routing-And-Deep-Links.asciidoc), with an end-to-end tutorial at [Tutorial-Routing-And-Deep-Links.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/Tutorial-Routing-And-Deep-Links.asciidoc).

The JavaScript port bridges the router into `window.history` so navigating the in-app router pushes a real entry into the browser history. Back and forward buttons in the browser drive the router; reloading the page lands at the deep-link URL. The Initializr, the Playground, the Skin Designer, and the new Build Cloud console all benefit.

## ORM, mappers, and binder (with validation)

[PR #5047](https://github.com/codenameone/CodenameOne/pull/5047) lands three more processors on the same SPI. [PR #5062](https://github.com/codenameone/CodenameOne/pull/5062) layers validation on the binder.

### SQLite ORM

`@Entity`, `@Id`, `@Column`, `@DbTransient` for the schema; `EntityManager` and `Dao` for the runtime:

```java
@Entity
public class TodoItem {
    @Id          @Column                          long id;
    @Column                                       String title;
    @Column(name = "completed_at")                Date completedAt;
    @DbTransient                                  Object cachedView;
}

Dao<TodoItem> dao = EntityManager.open("todos.db").dao(TodoItem.class);
dao.createTable();
dao.insert(new TodoItem(0, "Read the post", null));
List<TodoItem> open = dao.find("completed_at IS NULL", new Object[] {});
```

The generated DAO does the typed work underneath. No reflection in `insert`; the generated code calls `setString(1, e.title)` and `setLong(2, e.id)` directly. Validation at build time catches missing `@Id`, fields that look like relationships but are not yet supported, abstract entity classes.

### JSON / XML mapping

`@Mapped` is the entry point; `@JsonProperty` and `@XmlElement` (plus `@XmlRoot`, `@XmlAttribute`, `@JsonIgnore`, `@XmlTransient`) shape the wire format:

```java
@Mapped
public class User {
    @JsonProperty("user_id") long   id;
    @JsonProperty            String name;
    @JsonProperty("created_at")
                             Date   createdAt;
    @JsonIgnore              String passwordHash;
}

String json = Mappers.toJson(user);
User   back = Mappers.fromJson(json, User.class);
```

The same `@Mapped` POJO is the type the `Rest` helpers from PR #5055 will accept (`Rest.get(url).fetchAsMapped(User.class)`, `fetchAsMappedList(User.class)`).

### Component binding with validation

`@Bindable` marks a model class; `@Bind(name = "userField")` ties a field to a component on the form by its name. The build-time binder reads the field types and the components, generates the bidirectional wiring at compile time:

```java
@Bindable
public class SignupModel {
    @Bind(name = "userField")  @Required @Length(min = 3)        private String user;
    @Bind(name = "emailField") @Required @Email                  private String email;
    @Bind(name = "ageField")   @Numeric(min = 13, max = 120)     private String age;
    @Bind(name = "roleField")  @ExistIn({ "admin", "editor",
                                          "viewer" })             private String role;
}

Binding b = Binders.bind(model, form);
b.getValidator().addSubmitButtons(submitBtn);
```

`Binding` is the handle: `refresh()` re-reads the model into the components, `commit()` writes the components back, `disconnect()` tears the listeners down. Multiple validation annotations on a single field compose via the existing `Validator.addConstraint(Component, Constraint...)` varargs (`GroupConstraint`, first failure wins). `@Validate(MyClass.class)` is the escape hatch for hand-written `Constraint` implementations. The validation set: `@Required`, `@Length`, `@Regex`, `@Email`, `@Url`, `@Numeric`, `@ExistIn`, `@Validate`.

The new `BindAttr` enum lets `@Bind` target a specific attribute of the component (`TEXT`, `UIID`, `SELECTED`, ...) when the default is not what you want. The annotation framework reads it at build time and generates the matching `Component#setUiid(...)` / `Component#setSelected(...)` call.

Three new dev-guide chapters: [Annotation-JSON-XML-Mapping.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/Annotation-JSON-XML-Mapping.asciidoc), [Annotation-Component-Binding.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/Annotation-Component-Binding.asciidoc), and [Annotation-SQLite-ORM.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/Annotation-SQLite-ORM.asciidoc).

## The porting-exercise baseline

[PR #5055](https://github.com/codenameone/CodenameOne/pull/5055) ships as "Improvements to baseline based on porting exercise". The porting exercise was real: a substantial third-party mobile client onto Codename One. The port is still compiling cleanly through every change in that PR; the additions worth pulling out:

**Java subset additions.** Eleven Java 8 default methods on `Map` (`getOrDefault`, `putIfAbsent`, `remove(K, V)`, `replace(K, V)` / `replace(K, V, V)`, `forEach`, `replaceAll`, `computeIfAbsent`, `computeIfPresent`, `compute`, `merge`); `BiFunction`; `Iterable.forEach(Consumer)`, `Collection.removeIf(Predicate)`, `List.replaceAll(UnaryOperator)`, `List.sort(Comparator)`. All four primitive atomics: `AtomicReference`, `AtomicInteger`, `AtomicLong`, `AtomicBoolean`. Standard Java 8 surface that was simply missing.

**`Rest` typed helpers.** `Rest.fetchAsJsonList`, `Rest.fetchAsMapped(Class<T>)`, `Rest.fetchAsMappedList(Class<T>)` (these are the surface the mapper post above mentioned). `Rest.fetchAsJsonList` closes a long-standing rough edge: top-level JSON arrays no longer need the historical `{"root":[...]}` envelope trick.

**`URLImage.RequestDecorator`** plus `setDefaultRequestDecorator`, `setDefaultBearerToken`, and a per-call decorator overload on `createToStorage`. Authenticated image endpoints no longer require working around the "URLImage does not pass headers" gap; set a bearer token once and the image cache and the `URLImage` machinery use it everywhere.

**`JSONWriter`** is the complement of `JSONParser`. `JSONWriter.toJson(Object)` for one-shot, fluent `JSONWriter.object().put(...).toJson()` and `JSONWriter.array()` builders, streaming variants for `Writer` and `OutputStream`.

**`Tabs.setAnimatedIndicator(boolean)`** enables a Material 3 / iOS 26 sliding-underline indicator (gated by `tabsAnimatedIndicatorBool` plus duration / thickness constants and a new `TabIndicator` UIID). Off in the framework defaults; **on by default in the modern native themes** that we landed two weeks ago.

**`DefaultLookAndFeel.drawModernPullToRefresh`** is a Material 3 arc-spinner painted via `Graphics.drawArc`; sweep grows 0° to 330° as the user pulls, then spins while the task runs. Gated by `pullToRefreshModernBool`; on by default in the modern themes.

**`MorphTransition.snapshotMode(boolean)`** is the fix for an edge case that has been around forever: a morph from a source inside a scrolling container leaked off-viewport because the source was repainted live. The opt-in path captures source and destination as clipped `Image` snapshots at `initTransition()` and tweens those. Default live-paint path unchanged.

**`com.codename1.io.websocket`** moves into the core. Per-platform native impls remain in the cn1lib for now.

**`cn1:generate-openapi-client`** mojo reads an OpenAPI 3.x JSON spec and emits one `@Mapped` POJO per `components.schemas` entry plus one `<Tag>Api.java` per tag. The Petstore reference spec runs end-to-end. Dev-guide page: [appendix_goal_generate_openapi_client.adoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/appendix_goal_generate_openapi_client.adoc).

A natural question on this PR is "why is all of that in one PR rather than ten". The honest answer is that the porting exercise was the regression fixture for every single one of those additions, and breaking it up would have meant maintaining a long-lived branch where each split-out item had to be re-verified against the port independently.

## SVG and Lottie at build time

The last two PRs in this batch sit on the same "emit Java from declarative input at build time" pattern. [PR #5042](https://github.com/codenameone/CodenameOne/pull/5042) is the SVG transcoder; [PR #5066](https://github.com/codenameone/CodenameOne/pull/5066) is the Lottie / Bodymovin transcoder that reuses the SVG pipeline; [PR #5049](https://github.com/codenameone/CodenameOne/pull/5049) is the small set of iOS Metal and Android rendering fixes the SVG screenshot tests exposed. They share one chapter at [SVG-Transcoder.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/SVG-Transcoder.asciidoc).

**Important caveat before the details:** this is **Metal-only on iOS**. The GL ES 2 path that was the iOS default until [last Friday's flip](/blog/metal-default-new-build-cloud-and-a-new-format/#metal-is-the-default-on-ios) does not have the shape API coverage the SVG / Lottie pipeline emits. Apps that opted in to Metal pick up the transcoders automatically; apps still on `ios.metal=false` will see placeholders. Now that Metal is the default this stops being a thing most apps notice on their next build.

The shape:

```
src/main/svg/
    home.svg
    settings.svg
    profile.svg
src/main/lottie/
    spinner.json
    pulse.json
```

After the next build:

```java
Image home    = Resources.getGlobalResources().getImage("home");
Image spinner = Resources.getGlobalResources().getImage("spinner");
form.add(home).add(spinner);
```

The SVG transcoder is a `maven/svg-transcoder/` module that parses SVG with `javax.xml` StAX (no Batik, no Flamingo, no external deps) and emits a Codename One `Image` subclass rendering through the `Graphics` shape API. SVG coverage covers what real-world icon SVGs use: rect (rounded corners), circle, ellipse, line, polyline, polygon, the full `path` grammar (M / L / H / V / C / S / Q / T / A / Z plus relative-coordinate and smooth-curve reflection), groups with affine transforms, linear gradients, fill, stroke, opacity. SMIL animations are supported: `<animate>`, `<animateTransform>` (translate / scale / rotate), `<set>`. Time values interpolate against wall-clock time on every paint.

The Lottie pipeline reuses everything: each Bodymovin file is parsed into the same `SVGDocument` model the SVG path uses, the same `JavaCodeGenerator` emits the same `GeneratedSVGImage` subclass, the same `SVGRegistry` registers it. No new `Image` base class, no per-port wiring. v1 covers shape layers (`rc` / `el` / `sh`) with solid fills and strokes, layer transforms (anchor / position / scale / rotation / opacity), animated rotation / position / scale collapsed to a 2-keyframe loop, solid-color layers as filled rects.

Why Java at build time rather than parse SVG at runtime: parsing requires `javax.xml`, which is JVM-only by design; the generated code is allocation-light and deterministic (a path's commands become inlined `g.fillShape(new GeneralPath()...)` calls; an animation becomes a `currentTransform.translate(...)` against a wall-clock variable); and R8 / ParparVM rename and dead-code eliminate the generated code as freely as any other class, so SVGs you do not actually `getImage(...)` get dropped from the final binary.

### The Metal / Android rendering fixes

The SVG screenshot tests exercised the shape API harder than anything we had thrown at it before, and three rendering bugs surfaced; the fixes in [PR #5049](https://github.com/codenameone/CodenameOne/pull/5049) affect any code path that uses `setClip(GeneralPath)`, gradient paint, or text under a transform, not just the SVG pipeline:

1. **iOS Metal `setClip(GeneralPath)` triangle.** Metal's stencil clip's triangle fan was treating every Bezier control point as a polygon vertex. Non-rect `ClipShape`s are now midpoint-flattened into a polyline before reaching native.
2. **iOS Metal `drawString` skips the affine scale.** Text under a viewBox scale was rasterised at `font.pointSize` and stretched on the GPU. `CN1MetalDrawString` now reads the effective scale from `currentTransform`, picks an atlas font at `pointSize * scale`, and divides glyph metrics back into caller-side coords.
3. **Android and iOS Metal `gradient_circle.svg` double-circle.** `LinearGradientPaint.paint` was baking `getTranslateX/Y()` into a translate that sat before the SVG scale, sending the cell offset through the scale twice. The "translate dance" is dropped.

If your app uses `setClip(GeneralPath)` or paints text under a non-uniform transform anywhere, you pick these fixes up on next rebuild.

## What ties this together

The thread across all six PRs is the same pattern: **emit Java at build time, validate at build time, fail fast with a class name and a reason, R8 / ParparVM rename the generated code together with the rest of the app**. The router uses it to register `@Route` classes. The ORM uses it to generate typed DAOs. The mappers use it to generate typed JSON / XML readers and writers. The binder uses it to wire fields to components. The SVG and Lottie transcoders use it to turn declarative graphics into Java classes that render through the shape API.

The practical effect is that the kind of code that historically required reflection at runtime (with all the obfuscation hazards and surprise allocations that come with that) now happens once at build time and produces direct, dead-code-eliminable, rename-safe symbol references. That is the shape Codename One projects are going to look more and more like over the next year.

## Wrapping up

That closes out the post series for this release cycle. The May 29 weekly index is [here](/blog/metal-default-new-build-cloud-and-a-new-format/); the next weekly index lands on Friday in the same short format.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
