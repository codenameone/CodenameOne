JavaScript Port Status
======================

Implemented
-----------

- [x] PolyForm Noncommercial 1.0.0 license boundary for `Ports/JavaScriptPort/**`
- [x] Imported browser-port baseline into the repository as a working reference subtree
- [x] ParparVM-side production host bridge in [JavaScriptPortHost.java](/Users/shai/dev/cn1/Ports/JavaScriptPort/src/main/java/com/codename1/impl/platform/js/JavaScriptPortHost.java)
- [x] ParparVM translator registration for JavaScript-port host natives in [JavascriptNativeRegistry.java](/Users/shai/dev/cn1/vm/ByteCodeTranslator/src/com/codename1/tools/translator/JavascriptNativeRegistry.java)
- [x] Browser bundle bootstrap shell and host bridge in `vm/ByteCodeTranslator`
- [x] PolyForm smoke fixtures for the JavaScript port under `Ports/JavaScriptPort/tests/**`
- [x] ParparVM smoke and browser-bundle integration coverage in `vm/tests`
- [x] Extraction of reusable runtime helpers from `HTML5Implementation`
- [x] Extraction of input/bootstrap/event wiring helpers from `HTML5Implementation`
- [x] Initial rendering subsystem extraction from `HTML5Graphics` and `BufferedGraphics`
- [x] Shared native-image model and cache invalidation policy via [JavaScriptNativeImageAdapter.java](/Users/shai/dev/cn1/Ports/JavaScriptPort/src/main/java/com/codename1/impl/html5/JavaScriptNativeImageAdapter.java)
- [x] Async image load state coordination via [JavaScriptAsyncImageLoadCoordinator.java](/Users/shai/dev/cn1/Ports/JavaScriptPort/src/main/java/com/codename1/impl/html5/JavaScriptAsyncImageLoadCoordinator.java)
- [x] Rendering backend contract in [JavaScriptRenderingBackend.java](/Users/shai/dev/cn1/Ports/JavaScriptPort/src/main/java/com/codename1/impl/html5/JavaScriptRenderingBackend.java)
- [x] Neutral default backend implementation name: `BrowserDomRenderingBackend`
- [x] Backend-routed image creation, scaling, pixel readback, and canvas serialization for the main image runtime paths

In Progress
-----------

- [ ] Replace remaining direct browser-runtime assumptions in `HTML5Implementation` with backend-owned or adapter-owned seams
- [ ] Reduce the remaining `org.teavm.*` and `com.codename1.teavm.*` coupling inside the production runtime path
- [ ] Define a concrete ParparVM-native browser/runtime backend implementation behind `JavaScriptRenderingBackend`

TODO
----

- [ ] Implement a non-TeaVM production backend behind `JavaScriptRenderingBackend`
- [ ] Port the remaining image/media/photo-capture canvas paths onto backend-owned abstractions where appropriate
- [ ] Port browser peer integration and native peer lifecycle beyond the current extraction layer
- [ ] Port text editing and native text-field overlay behavior away from legacy assumptions
- [ ] Port networking and storage from extracted helper contracts into a clearly ParparVM-oriented runtime path
- [ ] Port browser/media/database/file-chooser integrations away from direct legacy dependencies
- [ ] Replace or adapt legacy `teavm` package names in production code where they are part of the long-term runtime contract
- [ ] Expand runtime tests from smoke/contract coverage into broader end-to-end JavaScript-port behavior coverage
- [ ] Add a dedicated implementation-factory selection path for the JavaScript port if the final runtime wiring needs one beyond current bootstrap behavior

Verification
------------

- Focused verification currently uses:
  ```bash
  export JAVA_HOME=/Users/shai/Library/Java/JavaVirtualMachines/azul-1.8.0_372/Contents/Home
  export PATH="$JAVA_HOME/bin:$PATH"
  mvn -B test -f /Users/shai/dev/cn1/vm/pom.xml -pl tests -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=JavaScriptRuntimeFacadeTest,JavaScriptPortSmokeIntegrationTest,JavascriptTargetIntegrationTest#generatesBrowserBundleForJavascriptTarget
  ```
