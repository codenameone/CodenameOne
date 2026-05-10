package com.codename1.ant;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

/**
 * @author srccodes.com
 * @version 1.0
 */
public class AntExecutor {
    /**
     * To execute the default target specified in the Ant build.xml file
     *
     * @param buildXmlFileFullPath
     */
    public static boolean executeAntTask(String buildXmlFileFullPath) {
        return executeAntTask(buildXmlFileFullPath, null, null);
    }

    /**
     * To execute a target specified in the Ant build.xml file
     *
     * @param buildXmlFileFullPath
     * @param target
     */
    public static boolean executeAntTask(String buildXmlFileFullPath, String target, Properties properties) {

        boolean success = false;

        // Tee stdout/stderr so that, on failure, we can recover server-reported
        // error details (such as the JSON body returned by the build server) that
        // the build client prints but does not propagate via the exception message.
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        PrintStream teeOut;
        PrintStream teeErr;
        try {
            teeOut = new PrintStream(new TeeOutputStream(originalOut, captured), true, "UTF-8");
            teeErr = new PrintStream(new TeeOutputStream(originalErr, captured), true, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 not supported", e);
        }

        DefaultLogger consoleLogger = new DefaultLogger();
        consoleLogger.setErrorPrintStream(teeErr);
        consoleLogger.setOutputPrintStream(teeOut);
        consoleLogger.setMessageOutputLevel(Project.MSG_INFO);

        System.setOut(teeOut);
        System.setErr(teeErr);

        try {
            // Prepare Ant project
            Project project = new Project();
            File buildFile = new File(buildXmlFileFullPath);

            project.setBasedir(buildFile.getParentFile().getAbsolutePath());
            project.setBaseDir(buildFile.getParentFile());

            project.setUserProperty("ant.file", buildFile.getAbsolutePath());
            if (properties != null) {
                for (String k : properties.stringPropertyNames()) {
                    project.setProperty(k, properties.getProperty(k));
                }
            }

            project.addBuildListener(consoleLogger);

            // Capture event for Ant script build start / stop / failure
            try {
                project.fireBuildStarted();
                project.init();
                ProjectHelper projectHelper = ProjectHelper.getProjectHelper();

                project.addReference("ant.projectHelper", projectHelper);

                projectHelper.parse(project, buildFile);

                // If no target specified then default target will be executed.
                String targetToExecute = (target != null && target.trim().length() > 0) ? target.trim() : project.getDefaultTarget();
                project.executeTarget(targetToExecute);
                project.fireBuildFinished(null);
                success = true;
            } catch (BuildException buildException) {
                project.fireBuildFinished(buildException);
                teeOut.flush();
                teeErr.flush();
                String capturedText;
                try {
                    capturedText = captured.toString("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalStateException("UTF-8 not supported", e);
                }
                String detail = extractServerErrorDetail(capturedText);
                StringBuilder message = new StringBuilder("Ant task failed: ").append(buildException.getMessage());
                if (detail != null) {
                    message.append(System.lineSeparator()).append(detail);
                }
                throw new RuntimeException(message.toString(), buildException);
            }
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }

        return success;
    }

    /**
     * Scans build output for server-reported error markers (HTTP status, response
     * message, JSON error body) and returns them joined by newlines, or {@code null}
     * if none were found.
     */
    static String extractServerErrorDetail(String log) {
        if (log == null || log.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        String[] lines = log.split("\\r?\\n");
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("Response message from server is:")
                    || line.startsWith("Server Detailed Error Message:")
                    || line.startsWith("Server returned HTTP response code:")
                    || line.contains("Server returned HTTP response code:")) {
                if (sb.length() > 0) {
                    sb.append(System.lineSeparator());
                }
                sb.append(line);
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    /**
     * OutputStream that writes to two underlying streams. Used to forward Ant
     * output to the original console while also retaining a copy for diagnostics.
     */
    private static final class TeeOutputStream extends OutputStream {
        private final OutputStream a;
        private final OutputStream b;

        TeeOutputStream(OutputStream a, OutputStream b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public void write(int byteValue) throws IOException {
            a.write(byteValue);
            b.write(byteValue);
        }

        @Override
        public void write(byte[] buf, int off, int len) throws IOException {
            a.write(buf, off, len);
            b.write(buf, off, len);
        }

        @Override
        public void flush() throws IOException {
            a.flush();
            b.flush();
        }

        @Override
        public void close() throws IOException {
            try {
                a.close();
            } finally {
                b.close();
            }
        }
    }
}
