package com.codename1.router;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/// Package-private to access `RouteMatch` directly.
class RouteMatchTest {

    @Test
    void literalMatches() {
        RouteMatch r = new RouteMatch("/about", null);
        assertNotNull(r.match("/about"));
        assertNotNull(r.match("/about/")); // trailing slash tolerated
        assertNull(r.match("/about/x"));
        assertNull(r.match("/other"));
    }

    @Test
    void namedParamExtraction() {
        RouteMatch r = new RouteMatch("/users/:id", null);
        Map<String, String> m = r.match("/users/42");
        assertNotNull(m);
        assertEquals("42", m.get("id"));
    }

    @Test
    void singleSegmentWildcard() {
        RouteMatch r = new RouteMatch("/files/*", null);
        assertNotNull(r.match("/files/foo.png"));
        assertNull(r.match("/files/sub/foo.png"));
    }

    @Test
    void catchAllWildcardMatchesEmptyAndDeep() {
        RouteMatch r = new RouteMatch("/files/**", null);
        Map<String, String> m1 = r.match("/files/");
        Map<String, String> m2 = r.match("/files/a/b/c");
        assertNotNull(m1);
        assertNotNull(m2);
        assertEquals("a/b/c", m2.get("*"));
    }

    @Test
    void catchAllWildcardMatchesBarePrefix() {
        // `/admin/**` should also match `/admin` (without trailing slash) —
        // Ant-style catch-all semantics. Real apps register guards as
        // `/admin/**` and expect the bare entry to be guarded too.
        RouteMatch r = new RouteMatch("/admin/**", null);
        Map<String, String> m = r.match("/admin");
        assertNotNull(m);
        assertEquals("", m.get("*"));
    }

    @Test
    void specificityFavorsLiteralsOverParams() {
        RouteMatch literal = new RouteMatch("/users/me", null);
        RouteMatch param = new RouteMatch("/users/:id", null);
        assertTrue(literal.specificity() > param.specificity(),
                "literal segment must outscore named param");
    }

    @Test
    void specificityFavorsParamOverWildcard() {
        RouteMatch param = new RouteMatch("/files/:name", null);
        RouteMatch wildcard = new RouteMatch("/files/**", null);
        assertTrue(param.specificity() > wildcard.specificity());
    }

    @Test
    void patternMustStartWithSlash() {
        RouteMatch r = new RouteMatch("about", null);
        // Constructor normalizes by prepending '/' — accept both forms.
        assertNotNull(r.match("/about"));
    }

    @Test
    void emptyPatternThrows() {
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override public void execute() { new RouteMatch("", null); }
        });
    }
}
