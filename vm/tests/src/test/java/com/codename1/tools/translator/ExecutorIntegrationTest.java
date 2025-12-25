package com.codename1.tools.translator;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ExecutorIntegrationTest {

    @Test
    public void testExecutorService() throws Exception {
        String code = "package test;\n" +
            "import java.util.concurrent.*;\n" +
            "import java.util.*;\n" +
            "public class Main {\n" +
            "    static class DirectExecutorService extends AbstractExecutorService {\n" +
            "        private boolean shutdown;\n" +
            "        public void execute(Runnable command) {\n" +
            "            if (shutdown) throw new RejectedExecutionException();\n" +
            "            command.run();\n" +
            "        }\n" +
            "        public void shutdown() { shutdown = true; }\n" +
            "        public List<Runnable> shutdownNow() { shutdown = true; return new ArrayList<Runnable>(); }\n" +
            "        public boolean isShutdown() { return shutdown; }\n" +
            "        public boolean isTerminated() { return shutdown; }\n" +
            "        public boolean awaitTermination(long timeout, TimeUnit unit) { return true; }\n" +
            "    }\n" +
            "    public static void main(String[] args) throws Exception {\n" +
            "        ExecutorService executor = new DirectExecutorService();\n" +
            "        Future<String> f1 = executor.submit(new Callable<String>() {\n" +
            "            public String call() throws Exception {\n" +
            "                return \"Hello\";\n" +
            "            }\n" +
            "        });\n" +
            "        Future<String> f2 = executor.submit(new Callable<String>() {\n" +
            "            public String call() throws Exception {\n" +
            "                return \"World\";\n" +
            "            }\n" +
            "        });\n" +
            "        System.out.println(f1.get() + \" \" + f2.get());\n" +
            "        executor.shutdown();\n" +
            "    }\n" +
            "}";

        assertTrue(CompilerHelper.compileAndRun(code, "Hello World"));
    }

    @Test
    public void testSingleThreadExecutor() throws Exception {
        String code = "package test;\n" +
            "import java.util.concurrent.*;\n" +
            "import java.util.concurrent.atomic.AtomicInteger;\n" +
            "import java.util.*;\n" +
            "public class Main {\n" +
            "    static class DirectExecutorService extends AbstractExecutorService {\n" +
            "        private boolean shutdown;\n" +
            "        public void execute(Runnable command) {\n" +
            "            if (shutdown) throw new RejectedExecutionException();\n" +
            "            command.run();\n" +
            "        }\n" +
            "        public void shutdown() { shutdown = true; }\n" +
            "        public List<Runnable> shutdownNow() { shutdown = true; return new ArrayList<Runnable>(); }\n" +
            "        public boolean isShutdown() { return shutdown; }\n" +
            "        public boolean isTerminated() { return shutdown; }\n" +
            "        public boolean awaitTermination(long timeout, TimeUnit unit) { return true; }\n" +
            "    }\n" +
            "    public static void main(String[] args) throws Exception {\n" +
            "        ExecutorService executor = new DirectExecutorService();\n" +
            "        final AtomicInteger counter = new AtomicInteger(0);\n" +
            "        Runnable task = new Runnable() {\n" +
            "            public void run() {\n" +
            "                counter.set(counter.get() + 1);\n" +
            "            }\n" +
            "        };\n" +
            "        Future<?> f1 = executor.submit(task);\n" +
            "        Future<?> f2 = executor.submit(task);\n" +
            "        f1.get();\n" +
            "        f2.get();\n" +
            "        System.out.println(counter.get());\n" +
            "        executor.shutdown();\n" +
            "    }\n" +
            "}";

        assertTrue(CompilerHelper.compileAndRun(code, "2"));
    }
}
