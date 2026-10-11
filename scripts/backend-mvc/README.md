# Compiled HTML catalog

A complete in-memory CRUD application using backend MVC controllers, typed HTML,
shared fragments, scalar form binding, CSRF protection, and htmx. Restarting the
server resets the catalog. It works with ordinary forms and with htmx updates.

Requires the backend and build plugins from this checkout installed locally.
Maven uses Java 8. Gradle itself requires Java 17 or newer.

From this directory:

```sh
mvn package
mvn com.codenameone:codenameone-maven-plugin:8.0-SNAPSHOT:backend
```

Open `http://localhost:8080/products`. To build and run the native executable:

```sh
mvn com.codenameone:codenameone-maven-plugin:8.0-SNAPSHOT:backend-package
./target/backend-mvc
```

The generated executable contains both the renderers and public assets. It can
run from another directory with only an `application.properties` file (or the
usual backend environment configuration). It does not read HTML files at runtime.

The included Gradle build uses the same sources and template compiler:

```sh
gradle classes
gradle runBackend
gradle backendPackage
```

If using a custom Maven local repository, pass its path as
`-Pcodename1.repository=/path/to/repository` to Gradle and configure the plugin
repository in `settings.gradle.kts` to match. Template and asset edits invalidate
Gradle compilation, including with its configuration cache enabled.

## Verify

With either server running:

```sh
python3 smoke.py http://127.0.0.1:8080
```

The script creates and removes its own test products, checks both normal and
htmx request flows, invalid form redisplay, CSRF rejection, Unicode escaping,
embedded assets, and inaccessible template sources.

Compiler, router, and generated test-context regressions run in the build engine:

```sh
cd ../../maven
mvn -pl backend,build-engine -am test -Plocal-dev-javase \
  -Dtest=MvcTemplatesTest,RestControllerAnnotationProcessorTest,BackendBeansTest,BackendTestGeneratorTest \
  -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.javadoc.skip=true
```

The optional `-Dcn1.mvc.benchmark=true` adds a rendering comparison and writes
`build-engine/target/mvc-render-benchmark.csv`. It compares identical escaped HTML
from generated and handwritten Java renderers, using the same input and buffer
size, with warmup and alternating trial order. It records time, throughput, and
thread allocation. It is a small JavaSE microbenchmark, not a native HTTP or
cross-framework performance claim; concurrent builds and garbage collection can
substantially affect its timings.

## Where to start

- `Catalog.java`: view names, `Model`, `@ModelAttribute`, `BindingResult`, and htmx selection.
- `templates/products.html`: typed iteration, URL generation, named fragments, and deletion forms.
- `templates/edit.html`: typed form fields, conversion errors, and shared layout fragments.
- `WebSecurity.java`: public access with CSRF enforcement enabled.

The complete syntax and migration limits are in
[the backend views chapter](../../docs/developer-guide/Backend-Views.asciidoc).
This is a practical Thymeleaf/Spring MVC subset; it does not embed either framework.

htmx 2.0.11 is vendored from the official npm distribution through jsDelivr:
`https://cdn.jsdelivr.net/npm/htmx.org@2.0.11/dist/htmx.min.js`.
Its Zero-Clause BSD license is in `HTMX-LICENSE.txt`.
