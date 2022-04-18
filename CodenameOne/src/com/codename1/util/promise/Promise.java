/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.util.promise;


import com.codename1.annotations.Async;
import com.codename1.io.Util;
import com.codename1.ui.CN;
import com.codename1.util.AsyncResource;
import com.codename1.util.AsyncResult;
import com.codename1.util.SuccessCallback;

import java.util.LinkedList;

import static com.codename1.ui.CN.invokeAndBlock;

/**
 * An implementation of <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise">Promise</a> for
 * use with Codename One applications.  Due to java reserved words, there are a few differences in this implementation:
 *
 * <ol>
 * <li>Instead of {@literal catch()}, we use {@literal except()}</li>
 * <li>Instead of {@literal finally()}, we use {@literal always()}</li>
 * </ol>
 *
 * <p>Since {@link #then(Functor, Functor) }, {@link #except(Functor) },
 * and {@link #always(Functor) } take Functors as parameters, which must have a return value, this implementation
 * provides convenience wrappers {@link #onSuccess(SuccessCallback) }, {@link #onFail(SuccessCallback) },
 * and {@link #onComplete(SuccessCallback) } which take {@link SuccessCallback} objects instead.  For simple cases,
 * these wrappers will be easier to use because you don't need to return a dummy {@literal null} at the end of the callback.</p>
 *
 * <p>For more complex cases, where the return value of one Functor is meant to be piped into the subsequent Functor, then the Functor
 * variants should be used.</p>
 *
 * @author shannah
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise">MDN documentation for Promise</a>
 * @since 8.0
 */
public class Promise<T> {

    private final LinkedList<PromiseHandler> then = new LinkedList<PromiseHandler>();

    private final Functor<T, ?> resolve;
    private final Functor<Throwable,?> reject;

    private State state = State.Pending;
    private Throwable error;
    private T value;


    private static class PromiseHandler {
        private Promise promise;
        private Functor resolve;
        private Functor reject;
    }

    /**
     * Encapsulates the state of a Promise.
     */
    public enum State {
        /**
         * initial state, neither fulfilled nor rejected.
         */
        Pending,

        /**
         *  the operation completed successfully.
         */
        Fulfilled,

        /**
         * the operation failed.
         */
        Rejected
    }


    /**
     * Creates a new promise with the given executor function.  Works the same as Javascript equivalent.
     * @param executor The executor function.  This is executed immediately, and should call either the passed {@literal resolve}
     * or {@literal reject} functor to mark success or failure.
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/Promise">MDN documentation for Promise</a>
     */
    @Async.Schedule
    public Promise(ExecutorFunction executor) {
        resolve = new Functor<T, Object>() {
            public Object call(final T o) {
                if (!CN.isEdt()) {
                    CN.callSerially(new Runnable() {
                        public void run() {
                            resolve.call(o);
                        }
                    });
                    return null;
                }
                state = State.Fulfilled;
                value = o;
                Promise.this.processThens(o, true);
                return value;

            }
        };
        reject = new Functor<Throwable, Object>() {
            public Object call(final Throwable o) {
                if (!CN.isEdt()) {
                    CN.callSerially(new Runnable() {
                        public void run() {
                            reject.call(o);
                        }
                    });
                    return null;
                }
                state = State.Rejected;
                error = o;
                Promise.this.processThens(o, false);
                return error;
            }
        };
        if (executor != null) {
            executor.call(resolve, reject);
        }



    }

    /**
     * Called when the promise is rejected or resolved to process all handler functions
     * that were registered via {@link #then(Functor, Functor) },
     * {@link #except(Functor) }, or {@link #always(Functor) }
     * @param o The value to pipe into the handler functors as an argument.
     * @param resolved Whether the promise was resolved.  If {@literal true}, it will call the resolve
     * handler.  If {@literal false}, it will call the reject handler.
     */
    @Async.Execute
    private void processThens(final Object o, final boolean resolved) {
        if (!CN.isEdt()) {
            CN.callSerially(new Runnable() {
                public void run() {
                    Promise.this.processThens(o, resolved);
                }
            });
            return;
        }
        while (!then.isEmpty()) {
            PromiseHandler p = then.remove(0);
            try {
                Object result = resolved ? p.resolve.call(o) : p.reject.call(o);
                if (result instanceof Promise) {
                    Promise promiseResult = (Promise)result;
                    switch (promiseResult.state) {
                        case Fulfilled:


                            p.promise.resolve.call(promiseResult.value);
                            break;
                        case Rejected:
                            p.promise.reject.call(promiseResult.error);
                            break;
                        case Pending:
                            promiseResult.then(p.promise.resolve, p.promise.reject);
                            break;
                    }
                } else {
                    p.promise.resolve.call(result);
                }

            } catch (Throwable ex) {
                p.promise.reject.call(ex);
            }
        }
    }

    /**
     * A wrapper for {@link #then(Functor, Functor) } that uses {@link SuccessCallback}
     * instead of {@link Functor}.
     *
     * @param resolutionFunc Callback to run on resolution of promise.
     * @param rejectionFunc Callback to run on rejection of promise.
     * @return
     */
    public Promise ready(final SuccessCallback<T> resolutionFunc, final SuccessCallback<Throwable> rejectionFunc) {
        return then(resolutionFunc == null ? null : new Functor<T, Object>() {
            public Object call(T o) {
                resolutionFunc.onSucess(o);
                return null;
            }
        }, rejectionFunc == null ? null : new Functor<Throwable, Object>() {
            public Object call(Throwable o) {
                rejectionFunc.onSucess(o);
                return null;
            }
        });
    }

    /**
     * A wrapper for {@link #then(Functor) } that uses {@link SuccessCallback} instead
     * of {@link Functor}.
     * @param resolutionFunc Callback called when project is fulfilled.
     * @return
     */
    public Promise onSuccess(SuccessCallback<T> resolutionFunc) {
        return ready(resolutionFunc, null);
    }


    /**
     * A wrapper for {@link #except(Functor) } that uses {@link SuccessCallback} instead
     * of {@link Functor}.
     * @param rejectionFunc
     * @return
     */
    public Promise onFail(SuccessCallback<Throwable> rejectionFunc) {
        return ready(null, rejectionFunc);
    }

    /**
     * A wrapper for {@link #always(Functor) } that uses {@link SuccessCallback} instead
     * of {@link Functor}.
     * @param handlerFunc
     * @return
     */
    public Promise onComplete(SuccessCallback handlerFunc) {
        return ready(handlerFunc, handlerFunc);
    }

    /**
     *
     * @param resolutionFunc
     * @return
     */
    public Promise then(Functor<T,?> resolutionFunc) {
        return then(resolutionFunc, null);
    }

    /**
     * The then() method returns a Promise. It takes up to two arguments: callback functions for the success and failure cases of the Promise.
     * @param resolutionFunc A Function called if the Promise is fulfilled. This function has one argument, the fulfillment value. If it is null, it is internally replaced with an "Identity" function (it returns the received argument).
     * @param rejectionFunc A Function called if the Promise is rejected. This function has one argument, the rejection reason. If it is null, it is internally replaced with a "Thrower" function (it throws an error it received as argument).
     *
     *
     * @return <p>Once a Promise is fulfilled or rejected, the respective handler function (resolutionFunc or rejectionFunc) will be called asynchronously (scheduled on the EDT). The behavior of the handler function follows a specific set of rules. If a handler function:</p>
     * <dl>
     * <dt>returns a value,</dt><dd> the promise returned by then gets resolved
     * with the returned value as its value.</dd>
     * <dt>doesn't return anything,</dt> <dd>the promise returned by then gets
     * resolved with an undefined value.</dd>
     * <dt>throws an error,</dt><dd> the promise returned by then gets rejected
     * with the thrown error as its value.</dd>
     * <dt>returns an already fulfilled promise</dt>, <dd>the promise returned
     * by then gets fulfilled with that promise's value as its value.</dd>
     * <dt>returns an already rejected promise,</dt> <dd>the promise returned by
     * then gets rejected with that promise's value as its value.</dd>
     * <dt>returns another pending promise object,</dt><dd> the
     * resolution/rejection of the promise returned by then will be subsequent
     * to the resolution/rejection of the promise returned by the handler. Also,
     * the resolved value of the promise returned by then will be the same as
     * the resolved value of the promise returned by the handler.</dd>
     * </dl>
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/then">See MDN documentation for then()</a>
     */
    public Promise then(Functor<T,?> resolutionFunc, Functor<Throwable,?> rejectionFunc) {
        if (resolutionFunc == null) {
            resolutionFunc = new Functor<T, Object>() {
                public Object call(T o) {
                    return o;
                }
            };
        }
        if (rejectionFunc == null) {
            rejectionFunc = new Functor<Throwable, Object>() {
                public Object call(Throwable o) {
                    throw (RuntimeException) o;
                }
            };
        }

        PromiseHandler handler = new PromiseHandler();
        handler.promise = new Promise(null);
        handler.resolve = resolutionFunc;
        handler.reject = rejectionFunc;
        then.add(handler);
        switch (state) {
            case Fulfilled:
                processThens(value, true);
                break;
            case Rejected:
                processThens(error, false);
                break;
        }
        return handler.promise;
    }

    /**
     * Implementation of Promise.catch(). Named "except" because of Java reserved word..
     * @param rejectionFunc Function called if promise is rejected.
     * @return
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/catch">See MDN documentation for catch()</a>
     */
    public Promise except(Functor<Throwable,?> rejectionFunc) {
        return then(null, rejectionFunc);
    }

    /**
     * Implementation of Promise.finally().  Named "always" because of Java reserved word.
     * @param handlerFunc
     * @return
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/finally">See MDN documentation for finally()</a>
     */
    public Promise always(Functor handlerFunc) {
        return then(handlerFunc, handlerFunc);
    }

    /**
     * Gets the return value once the promise is fulfilled.  If the promise isnt resolved, this just returns null.
     * @return
     */
    public T getValue() {
        return value;
    }

    /**
     * Returns the current state of the promise.
     * @return
     */
    public State getState() {
        return state;
    }

    /**
     * The Promise.all() method takes an iterable of promises as an input, and returns a single Promise that resolves to an array of the results of the input promises. This returned promise will resolve when all of the input's promises have resolved, or if the input iterable contains no promises. It rejects immediately upon any of the input promises rejecting or non-promises throwing an error, and will reject with this first rejection message / error.
     * @param promises
     * @return
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/all">See MDN documentation for all()</a>
     */
    public static Promise all(final Promise... promises) {

        return new Promise(new ExecutorFunction() {
            public void call(final Functor resolve, final Functor reject) {
                final int[] complete = new int[1];
                final int len = promises.length;
                final Object[] results = new Object[len];
                if (len > 0) {
                    for (int i = 0; i < len; i++) {
                        final int index = i;
                        final Promise p = promises[i];
                        p.then(new Functor() {
                            public Object call(Object res) {

                                results[index] = res;
                                complete[0]++;
                                if (complete[0] == len) {
                                    resolve.call(results);
                                }
                                return null;
                            }
                        }).except(new Functor() {
                            public Object call(Object error) {
                                reject.call(error);
                                return null;
                            }
                        });
                    }
                } else {
                    resolve.call(results);
                }

            }
        });
    }

    /**
     * <p>
     * The Promise.allSettled() method returns a promise that resolves after all
     * of the given promises have either fulfilled or rejected, with an array of
     * objects that each describes the outcome of each promise.</p>
     *
     * <p>
     * It is typically used when you have multiple asynchronous tasks that are
     * not dependent on one another to complete successfully, or you'd always
     * like to know the result of each promise.</p>
     *
     * <p>
     * In comparison, the Promise returned by Promise.all() may be more
     * appropriate if the tasks are dependent on each other / if you'd like to
     * immediately reject upon any of them rejecting.</p>
     *
     * @param promises
     * @return
     * @see
     * <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/allSettled">See MDN documentation for allSettled()</a>
     *
     */
    public static Promise allSettled(final Promise... promises) {
        return new Promise(new ExecutorFunction() {
            public void call(final Functor resolve, Functor reject) {
                final int[] complete = new int[1];
                final int len = promises.length;
                if (len > 0) {
                    for (int i = 0; i < len; i++) {
                        Promise p = promises[i];
                        p.always(new Functor() {
                            public Object call(Object res) {
                                complete[0]++;
                                if (complete[0] == len) {
                                    resolve.call(promises);
                                }

                                return null;
                            }
                        });
                    }
                } else {
                    resolve.call(promises);
                }
            }
        });
    }

    /**
     * Uses invokeAndBlock to wait for this promise to be either resolved or rejected.
     * This will throw an exception of type {@link AsyncResource.AsyncExecutionException} if the
     * promise failed.  Otherwise it will return the resolved value.
     * @return
     */
    public T await() {
        final boolean[] complete = new boolean[1];
        final Object[] out = new Object[1];
        final Throwable[] ex = new Throwable[1];
        this.onSuccess(new SuccessCallback<T>() {
            public void onSucess(T res) {
                synchronized (complete) {
                    out[0] = res;
                    complete[0] = true;
                    complete.notifyAll();
                }
            }
        }).onFail(new SuccessCallback() {
            public void onSucess(Object res) {
                synchronized (complete) {
                    ex[0] = (Throwable) res;
                    complete[0] = true;
                    complete.notifyAll();

                }
            }
        });

        while (!complete[0]) {
            invokeAndBlock(new Runnable() {
                public void run() {
                    synchronized (complete) {
                        Util.wait(complete, 500);
                    }
                }
            });
        }
        if (ex[0] != null) {
            throw new AsyncResource.AsyncExecutionException((Throwable)ex[0]);
        }
        return (T)out[0];
    }

    public static <V> Promise<V> resolve(final V value) {
        return new Promise<V>(new ExecutorFunction() {
            public void call(Functor resolutionFunc, Functor rejectionFunc) {
                resolutionFunc.call(value);
            }
        });
    }

    public static Promise reject(final Throwable err) {
        return new Promise(new ExecutorFunction() {
            public void call(Functor resolve, Functor reject) {
                reject(err);
            }
        });
    }

    public static <V> Promise<V> promisify(final AsyncResource<V> res) {
        return new Promise<V>(new ExecutorFunction() {
            public void call(final Functor resolutionFunc, final Functor rejectionFunc) {
                res.onResult(new AsyncResult<V>() {
                    public void onReady(V r, Throwable err) {
                        if (err != null) {
                            rejectionFunc.call(err);
                        } else {
                            resolutionFunc.call(r);
                        }
                    }
                });
            }
        });
    }

    public AsyncResource<T> asAsyncResource() {
        final AsyncResource<T> out = new AsyncResource<T>();
        this.onSuccess(new SuccessCallback<T>() {
            public void onSucess(T res) {
                out.complete(res);
            }
        });
        this.onFail(new SuccessCallback<Throwable>() {
            public void onSucess(Throwable err) {
                out.error(err);
            }
        });
        return out;
    }

}
