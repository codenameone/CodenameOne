/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package java.util.concurrent.atomic;

/**
 *
 * @author shannah
 */
public class AtomicReference<V> {
    private Object lock = new Object();
    V ref;
    
    public AtomicReference() {
        
    }
    
    public AtomicReference(V initialValue) {
        ref = initialValue;
    }
    
    public final boolean compareAndSet(V expect, V update) {
        synchronized(lock) {
            if (expect == ref) {
                ref = update;
                return true;
            }
            return false;
        }
        
    }
    
    public V get() {
        synchronized(lock) {
            return ref;
        }
    }
    
    public final V getAndSet(V newValue) {
        synchronized(lock) {
            V old = ref;
            ref = newValue;
            return old;
        }
    }
    
    public final  void lazySet(V newValue) {
        synchronized(lock) {
            ref = newValue;
        }
    }
    
    public String toString() {
        synchronized(lock) {
            return String.valueOf(ref);
        }
    }
    
    public final boolean weakCompareAndSet(V expect, V update) {
        return compareAndSet(expect, update);
    }
    
    public final void set(V newValue) {
        synchronized(lock) {
            ref = newValue;
        }
        
    }
}
