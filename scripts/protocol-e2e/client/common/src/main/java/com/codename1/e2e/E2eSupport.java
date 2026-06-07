package com.codename1.e2e;

/** Small shared helpers for the protocol e2e tests. */
public final class E2eSupport {
    private E2eSupport() {}

    /** Base URL of the running Spring Boot test server. */
    public static String baseUrl() {
        String u = System.getProperty("e2e.server.url");
        return (u == null || u.length() == 0) ? "http://localhost:8080" : u;
    }

    /** Blocks the (off-EDT) test thread until {@code done[0]} flips or the timeout elapses. */
    public static void await(boolean[] done) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 60000;
        while (!done[0] && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
    }
}
