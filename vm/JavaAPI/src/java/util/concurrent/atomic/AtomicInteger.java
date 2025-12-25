package java.util.concurrent.atomic;

public class AtomicInteger extends Number implements java.io.Serializable {
    private volatile int value;

    public AtomicInteger(int initialValue) {
        value = initialValue;
    }

    public AtomicInteger() {
    }

    public final int get() {
        return value;
    }

    public final void set(int newValue) {
        value = newValue;
    }

    public final void lazySet(int newValue) {
        value = newValue;
    }

    public final int getAndSet(int newValue) {
        synchronized(this) {
            int prev = value;
            value = newValue;
            return prev;
        }
    }

    public final boolean compareAndSet(int expect, int update) {
        synchronized(this) {
            if (value == expect) {
                value = update;
                return true;
            }
            return false;
        }
    }

    public final boolean weakCompareAndSet(int expect, int update) {
        return compareAndSet(expect, update);
    }

    public final int getAndIncrement() {
        synchronized(this) {
            return value++;
        }
    }

    public final int getAndDecrement() {
        synchronized(this) {
            return value--;
        }
    }

    public final int getAndAdd(int delta) {
        synchronized(this) {
            int prev = value;
            value += delta;
            return prev;
        }
    }

    public final int incrementAndGet() {
        synchronized(this) {
            return ++value;
        }
    }

    public final int decrementAndGet() {
        synchronized(this) {
            return --value;
        }
    }

    public final int addAndGet(int delta) {
        synchronized(this) {
            return value += delta;
        }
    }

    public String toString() {
        return Integer.toString(get());
    }

    public int intValue() {
        return get();
    }

    public long longValue() {
        return (long)get();
    }

    public float floatValue() {
        return (float)get();
    }

    public double doubleValue() {
        return (double)get();
    }
}
