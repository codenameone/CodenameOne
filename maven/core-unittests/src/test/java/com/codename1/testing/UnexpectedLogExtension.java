package com.codename1.testing;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.annotation.AnnotatedElement;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Captures console output generated during each test execution and fails the test if any unexpected
 * messages are produced. This helps surface silent failures that only manifest via stack traces or
 * log statements printed to {@code System.out} or {@code System.err}.
 */
public class UnexpectedLogExtension implements BeforeEachCallback, AfterEachCallback {
    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(UnexpectedLogExtension.class);

    @Override
    public void beforeEach(ExtensionContext context) {
        CapturedStreams captured = new CapturedStreams(System.out, System.err);
        getStore(context).put(context.getUniqueId(), captured);

        System.setOut(captured.createInterceptingStream(captured.originalOut, captured.capturedOut));
        System.setErr(captured.createInterceptingStream(captured.originalErr, captured.capturedErr));
    }

    @Override
    public void afterEach(ExtensionContext context) {
        CapturedStreams captured =
                getStore(context).remove(context.getUniqueId(), CapturedStreams.class);
        if (captured == null) {
            return;
        }

        System.setOut(captured.originalOut);
        System.setErr(captured.originalErr);

        if (isOutputAllowed(context)) {
            return;
        }

        List<String> problems = new ArrayList<>();
        String stdout = new String(captured.capturedOut.toByteArray(), StandardCharsets.UTF_8);
        String stderr = new String(captured.capturedErr.toByteArray(), StandardCharsets.UTF_8);

        if (!stdout.trim().isEmpty()) {
            problems.add("System.out:\n" + stdout.trim());
        }
        if (!stderr.trim().isEmpty()) {
            problems.add("System.err:\n" + stderr.trim());
        }

        if (!problems.isEmpty()) {
            String message =
                    "Unexpected console output detected during test '"
                            + context.getDisplayName()
                            + "'.";
            throw new AssertionError(message + System.lineSeparator() + System.lineSeparator()
                    + String.join(System.lineSeparator() + System.lineSeparator(), problems));
        }
    }

    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(NAMESPACE);
    }

    private boolean isOutputAllowed(ExtensionContext context) {
        if (context.getElement().isPresent()
                && isAnnotatedWithAllow(context.getElement().get())) {
            return true;
        }
        Class<?> testClass = context.getTestClass().orElse(null);
        return testClass != null && isAnnotatedWithAllow(testClass);
    }

    private boolean isAnnotatedWithAllow(AnnotatedElement element) {
        AllowConsoleOutput annotation = element.getAnnotation(AllowConsoleOutput.class);
        return annotation != null && annotation.value();
    }

    private static class CapturedStreams {
        final PrintStream originalOut;
        final PrintStream originalErr;
        final ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();
        final ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();

        CapturedStreams(PrintStream originalOut, PrintStream originalErr) {
            this.originalOut = originalOut;
            this.originalErr = originalErr;
        }

        PrintStream createInterceptingStream(PrintStream original, ByteArrayOutputStream capture) {
            return new PrintStream(new TeeOutputStream(original, capture), true);
        }
    }

    private static class TeeOutputStream extends OutputStream {
        private final OutputStream first;
        private final OutputStream second;

        TeeOutputStream(OutputStream first, OutputStream second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public void write(int b) {
            try {
                first.write(b);
            } catch (Exception ignore) {
                // Swallow exceptions from the original stream to ensure we still capture the output.
            }
            try {
                second.write(b);
            } catch (Exception ignore) {
                // ByteArrayOutputStream should not fail, but swallow just in case.
            }
        }

        @Override
        public void write(byte[] b, int off, int len) {
            try {
                first.write(b, off, len);
            } catch (Exception ignore) {
                // Swallow to ensure output continues to be captured even if the original stream fails.
            }
            try {
                second.write(b, off, len);
            } catch (Exception ignore) {
                // Ignore failures while capturing.
            }
        }

        @Override
        public void flush() {
            try {
                first.flush();
            } catch (Exception ignore) {
                // Ignore flush failures on the original stream.
            }
            try {
                second.flush();
            } catch (Exception ignore) {
                // Ignore flush failures on the captured stream.
            }
        }
    }
}
