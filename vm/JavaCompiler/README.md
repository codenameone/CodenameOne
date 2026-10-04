# Java Compiler

`com.codename1.tools.javac` compiles Java 17 and later source to class files. It is
written to run *inside* Codename One: the Playground translates it with ParparVM and
runs it in the browser, where it compiles the user's code, which the translator --
also running in the page -- turns into JavaScript and loads into the live VM.

That use sets its constraints:

- **Translatable.** It is compiled against ParparVM's class library
  (`vm/JavaAPI`) rather than the JDK -- no reflection, no regular expressions, no
  `java.nio`. It reads library classes through the translator's own class-file reader.
- **Self-contained output.** No annotation processing and no modules. Lambdas and
  method references become `LambdaMetafactory` call sites, which ParparVM lowers;
  everything else the JDK bootstraps at run time (string concatenation, records'
  `toString`/`equals`/`hashCode`, pattern switch) is lowered by the compiler itself.
- **Fast and robust on partial input.** The Playground compiles while the user types,
  so malformed source is always a diagnostic, never an exception.
- **No `native` methods.** A `native` declaration is an error (javac accepts it):
  the code this compiles runs in a live ParparVM or the simulator, neither of which
  can bind one, and Codename One reaches platform code through `NativeInterface`.

How the compiler fits into the Playground -- the stub library, the translator
running in the page and the loader -- is described in the developer guide's
"Java in the browser" chapter (`../../docs/developer-guide/Java-In-The-Browser.asciidoc`).

## Layout

| Path | What |
| --- | --- |
| `src/` | The compiler (`maven/javac` builds it as `codenameone-javac`). |
| `tools-src/` | JVM-only build tools: `BuildStubLibrary` packs jars into a stub library. |
| `tests/corpus/` | Programs that must behave exactly as javac's output does. |
| `tests/negative/` | Programs javac rejects, with the diagnostic expected (`// expect: LINE: message`). |

## Pipeline

`JavaSourceParser` (a recursive-descent parser over the `Lexer`'s tokens) builds the
tree; `Enter` declares classes and members; `Attr` resolves names, overloads and
generic inference and type-checks; `Flow` checks definite assignment and
reachability; `Gen` lowers and emits bytecode, and `FrameComputer` derives the
`StackMapTable` from the finished code. Output is class-file version 61 with
`NestHost`/`NestMembers`.

## Use

```java
JavaCompiler.Result r = new JavaCompiler(library)        // a ClassLibrary: bytes by internal name
        .addSource("Main.java", text)
        .compile();
r.isSuccess();          // no errors
r.getDiagnostics();     // javac-worded, with line and column
r.getClasses();         // internal name -> class file
r.isWellFormed();       // every source parsed (errors, if any, are semantic)
```

`addScript` compiles top-level statements, methods and classes as one class (the
Playground's script mode, configured by a `ScriptSpec`); `addDefaultImport` adds
imports every unit sees; `redirectCall` rewrites calls to a method (the Playground
turns `Form.show()` into "make this form the preview").

A `StubLibrary` is a compact `ClassLibrary`: class files with code stripped, packed
into one blob. The Playground ships one generated from the framework and ParparVM's
class library, so user code compiles against exactly the API the browser VM has.

## Tests

`vm/tests` holds `JavaCompilerConformanceTest`:

- every corpus program is compiled by javac 21 and by this compiler, and both run
  under `-Xverify:all` must print the same output and exit the same way;
- every negative case must report javac's diagnostic on javac's line;
- no prefix of a corpus program may make the compiler throw or hang.

```bash
source tools/env.sh
cd vm && JDK_21_HOME=/path/to/jdk-21 mvn -pl tests test -Dtest=JavaCompilerConformanceTest
```

The Playground's own suites exercise it end to end, on the JVM
(`scripts/cn1playground/tools/run-playground-smoke-tests.sh`) and translated in the
browser (`run-playground-browser-tests.sh`). The module is held to the zero-findings
SpotBugs gate (`maven/javac/spotbugs-exclude.xml`).
