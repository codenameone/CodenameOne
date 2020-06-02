package com.codename1.util;

/**
 * Generic object wrapper, as workaround for the issue "Local variables
 * referenced from a lambda expression must be final or effectively final".
 */
public class Wrapper<T> {
    
    private T object;
    
    public Wrapper(T obj) {
        this.object = obj;
    }

    public T get() {
        return object;
    }
    
    public void set(T obj) {
        this.object = obj;
    }
    
}
