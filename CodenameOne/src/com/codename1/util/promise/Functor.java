package ca.weblite.demos.cn1gram.util.promise;


/**
 *
 * @author shannah
 */
public interface Functor<T,V> {
    public V call(T arg);
}