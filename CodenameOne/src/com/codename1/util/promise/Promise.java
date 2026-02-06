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

/// An implementation of [Promise](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise) for
/// use with Codename One applications.  Due to java reserved words, there are a few differences in this implementation:
///
/// - Instead of catch(), we use except()
///
/// - Instead of finally(), we use always()
///
/// Since `Functor)`, `#except(Functor)`,
/// and `#always(Functor)` take Functors as parameters, which must have a return value, this implementation
/// provides convenience wrappers `#onSuccess(SuccessCallback)`, `#onFail(SuccessCallback)`,
/// and `#onComplete(SuccessCallback)` which take `SuccessCallback` objects instead.  For simple cases,
/// these wrappers will be easier to use because you don't need to return a dummy null at the end of the callback.
///
/// For more complex cases, where the return value of one Functor is meant to be piped into the subsequent Functor, then the Functor
/// variants should be used.
///
/// @author shannah
///
/// #### Since
///
/// 8.0
///
/// #### See also
///
/// - [MDN documentation for Promise](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise)
public class Promise<T> {

    private final LinkedList<PromiseHandler> then = new LinkedList<PromiseHandler>();

    private final Functor<T, ?> resolve;
    private final Functor<Throwable, ?> reject;

    private State state = State.Pending;
    private Throwable error;
    private T value;


    /// Creates a new promise with the given executor function.  Works the same as Javascript equivalent.
    ///
    /// #### Parameters
    ///
    /// - `executor`: @param executor The executor function.  This is executed immediately, and should call either the passed resolve
    ///                 or reject functor to mark success or failure.
    ///
    /// #### See also
    ///
    /// - [MDN documentation for Promise](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/Promise)
    @Async.Schedule
    public Promise(ExecutorFunction executor) {
        resolve = new Functor<T, Object>() {
            @Override
            public Object call(final T o) {
                if (!CN.isEdt()) {
                    CN.callSerially(new Runnable() {
                        @Override
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
            @Override
            public Object call(final Throwable o) {
                if (!CN.isEdt()) {
                    CN.callSerially(new Runnable() {
                        @Override
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

    /// The Promise.all() method takes an iterable of promises as an input, and returns a single Promise that resolves to an array of the results of the input promises. This returned promise will resolve when all of the input's promises have resolved, or if the input iterable contains no promises. It rejects immediately upon any of the input promises rejecting or non-promises throwing an error, and will reject with this first rejection message / error.
    ///
    /// #### Parameters
    ///
    /// - `promises`
    ///
    /// #### See also
    ///
    /// - [See MDN documentation for all()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/all)
    public static Promise all(final Promise... promises) {

        return new Promise(new ExecutorFunction() {
            @Override
            public void call(final Functor resolve, final Functor reject) {
                final int[] complete = new int[1];
                final int len = promises.length;
                final Object[] results = new Object[len];
                if (len > 0) {
                    for (int i = 0; i < len; i++) {
                        final int index = i;
                        final Promise p = promises[i];
                        p.then(new Functor() {
                            @Override
                            public Object call(Object res) {

                                results[index] = res;
                                complete[0]++;
                                if (complete[0] == len) {
                                    resolve.call(results);
                                }
                                return null;
                            }
                        }).except(new Functor() {
                            @Override
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

    /// The Promise.allSettled() method returns a promise that resolves after all
    /// of the given promises have either fulfilled or rejected, with an array of
    /// objects that each describes the outcome of each promise.
    ///
    /// It is typically used when you have multiple asynchronous tasks that are
    /// not dependent on one another to complete successfully, or you'd always
    /// like to know the result of each promise.
    ///
    /// In comparison, the Promise returned by Promise.all() may be more
    /// appropriate if the tasks are dependent on each other / if you'd like to
    /// immediately reject upon any of them rejecting.
    ///
    /// #### Parameters
    ///
    /// - `promises`
    ///
    /// #### See also
    ///
    /// - [See MDN documentation for allSettled()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/allSettled)
    public static Promise allSettled(final Promise... promises) {
        return new Promise(new ExecutorFunction() {
            @Override
            public void call(final Functor resolve, Functor reject) {
                final int[] complete = new int[1];
                final int len = promises.length;
                if (len > 0) {
                    for (int i = 0; i < len; i++) {
                        Promise p = promises[i];
                        p.always(new Functor() {
                            @Override
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

    public static <V> Promise<V> resolve(final V value) {
        return new Promise<V>(new ExecutorFunction() {
            @Override
            public void call(Functor resolutionFunc, Functor rejectionFunc) {
                resolutionFunc.call(value);
            }
        });
    }

    public static Promise reject(final Throwable err) {
        return new Promise(new ExecutorFunction() {
            @Override
            public void call(Functor resolve, Functor reject) {
                reject(err);
            }
        });
    }

    public static <V> Promise<V> promisify(final AsyncResource<V> res) {
        return new Promise<V>(new ExecutorFunction() {
            @Override
            public void call(final Functor resolutionFunc, final Functor rejectionFunc) {
                res.onResult(new AsyncResult<V>() {
                    @Override
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

    /// Called when the promise is rejected or resolved to process all handler functions
    /// that were registered via `Functor)`,
    /// `#except(Functor)`, or `#always(Functor)`
    ///
    /// #### Parameters
    ///
    /// - `o`: The value to pipe into the handler functors as an argument.
    ///
    /// - `resolved`: @param resolved Whether the promise was resolved.  If true, it will call the resolve
    ///                 handler.  If false, it will call the reject handler.
    @Async.Execute
    private void processThens(final Object o, final boolean resolved) {
        if (!CN.isEdt()) {
            CN.callSerially(new Runnable() {
                @Override
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
                    Promise promiseResult = (Promise) result;
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
                        default:
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

    /// A wrapper for `Functor)` that uses `SuccessCallback`
    /// instead of `Functor`.
    ///
    /// #### Parameters
    ///
    /// - `resolutionFunc`: Callback to run on resolution of promise.
    ///
    /// - `rejectionFunc`: Callback to run on rejection of promise.
    public Promise ready(final SuccessCallback<T> resolutionFunc, final SuccessCallback<Throwable> rejectionFunc) {
        return then(resolutionFunc == null ? null : new Functor<T, Object>() {
            @Override
            public Object call(T o) {
                resolutionFunc.onSucess(o);
                return null;
            }
        }, rejectionFunc == null ? null : new Functor<Throwable, Object>() {
            @Override
            public Object call(Throwable o) {
                rejectionFunc.onSucess(o);
                return null;
            }
        });
    }

    /// A wrapper for `#then(Functor)` that uses `SuccessCallback` instead
    /// of `Functor`.
    ///
    /// #### Parameters
    ///
    /// - `resolutionFunc`: Callback called when project is fulfilled.
    public Promise onSuccess(SuccessCallback<T> resolutionFunc) {
        return ready(resolutionFunc, null);
    }

    /// A wrapper for `#except(Functor)` that uses `SuccessCallback` instead
    /// of `Functor`.
    ///
    /// #### Parameters
    ///
    /// - `rejectionFunc`
    public Promise onFail(SuccessCallback<Throwable> rejectionFunc) {
        return ready(null, rejectionFunc);
    }

    /// A wrapper for `#always(Functor)` that uses `SuccessCallback` instead
    /// of `Functor`.
    ///
    /// #### Parameters
    ///
    /// - `handlerFunc`
    public Promise onComplete(SuccessCallback handlerFunc) {
        return ready(handlerFunc, handlerFunc);
    }

    /// #### Parameters
    ///
    /// - `resolutionFunc`
    public Promise then(Functor<T, ?> resolutionFunc) {
        return then(resolutionFunc, null);
    }

    /// The then() method returns a Promise. It takes up to two arguments: callback functions for the success and failure cases of the Promise.
    ///
    /// #### Parameters
    ///
    /// - `resolutionFunc`: A Function called if the Promise is fulfilled. This function has one argument, the fulfillment value. If it is null, it is internally replaced with an "Identity" function (it returns the received argument).
    ///
    /// - `rejectionFunc`: A Function called if the Promise is rejected. This function has one argument, the rejection reason. If it is null, it is internally replaced with a "Thrower" function (it throws an error it received as argument).
    ///
    /// #### Returns
    ///
    /// @return
    ///
    /// Once a Promise is fulfilled or rejected, the respective handler function (resolutionFunc or rejectionFunc) will be called asynchronously (scheduled on the EDT). The behavior of the handler function follows a specific set of rules. If a handler function:
    ///
    /// returns a value, the promise returned by then gets resolved
    /// with the returned value as its value.
    /// doesn't return anything, the promise returned by then gets
    /// resolved with an undefined value.
    /// throws an error, the promise returned by then gets rejected
    /// with the thrown error as its value.
    /// returns an already fulfilled promise, the promise returned
    /// by then gets fulfilled with that promise's value as its value.
    /// returns an already rejected promise, the promise returned by
    /// then gets rejected with that promise's value as its value.
    /// returns another pending promise object, the
    /// resolution/rejection of the promise returned by then will be subsequent
    /// to the resolution/rejection of the promise returned by the handler. Also,
    /// the resolved value of the promise returned by then will be the same as
    /// the resolved value of the promise returned by the handler.
    ///
    /// #### See also
    ///
    /// - [See MDN documentation for then()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/then)
    public Promise then(Functor<T, ?> resolutionFunc, Functor<Throwable, ?> rejectionFunc) {
        if (resolutionFunc == null) {
            resolutionFunc = new Functor<T, Object>() {
                @Override
                public Object call(T o) {
                    return o;
                }
            };
        }
        if (rejectionFunc == null) {
            rejectionFunc = new Functor<Throwable, Object>() {
                @Override
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
            default:
                break;
        }
        return handler.promise;
    }

    /// Implementation of Promise.catch(). Named "except" because of Java reserved word..
    ///
    /// #### Parameters
    ///
    /// - `rejectionFunc`: Function called if promise is rejected.
    ///
    /// #### See also
    ///
    /// - [See MDN documentation for catch()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/catch)
    public Promise except(Functor<Throwable, ?> rejectionFunc) {
        return then(null, rejectionFunc);
    }

    /// Implementation of Promise.finally().  Named "always" because of Java reserved word.
    ///
    /// #### Parameters
    ///
    /// - `handlerFunc`
    ///
    /// #### See also
    ///
    /// - [See MDN documentation for finally()](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/finally)
    public Promise always(Functor handlerFunc) {
        return then(handlerFunc, handlerFunc);
    }

    /// Gets the return value once the promise is fulfilled.  If the promise isnt resolved, this just returns null.
    public T getValue() {
        return value;
    }

    /// Returns the current state of the promise.
    public State getState() {
        return state;
    }

    /// Uses invokeAndBlock to wait for this promise to be either resolved or rejected.
    /// This will throw an exception of type `AsyncResource.AsyncExecutionException` if the
    /// promise failed.  Otherwise it will return the resolved value.
    public T await() {
        final boolean[] complete = new boolean[1];
        final Object[] out = new Object[1];
        final Throwable[] ex = new Throwable[1];
        this.onSuccess(new SuccessCallback<T>() {
            @Override
            public void onSucess(T res) {
                synchronized (complete) {
                    out[0] = res;
                    complete[0] = true;
                    complete.notifyAll();
                }
            }
        }).onFail(new SuccessCallback() {
            @Override
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
                @Override
                public void run() {
                    synchronized (complete) {
                        Util.wait(complete, 500);
                    }
                }
            });
        }
        if (ex[0] != null) {
            throw new AsyncResource.AsyncExecutionException(ex[0]);
        }
        return (T) out[0];
    }

    public AsyncResource<T> asAsyncResource() {
        final AsyncResource<T> out = new AsyncResource<T>();
        this.onSuccess(new SuccessCallback<T>() {
            @Override
            public void onSucess(T res) {
                out.complete(res);
            }
        });
        this.onFail(new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable err) {
                out.error(err);
            }
        });
        return out;
    }

    /// Encapsulates the state of a Promise.
    public enum State {
        /// initial state, neither fulfilled nor rejected.
        Pending,

        /// the operation completed successfully.
        Fulfilled,

        /// the operation failed.
        Rejected
    }

    private static class PromiseHandler {
        private Promise promise;
        private Functor resolve;
        private Functor reject;
    }

}
