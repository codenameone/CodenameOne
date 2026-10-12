package com.demo;

import com.codename1.backend.Backend;
import com.codename1.backend.Config;
import com.codename1.backend.HttpServer;
import com.codename1.backend.Tasks;
import com.codename1.backend.VirtualThread;
import com.codename1.io.Process;
import com.codename1.io.ProcessBuilder;
import java.util.Properties;

/** A blocked process pipe/wait must let another task run on the sole host. */
public final class ProcessVirtualConformance {
    private static volatile boolean entered;
    private static volatile boolean marker;
    private static volatile boolean finished;
    private static volatile boolean virtual;
    public static void main(String[] args) throws Exception {
        Backend backend = Backend.builder(Config.of(new Properties(), "test"))
                .quiet().host("127.0.0.1").port(0).workers(1)
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) {
                        return HttpServer.Response.text(200, "ok");
                    }
                }).start();
        try {
            for (int mode = 0; mode < 2; ++mode) {
                entered = marker = finished = virtual = false;
                System.out.println("process virtual mode=" + mode);
                final boolean pipe = mode == 0;
                final Process child = new ProcessBuilder(args[0], "sleep").start();
                try {
                    Tasks.virtual(new Runnable() {
                        public void run() {
                            virtual = VirtualThread.isVirtual();
                            entered = true;
                            try {
                                if (pipe) child.getInputStream().read();
                                else child.waitFor();
                            } catch (Exception expectedOnClose) { System.out.println("process task closed: " + expectedOnClose); }
                            finally { finished = true; System.out.println("process task finished"); }
                        }
                    });
                    long deadline = System.currentTimeMillis() + 5000;
                    while (!entered && System.currentTimeMillis() < deadline) Thread.sleep(1);
                    if (!entered || !virtual) throw new AssertionError("not running on a virtual thread");
                    Tasks.virtual(new Runnable() { public void run() { marker = true; } });
                    deadline = System.currentTimeMillis() + 2000;
                    while (!marker && System.currentTimeMillis() < deadline) Thread.sleep(1);
                    System.out.println("process marker=" + marker);
                    if (!marker) throw new AssertionError("process operation pinned the sole host");
                } finally {
                    child.close();
                    long deadline = System.currentTimeMillis() + 5000;
                    while (!finished && System.currentTimeMillis() < deadline) Thread.sleep(1);
                }
                if (!finished) throw new AssertionError("task did not finish after close");
            }
        } finally { backend.stop(); }
        System.out.println("PROCESS_VIRTUAL_CONFORMANCE_OK");
    }
}
