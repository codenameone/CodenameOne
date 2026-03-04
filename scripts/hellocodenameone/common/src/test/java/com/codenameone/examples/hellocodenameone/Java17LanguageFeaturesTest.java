package com.codenameone.examples.hellocodenameone;

import com.codename1.testing.AbstractTest;

public class Java17LanguageFeaturesTest extends AbstractTest {

    @Override
    public boolean runTest() throws Exception {
        var greeting = "Hello";
        var target = "Codename One";

        var message = switch (greeting.length()) {
            case 5 -> greeting + " " + target;
            default -> "unexpected";
        };

        var textBlock = """
                Java 17 language features
                should compile in tests.
                """;

        assertEqual("Hello Codename One", message);
        assertEqual("Java 17 language features\nshould compile in tests.\n", textBlock);

        return true;
    }
}
