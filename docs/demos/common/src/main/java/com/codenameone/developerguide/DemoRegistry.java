package com.codenameone.developerguide;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Registry of available demos so that the browser can enumerate them.
 */
public final class DemoRegistry {
    private static final List<Demo> DEMOS = Collections.unmodifiableList(
            Arrays.asList(
                    new HelloWorldDemo(),
                    new CounterDemo()
            )
    );

    private DemoRegistry() {
        // utility class
    }

    public static List<Demo> getDemos() {
        return DEMOS;
    }
}
