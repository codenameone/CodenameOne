package com.codenameone.examples.hellocodenameone.tests;

public class Java17Tests extends BaseTest {
    record MyRecord(int val, String otherVal) {
        @Override
        public String toString() {
            return "MyRecord[val=" + val + ", otherVal=" + otherVal + "]";
        }

        @Override
        public int hashCode() {
            return 31 * val + otherVal.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MyRecord)) {
                return false;
            }
            MyRecord other = (MyRecord)obj;
            return val == other.val && otherVal.equals(other.otherVal);
        }
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() throws Exception {
        try {
            var greeting = "Hello";
            var target = "Codename One";
            var record = new MyRecord(2, "V");

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
            assertEqual(2, record.val());
            assertEqual("V", record.otherVal());
            assertEqual("MyRecord[val=2, otherVal=V]", record.toString());
            assertEqual(148, record.hashCode());
            assertTrue(record.equals(new MyRecord(2, "V")));
            return true;
        } catch (Throwable t) {
            fail(String.valueOf(t));
            return false;
        } finally {
            done();
        }
    }
}
