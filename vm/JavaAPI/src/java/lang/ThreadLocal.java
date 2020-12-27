/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package java.lang;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author shannah
 */
public class ThreadLocal<T> extends Object {

    private Map<Thread,T> value = new HashMap<Thread,T>();
    private Set<Thread> _initialized = new HashSet<Thread>();

    public ThreadLocal() {
        super();
    }

    protected T initialValue() {
        return null;
    }

    public T get() {
        Thread t = Thread.currentThread();
        if (!_initialized.contains(t)) {
            _initialized.add(t);
            value.put(t, initialValue());
        }
        return value.get(t);
        
        
    }

    public void set(T value) {
        Thread t = Thread.currentThread();
        
        _initialized.add(t);
        this.value.put(t, value);
    }

    public void remove() {
        Thread t = Thread.currentThread();
        _initialized.remove(t);
        value.remove(t);
    }
}
