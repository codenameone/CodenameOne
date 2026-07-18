package dart.async;

/**
 * The lowering target of Dart's {@code await} (M3 blocking model).
 *
 * <p>On the EDT the wait runs inside {@code Display.invokeAndBlock}, which
 * keeps dispatching events — other Dart callbacks, timers and UI input keep
 * running while this frame is parked, matching Dart's "other events run
 * during an await" semantics. Off the EDT (background threads, headless
 * tests) it is a plain monitor wait.</p>
 */
public final class Await {

    private Await() {
    }

    public static <T> T await$(final Future<T> f) {
        if (f == null) {
            throw new dart.core.TypeError("await on null Future");
        }
        if (f.isDone()) {
            return f.valueOrThrow();
        }
        if (com.codename1.ui.Display.isInitialized()
                && com.codename1.ui.Display.getInstance().isEdt()) {
            com.codename1.ui.Display.getInstance().invokeAndBlock(new Runnable() {
                @Override
                public void run() {
                    parkUntilDone(f);
                }
            });
        } else {
            parkUntilDone(f);
        }
        return f.valueOrThrow();
    }

    private static void parkUntilDone(Future<?> f) {
        synchronized (f.monitor()) {
            while (!f.isDone()) {
                try {
                    f.monitor().wait(500);
                } catch (InterruptedException ignore) {
                    // spurious wakeup handling via the loop condition
                }
            }
        }
    }
}
