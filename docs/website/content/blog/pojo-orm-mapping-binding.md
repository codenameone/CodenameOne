---
title: A POJO ORM, JSON / XML Mapping, Component Binding, And A Lot More
slug: pojo-orm-mapping-binding
url: /blog/pojo-orm-mapping-binding/
date: '2026-06-06'
author: Shai Almog
description: Three new build-time annotation processors for SQLite ORM (@Entity / @Id / @Column), JSON / XML mapping (@Mapped / @JsonProperty / @XmlElement), and component binding (@Bindable / @Bind) with validation. No Class.forName, no field reflection, ParparVM-rename and R8 safe. Plus the framework groundwork from the Immich port: Java 8 default methods on Map, WebSocket in core, an OpenAPI codegen, a modern arc-spinner pull-to-refresh, and more.
feed_html: '<img src="https://www.codenameone.com/blog/pojo-orm-mapping-binding.jpg" alt="A POJO ORM, JSON / XML Mapping, Component Binding, And A Lot More" /> Build-time annotation processors for SQLite ORM, JSON/XML mapping, and component binding with validation. No reflection, no Class.forName, ParparVM-rename and R8 safe. Plus the Immich-port baseline additions: Map default methods, WebSocket in core, OpenAPI codegen, modern pull-to-refresh, and more.'
---

![A POJO ORM, JSON / XML Mapping, Component Binding, And A Lot More](/blog/pojo-orm-mapping-binding.jpg)

Three PRs in this post. [PR #5047](https://github.com/codenameone/CodenameOne/pull/5047) is the annotation framework half: a SQLite ORM, a JSON / XML mapper, and a component binder, all driven by the bytecode `AnnotationProcessor` SPI from the [router post](/blog/declarative-router-and-deep-links/). [PR #5062](https://github.com/codenameone/CodenameOne/pull/5062) layers validation annotations on top of the binder. And [PR #5055](https://github.com/codenameone/CodenameOne/pull/5055) lands as "Improvements to baseline based on porting exercise"; the porting exercise was real (Immich, the Flutter mobile client, into Codename One), and the PR carries a long list of small framework additions surfaced by that exercise that are individually worth knowing about.

I am going to take the annotation work first and the Immich-port additions second.

## How the annotation framework is shaped

Every processor in this batch sits on the same SPI: the build-time scanner ASM-scans `target/classes`, the processor identifies its annotated classes, validates them, and emits a *typed runtime artifact* next to each one plus a tiny `Index` class that registers everything with a public runtime registry. The runtime artifact is a direct symbol reference to the generated `.class`, so ParparVM rename and R8 obfuscation rewrite the field reads and writes together with the class they target.

The three rules the design enforces are non-negotiable and worth repeating: **no `Class.forName`, no service loader, no field reflection**. Anything that worked in the simulator and broke in production because R8 renamed a class or a field; that whole shape of bug is structurally absent from the generated code.

cn1-core ships a no-op stub of each generated index (`MappersIndex`, `BindersIndex`, `DaosIndex`) so application code compiles even when the project has no annotated classes. The build-time processor shadows the stub with the real implementation under `target/classes` before packaging. The same play as the router from yesterday's post.

## SQLite ORM

`@Entity`, `@Id`, `@Column`, `@DbTransient` for the schema, `EntityManager` and `Dao` for the runtime:

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
TodoItem byId      = dao.findById(42);
```

`Dao#createTable / #insert / #update / #delete / #findById / #findAll / #find(where, params)` are the standard surface; the generated DAO for each entity is the part doing the typed work underneath. No reflection in `insert`; the generated code calls `setString(1, e.title)` and `setLong(2, e.id)` directly.

The processor's validation catches the usual mistakes at build time: missing `@Id`, fields that look like relationships but are not yet supported (`List<SomeOtherEntity>`), abstract entity classes. You get a compile error with a class name, not a runtime stack trace.

## JSON / XML mapping

`@Mapped` is the entry point; `@JsonProperty` and `@XmlElement` (plus `@XmlRoot`, `@XmlAttribute`, `@JsonIgnore`, `@XmlTransient`) shape the wire format:

```java
@Mapped
public class User {
    @JsonProperty("user_id") long   id;
    @JsonProperty           String  name;
    @JsonProperty("created_at")
                             Date   createdAt;
    @JsonIgnore              String passwordHash;
}

String json = Mappers.toJson(user);
User   back = Mappers.fromJson(json, User.class);
```

The XML side works the same way; `@XmlRoot` names the document element, `@XmlElement` and `@XmlAttribute` decorate the fields. Both directions are symmetrical and round-trip-safe.

The same `@Mapped` POJO is the type the `Rest` helpers from PR #5055 will accept:

```java
Rest.get(url)
    .fetchAsMapped(User.class)
    .onResult((user, err) -> { /* ... */ });

Rest.get(url)
    .fetchAsMappedList(User.class)
    .onResult((users, err) -> { /* ... */ });
```

That last surface is the one that closes a long-standing rough edge: top-level JSON arrays no longer need the historical `{"root":[...]}` envelope trick.

## Component binding (with validation)

`@Bindable` marks a model class; `@Bind(name = "userField")` ties a field to a component on the form by its name (the `name` your form code sets on the component, not a CSS selector). The build-time binder reads the field types and the components, and generates the bidirectional wiring at compile time:

```java
@Bindable
public class SignupModel {
    @Bind(name = "userField")  @Required @Length(min = 3)
    private String user;

    @Bind(name = "emailField") @Required @Email
    private String email;

    @Bind(name = "ageField")   @Numeric(min = 13, max = 120)
    private String age;

    @Bind(name = "roleField")  @ExistIn({ "admin", "editor", "viewer" })
    private String role;
}
```

The runtime side is two lines:

```java
Binding b = Binders.bind(model, form);
b.getValidator().addSubmitButtons(submitBtn);
```

`Binding` is the handle: `refresh()` re-reads the model into the components, `commit()` writes the components back into the model, `disconnect()` tears the listeners down. Multiple validation annotations on a single field compose via the existing `Validator.addConstraint(Component, Constraint...)` varargs overload (`GroupConstraint`, first failure wins). `@Validate(MyClass.class)` is the escape hatch for hand-written `Constraint` implementations.

The new `BindAttr` enum lets `@Bind` target a specific attribute of the component (`TEXT`, `UIID`, `SELECTED`, ...) when the default ("write a `String` field into the component's text") is not what you want. The annotation framework reads it at build time and generates the matching `Component#setUiid(...)` / `Component#setSelected(...)` call.

The validation set from [PR #5062](https://github.com/codenameone/CodenameOne/pull/5062): `@Required`, `@Length`, `@Regex`, `@Email`, `@Url`, `@Numeric`, `@ExistIn`, `@Validate`. The build-time binder constructs the matching `com.codename1.ui.validation.Constraint` instances at `bind()` time and hands you a populated `Validator` through `Binding#getValidator()` (which is never null).

Three new dev-guide chapters for these: [Annotation-JSON-XML-Mapping.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/Annotation-JSON-XML-Mapping.asciidoc), [Annotation-Component-Binding.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/Annotation-Component-Binding.asciidoc), and [Annotation-SQLite-ORM.asciidoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/Annotation-SQLite-ORM.asciidoc).

## The Immich-port additions

[PR #5055](https://github.com/codenameone/CodenameOne/pull/5055)'s headline is dry; the contents are not. Each entry was driven by a concrete pain point in the port of the Immich Flutter mobile client to Codename One, and the port is still compiling cleanly through every change in that PR. The additions worth pulling out:

### Java subset

Eleven Java 8 default methods on `Map` (`getOrDefault`, `putIfAbsent`, `remove(K, V)`, `replace(K, V)` / `replace(K, V, V)`, `forEach`, `replaceAll`, `computeIfAbsent`, `computeIfPresent`, `compute`, `merge`); `BiFunction`; `Iterable.forEach(Consumer)`, `Collection.removeIf(Predicate)`, `List.replaceAll(UnaryOperator)`, `List.sort(Comparator)`. All four primitive atomics: `AtomicReference`, `AtomicInteger`, `AtomicLong`, `AtomicBoolean`. Standard Java 8 surface that was simply missing from the subset; every one of these is a paper cut that you previously had to work around in app code.

### Core framework

`Rest.fetchAsJsonList`, `Rest.fetchAsMapped(Class<T>)`, `Rest.fetchAsMappedList(Class<T>)` are the ones I mentioned above in the mapping section. `Component.setPullToRefresh(Runnable)` is an alias for `addPullToRefresh` for consistency.

`URLImage.RequestDecorator` plus `setDefaultRequestDecorator`, `setDefaultBearerToken`, and a per-call decorator overload on `createToStorage` close the "URLImage does not pass auth headers" workaround. Any app that hits authenticated image endpoints now sets a bearer token once and the image cache and the `URLImage` machinery use it everywhere.

`JSONWriter` is the complement of `JSONParser`. `JSONWriter.toJson(Object)` for one-shot, fluent `JSONWriter.object().put(...).toJson()` and `JSONWriter.array()` builders, and streaming variants for `Writer` and `OutputStream`. The standard library piece that should have been there years ago.

`Tabs.setAnimatedIndicator(boolean)` enables a Material 3 / iOS 26 sliding-underline indicator (gated by `tabsAnimatedIndicatorBool` plus duration / thickness constants and a new `TabIndicator` UIID). Off in the framework defaults; **on by default in the modern native themes**.

`MorphTransition.snapshotMode(boolean)` is the fix for an edge case that has been around forever: a morph from a source inside a scrolling container would leak off-viewport because the source was repainted live. The new opt-in path captures source and destination as clipped `Image` snapshots at `initTransition()` and tweens those, which solves the leak. The default live-paint path is unchanged.

`DefaultLookAndFeel.drawModernPullToRefresh` is a Material 3 arc-spinner painted via `Graphics.drawArc`; the sweep grows from 0° to 330° as the user pulls and then spins while the task runs. Gated by `pullToRefreshModernBool`; on by default in the modern themes.

`com.codename1.io.websocket` moves into the core. The Java API was previously in the `cn1-websockets` cn1lib; the API moves into core so apps do not need the cn1lib for the Java side. Per-platform native impls remain in the cn1lib for now and will follow.

### Maven plugin: OpenAPI codegen

The Maven plugin gains `cn1:generate-openapi-client`, which reads an OpenAPI 3.x JSON spec (a URL or a local file) and emits one `@Mapped` POJO per `components.schemas` entry plus one `<Tag>Api.java` per tag. The generated methods route through `Rest.<verb>`, `Mappers.toJson`, and `fetchAsMapped` / `fetchAsMappedList`. The Petstore reference spec runs end-to-end (6 models plus 3 Api classes, 9 `.class` files that compile cleanly against `codenameone-core`).

The dev-guide page for the goal is at [appendix_goal_generate_openapi_client.adoc](https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/appendix_goal_generate_openapi_client.adoc) and the Maven Appendix index links it.

### Tiny but useful

`StubGenerator.isValidType` now accepts `String[]` (the rest of the primitive arrays were already accepted). The kind of fix that quietly removes a one-line workaround from a handful of native interface declarations.

## Why a single PR for the baseline additions

A natural question on [#5055](https://github.com/codenameone/CodenameOne/pull/5055) is "why is all of that in one PR rather than ten". The honest answer is that the porting exercise was the regression fixture for every single one of those additions, and breaking it up would have meant maintaining a long-lived branch where each split-out item had to be re-verified against the port independently. Bundling them under a single fixture that we know still ports cleanly is the cheaper path to "no regressions". When something is structurally separable (the WebSocket move, the OpenAPI codegen, the modern pull-to-refresh) we kept it in the same PR rather than fan out and ask reviewers to track six branches in parallel.

## Wrapping up

Three annotation processors. One validation processor. A long list of framework groundwork. The combined effect is that the kind of "I have a backend, I want to display its data, I want to bind a form to it, I want to validate the form, I want to cache the result in SQLite" loop that takes the most boilerplate in mobile development is now several lines shorter on every step.

Tomorrow's post is the last in this batch: build-time SVG and Lottie support.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
