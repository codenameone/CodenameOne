package ca.weblite.demos.cn1gram.util.promise;


/**
 *
 * @author shannah
 */
public interface ExecutorFunction {
    public void call(Functor resolutionFunc, Functor rejectionFunc);
}