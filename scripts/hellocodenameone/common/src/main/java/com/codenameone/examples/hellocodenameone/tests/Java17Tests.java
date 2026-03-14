package com.codenameone.examples.hellocodenameone.tests;

public class Java17Tests extends BaseTest {
    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    //record MyRecord(int val, String otherVal) {}

    @Override
    public boolean runTest() throws Exception {
        var greeting = "Hello";
        var target = "Codename One";

        var message = switch (greeting.length()) {
            case 5 -> greeting + " " + target;
            //case 7 -> new MyRecord(2, "V");
            default -> "unexpected";
        };

        var textBlock = """
                Java 17 language features
                should compile in tests.
                """;

        assertEqual("Hello Codename One", message);
        assertEqual("Java 17 language features\nshould compile in tests.\n", textBlock);
        done();
        return true;
    }
}
