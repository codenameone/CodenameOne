// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::java-in-the-browser-bash-001[]
$ source tools/env.sh
$ cd vm
$ JDK_21_HOME=/path/to/jdk-21 mvn -pl tests test -Dtest=JavaCompilerConformanceTest
// end::java-in-the-browser-bash-001[]

// tag::java-in-the-browser-bash-002[]
$ source tools/env.sh
$ vm/selfhost/build-selfhost.sh
$ CORPUS="$PWD/vm/selfhost/target/asm-classes;$PWD/vm/selfhost/target/classes"
$ vm/selfhost/verify-selfhost.sh "$CORPUS" \
    com_codename1_tools_translator_ByteCodeTranslator com.codename1.tools.translator
$ vm/selfhost/verify-selfhost-js.sh "$CORPUS" \
    com_codename1_tools_translator_ByteCodeTranslator com.codename1.tools.translator
// end::java-in-the-browser-bash-002[]

// tag::java-in-the-browser-bash-003[]
$ cd scripts/cn1playground
$ tools/run-playground-smoke-tests.sh
$ tools/run-playground-browser-tests.sh
// end::java-in-the-browser-bash-003[]
