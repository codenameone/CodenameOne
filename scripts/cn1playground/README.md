# Playground

The Playground is an interactive environment for writing, running and iterating on Codename One UI code. The code is real Java: an in-tree Java compiler (`vm/JavaCompiler`) compiles it to class files in the running Playground, which loads and runs them immediately -- on the JavaSE simulator through a class loader, and in the browser by translating the classes to JavaScript with the ParparVM translator, also running in the page, and loading them into the live VM. Nothing is sent to a server.

## Features

### Script Execution

- **Scripts**: write statements, classes and methods at the top level. The value of a trailing expression is the preview; without one, the form the script shows (or the first form or component it creates) is used:
  ```java
  Container root = new Container(BoxLayout.y());
  Button btn = new Button("Click me");
  btn.addActionListener(e -> Dialog.show("Hello", "World", "OK", null));
  root.add(btn);
  root
  ```

- **Lifecycle classes**: a class with `init(Object)` and `start()` runs like an application:
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

- **`build(PlaygroundContext)`**: return a Component from a method named `build`:
  ```java
  Component build(PlaygroundContext ctx) {
      Container root = new Container(BoxLayout.y());
      ctx.log("Building UI");
      root.add(new Label("Hello"));
      return root;
  }
  ```

`form.show()` and `showBack()` make that form the preview rather than replacing the Playground, including when a listener shows a second form after the run.

### Language

User code is Java 17 and later, compiled with javac's rules and diagnostics: classes, interfaces, enums, records (with compact constructors), sealed hierarchies, nested, inner, local and anonymous classes, generics, lambdas and method references for any functional interface, switch expressions and `yield`, pattern `instanceof`, pattern switch and record patterns, text blocks, `var`, try-with-resources and multi-catch.

Not supported: annotation processing, modules and reflection (`java.lang.reflect`, `Class.forName`), which Codename One does not offer on devices either.

### Available API

Code compiles against exactly the API the running VM contains -- the Codename One framework and ParparVM's Java class library -- so a call that compiles here also exists on device. It is the API the browser build ships, which is narrower than a desktop JDK (there is no `List.of` or `IntStream`, for example); `Collection.stream()`, `Optional`, `Comparator`'s combinators and the common `Stream` operations are available.

These are imported by default; other imports can be declared anywhere in a script:
- `java.util.*`, `java.io.*`
- `com.codename1.ui.*`, `com.codename1.ui.layouts.*`, `com.codename1.ui.events.*`, `com.codename1.ui.geom.*`
- `com.codename1.components.*`
- `com.codename1.ui.plaf.Style`, `com.codename1.ui.plaf.UIManager`, `com.codename1.ui.util.Resources`
- `PlaygroundContext`, `GameScripting` and `GpuScripting`

### Diagnostics

Compile errors are reported with javac's wording at their line and column, and marked in the editor. An exception thrown at run time -- during the run, or later by a listener or timer -- is reported in the message panel below the editor.

### REST APIs

```java
import com.codename1.io.rest.Rest;

Rest.get("https://example.com/api/data").fetchAsString(response -> {
    String text = response.getResponseData();
    // Process the response
});
```

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

## How It Works

The developer guide's "Java in the browser" chapter (`docs/developer-guide/Java-In-The-Browser.asciidoc`) describes the whole pipeline -- compiler, stub library, in-page translator, open-world bundle and loader -- and the gates that keep each part correct. In short:

1. `PlaygroundRunner` compiles the editor's text with `com.codename1.tools.javac.JavaCompiler` in script mode: top-level statements become the body of an entry method, top-level methods and classes become members and nested classes, and the script's trailing value is returned. A class-shaped entry (lifecycle or `build`) gets a small generated launcher.
2. The compiler reads the API from `playground-api.cn1stubs`, a stub library the `common` build generates (`BuildStubLibrary`) from the framework jar and ParparVM's Java class library: signatures, generic signatures and constants, no code.
3. The class files are loaded:
   - **JavaSE**: `PlaygroundClassDefiner` defines them in a fresh class loader per run. The simulator registers it through `PlaygroundLoaderNativeImpl`; the test harnesses through `HarnessSupport`.
   - **JavaScript**: `JavascriptIncremental` translates them against the running bundle and `PlaygroundJs.loadClasses` evaluates the result in the VM, redefining classes of the same name on every run.

The JavaScript bundle is built **open-world** for the API user code may call (`javascript/translator-opts.txt`, read by `build.sh` and the website build): every class under the listed prefixes keeps all its methods and fields under their canonical names, and methods user code may override stay suspendable. Everything else -- the compiler and translator themselves, the port implementation -- is culled and minified as usual. See `JavascriptOpenWorld` in the translator.

## JavaScript Port

The `javascript` module builds with the local ParparVM JavaScript target (`codename1.buildTarget=local-javascript`), the same path the initializr uses, so it tracks the current Codename One sources directly.

To compare a bundle's size against another ParparVM artifact:

```bash
PLAYGROUND_PARPARVM_BUNDLE=/path/to/parparvm/dist ./build.sh javascript_compare
```

This uses
[`compare-javascript-bundles.sh`](tools/compare-javascript-bundles.sh)
to report total and JavaScript payload sizes.

## JavaScript Port Considerations

### Lightweight editor input

The Java and CSS source panes are `CodeEditor` components rendered by Codename One. They don't use a `BrowserComponent`, an iframe, or a contenteditable element. The JavaScript port binds the focused editor to its low-level text-input source for keyboard, selection, IME, and negotiated clipboard events while the editor itself remains in the CN1 component hierarchy. Dialog and responsive-layout transitions therefore preserve the document and editor state without peer-lifecycle workarounds.

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

## Building

```bash
cd scripts/cn1playground
mvn clean install
./build.sh javascript   # the browser bundle
```

## Testing

```bash
cd scripts/cn1playground
bash tools/run-playground-smoke-tests.sh     # JavaSE harnesses
bash tools/run-playground-browser-tests.sh   # the browser bundle, in headless Chromium
```

The smoke command runs `PlaygroundSmokeHarness`, `PlaygroundSyntaxMatrixHarness`, `PlaygroundLayoutHarness`, `PlaygroundPreviewResolutionHarness` and `PlaygroundSamplesHarness`. The browser command is described in [`tools/README.md`](tools/README.md). The `CN1 Playground Language Tests` workflow runs both.

The compiler has its own tests under `vm/JavaCompiler/tests`: a corpus whose output must match javac's, and negative cases whose diagnostics must match javac's wording and position.

## Language Feature Process

1. Add a focused case to `PlaygroundSyntaxMatrixHarness` with the outcome javac would give: `SUCCESS`, `PARSE_ERROR` (rejected at compile time) or `EVAL_ERROR` (compiles, fails at run time).
2. Fix the compiler, adding a corpus or negative case under `vm/JavaCompiler/tests`.
3. Run the smoke and browser tests locally, and require CI green before merging.

## Known Issues

1. **EDT**: scripts run on the EDT. Long-running work belongs on a background thread, with UI updates through `CN.callSerially()`.
2. **Older share links**: a link written for the earlier BeanShell-based Playground that relies on syntax BeanShell accepted but Java does not now reports a compile error at the offending line.

## Contributing

See the main Codename One repository for contribution guidelines.
