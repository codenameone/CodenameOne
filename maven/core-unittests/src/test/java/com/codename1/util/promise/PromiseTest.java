package com.codename1.util.promise;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.util.SuccessCallback;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PromiseTest extends UITestBase {

    @FormTest
    void testPromiseResolvesAndTriggersHandlers() {
        final AtomicReference<String> value = new AtomicReference<String>();
        Promise<String> promise = new Promise<String>(new ExecutorFunction() {
            public void call(Functor resolve, Functor reject) {
                resolve.call("done");
            }
        });

        promise.onSuccess(new SuccessCallback<String>() {
            public void onSucess(String result) {
                value.set(result);
            }
        });

        flushSerialCalls();
        assertEquals("done", value.get());
        assertEquals("done", promise.getValue());
    }

    @FormTest
    void testPromiseRejectsAndExceptHandlesError() {
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        Promise<String> promise = new Promise<String>(new ExecutorFunction() {
            public void call(Functor resolve, Functor reject) {
                reject.call(new RuntimeException("fail"));
            }
        });

        promise.except(new Functor<Throwable, Object>() {
            public Object call(Throwable t) {
                error.set(t);
                return null;
            }
        });

        flushSerialCalls();
        assertNotNull(error.get());
        assertEquals("fail", error.get().getMessage());
    }

    @FormTest
    void testPromiseAllAggregatesResults() {
        Promise<String> first = new Promise<String>(new ExecutorFunction() {
            public void call(Functor resolve, Functor reject) {
                resolve.call("A");
            }
        });
        Promise<String> second = new Promise<String>(new ExecutorFunction() {
            public void call(Functor resolve, Functor reject) {
                resolve.call("B");
            }
        });

        final AtomicReference<Object[]> results = new AtomicReference<Object[]>();
        Promise.all(first, second).onSuccess(new SuccessCallback<Object>() {
            public void onSucess(Object res) {
                results.set((Object[]) res);
            }
        });

        flushSerialCalls();
        assertNotNull(results.get());
        assertArrayEquals(new Object[]{"A", "B"}, results.get());
    }

    @FormTest
    void testPromiseAlwaysInvoked() {
        final AtomicBoolean alwaysCalled = new AtomicBoolean(false);
        Promise<String> promise = new Promise<String>(new ExecutorFunction() {
            public void call(Functor resolve, Functor reject) {
                resolve.call("value");
            }
        });

        promise.always(new Functor<Object, Object>() {
            public Object call(Object value) {
                alwaysCalled.set(true);
                return null;
            }
        });

        flushSerialCalls();
        assertTrue(alwaysCalled.get());
    }
}
