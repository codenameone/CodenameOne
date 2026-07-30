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

    /**
     * A thread blocked in {@code get()} wakes when the resource is cancelled.
     *
     * <p>{@code cancel} used to call {@code setChanged()} and stop there,
     * without {@code notifyObservers()}. {@code get()} adds an observer,
     * checks {@code isDone()}, then waits -- so a cancel landing after that
     * check set a flag nobody was watching for and woke nobody, and the
     * waiter blocked forever on a resource that had already finished.</p>
     *
     * <p>Run on a worker with a bounded join, so the old behaviour shows up as
     * this test failing rather than as the suite hanging.</p>
     */
    @FormTest
    void aCancelWakesAThreadWaitingInGet() throws Exception {
        final AsyncResource<String> res = new AsyncResource<String>();
        final boolean[] returned = new boolean[1];
        final Throwable[] thrown = new Throwable[1];
        Thread waiter = new Thread(new Runnable() {
            public void run() {
                try {
                    res.get();
                } catch (Throwable t) {
                    thrown[0] = t;
                }
                returned[0] = true;
            }
        });
        waiter.setDaemon(true);
        waiter.start();

        // Let it get past the isDone() check and into the wait.
        Thread.sleep(150L);
        assertFalse(returned[0], "the waiter must still be blocked");

        res.cancel(true);
        waiter.join(5000L);

        assertTrue(returned[0],
                "a cancelled resource must not leave get() blocked");
        assertNotNull(thrown[0], "and the waiter learns it was cancelled");
    }

    /**
     * Cancellation wakes waiters without publishing to the caller.
     *
     * <p>Both halves matter and they pull in opposite directions. The waiter
     * has to be released, or {@code get()} blocks forever. The callbacks must
     * <em>not</em> run: cancelling means the caller stopped listening, and the
     * framework tests that contract elsewhere -- the AI language and vision
     * suites assert a cancelled operation delivers neither a value nor an
     * error even when the backend answers afterwards. A first attempt at
     * fixing the hang fired the error callback too, and those suites caught
     * it.</p>
     */
    @FormTest
    void aCancelDoesNotPublishToTheCaller() {
        AsyncResource<String> res = new AsyncResource<String>();
        final boolean[] delivered = new boolean[1];
        RecordingErrorCallback failure = new RecordingErrorCallback();
        res.ready(new SuccessCallback<String>() {
            public void onSucess(String value) {
                delivered[0] = true;
            }
        }).except(failure);

        assertTrue(res.cancel(true));

        assertFalse(delivered[0], "a cancelled caller gets no value");
        assertNull(failure.lastError, "and no error either");
        assertTrue(res.isCancelled());
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
