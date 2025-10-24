package com.codenameone.developerguide.animations;

/**
 * Documentation helper for the LazyValue interface snippet.
 */
public final class LazyValueDocSnippet {
    private LazyValueDocSnippet() {
        // Prevent instantiation.
    }

    // tag::lazyValueInterface[]
    /**
     * Represents a value that is provided lazily when needed.
     */
    public interface LazyValue<T> {
        T get(Object... args);
    }
    // end::lazyValueInterface[]
}
