package com.codename1.util;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.util.SuccessCallback;

import static org.junit.jupiter.api.Assertions.*;

class AsyncResourceTest extends UITestBase {

    @FormTest
    void allCompletesWhenAllResourcesFinish() throws Exception {
        AsyncResource<String> first = new AsyncResource<String>();
        AsyncResource<String> second = new AsyncResource<String>();
        AsyncResource<Boolean> combined = AsyncResource.all(first, second);
        RecordingCallback success = new RecordingCallback();
        combined.ready(success);

        first.complete("one");
        assertFalse(success.invoked);
        second.complete("two");
        assertTrue(success.invoked);

        AsyncResource.await(first, second);
    }

    @FormTest
    void errorPropagatesThroughCombinedResources() {
        AsyncResource<String> first = new AsyncResource<String>();
        AsyncResource<String> second = new AsyncResource<String>();
        AsyncResource<Boolean> combined = AsyncResource.all(first, second);
        RecordingErrorCallback errors = new RecordingErrorCallback();
        combined.except(errors);

        RuntimeException failure = new RuntimeException("boom");
        second.error(failure);
        assertSame(failure, errors.lastError);

        AsyncResource.AsyncExecutionException ex = assertThrows(AsyncResource.AsyncExecutionException.class,
                () -> AsyncResource.await(first, second));
        assertSame(failure, ex.getCause());
        assertFalse(AsyncResource.isCancelled(ex));
        assertTrue(AsyncResource.isCancelled(new AsyncResource.AsyncExecutionException(new AsyncResource.CancellationException())));
    }

    private static class RecordingCallback implements SuccessCallback<Boolean> {
        private boolean invoked;

        public void onSucess(Boolean value) {
            invoked = true;
        }
    }

    private static class RecordingErrorCallback implements SuccessCallback<Throwable> {
        private Throwable lastError;

        public void onSucess(Throwable value) {
            lastError = value;
        }
    }
}
