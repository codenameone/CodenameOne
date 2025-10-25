package com.codename1.io;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class LogTest {
    private Log originalLog;
    private CodenameOneImplementation originalImplementation;
    private CodenameOneImplementation mockImplementation;
    private TestLog testLog;
    private Field initializedField;
    private boolean originalInitialized;
    private final List<String> systemOutMessages = new ArrayList<>();
    private final AtomicReference<ActionListener> listenerRef = new AtomicReference<>();

    @BeforeEach
    void setup() throws Exception {
        originalLog = Log.getInstance();
        originalImplementation = Util.getImplementation();

        mockImplementation = mock(CodenameOneImplementation.class);
        doAnswer(invocation -> {
            systemOutMessages.add(invocation.getArgument(0, String.class));
            return null;
        }).when(mockImplementation).systemOut(anyString());
        doAnswer(invocation -> null).when(mockImplementation).cleanup(any());
        doAnswer(invocation -> {
            Writer writer = invocation.getArgument(1);
            writer.write("stack");
            return null;
        }).when(mockImplementation).printStackTraceToStream(any(), any());
        doAnswer(invocation -> {
            listenerRef.set(invocation.getArgument(0));
            return null;
        }).when(mockImplementation).setLogListener(any());

        Util.setImplementation(mockImplementation);

        testLog = new TestLog();
        Log.install(testLog);

        initializedField = Log.class.getDeclaredField("initialized");
        initializedField.setAccessible(true);
        originalInitialized = initializedField.getBoolean(null);
        initializedField.setBoolean(null, true);

        systemOutMessages.clear();
        listenerRef.set(null);
        Log.setLevel(Log.DEBUG);
    }

    @AfterEach
    void tearDown() throws Exception {
        Log.install(originalLog);
        Util.setImplementation(originalImplementation);
        if (initializedField != null) {
            initializedField.setBoolean(null, originalInitialized);
        }
        Log.setLevel(Log.DEBUG);
    }

    @Test
    void printRespectsLogLevelAndWritesToWriter() throws IOException {
        testLog.resetContent();
        systemOutMessages.clear();
        Log.setLevel(Log.WARNING);

        Log.p("ignored", Log.INFO);
        assertEquals("", testLog.getWrittenContent());

        Log.p("accepted", Log.ERROR);
        String content = testLog.getWrittenContent();
        assertTrue(content.contains("accepted"));
        assertTrue(systemOutMessages.stream().anyMatch(s -> s.contains("accepted")));
    }

    @Test
    void setFileURLRecreatesWriter() throws IOException {
        testLog.resetContent();
        Log.p("initial", Log.INFO);
        int createdInitially = testLog.getCreateWriterCalls();

        testLog.setFileURL("file:///tmp/log-a.txt");
        int afterUpdate = testLog.getCreateWriterCalls();
        assertTrue(afterUpdate > createdInitially);

        testLog.resetContent();
        Log.p("after", Log.INFO);
        assertTrue(testLog.getWrittenContent().contains("after"));
    }

    @Test
    void trackFileSystemLogsEvents() throws IOException {
        Log.p("warmup", Log.INFO);
        testLog.resetContent();
        systemOutMessages.clear();

        testLog.trackFileSystem();
        ActionListener listener = listenerRef.get();
        assertNotNull(listener);

        listener.actionPerformed(new ActionEvent("stream opened"));

        String content = testLog.getWrittenContent();
        assertTrue(content.contains("stream opened"));
        assertTrue(systemOutMessages.stream().anyMatch(s -> s.contains("stream opened")));
    }

    private static class TestLog extends Log {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private OutputStreamWriter writer = new OutputStreamWriter(buffer, StandardCharsets.UTF_8);
        private int createWriterCalls;

        @Override
        protected Writer createWriter() {
            createWriterCalls++;
            writer = new OutputStreamWriter(buffer, StandardCharsets.UTF_8);
            return writer;
        }

        void resetContent() throws IOException {
            writer.flush();
            buffer.reset();
        }

        String getWrittenContent() throws IOException {
            writer.flush();
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }

        int getCreateWriterCalls() {
            return createWriterCalls;
        }
    }
}
