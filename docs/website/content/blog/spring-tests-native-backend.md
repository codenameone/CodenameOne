---
title: "How Do You Test Spring Compatibility?"
slug: spring-tests-native-backend
url: /blog/spring-tests-native-backend/
date: '2026-10-12'
author: Shai Almog
description: "Write behavior tests in your Spring application, then run the same assertions against its Codename One port. Check responses, failures and state on the JVM and in a native build."
feed_html: '<img src="https://www.codenameone.com/blog/spring-tests-native-backend.jpg" alt="The same service contract is checked on the JVM and native runtime" /> Write behavior tests in your Spring application, then run the same assertions against its Codename One port. Check responses, failures and state on the JVM and in a native build.'
series: ["release-2026-10-09"]
---

![The same service contract is checked on the JVM and native runtime](/blog/spring-tests-native-backend.jpg)

The Codename One backend is not Spring compatible. We use similar syntax to ease migration and porting, but familiar annotations do not tell you whether the ported application behaves the same way. Tests do.

Write those tests in the original Spring application first. Capture the behavior your clients depend on, get the tests passing, then bring the same assertions to Codename One. Here, compatibility means that your application's behavior survives the move. It does not mean that Codename One implements the Spring framework.

The new testing API gives you familiar request builders and assertions for that job. You can run them against the port on the JVM, then check the compiled native backend too.

## Establish the behavior in Spring first

Start with an endpoint whose contract you already know. For a Spring application that exposes `/greet/{name}`, this test records the successful response and what happens for an unknown route:

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GreetingApiTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void greetsByName() throws Exception {
        mvc.perform(get("/greet/{name}", "Ada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.greeting").value("Hello, Ada"));
    }

    @Test
    void unknownRoutesAreNotFound() throws Exception {
        mvc.perform(get("/nowhere")).andExpect(status().isNotFound());
    }
}
```

This example uses the Spring Boot 3 test imports. Run it against the original application before porting. If the application protects these routes, configure the test identity to match its access rules rather than disabling security to get a green result.

For a real service, go beyond the successful response. Test validation errors, missing records and denied access. For a write endpoint, verify the stored state as well as the response. Those expectations define what the port must preserve.

## Port the test setup, keep the assertions

For the Codename One version, change the framework imports and test annotations. The request inputs and expected results above stay the same:

```diff
-import org.springframework.beans.factory.annotation.Autowired;
-import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
-import org.springframework.boot.test.context.SpringBootTest;
-import org.springframework.test.web.servlet.MockMvc;
+import com.codename1.backend.annotations.Autowired;
+import com.codename1.backend.test.BackendTest;
+import com.codename1.backend.test.MockMvc;

-import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
-import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
-import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
+import static com.codename1.backend.test.MockMvcRequestBuilders.get;
+import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
+import static com.codename1.backend.test.MockMvcResultMatchers.status;

-@SpringBootTest
-@AutoConfigureMockMvc
+@BackendTest
 class GreetingApiTest {
```

`@BackendTest` starts the module's application and injects test fields from its generated wiring. `MockMvc` passes requests through routing, sessions, scoped beans and error handling inside the process. The matching Codename One controller, service and test are in the [compiled guide examples](https://github.com/codenameone/CodenameOne/tree/ebe0e64be3/docs/demos/backend/src/main/java/com/codenameone/developerguide/backend/testing).

New backend templates include the test setup. Existing modules need the test dependency and annotation-processing goals in the [testing guide](/developer-guide/backend-testing/). Keep the same fixtures and expected values on both sides. Changing an assertion merely to accept the port's output hides the difference you were trying to detect.

These import changes work for this example. A larger suite can use Spring test features that Codename One does not implement. Port that setup explicitly, and keep track of any case that cannot run yet. Passing the tests demonstrates the behavior they cover, not every possible interaction in the application.

## Decide what the test needs to cross

Use `MockMvc` for request and response behavior without sending that request through a socket. Use `TestRestTemplate` when the HTTP transport is part of the contract. Both work with the test application's beans.

{{< mermaid >}}
flowchart LR
    Tests[Same request inputs and expected results] --> Spring[Original Spring application]
    Tests --> Port[Ported Codename One test setup]
    Port --> JVM[JVM backend]
    Port --> Native[Compiled native backend]
    Spring --> Contract[Verify status, body and stored state]
    JVM --> Contract
    Native --> Contract
{{< /mermaid >}}

A test can add a `@TestConfiguration` and substitute a `@Primary` bean. That is useful when you need deterministic behavior from a dependency while still testing the controller and generated wiring. It also works in the compiled runner.

`@MockitoBean` is available on the JVM with Mockito on the test classpath. It cannot follow you into a native binary because Mockito creates classes at runtime. Prefer an explicit test implementation when the same scenario needs to run in both environments.

Security tests can use `@WithMockUser` or request post-processors. A mocked JWT identity tests authorization after authentication; it does not test signature verification or the token exchange. Keep a real-client integration test for those boundaries, as [Saturday's article](/blog/sign-in-client-server-contract/) describes.

## Run the same supported tests after compilation

In a project with a backend module:

```bash
mvn -pl backend -Dcodename1.platform=backend test
mvn -pl backend -Dcodename1.platform=backend test \
    -Dcn1.backend.compiledTests=true \
    -Dcn1.backend.compiledTests.strict=true
```

The second command runs Surefire, generates direct test calls and builds a native test binary with the application's generated wiring and native code. It writes `TEST-<class>-compiled.xml` reports beside the JVM reports. This is a ParparVM test binary; it does not embed JUnit's reflection-based runner.

Strict mode matters when porting a suite. Without it, Mockito-dependent classes can be excluded or reported skipped. Strict mode makes those unsupported cases fail the build so an apparently successful native run does not hide the tests you meant to keep.

The native path needs the backend toolchain, including clang and the OpenSSL, libcurl and nghttp2 headers. It builds on Linux and macOS. Windows has no native backend runner. Run native tests locally as part of assessing your migration.

## Account for differences in the test framework

There are no `@WebMvcTest` slices; each test runs the application. The generated test application excludes management endpoints, MCP and OpenTelemetry exporters even if production configuration enables them. Check those integrations separately.

The compiled runner supports a defined JUnit 5 subset. Nested test classes, Kotlin tests and some selectors cannot simply be carried over. JSONPath supports common property and index operations, not every filter or function. `MOCK` and `NONE` still start a loopback listener for the server's runtime tasks, even though `MockMvc.perform` dispatches in-process.

Keep the Spring run as your reference while you port. Run the same scenarios against the JVM backend, investigate differences, then run the compiled path and inspect both failures and skips. Replace unsupported test machinery with explicit fixtures where practical. A test that never ran cannot tell you whether the migration preserved its behavior.

[PR #5932](https://github.com/codenameone/CodenameOne/pull/5932) introduced the framework, and current master adds the security test support. Start with the endpoint whose behavior would be most expensive to get wrong.

## Discussion

_Which behavior would you test first before porting your Spring application?_

{{< giscus >}}
