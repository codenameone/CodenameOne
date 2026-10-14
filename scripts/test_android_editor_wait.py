#!/usr/bin/env python3
"""Compile the real Android editor handoff and control its thread interleavings.

Only the Activity and editor are stubs. A hook immediately before the caller's
monitor acquisition schedules completion in the old check-before-wait gap.
"""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


class AndroidEditorWaitTest(unittest.TestCase):
    def test_completion_handoff(self):
        source = (ROOT / "Ports/Android/src/com/codename1/impl/android/AndroidImplementation.java").read_text()
        start = source.index("    public static void stopEditing(final boolean forceVKBClose)")
        method = source[start:source.index("\n    @Override", start)]
        position = method.rindex("synchronized (flag)")
        method = method[:position] + "beforeWaitMonitor(flag);\n            " + method[position:]
        harness = r'''
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
public class EditorWaitTest {
    static String mode;
    static Activity activity;
    static volatile Runnable callback;
    static volatile boolean[] completion;
    static volatile boolean stopped;
    static CountDownLatch acquired;
    static class Activity {
        void runOnUiThread(Runnable action) {
            callback = action;
            if (mode.equals("inline")) action.run();
        }
    }
    static Activity getActivity() { return activity; }
    static class InPlaceEditView {
        static void stopEdit(boolean close) { check(close, "close flag lost"); stopped = true; }
    }
    static void beforeWaitMonitor(boolean[] flag) {
        completion = flag;
        if (mode.equals("gap")) callback.run();
        acquired.countDown();
    }
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    static void awaitWaiting(Thread caller) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (caller.isAlive() && caller.getState() != Thread.State.WAITING
                && System.nanoTime() < deadline) Thread.sleep(1);
        check(caller.getState() == Thread.State.WAITING, "caller did not await completion");
    }
METHOD
    public static void main(String[] args) throws Exception {
        mode = args[0];
        activity = mode.equals("no-activity") ? null : new Activity();
        acquired = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        Thread caller = new Thread(() -> {
            try {
                stopEditing(true);
                check(activity == null || stopped, "returned before editor stopped");
                if (mode.equals("interrupt")) check(Thread.currentThread().isInterrupted(), "interrupt status lost");
            } catch (Throwable error) { failure.set(error); }
        });
        caller.setDaemon(true); // A broken handoff must fail, not hang the test JVM.
        caller.start();
        if (mode.equals("delayed") || mode.equals("spurious") || mode.equals("interrupt")) {
            check(acquired.await(2, TimeUnit.SECONDS), "caller did not reach handoff");
            awaitWaiting(caller);
            if (mode.equals("spurious")) {
                synchronized (completion) { completion.notifyAll(); }
            } else if (mode.equals("interrupt")) {
                caller.interrupt();
            }
            if (!mode.equals("delayed")) {
                Thread.sleep(50);
                check(caller.isAlive(), "wake without completion released caller");
                awaitWaiting(caller);
            }
            callback.run();
        }
        caller.join(2000);
        check(!caller.isAlive(), "lost editor completion notification");
        if (failure.get() != null) throw new AssertionError(failure.get());
    }
}
'''.replace("METHOD", method)
        java_home = os.environ.get("JAVA_HOME")
        javac = str(Path(java_home) / "bin/javac") if java_home else "javac"
        java = str(Path(java_home) / "bin/java") if java_home else "java"
        with tempfile.TemporaryDirectory(prefix="cn1-editor-wait-") as directory:
            test = Path(directory) / "EditorWaitTest.java"
            test.write_text(harness)
            subprocess.run([javac, "-encoding", "UTF-8", str(test)], check=True)
            for mode in ("gap", "delayed", "inline", "spurious", "interrupt", "no-activity"):
                with self.subTest(mode=mode):
                    subprocess.run([java, "-cp", directory, "EditorWaitTest", mode],
                                   check=True, timeout=10)


if __name__ == "__main__":
    unittest.main()
