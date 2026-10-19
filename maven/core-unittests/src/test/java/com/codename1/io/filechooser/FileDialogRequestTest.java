package com.codename1.io.filechooser;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FileDialogRequestTest {

    @Test
    void filterNormalizesExtensionsAndMatchesNames() {
        FileTypeFilter f = new FileTypeFilter("Images").extensions(".PNG", " jpg ", "", null);
        assertArrayEquals(new String[]{"PNG", "jpg"}, f.getExtensions());
        assertTrue(f.acceptsName("a.png"));
        assertTrue(f.acceptsName("/x/b.JPG"));
        assertFalse(f.acceptsName("png"));
        assertFalse(f.acceptsName("a.jpeg"));
        assertTrue(new FileTypeFilter("All").acceptsName("anything"));
    }

    @Test
    void acceptStringJoinsExtensionsAndMimeTypes() {
        OpenFileRequest r = new OpenFileRequest()
                .addFilter(new FileTypeFilter("PDF").extensions("pdf").mimeTypes("application/pdf"))
                .addFilter(new FileTypeFilter("Text").extensions("txt"));
        assertEquals("pdf,application/pdf,txt", r.toAcceptString());
        assertNull(new OpenFileRequest().toAcceptString());
    }

    @Test
    void selectedFilterIsClamped() {
        SaveFileRequest r = new SaveFileRequest();
        assertEquals(-1, r.getSelectedFilter());
        r.addFilter(new FileTypeFilter("A")).addFilter(new FileTypeFilter("B")).selectedFilter(1);
        assertEquals(1, r.getSelectedFilter());
        r.selectedFilter(7);
        assertEquals(0, r.getSelectedFilter());
    }

    @Test
    void hintsAreTyped() {
        OpenFileRequest r = new OpenFileRequest()
                .hint(FileDialogHints.MAC_SHOWS_HIDDEN_FILES, Boolean.TRUE)
                .hint(FileDialogHints.WEB_START_IN, "documents")
                .hint("x.flag", "TRUE");
        assertTrue(r.getBooleanHint(FileDialogHints.MAC_SHOWS_HIDDEN_FILES, false));
        assertTrue(r.getBooleanHint("x.flag", false));
        assertTrue(r.getBooleanHint("missing", true));
        assertEquals("documents", r.getStringHint(FileDialogHints.WEB_START_IN));
        assertNull(r.getStringHint("missing"));
    }

    @Test
    void resultFillsOptionDefaultsAndFilter() {
        SaveFileRequest r = new SaveFileRequest()
                .addFilter(new FileTypeFilter("PDF").extensions("pdf"))
                .addFilter(new FileTypeFilter("Text").extensions("txt"))
                .addOption(FileDialogOption.checkbox("charts", "Charts", true))
                .addOption(FileDialogOption.choice("q", "Quality", "High", "Low"))
                .addOption(FileDialogOption.text("t", "Title", "x"));
        Map<String, String> chosen = new HashMap<String, String>();
        chosen.put("q", "Low");
        FileDialogResult res = new FileDialogResult(r, new String[]{"/a/b.txt"}, 1, chosen);
        assertFalse(res.isCancelled());
        assertEquals("/a/b.txt", res.getPath());
        assertEquals("Text", res.getSelectedFilter().getLabel());
        assertEquals("true", res.getOptionValue("charts"));
        assertEquals("Low", res.getOptionValue("q"));
        assertEquals("x", res.getOptionValue("t"));

        FileDialogResult cancelled = FileDialogResult.cancelled(r);
        assertTrue(cancelled.isCancelled());
        assertNull(cancelled.getPath());
        assertEquals(0, cancelled.getSelectedFilterIndex());
        assertEquals("High", cancelled.getOptionValue("q"));
    }

    @Test
    void writeContentPrefersTheWriterAndFailsWithoutContent() throws IOException {
        SaveFileRequest r = new SaveFileRequest().sourceFile("/never/read").content(new SaveContent() {
            @Override
            public void write(FileDialogResult result, OutputStream out) throws IOException {
                out.write(result.getPath().getBytes("UTF-8"));
            }
        });
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        r.writeContent(new FileDialogResult(r, new String[]{"p"}, -1, null), out);
        assertEquals("p", out.toString("UTF-8"));

        SaveFileRequest empty = new SaveFileRequest();
        assertThrows(IOException.class, () -> empty.writeContent(FileDialogResult.cancelled(empty), out));
    }
}
