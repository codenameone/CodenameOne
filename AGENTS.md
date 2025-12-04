# Agent Instructions

## Scope
These instructions apply to the entire repository. They are aimed at helping Codex agents build and run the core unit tests that live under `maven/core-unittests`.

## Java runtime
- Use Java 8 when running the Maven build. This matches the CI configuration in `.github/workflows/pr.yml` and avoids JaCoCo instrumentation errors with newer JDKs.
- Set `JAVA_HOME` and update `PATH` before invoking Maven:
  ```bash
  export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
  export PATH="$JAVA_HOME/bin:$PATH"
  ```

## Building and testing `maven/core-unittests`
- From the repository root, run the same Maven goal the PR CI uses for the core unit tests:
  ```bash
  cd maven
  mvn clean verify -DunitTests=true -pl core-unittests -am -Dmaven.javadoc.skip=true -Plocal-dev-javase
  ```
- This command will compile the dependencies, run the `core-unittests` test suite, and generate quality reports (SpotBugs/PMD/Checkstyle/JaCoCo) in `maven/core-unittests/target`.
- For a quicker edit/build cycle while iterating on tests, you can skip the clean step and run just the module’s tests:
  ```bash
  cd maven
  mvn -pl core-unittests -am -DunitTests=true -Dmaven.javadoc.skip=true -Plocal-dev-javase test
  ```
- For the fastest smoke check Codex can run while editing tests, use the helper script to execute a single lightweight test with the CI flags:
  ```bash
  ./scripts/fast-core-unit-smoke.sh
  ```
  This keeps the Java 8 toolchain, skips the top-level clean, and targets `ButtonGroupTest` for a sub-minute feedback loop.

## Artifacts to check
- Test reports: `maven/core-unittests/target/surefire-reports/`
- Coverage: `maven/core-unittests/target/site/jacoco/`
- Static analysis: `maven/core-unittests/target/spotbugs*.xml`, `maven/core-unittests/target/pmd.xml`, `maven/core-unittests/target/checkstyle-result.xml`
