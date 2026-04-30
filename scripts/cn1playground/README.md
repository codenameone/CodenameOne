# Playground

The Playground is an interactive scripting environment that lets developers write, test, and iterate on Codename One UI code using a custom [BeanShell](https://github.com/beanshell/beanshell) interpreter. It provides instant feedback for UI development without requiring full app rebuilds.

## Features

### Script Execution

- **Loose scripts**: Write imperative code that returns a Component, and the playground renders it immediately:
  ```java
  Container root = new Container(BoxLayout.y());
  Button btn = new Button("Click me");
  btn.addActionListener(e -> Dialog.show("Hello", "World", "OK", null));
  root.add(btn);
  root;  // Return the component
  ```

- **Lifecycle scripts**: Define a class with `init(Object)` and `start()` methods for more structured applications:
  ```java
  public class MyApp {
      private Label status;
      
      public void init(Object context) {}
      
      public void start() {
          Form form = new Form("My App", BoxLayout.y());
          status = new Label("Ready");
          Button btn = new Button("Tap");
          btn.addActionListener(e -> status.setText("Tapped"));
          form.addAll(status, btn);
          form.show();
      }
  }
  ```

- **`build(PlaygroundContext)` contract**: Return a Component from a method named `build`:
  ```java
  Component build(PlaygroundContext ctx) {
      Container root = new Container(BoxLayout.y());
      ctx.log("Building UI");
      root.add(new Label("Hello"));
      return root;
  }
  ```

### Lambda Support

The playground supports Java 8 lambda syntax for functional interfaces:

```java
button.addActionListener(e -> Dialog.show("Clicked", "Button pressed", "OK", null));
button.addActionListener(() -> doSomething());
```

#### How Lambdas Are Transformed

BeanShell does not natively support Java 8 lambda expressions. The playground transforms lambda syntax into SAM (Single Abstract Method) adapters at script evaluation time:

1. **Lambda-to-SAM conversion**: A lambda like `e -> doSomething(e)` is converted to a call like `__lambdaSupport.lambda(new String[]{"e"}, "doSomething(e)")`. The `PlaygroundLambdaBridge` evaluates the body in a sandboxed interpreter with access to the enclosing scope's variables.

2. **Scope capture**: Variables from the enclosing scope are captured by running the lambda body through the same BeanShell interpreter that has access to those variables. A `CURRENT_NAMESPACE` thread-local ensures the correct call stack is used.

3. **Known SAM type rewriting**: For commonly used listener interfaces (`ActionListener`, `NetworkListener`, `Runnable`, `OnComplete`), the playground rewrites calls like `button.addActionListener(evt -> ...)` to use factory methods on `PlaygroundListenerBridge` that create properly typed adapter objects.

4. **Nested lambda handling**: Inner lambdas are rewritten before outer lambdas execute, ensuring correct evaluation order.

5. **Anonymous inner class conversion**: Anonymous inner classes with a single method are detected and converted to the same lambda-to-SAM pattern:
   ```java
   // This:
   button.addActionListener(new ActionListener() {
       public void actionPerformed(ActionEvent evt) { doSomething(); }
   });
   // Becomes:
   button.addActionListener(__listenerSupport.actionListener(__lambdaSupport.lambda(new String[]{"evt"}, "doSomething();")));
   ```

#### Performance Considerations

Lambda bodies are evaluated lazily by the BeanShell interpreter, which means:
- Each lambda invocation incurs interpreter overhead
- Complex expressions in lambda bodies should be extracted to named methods when possible
- The first invocation of a lambda may be slower due to expression parsing

#### Lambda Caveats

The lambda transformation has some edge cases that may not work as expected:

**Variables referenced after modification**:
```java
// May fail to capture updated value:
int counter = 0;
button.addActionListener(e -> {
    counter++;  // Modifies captured variable
    System.out.println(counter);
});
```

**Lambdas passed to unrecognized SAM methods**:
```java
// Supported - recognized SAM type:
button.addActionListener(e -> { });  // ActionListener is recognized

// May not work - unrecognized SAM type:
someObject.customCallback(x -> { });  // Not pre-wired, might fail
```
For unrecognized SAM types, use anonymous inner class syntax instead.

**Break/continue in lambda bodies**:
```java
// Not supported - break/continue don't work in lambda bodies:
button.addActionListener(e -> {
    for (int i = 0; i < 10; i++) {
        if (i == 5) break;  // Break may not work correctly
    }
});
```

### Anonymous Inner Class Support

Traditional anonymous inner classes are also supported:

```java
button.addActionListener(new ActionListener() {
    public void actionPerformed(ActionEvent evt) {
        Dialog.show("Clicked", "Button pressed", "OK", null);
    }
});
```

### REST APIs

The playground includes pre-configured access to Codename One's REST APIs:

```java
RequestBuilder builder = Rest.get("https://example.com/api/data");
builder.fetchAsString(response -> {
    String text = response.getResponseData();
    // Process the response
});
```

### Available Imports

The following packages are pre-imported:
- `com.codename1.ui.*`
- `com.codename1.ui.layouts.*`
- `com.codename1.components.*`
- `com.codename1.ui.geom.*`

Additional imports can be declared in scripts as needed.

### Shareable Links

The playground can generate shareable URLs that contain both the Java script source and CSS editor content. Use the **"Copy Shareable Playground URL"** option in the side menu to copy a link to the clipboard.

**URL Format**:
- Java source is stored in the `code` query parameter.
- CSS source is stored in the `css` query parameter.
- Both values use URL-safe Base64 encoding.

```
https://example.com/playground?code=<base64-encoded-script>&css=<base64-encoded-css>
```

If the CSS editor is empty, the `css` parameter is omitted.

The encoding uses URL-safe Base64 (replacing `+` with `-` and `/` with `_`, with padding removed).

**Sample Links**: You can also link to built-in samples using the `sample` query parameter:
```
https://example.com/playground?sample=<sample-slug>
```

### Inspector Tab

The playground includes an **Inspector** tab that displays the component hierarchy of your running UI:

- **Component Tree**: Shows the hierarchical structure of all components in the preview
- **Component Selection**: Click any node in the tree to see its details
- **Visual Highlighting**: Selected components are highlighted in the preview using a translucent overlay
- **Property Editor**: View and edit common properties:
  - **Type**: The component class name (read-only)
  - **UIID**: The UIID/styling identifier
  - **Text**: The text content (for Label, Button, TextField, TextArea components)
  - **Position**: X and Y coordinates
  - **Size**: Width and Height

Changes made in the property editor are immediately reflected in the preview. The component tree updates automatically when your script re-runs.

## JavaScript Port Tracking

The current `javascript` module still represents the legacy JavaScript build
path. While the new ParparVM-backed JavaScript port is being integrated, you
can compare bundle size against a ParparVM artifact with:

```bash
PLAYGROUND_PARPARVM_BUNDLE=/path/to/parparvm/dist ./build.sh javascript_compare
```

This uses
[`compare-javascript-bundles.sh`](/Users/shai/dev/cn1/scripts/cn1playground/tools/compare-javascript-bundles.sh)
to report total and JavaScript payload sizes. The long-term goal is to replace
the legacy `javascript` module build itself with the ParparVM-backed port once
the runtime and browser harness are complete.

## BeanShell Interpreter Tradeoffs

The playground uses a customized version of [BeanShell](https://github.com/beanshell/beanshell) with several Codename One-specific adaptations.

### What Works

- **Method dispatch**: Calls to Codename One APIs work transparently
- **Constructor invocation**: `new Container()`, `new Button("text")`, etc.
- **Static methods**: `BoxLayout.y()`, `Display.getInstance()`, etc.
- **Static fields**: `Style.UNIT_TYPE_DIPS`, etc.
- **Lambda expressions**: Converted to SAM (Single Abstract Method) adapters at runtime
- **Anonymous inner classes**: Converted to lambda adapters for single-method interfaces
- **Variable capture**: Lambdas capture variables from enclosing scopes
- **Nested lambdas**: Inner lambdas are rewritten before outer lambdas execute

### Class, Interface, Enum, and Record Support

The playground includes a CN1-safe scripted-class runtime so user-declared
types work without runtime bytecode generation or reflection. The
`PlaygroundSyntaxMatrixHarness` (in `common/src/test/java/...`) is a
table-driven matrix that pins exactly what is supported — every entry
either reaches `SUCCESS` or documents a known gap with its diagnostic.

What works:

- **Classes** with fields, constructors (overloaded), methods (overloaded),
  generic type parameters (`class Pair<T>`), inheritance with method
  overrides, and `super.method()` / `super(args)` dispatch.
- **Static nested classes** (`Outer.Inner`) with `Outer.Inner.staticField` /
  `new Outer.Inner()` access.
- **Interfaces** with static methods, default methods, and anonymous
  implementations (`new Greeter() { public String greet() { ... } }`).
- **Enums** with simple constants, constants taking constructor args,
  per-constant method bodies, and built-in `name()` / `ordinal()` /
  `values()` / `valueOf()`.
- **Records** (`record Point(int x, int y) {}`) with auto-generated
  accessors, an optional body block, and the compact-constructor form
  (`Range { if (lo > hi) { ... } }`) which runs validation/normalisation
  before the implicit field assignments.
- **Sealed / non-sealed / permits** with runtime-enforced permit lists:
  declaring a subclass that isn't named in the parent's `permits` clause
  fails at evaluation time with a clear diagnostic.
- **Pattern-matching switch statements** with type bindings:
  `switch (o) { case Integer i -> useInt(i); case String s -> useStr(s); default -> ...; }`.
- **Non-static inner classes** — `class Outer { class Inner { ... } }`
  works, with Inner's methods reading/writing Outer's instance fields
  through the namespace chain. Construction via
  `new Outer().new Inner()` is supported; `new Inner()` inside an
  Outer method also works (the enclosing `this` is auto-resolved).
- **Interface method enforcement at declaration time** — a concrete
  class that says `implements Iface` must provide every abstract
  method Iface declares. Fires for both Java interfaces (signatures
  pulled from the CN1 registry) and scripted interfaces (abstract
  methods read from the interface's own `ScriptedClass`). A bare
  `class Other implements ActionListener {}` now fails with
  `class 'Other' is not abstract and does not implement all methods
  from com.codename1.ui.events.ActionListener. Missing:
  actionPerformed.`
- **Diagnostic suggestions** — missing static fields, static methods,
  and instance methods on scripted classes all produce a "Did you
  mean: X?" hint drawn from the relevant name table. Helps spot
  typos like `Display.PICKER_TYP_DATE` or `myObj.sayz()`.

What still doesn't work:

- Reflection APIs (`java.lang.reflect.*`, `Class.forName`) — forbidden in
  CN1 and out of scope.
- Cross-snippet sealed hierarchies — sealed enforcement operates on a
  single snippet because the Interpreter is per-run.
- JDK surface that isn't in CN1's runtime (`Optional`, `List.of`,
  `Map.of`, `Set.of`, `Stream.of`, `IntStream.range`, extended
  Collectors, etc.). The playground mirrors CN1's actual API surface
  rather than full JDK parity — scripts that compile here also run
  on device.

### Streams

`Collection.stream()` is wired through a minimal in-process shim
(`bsh.cn1.CN1StreamBridge`) because CN1's collection backport doesn't
expose `stream()` natively. Supported intermediate ops: `filter`,
`map`, `flatMap`, `peek`, `sorted` / `sorted(Comparator)`, `distinct`,
`limit`, `skip`. Supported terminal ops: `forEach`, `count`, `collect`
(returns a `List`, ignores the collector argument), `toList`,
`toArray`, `iterator`, `anyMatch` / `allMatch` / `noneMatch`,
`findFirst` / `findAny`, `min` / `max`, `reduce(BinaryOperator)`,
`reduce(identity, BinaryOperator)`. Methods that ordinarily return
`Optional` return the value directly, or `null` when the stream is
empty — CN1's runtime omits `java.util.Optional`. `reduce` keys off
`BinaryOperator` rather than `BiFunction` for the same reason.

### Lambdas and Method References

- Lambdas in any context: assignment (`Runnable r = () -> {};`), return
  expressions, method-call arguments, and as fields.
- Lambdas implement common SAM types directly: `Runnable`, `Supplier`,
  `Consumer`, `BiConsumer`, `Function`, `Predicate`, `Comparator`. Other
  CN1-specific listener interfaces are wrapped via `PlaygroundListenerBridge`.
- Method references for static (`System.out::println`), bound-instance
  (`prefix::concat`), unbound-instance (`String::length` →
  `(s) -> s.length()`), and constructor (`ArrayList::new`) forms.

### Switch and Pattern Matching

- Classic switch statements (int, String, fall-through with explicit break).
- Switch expression arrow form: `String s = switch (x) { case 1 -> "one"; default -> "?"; };`.
- Switch expression yield form: `case 1: yield "one"; default: yield "?";`.
- Arrow-form switch statements (no result value).
- Pattern matching for instanceof: `if (o instanceof String s) { use(s); }`.

### Try-with-resources, Multi-catch, var

- `try (Reader r = ...)` with single, multiple, and trailing-semicolon
  resource lists.
- Multi-catch `catch (E1 | E2 e)`.
- Local variable type inference with `var` (BSH already treats `var` as a
  loose type).

### Generic Type Parameters Are Erased

Generic type parameters are not enforced at runtime. Methods that rely on
specific generic types may require casting:

```java
// Generic types are erased, so explicit casting may be needed:
List<String> items = (List<String>) someMethod();
```

### Error Diagnostics

When a static field, static method, or instance method on a scripted
class misses, the playground searches the relevant name table (CN1
registry for Java types, the scripted class's own method list for
user types) for the closest match by case-insensitive prefix or
short Levenshtein distance and appends up to three suggestions.
Typos like `Display.PICKER_TYP_DATE` surface as `... (did you mean:
PICKER_TYPE_DATE, PICKER_TYPE_DATE_AND_TIME?)`, and
`myThing.sayz()` surfaces as `No instance method Thing.sayz/0. Did
you mean: say?`.

Interface-method enforcement fires at class-declaration time, so
`class X implements ActionListener {}` fails immediately with
`Missing: actionPerformed.` rather than deferring to the first
invocation site.

### Cold-start Performance

`PlaygroundColdStartHarness` in the test sources prints baseline
timings for the cold-start phases (registry first use, first
package-helper load, first FIELD_INDEX lookup, CN1 `Display.init`,
first full `PlaygroundRunner.run`). Run it against a fresh JVM via
`java -cp ...` to catch regressions. Typical median on a dev
laptop: first registry hit ~27 ms, first `PlaygroundRunner.run`
~60 ms, CN1's own `Display.init` ~800 ms. Only the
playground-controlled phases are actionable; the rest is CN1
runtime wiring.

## JavaScript Port Considerations

### `BrowserComponent` and Dialogs

When a `Dialog` is shown, the parent `Form` is deinitialized. This normally causes any `BrowserComponent` instances (including the code editor) to be removed from the DOM. When the `Dialog` is dismissed, the `Form` is re-shown but the browser state is lost.

**The fix**: The playground editor sets the client property `"HTML5Peer.removeOnDeinitialize"` to `Boolean.FALSE`, which preserves the iframe in the DOM across deinitialization cycles. If you use `BrowserComponent` in your own playground scripts, consider applying the same fix:

```java
BrowserComponent browser = new BrowserComponent();
browser.putClientProperty("HTML5Peer.removeOnDeinitialize", Boolean.FALSE);
```

### Cross-Origin Restrictions

The JavaScript port runs in a browser and is subject to [Same-Origin Policy (SOP)](https://developer.mozilla.org/en-US/docs/Web/Security/Same-origin_policy) and [CORS](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS) restrictions:

**What won't work**:
- Network requests to servers that don't send `Access-Control-Allow-Origin` headers
- Accessing resources from a different origin (protocol, domain, or port)
- Reading response headers from cross-origin requests

**What works**:
- Requests to the same origin (same protocol, domain, port)
- Requests to servers with proper CORS headers (`Access-Control-Allow-Origin: *` or your origin)
- JSONP callbacks (if the server supports them)
- Using a proxy server to bypass CORS

**For testing**:
- Use endpoints that support CORS (many public APIs do)
- Run in the native simulator where CORS doesn't apply
- Set up a local proxy server

**The playground's REST demo** uses endpoints that support CORS, so networking examples work in the JavaScript port. Your own URLs may need CORS configuration on the server side.

## Architecture

### CN1 Access Registry

The playground generates a hardcoded registry of accessible Codename One APIs at build time. This registry (`bsh.cn1.gen.GeneratedCN1Access`) provides:

- Class lookup
- Constructor invocation
- Method dispatch
- Static method calls
- Field access

The registry is generated by `tools/generate-cn1-access-registry.sh` and includes commonly used classes. To add more classes, modify the generation configuration and rebuild.

### Lambda Bridge

`PlaygroundLambdaBridge` provides factory methods that create SAM adapters for functional interfaces:

- `lambda(String[] paramNames, String body)` - Creates a lambda from parameter names and expression body
- `actionListener(Runnable r)` - Wraps a Runnable as an ActionListener
- `networkListener(Consumer<NetworkEvent> c)` - Wraps a Consumer as a NetworkListener
- `onComplete(Consumer<T> c)` - Wraps a Consumer for async callbacks

### Known SAM Types

The playground pre-wires common listener interfaces:

- `addActionListener`
- `addResponseListener`
- `callSerially`
- `callSeriallyAndWait`
- `fetchAsString`

For other SAM interfaces, use anonymous inner class syntax or the lambda bridge directly.

## Building

```bash
cd scripts/cn1playground
mvn clean install
```

## Testing

```bash
cd scripts/cn1playground
bash tools/run-playground-smoke-tests.sh
```

This smoke command currently runs:

1. CN1 access registry generation (`tools/generate-cn1-access-registry.sh`).
2. Registry sanity checks (expected/forbidden class entries).
3. `PlaygroundSmokeHarness` end-to-end behavior checks.
4. `PlaygroundSyntaxMatrixHarness` syntax regression checks.

## Language Feature Rollout Process

Use this process when adding or fixing Java syntax support in Playground:

1. **Add/adjust matrix coverage first**  
   Update `common/src/test/java/com/codenameone/playground/PlaygroundSyntaxMatrixHarness.java` with a focused snippet for the target syntax.
   - For currently unsupported syntax, add as `ExpectedOutcome.FAILURE`.
   - When support lands, flip that case to `ExpectedOutcome.SUCCESS`.

2. **Implement parser/runtime change in small steps**  
   Prefer one syntax feature per PR (e.g. method references only) to keep regressions easy to isolate.

3. **Run smoke + syntax matrix locally**  
   Run `bash tools/run-playground-smoke-tests.sh` from `scripts/cn1playground`.

4. **Require CI green before merge**  
   The `CN1 Playground Language Tests` workflow runs the same smoke command under CI (`xvfb-run`) and should pass before merging syntax updates.

5. **Document behavior changes**  
   Update this README's known issues/limitations when syntax support changes so users know what is now supported.

## Known Issues

1. **Parse errors with complex expressions**: BeanShell's parser may fail on some Java syntax. Simplify complex expressions or break them into multiple statements.

2. **Type ambiguity in overloaded methods**: When a method has overloads like `method(String)` and `method(String[])`, BeanShell may select the wrong overload. Cast arguments explicitly: `method((String) myValue)`.

3. **EDT warnings**: The playground runs script execution on the EDT. Long-running operations should use `CN.callSerially()` or background threads.

## Contributing

See the main Codename One repository for contribution guidelines.
