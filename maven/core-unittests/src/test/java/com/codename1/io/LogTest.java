package com.codename1.io;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class LogTest extends UITestBase {
    private Log originalLog;
    private TestLog testLog;

    @BeforeEach
    void installTestLog() throws Exception {
        originalLog = Log.getInstance();
        testLog = new TestLog();
        Log.install(testLog);
        Log.setLevel(Log.DEBUG);
        implementation.clearSystemOutMessages();
    }

    @AfterEach
    void restoreLog() {
        Log.install(originalLog);
        Log.setLevel(Log.DEBUG);
        implementation.clearSystemOutMessages();
    }

    @FormTest
    void printRespectsLogLevelAndWritesToWriter() throws Exception {
        testLog.resetContent();
        implementation.clearSystemOutMessages();
        Log.setLevel(Log.WARNING);

        Log.p("ignored", Log.INFO);
        assertEquals("", testLog.getWrittenContent());

        Log.p("accepted", Log.ERROR);
        String content = testLog.getWrittenContent();
        assertTrue(content.contains("accepted"));
        assertTrue(containsMessage(implementation, "accepted"));
    }

    @FormTest
    void setFileURLRecreatesWriter() throws Exception {
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

    @FormTest
    void trackFileSystemLogsEvents() throws Exception {
        Log.p("warmup", Log.INFO);
        testLog.resetContent();
        implementation.clearSystemOutMessages();

        testLog.trackFileSystem();
        implementation.fireLogEvent("stream opened");

        String content = testLog.getWrittenContent();
        assertTrue(content.contains("stream opened"));
        assertTrue(containsMessage(implementation, "stream opened"));
    }

    private boolean containsMessage(TestCodenameOneImplementation impl, String text) {
        for (String message : impl.getSystemOutMessages()) {
            if (message.contains(text)) {
                return true;
            }
        }
        return false;
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
