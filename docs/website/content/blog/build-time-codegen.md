---
title: "Routing, ORM, OpenAPI, And Build-Time SVG / Lottie"
slug: build-time-codegen
url: /blog/build-time-codegen/
date: '2026-06-03'
author: Shai Almog
description: A declarative router and a unified deep-link API; a JPA-shaped SQLite ORM; JAXB-shaped JSON / XML mappers; an OpenAPI 3.x client generator that turns a spec into typed Codename One code; SVGs and Lottie animations transcoded to Java Image subclasses at build time. All four pieces sit on the same build-time codegen pipeline; the details of that pipeline are at the end.
feed_html: '<img src="https://www.codenameone.com/blog/build-time-codegen.jpg" alt="Routing, ORM, OpenAPI, And Build-Time SVG / Lottie" /> A declarative router with deep links, a JPA-shaped SQLite ORM, JAXB-shaped JSON / XML mappers, an OpenAPI client generator, and build-time SVG / Lottie transcoders.'
---

![Routing, ORM, OpenAPI, And Build-Time SVG / Lottie](/blog/build-time-codegen.jpg)

This is the third follow-up to [Friday's release post](/blog/metal-default-new-build-cloud-and-a-new-format/). Saturday's was about how you iterate; Monday's was about new platform APIs in the core; today's is about four pieces that change how you write the structural parts of an app.

The four are routing, persistence, network bindings, and graphics. All four use **build-time codegen** under the hood: a Maven-plugin pass that reads annotations or declarative source files at build time and emits typed Java that compiles into your binary. No reflection, no service loader, no `Class.forName`. The "How it works" section at the end of this post is the place to read about the codegen plumbing once you have seen what it powers. The earlier sections focus on the features themselves.

## Deep links and routing

The piece that motivates everything else in this section is deep links. Modern mobile apps need to handle URLs from a wide variety of sources: a notification that wants to land on a specific screen, a marketing email with a link into the app, a "share" sheet that hands the user a URL to a particular item, an associated-domains rule that opens the app when a friend taps `https://yourapp.com/users/42` in Safari. iOS treats these through Universal Links; Android treats them through App Links; the framework collapses both into a single in-app concept.

`Display.setDeepLinkHandler(LinkHandler)` registers a handler that receives a normalised `DeepLink` (scheme, host, path, segments, query map, fragment). The same handler fires for cold launches (the app was not running; the OS started it because of a link) and warm launches (the app was already running and got the URL via app-resume). iOS and Android need no port changes for this to work; the existing platform plumbing already writes URL-shaped values into `Display.setProperty("AppArg", url)` and the new handler intercepts those.

```java
Display.getInstance().setDeepLinkHandler(link -> {
    if ("/users".equals(link.path()) && link.segments().size() == 2) {
        showUserDetailForm(link.segments().get(1));
        return true;
    }
    return false;
});
```

That works, but as the surface grows the if/else chain in the handler becomes a real maintenance burden. Five URL patterns, ten patterns, twenty patterns; the handler grows arms and legs, the routing decisions creep into the form constructors that need to read query parameters, and the "what screens does this app have, and at what paths" question is answered by reading through hundreds of lines of switch statements scattered across files.

The declarative router in `com.codename1.router` is what we built to keep that question tractable. Each form declares its own path with a `@Route` annotation:

```java
@Route("/")
public class HomeForm extends Form { /* ... */ }

@Route("/users/:id")
public class UserDetailForm extends Form {
    public UserDetailForm(RouteMatch match) {
        String userId = match.param("id");
        // build UI for user `userId`
    }
}

@Route("/about")
public class AboutForm extends Form { /* ... */ }
```

`Router.navigate("/users/42")` resolves the path, instantiates `UserDetailForm`, and shows it. The deep-link handler then collapses to a single line: `Display.getInstance().setDeepLinkHandler(link -> Router.navigate(link.path()))`. Each form owns its own routing rule; adding or moving a screen is a one-class change.

**For Spring developers,** the shape is familiar by design. `@Route` plays the same role as Spring MVC's `@RequestMapping`: a class-level declaration that announces "this controller handles URLs of this shape". The `:id` parameter syntax mirrors Spring's `{id}` path-variable syntax; `RouteMatch.param("id")` is the same kind of accessor as Spring's `@PathVariable`. The mental model carries over from server-side Java with almost no friction. The same recognition is available to anyone with React Router, Vue Router, or Angular Router experience; the `:param` convention is the cross-framework default.

The build-time processor validates that each annotated class extends `Form`, that the path starts with `/`, that the constructor is accessible, and that there are no duplicate patterns. Any rule violation fails the build with a class name and a reason, not at runtime with a stack trace.

The rest of the router surface covers the kind of thing that has become table stakes in modern client routing:

- **Route guards** run before navigation completes and can cancel or redirect.
- **Per-tab navigation stacks** via `TabsForm`, where each tab keeps its own back stack.
- **Location listeners** so anything in the app can subscribe to "the route changed".
- **`Form.setPopGuard(PopGuard)`** intercepts hardware back, toolbar back, or `Router.pop()` with a chance to ask "are you sure?".
- **`Sheet.showForResult()`** returns an `AsyncResource<T>` that auto-cancels with `null` if the user dismisses the sheet.

The API is opt-in. Apps that prefer the existing `Form.show()` / `Form.showBack()` flow keep using that; nothing changes.

For the link-publishing side, an `AasaBuilder` emits the iOS `apple-app-site-association` JSON and an `AssetLinksBuilder` emits the Android `assetlinks.json`. The full setup walk-through (entitlements, the Android `intent-filter`, the `.well-known/` upload on your origin server) is at [Routing and Deep Links](https://www.codenameone.com/developer-guide/#_routing_and_deep_links) in the developer guide.

The JavaScript port bridges the router into `window.history` so navigating the in-app router pushes a real entry into the browser's session history. Back and forward in the browser drive the router; reloading the page lands at the deep-link URL; sharing the URL out of the address bar takes a colleague to the same in-app location. The Initializr, the Playground, the Skin Designer, and the new Build Cloud console are all working examples.

## SQLite ORM

The second piece is a SQLite ORM, also driven by annotations. `@Entity` marks the class; `@Id` and `@Column` shape the schema; `@DbTransient` opts a field out:

```java
@Entity
public class TodoItem {
    @Id @Column                  long id;
    @Column                      String title;
    @Column(name = "completed_at")
                                 Date completedAt;
    @DbTransient                 Object cachedView;
}

Dao<TodoItem> dao = EntityManager.open("todos.db").dao(TodoItem.class);
dao.createTable();
dao.insert(new TodoItem(0, "Read the post", null));

List<TodoItem> open = dao.find("completed_at IS NULL", new Object[] {});
TodoItem byId = dao.findById(42);
dao.delete(byId);
```

The generated DAO does the typed work underneath. No reflection in `insert`; the generated code calls `setString(1, e.title)` and `setLong(2, e.id)` directly against the SQLite `PreparedStatement`. Validation at build time catches missing `@Id`, fields that look like relationships but are not yet supported, and abstract entity classes; the build fails with a class name and a reason.

**For JPA / Hibernate developers,** the API is intentionally familiar. `@Entity`, `@Id`, `@Column`, and `@Transient` (here renamed `@DbTransient` to avoid colliding with `java.beans.Transient`) carry the same meaning they do under `javax.persistence` / `jakarta.persistence`. The `EntityManager` name is the same. `Dao#findById`, `Dao#findAll`, `Dao#find(where, params)`, `Dao#insert`, `Dao#update`, `Dao#delete` line up with the basic JPA repository contract. The query language is plain SQL (there is no JPQL or Criteria DSL) but the annotation surface, the lifecycle, and the runtime methods will feel like a long-lost friend to anyone with server-side Java persistence experience.

Three rules the design enforces: no `Class.forName`, no service loader, no field reflection. The "this code only breaks in production because R8 renamed a field" shape of JPA-on-Android bug is structurally absent.

## JSON / XML mapping

`@Mapped` marks a class as a transferable POJO. `@JsonProperty` and `@XmlElement` (plus `@XmlRoot`, `@XmlAttribute`, `@JsonIgnore`, `@XmlTransient`) shape the wire format. The runtime entry points are `Mappers.toJson(...)`, `Mappers.fromJson(...)`, `Mappers.toXml(...)`, `Mappers.fromXml(...)`:

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

The same `@Mapped` POJO is the type the typed `Rest` helpers accept:

```java
Rest.get("https://api.example.com/users/42")
    .fetchAsMapped(User.class)
    .onResult((user, err) -> { /* ... */ });

Rest.get("https://api.example.com/users")
    .fetchAsMappedList(User.class)
    .onResult((users, err) -> { /* ... */ });
```

`Rest.fetchAsJsonList` (top-level JSON arrays, no `{"root":[...]}` envelope trick), `JSONWriter` (the complement of `JSONParser`, with fluent builders and streaming variants for `Writer` and `OutputStream`), and `URLImage.setDefaultBearerToken` (auth headers on image fetches) all ship alongside.

**For JAXB developers,** the XML surface (`@XmlRoot`, `@XmlElement`, `@XmlAttribute`, `@XmlTransient`) is a direct port of the long-established `javax.xml.bind.annotation` surface. The same model class can be both `@XmlRoot`-decorated and `@JsonProperty`-decorated, which gives you a single source of truth for both wire formats. The JSON surface adopts the Jackson convention (`@JsonProperty`, `@JsonIgnore`) that nearly every modern JVM JSON binding (Jackson, Moshi, kotlinx-serialization) inherited.

## OpenAPI client generation

This is the headline of the codegen post and arguably the most useful single feature in this release for any team that talks to a backend.

A new `cn1:generate-openapi-client` Mojo reads an OpenAPI 3.x JSON spec (a URL or a local file) and writes typed Codename One client code that compiles into your app:

- One `@Mapped` POJO per `components.schemas` entry.
- One `<Tag>Api.java` class per OpenAPI tag, with one fluent method per operation.
- Every method routes through `Rest.<verb>` + `Mappers.toJson` + `fetchAsMapped` / `fetchAsMappedList`, so the generated surface integrates with the rest of the framework instead of dragging in a separate HTTP stack.

Wire it into the project's `pom.xml`:

```xml
<plugin>
    <groupId>com.codenameone</groupId>
    <artifactId>codenameone-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>petstore-client</id>
            <goals><goal>generate-openapi-client</goal></goals>
            <configuration>
                <specUrl>https://petstore3.swagger.io/api/v3/openapi.json</specUrl>
                <basePackage>com.example.petstore</basePackage>
            </configuration>
        </execution>
    </executions>
</plugin>
```

`mvn generate-sources` picks the spec up, downloads it, and writes one file per schema and one per tag under `target/generated-sources/`. The Petstore reference spec exercised end-to-end produces six model classes (`Pet`, `Order`, `Customer`, `Tag`, `Category`, `User`) and three Api classes (`PetApi`, `StoreApi`, `UserApi`), and the nine generated `.class` files compile cleanly against `codenameone-core`. Documented at [the OpenAPI codegen Maven goal](https://www.codenameone.com/developer-guide/#_appendix_goal_generate_openapi_client).

In application code you call the generated `Api` class the same way you would call any other Java method:

```java
PetApi pets = new PetApi();

// Returns AsyncResource<Pet>; resolves with the deserialised object.
pets.getPetById(42).onResult((pet, err) -> {
    if (err == null) Log.p("Got " + pet.getName());
});

// Returns AsyncResource<List<Pet>>.
pets.findPetsByStatus("available").onResult((list, err) -> {
    if (err == null) {
        for (Pet p : list) Log.p(p.getName());
    }
});

// POST with a request body. addPet takes a Pet, returns a Pet.
Pet candidate = new Pet();
candidate.setName("Mittens");
candidate.setStatus("available");
pets.addPet(candidate).onResult((created, err) -> { /* ... */ });
```

There is no hand-rolled `ConnectionRequest` setup, no manual JSON parsing, no string-typed request bodies. The generated client takes a typed `Pet`, serialises it with `Mappers.toJson(...)`, fires the right HTTP verb, deserialises the response with `Mappers.fromJson(...)`, and surfaces the result through the framework's `AsyncResource` so your callback fires on the EDT.

For teams who already publish an OpenAPI spec as part of their backend (most modern backend frameworks do this automatically; FastAPI, Spring's `springdoc-openapi`, NestJS, ASP.NET Core, Go's `gnostic`), the practical effect is that the mobile client's bindings stay in sync with the backend without anyone hand-writing a single network call. Update the spec, re-run `mvn generate-sources`, and the new and changed endpoints land in your app as typed Java the IDE picks up immediately.

It is the kind of change that is most useful when you do not know you have it: pull a fresh spec, rebuild, and your IDE highlights every place in the codebase that called a renamed endpoint or passed the wrong type to a parameter.

## Component binding with validation

The fourth annotation processor on the same pipeline is the component binder. `@Bindable` marks a model class; `@Bind(name = "userField")` ties a field to a component on a form by the component's `name`. Field-level validation annotations compose with `@Bind` on the same field:

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

`Binding` is the handle: `refresh()` re-reads the model into the components, `commit()` writes the components back, `disconnect()` tears the listeners down. Multiple validation annotations on a single field compose via `Validator.addConstraint(Component, Constraint...)` and `GroupConstraint` (first failure wins). `@Validate(MyClass.class)` is the escape hatch for hand-written `Constraint` implementations. The validation set: `@Required`, `@Length`, `@Regex`, `@Email`, `@Url`, `@Numeric`, `@ExistIn`, `@Validate`.

The new `BindAttr` enum lets `@Bind` target a specific attribute of the component (`TEXT`, `UIID`, `SELECTED`, ...) when the default ("write a `String` field into the component's text") is not what you want.

## SVG at build time

SVG and Lottie use the same codegen pipeline, but emit different output: instead of reading annotations from your Java, they read declarative graphics files from `src/main/svg/` and `src/main/lottie/` and emit Codename One `Image` subclasses that render through the `Graphics` shape API on every platform.

Drop an SVG into `src/main/svg/`:

```
src/main/svg/
    star.svg
    gradient_circle.svg
    path_arrow.svg
    rounded_button.svg
    wave.svg
    pro_badge.svg
    clipped_badge.svg
```

After the next build:

```java
Image star = Resources.getGlobalResources().getImage("star.svg");
Image star2 = Resources.getGlobalResources().getImage("star"); // either form
form.add(star);
```

A grid of the static SVGs from the hellocodenameone fixture, rendered through the new pipeline:

![Static SVGs rendered by the build-time transcoder on iOS Metal: filled star, gradient-filled circle, path arrow, rounded button, two stroked wave paths, gradient-filled PRO badge, clipped badge](/blog/build-time-codegen/svg-static.png)

The transcoder is a `maven/svg-transcoder/` module that parses SVG with `javax.xml` StAX. No Batik, no Flamingo, no external dependencies. Coverage targets what real-world icon SVGs use: `rect` (rounded corners included), `circle`, `ellipse`, `line`, `polyline`, `polygon`, the full `path` grammar (`M` / `L` / `H` / `V` / `C` / `S` / `Q` / `T` / `A` / `Z` plus relative-coordinate and smooth-curve reflection), groups with affine transforms (`translate`, `scale`, `rotate`, `skew`, `matrix`), linear gradients via `LinearGradientPaint`, fill, stroke, stroke-width, linecap, linejoin, opacity.

SMIL animations are supported in the same pipeline: `<animate>`, `<animateTransform>` (`translate`, `scale`, `rotate`), and `<set>`. Time values interpolate against wall-clock time on every paint, with `from` / `to` / `values` / `begin` / `dur` / `repeatCount` / `fill="freeze"` honoured. So an SVG with a rotating sub-element becomes a real animated `Image` you can drop into a `Form` and watch spin without writing any animation code yourself.

**Important caveat:** this is **Metal-only on iOS**. The GL ES 2 path that was the iOS default until [last Friday's flip](/blog/metal-default-new-build-cloud-and-a-new-format/#metal-is-the-default-on-ios) does not have the shape API coverage the SVG / Lottie pipeline emits. Apps that opted in to Metal pick the transcoders up automatically; apps still on `ios.metal=false` will see placeholders. Now that Metal is the default this stops being a thing most apps notice on their next build.

Coverage and the troubleshooting section are at [SVG Transcoder](https://www.codenameone.com/developer-guide/#_svg_transcoder) in the developer guide. Explicit non-coverage in v1: SVG `text`, masks / clip-paths, filters, radial-gradient paint (falls back to first stop colour), CSS keyframe animations.

## Lottie at build time

The same pipeline carries Lottie. Drop a Bodymovin export into `src/main/lottie/`:

```
src/main/lottie/
    pulse.json
    spinner.json
```

After the next build, both are real `Image` instances on every platform that exposes the shape API:

```java
Image pulse   = Resources.getGlobalResources().getImage("pulse");
Image spinner = Resources.getGlobalResources().getImage("spinner");
form.add(pulse).add(spinner);
```

The animation runs against wall-clock time on every paint, with no `Timer` and no allocation in the hot path. A capture of the hellocodenameone Lottie fixture (one pulsing circle and one rotating bar) at six points in the loop:

![Animated Lottie playback: a red bar that pulses and rotates next to a blue ellipse that scales up and down](/blog/build-time-codegen/lottie-pulse-spinner.gif)

The Lottie transcoder lives in `maven/lottie-transcoder/`. It parses Bodymovin JSON with no external dependencies (the framework's built-in JSON parser carries the load) and lowers each file into the same `SVGDocument` model the SVG path uses. The same `JavaCodeGenerator` emits the same `GeneratedSVGImage` subclass, and the same `SVGRegistry` registers it under the source filename. **No new `Image` base class, no new registry, no per-port wiring**, since the SVG path's JavaSE reflective load and iOS / Android Stub weaving already cover the new format.

Coverage in v1: shape layers (`rc` / `el` / `sh`) with solid fills and strokes; layer transforms (anchor, position, scale, rotation, opacity); animated rotation, position, and scale collapsed to a two-keyframe loop; solid-color layers as filled rects. Most icon-grade Bodymovin exports lower cleanly. Complex character animations from After Effects with image references, masks, and effects do not, and the transcoder logs which layers it dropped so the source of any blank output is obvious.

## How it works: the build-time codegen pipeline

Everything above sits on a single Maven-plugin pass.

The plugin has an `AnnotationProcessor` SPI and two new Mojos: `cn1:generate-annotation-stubs` (in `generate-sources`) and `cn1:process-annotations` (in `process-classes`). The orchestrator ASM-scans `target/classes`, dispatches to every registered processor, validates the annotated classes, and emits a typed runtime artifact next to each one plus a tiny `Index` class that registers everything with a public runtime registry. Adding a new processor later is a matter of dropping it into `META-INF/services` with no orchestrator changes.

The reason this runs against bytecode rather than against source text is that the source-regex prototype was scrapped early. The bytecode pass sees the JVM's view of the project (`extends Form` is a thing the JVM actually knows, not a pattern we have to hope the user wrote a specific way), rule violations come back with class names and reasons, and the build fails fast before any generated `.class` lands on disk. The infrastructure shares the ASM passes that the `BytecodeComplianceMojo`'s existing String rewrites already use.

A small stub source is emitted under `target/generated-sources/cn1-annotations/` during `generate-sources` so application code that references the generated registry resolves at compile time. The real `.class` overwrites the stub later in `process-classes`. Standard "compile against a stub, link against the real thing" pattern; it just works inside a single Maven build instead of needing a multi-module split.

cn1-core ships a no-op stub of each generated index (`RoutesIndex`, `MappersIndex`, `BindersIndex`, `DaosIndex`) so application code compiles even when the project has no annotated classes. The build-time processor shadows each stub with the real implementation before packaging.

Three non-negotiable rules across every processor:

- **No `Class.forName`** anywhere in generated code.
- **No service loader.** Everything is wired through the typed registry the codegen emits.
- **No field reflection.** Every read and write in the generated code is a direct symbol reference that ParparVM rename and R8 obfuscation rewrite together with the class they target.

The SVG and Lottie transcoders sit on a parallel pipeline (declarative graphics files in place of annotations), but follow the same rules and emit code that obeys the same constraints. The practical effect is that the kind of code that historically required reflection at runtime (with all the obfuscation hazards and surprise allocations that come with that) now happens once at build time and produces direct, dead-code-eliminable, rename-safe symbol references. That is the shape Codename One projects are going to look more and more like over the next year.

## Wrapping up

That closes the post series for this release. The next weekly index lands on Friday in the same short format.

Back to the [weekly index](/blog/metal-default-new-build-cloud-and-a-new-format/).

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
