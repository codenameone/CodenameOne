package com.codename1.capture;

import com.codename1.io.Util;
import com.codename1.test.UITestBase;
import com.codename1.ui.Image;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.util.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CaptureTest extends UITestBase {
    private static final String ORIGINAL_PATH = "/tmp/photo.jpg";

    @BeforeEach
    void configureUtil() {
        Util.setImplementation(implementation);
    }

    @AfterEach
    void resetUtil() {
        Util.setImplementation(null);
        reset(implementation);
    }

    @Test
    void callBackStoresUrlFromActionEvent() {
        Capture.CallBack callBack = new Capture.CallBack();
        ActionEvent event = new ActionEvent(ORIGINAL_PATH);

        callBack.actionPerformed(event);

        assertEquals(ORIGINAL_PATH, callBack.url);
    }

    @Test
    void callBackSetsUrlToNullWhenEventIsNull() {
        Capture.CallBack callBack = new Capture.CallBack();

        callBack.actionPerformed(null);

        assertNull(callBack.url);
    }

    @Test
    void runSkipsProcessingWhenUrlIsNull() {
        Capture.CallBack callBack = new Capture.CallBack();
        callBack.actionPerformed(null);

        callBack.run();

        verify(implementation, never()).getImageIO();
    }

    @Test
    void runRescalesImageWhenDimensionsProvided() throws Exception {
        Capture.CallBack callBack = new Capture.CallBack();
        callBack.actionPerformed(new ActionEvent(ORIGINAL_PATH));
        setPrivateInt(callBack, "targetWidth", 120);
        setPrivateInt(callBack, "targetHeight", 80);

        StubImageIO imageIO = new StubImageIO();
        when(implementation.getImageIO()).thenReturn(imageIO);

        AtomicReference<String> savedPath = new AtomicReference<>();
        AtomicReference<String> deletedPath = new AtomicReference<>();
        when(implementation.openFileOutputStream(anyString())).thenAnswer(invocation -> {
            savedPath.set(invocation.getArgument(0));
            return new ByteArrayOutputStream();
        });
        doAnswer(invocation -> {
            deletedPath.set(invocation.getArgument(0));
            return null;
        }).when(implementation).deleteFile(anyString());
        when(implementation.openFileInputStream(anyString())).thenThrow(new IOException("Not expected"));

        callBack.run();

        String expectedScaledPath = "/tmp/photos.jpg";
        assertEquals(expectedScaledPath, callBack.url);
        assertEquals(expectedScaledPath, savedPath.get());
        assertEquals(ORIGINAL_PATH, deletedPath.get());
        assertEquals(Collections.singletonList(ORIGINAL_PATH), imageIO.savedFiles);
    }

    private void setPrivateInt(Object target, String fieldName, int value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setInt(target, value);
    }

    private static class StubImageIO extends ImageIO {
        private final List<String> savedFiles = new ArrayList<>();

        @Override
        public void save(String imageFilePath, OutputStream response, String format, int width, int height, float quality) {
            savedFiles.add(imageFilePath);
            try {
                response.write(1);
            } catch (IOException ignored) {
            }
        }

        @Override
        public void save(java.io.InputStream image, OutputStream response, String format, int width, int height, float quality) throws IOException {
            // Not used in this test
            response.write(1);
        }

        @Override
        protected void saveImage(Image img, OutputStream response, String format, float quality) throws IOException {
            response.write(1);
        }

        @Override
        public boolean isFormatSupported(String format) {
            return true;
        }
    }
}
