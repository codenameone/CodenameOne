package com.codename1.junit;

import com.codename1.io.Log;

import java.util.ArrayList;
import java.util.List;

public class TestLogger extends Log {
    private List<Throwable> throwables = new ArrayList<>();
    private List<String> printed = new ArrayList<>();
    private static Log original;
    public static void install() {
        if(getInstance() instanceof TestLogger) {
            throw new IllegalStateException("Test logger already installed");
        }
        original = getInstance();
        install(new TestLogger());
    }

    public static void remove() {
        install(original);
    }

    @Override
    protected void logThrowable(Throwable t) {
        throwables.add(t);
    }

    @Override
    protected void print(String text, int level) {
        printed.add(text);
    }

    public static List<String> getPrinted() {
        return ((TestLogger)getInstance()).printed;
    }

    public static List<Throwable> getThrowables() {
        return ((TestLogger)getInstance()).throwables;
    }
}
