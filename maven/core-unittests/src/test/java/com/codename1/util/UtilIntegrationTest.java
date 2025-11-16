package com.codename1.util;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class UtilIntegrationTest extends UITestBase {

    @FormTest
    void asyncResourceSupportsErrorAndSuccessPaths() throws AsyncExecutionException {
        AsyncResource<String> resource = new AsyncResource<String>();
        AtomicBoolean successCalled = new AtomicBoolean();
        AtomicBoolean errorCalled = new AtomicBoolean();
        resource.onSuccess(new SuccessCallback<String>() {
            public void onSucess(String value) {
                successCalled.set(true);
            }
        });
        resource.onFail(new FailureCallback() {
            public void onError(Object error) {
                errorCalled.set(true);
            }
        });

        resource.complete("done");
        assertTrue(successCalled.get());
        assertEquals("done", resource.get());

        AsyncResource<String> failing = new AsyncResource<String>();
        failing.onFail(new FailureCallback() {
            public void onError(Object error) {
                errorCalled.set(true);
            }
        });
        failing.error(new RuntimeException("boom"));
        assertTrue(errorCalled.get());
        assertThrows(AsyncExecutionException.class, failing);
    }

    @FormTest
    void asyncResourceAllAggregatesAndAwaits() throws AsyncExecutionException {
        AsyncResource<String> first = new AsyncResource<String>();
        AsyncResource<String> second = new AsyncResource<String>();
        AsyncResource<Boolean> combined = AsyncResource.all(first, second);
        AtomicInteger completion = new AtomicInteger();
        combined.onSuccess(new SuccessCallback<Boolean>() {
            public void onSucess(Boolean value) {
                completion.incrementAndGet();
            }
        });

        first.complete("a");
        assertEquals(0, completion.get());
        second.complete("b");
        assertEquals(1, completion.get());
        AsyncResource.await(Arrays.asList(first, second));
    }

    @FormTest
    void stringUtilAndMathUtilHelpersWork() {
        List<String> tokens = StringUtil.tokenize("a,b,c", ",");
        assertEquals(3, tokens.size());
        String joined = StringUtil.join(tokens, "-");
        assertEquals("a-b-c", joined);

        int clamp = MathUtil.clamp(15, 0, 10);
        assertEquals(10, clamp);
        int lerp = MathUtil.lerp(0, 10, 0.5f);
        assertEquals(5, lerp);
    }
}
