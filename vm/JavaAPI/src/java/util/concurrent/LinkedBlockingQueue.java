package java.util.concurrent;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

public class LinkedBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {
    private final LinkedList<E> list = new LinkedList<E>();
    private final int capacity;

    public LinkedBlockingQueue() {
        this(Integer.MAX_VALUE);
    }

    public LinkedBlockingQueue(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException();
        this.capacity = capacity;
    }

    public Iterator<E> iterator() {
        return list.iterator();
    }

    public int size() {
        synchronized(list) {
            return list.size();
        }
    }

    public boolean offer(E e) {
        if (e == null) throw new NullPointerException();
        synchronized(list) {
            if (list.size() >= capacity) return false;
            list.add(e);
            list.notifyAll();
            return true;
        }
    }

    public E poll() {
        synchronized(list) {
            if (list.isEmpty()) return null;
            E x = list.removeFirst();
            list.notifyAll();
            return x;
        }
    }

    public E peek() {
        synchronized(list) {
            return list.isEmpty() ? null : list.getFirst();
        }
    }

    public void put(E e) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        synchronized(list) {
            while (list.size() >= capacity) {
                list.wait();
            }
            list.add(e);
            list.notifyAll();
        }
    }

    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        long millis = unit.toMillis(timeout);
        long end = System.currentTimeMillis() + millis;
        synchronized(list) {
            while (list.size() >= capacity) {
                long delay = end - System.currentTimeMillis();
                if (delay <= 0) return false;
                list.wait(delay);
            }
            list.add(e);
            list.notifyAll();
            return true;
        }
    }

    public E take() throws InterruptedException {
        synchronized(list) {
            while (list.isEmpty()) {
                list.wait();
            }
            E x = list.removeFirst();
            list.notifyAll();
            return x;
        }
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long millis = unit.toMillis(timeout);
        long end = System.currentTimeMillis() + millis;
        synchronized(list) {
            while (list.isEmpty()) {
                long delay = end - System.currentTimeMillis();
                if (delay <= 0) return null;
                list.wait(delay);
            }
            E x = list.removeFirst();
            list.notifyAll();
            return x;
        }
    }

    public int remainingCapacity() {
        synchronized(list) {
            return capacity - list.size();
        }
    }

    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null) throw new NullPointerException();
        if (c == this) throw new IllegalArgumentException();
        synchronized(list) {
            int n = 0;
            while (n < maxElements && !list.isEmpty()) {
                c.add(list.removeFirst());
                n++;
            }
            if (n > 0) list.notifyAll();
            return n;
        }
    }
}
