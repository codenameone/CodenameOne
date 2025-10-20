package com.codename1.processing;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProcessingPackageTest {
    private static final String SAMPLE_JSON =
            "{" +
            "\"results\":[" +
            "{" +
            "\"id\":\"1\"," +
            "\"name\":\"First\"," +
            "\"short_name\":\"New York\"," +
            "\"price\":10.5," +
            "\"active\":\"true\"," +
            "\"tags\":[\"alpha\",\"beta\"]," +
            "\"types\":[\"neighborhood\",\"political\"]," +
            "\"meta\":{\"attributes\":[{\"key\":\"color\",\"value\":\"red\"},{\"key\":\"size\",\"value\":\"L\"}]}" +
            "}," +
            "{" +
            "\"id\":\"2\"," +
            "\"name\":\"Second\"," +
            "\"short_name\":\"Albany\"," +
            "\"price\":20.75," +
            "\"active\":\"false\"," +
            "\"tags\":[\"gamma\",\"delta\"]," +
            "\"types\":[\"political\"]," +
            "\"meta\":{\"attributes\":[{\"key\":\"color\",\"value\":\"blue\"},{\"key\":\"size\",\"value\":\"M\"}]}" +
            "}" +
            "]," +
            "\"settings\":{\"toggle\":true,\"max_retries\":3,\"timeout_milliseconds\":5000}" +
            "}";

    private static final String SAMPLE_XML =
            "<root xmlns:ex=\"http://example.com/schema\">" +
            "<item id=\"1\" category=\"alpha\">First</item>" +
            "<item id=\"2\" category=\"beta\">Second</item>" +
            "<ex:entry ex:code=\"X\" value=\"External\">Namespace</ex:entry>" +
            "</root>";

    @Test
    public void testJsonExtractionAcrossDataTypes() {
        Result result = Result.fromContent(SAMPLE_JSON, Result.JSON);

        assertEquals("First", result.getAsString("/results[0]/name"));
        assertEquals(2, result.getAsInteger("/results[1]/id"));
        assertEquals(10.5, result.getAsDouble("/results[0]/price"), 0.0001);
        assertTrue(result.getAsBoolean("/settings/toggle"));
        assertEquals(5000L, result.getAsLong("/settings/timeout_milliseconds"));

        String[] tags = result.getAsStringArray("/results[0]/tags");
        assertArrayEquals(new String[]{"alpha", "beta"}, tags);

        int[] ids = result.getAsIntegerArray("//id");
        assertArrayEquals(new int[]{1, 2}, ids);

        boolean[] flags = result.getAsBooleanArray("//active");
        assertArrayEquals(new boolean[]{true, false}, flags);

        List list = result.getAsArray("/results[0]/types");
        assertEquals(2, list.size());
        assertEquals("neighborhood", list.get(0));
        assertEquals("political", list.get(1));
    }

    @Test
    public void testContainsEvaluatorMatchesArraysAndStrings() {
        Result result = Result.fromContent(SAMPLE_JSON, Result.JSON);

        String[] multiMatch = result.getAsStringArray("/results[types % (neighborhood, political)]/name");
        assertArrayEquals(new String[]{"First"}, multiMatch);

        String[] stringContains = result.getAsStringArray("/results[short_name % York]/name");
        assertArrayEquals(new String[]{"First"}, stringContains);

        String[] singleContains = result.getAsStringArray("/results[tags % (delta)]/name");
        assertArrayEquals(new String[]{"Second"}, singleContains);
    }

    @Test
    public void testIndexEvaluatorSupportsNumericAndFunctions() {
        Result result = Result.fromContent(SAMPLE_JSON, Result.JSON);

        assertEquals("Second", result.getAsString("/results[1]/name"));
        assertEquals("Second", result.getAsString("/results[last()]/name"));

        String[] greaterThan = result.getAsStringArray("/results[position() > 0]/name");
        assertArrayEquals(new String[]{"Second"}, greaterThan);

        String[] lessThan = result.getAsStringArray("/results[position() < 1]/name");
        assertArrayEquals(new String[]{"First"}, lessThan);
    }

    @Test
    public void testXmlAttributesAndNamespaceAliases() {
        Result result = Result.fromContent(SAMPLE_XML, Result.XML);

        assertEquals("Second", result.getAsString("/root/item[@category='beta']/text()"));
        assertEquals("1", result.getAsString("/root/item[@category='alpha']/@id"));

        result.mapNamespaceAlias("http://example.com/schema", "alias");
        assertEquals("External", result.getAsString("/root/alias:entry/text()"));
        assertEquals("X", result.getAsString("/root/alias:entry/@alias:code"));
    }

    @Test
    public void testResultTokenizerHandlesNestedPredicates() {
        ResultTokenizer tokenizer = new ResultTokenizer("/result/address_component[/type[position() < 5]='locality']/long_name");
        List tokens = tokenizer.tokenize(null);
        assertTrue(tokens.contains("/"));
        assertTrue(tokens.contains("result"));
        assertTrue(tokens.contains("address_component"));
        assertTrue(tokens.contains("/type[position() < 5]='locality'"));
    }

    @Test
    public void testEvaluatorFactoryResolvesAppropriateImplementations() {
        assertTrue(EvaluatorFactory.createEvaluator("0") instanceof IndexEvaluator);
        assertTrue(EvaluatorFactory.createEvaluator("@code='200'") instanceof AttributeEvaluator);
        assertTrue(EvaluatorFactory.createEvaluator("last() - 1") instanceof IndexEvaluator);
        assertTrue(EvaluatorFactory.createEvaluator("text()='value'") instanceof TextEvaluator);
        assertTrue(EvaluatorFactory.createEvaluator("types % (a, b)") instanceof ContainsEvaluator);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> EvaluatorFactory.createEvaluator("unknown"));
        assertTrue(ex.getMessage().contains("Could not create a comparator"));
    }

    @Test
    public void testResultFromContentNullChecks() {
        assertThrows(IllegalArgumentException.class, () -> Result.fromContent((String) null, Result.JSON));
        assertThrows(IllegalArgumentException.class, () -> Result.fromContent("{}", null));
        assertThrows(IllegalArgumentException.class, () -> Result.fromContent((StructuredContent) null));
    }
}
