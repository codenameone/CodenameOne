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

The playground can generate shareable URLs that contain the script source code. Use the **"Copy Shareable Playground URL"** option in the side menu to copy a link to the clipboard.

**URL Format**: The generated URL uses a `code` query parameter with URL-safe Base64-encoded script content:
```
https://example.com/playground?code=<base64-encoded-script>
```

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

### Limitations

#### Class Declarations Have Limited Support

BeanShell's class generation is disabled in this playground, but single top-level classes are automatically unwrapped:

```java
// This works - playground unwraps the class:
public class DemoApp {
    private Label status;  // Becomes script variable: status = null;
    
    public void start() {
        status = new Label("Ready");
        // ...
    }
}
```

**What doesn't work**:
- Nested classes
- Multiple top-level classes
- Interfaces or enums
- Static fields or methods that reference instance fields

For complex state management, consider using loose methods with script-level variables instead of class fields.

#### Instance Field Access in Lambdas

Variables from enclosing scopes work in lambdas, but instance field references without `this` may not resolve correctly:

```java
// Works:
String prefix = "Hello ";
button.addActionListener(e -> status.setText(prefix + "World"));

// May not work as expected if 'status' is an instance field:
// Use 'this.status' or move the field to script scope
```

#### Generic Type Parameters Are Erased

Generic type parameters are erased at runtime. Methods that rely on specific generic types may require casting:

```java
// Generic types are erased, so explicit casting may be needed:
List<String> items = (List<String>) someMethod();
```

#### Some Java Syntax Is Unsupported

- **Enhanced for-each with arrays**: BeanShell's for-each works for `Collection` types but not for arrays when using the generated access registry. Use traditional `for (int i = 0; i < arr.length; i++)` loops for arrays, or convert to a List first:
  ```java
  // Doesn't work with arrays:
  String[] arr = {"a", "b", "c"};
  for (String s : arr) { }  // May fail
  
  // Workaround:
  for (int i = 0; i < arr.length; i++) { String s = arr[i]; }
  // Or convert to List:
  for (String s : java.util.Arrays.asList(arr)) { }
  ```
- **Method references**: `Object::toString` syntax is not supported; use lambdas instead
- **Try-with-resources**: Use try/finally blocks manually

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
./scripts/run-tests.sh
```

## Known Issues

1. **Parse errors with complex expressions**: BeanShell's parser may fail on some Java syntax. Simplify complex expressions or break them into multiple statements.

2. **Type ambiguity in overloaded methods**: When a method has overloads like `method(String)` and `method(String[])`, BeanShell may select the wrong overload. Cast arguments explicitly: `method((String) myValue)`.

3. **EDT warnings**: The playground runs script execution on the EDT. Long-running operations should use `CN.callSerially()` or background threads.

## Contributing

See the main Codename One repository for contribution guidelines.