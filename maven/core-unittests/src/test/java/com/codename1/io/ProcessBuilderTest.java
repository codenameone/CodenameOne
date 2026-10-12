package com.codename1.io;

import com.codename1.impl.ImplementationFactory;
import com.codename1.impl.process.ProcessSupport;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProcessBuilderTest {
    private final ImplementationFactory previous = ImplementationFactory.getInstance();
    @AfterEach void restore() { ImplementationFactory.setInstance(previous); }

    @Test void unsupportedPortNeedsNoDisplayAndFailsExplicitly() {
        ImplementationFactory.setInstance(new ImplementationFactory() {
            public Object createProcessSupport() { return null; }
        });
        assertFalse(ProcessBuilder.isSupported());
        ProcessBuilder builder = new ProcessBuilder("tool");
        assertThrows(UnsupportedOperationException.class, builder::start);
        assertThrows(UnsupportedOperationException.class, builder::environment);
    }

    @Test void validatesMutatedCommandAndEnvironmentBeforeNativeBoundary() {
        final int[] calls = {0};
        ImplementationFactory.setInstance(new ImplementationFactory() {
            public Object createProcessSupport() {
                return new ProcessSupport() {
                    public Map<String, String> environment() { return new HashMap<String, String>(); }
                    public Process start(String[] args, String dir, Map<String, String> env, boolean merge) throws IOException {
                        calls[0]++; throw new IOException("adapter reached");
                    }
                };
            }
        });
        ProcessBuilder builder = new ProcessBuilder("tool");
        builder.command().add("bad\0arg");
        assertThrows(IllegalArgumentException.class, builder::start);
        builder.command("tool");
        builder.environment().put("KEY", "value");
        builder.environment().entrySet().iterator().next().setValue("bad\0value");
        assertThrows(IllegalArgumentException.class, builder::start);
        builder.environment().clear();
        builder.environment().put("KEY=bad", "value");
        assertThrows(IllegalArgumentException.class, builder::start);
        builder.environment().clear();
        builder.environment().put("KEY", null);
        assertThrows(NullPointerException.class, builder::start);
        assertEquals(0, calls[0]);
        builder.environment().clear();
        assertThrows(IOException.class, builder::start);
        assertEquals(1, calls[0]);
    }
}
