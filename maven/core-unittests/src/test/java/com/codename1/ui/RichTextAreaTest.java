package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deep coverage for {@link RichTextArea}. The component routes every operation through the semantic
 * command/query channel of {@link AbstractEditorComponent}; these tests drive it against the
 * deterministic native editor backend provided by the test implementation so the full command, query,
 * event and ready-queue machinery is exercised without a real web view. A couple of tests additionally
 * verify the cross-platform {@code BrowserComponent} fallback selection.
 */
class RichTextAreaTest extends UITestBase {

    private void pump() {
        for (int i = 0; i < 6; i++) {
            flushSerialCalls();
        }
    }

    private RichTextArea showNativeEditor() {
        implementation.setEditorNativePeerSupported(true);
        RichTextArea editor = new RichTextArea();
        Form f = new Form("rt", new BorderLayout());
        f.add(BorderLayout.CENTER, editor);
        f.show();
        pump();
        return editor;
    }

    private List<String> cmds() {
        return implementation.getEditorCommands();
    }

    @FormTest
    void testNativeBackendSelectedAndReady() {
        RichTextArea editor = showNativeEditor();
        assertTrue(editor.isNativeEditor());
        assertTrue(editor.isEditorReady());
        assertEquals("richtext", implementation.getLastEditorType());
        assertNull(editor.getInternalBrowser());
    }

    @FormTest
    void testQueuedCommandsFlushAfterReady() {
        implementation.setEditorNativePeerSupported(true);
        RichTextArea editor = new RichTextArea();
        // issued before the backend is ready -> must be queued, not lost
        editor.setHtml("<p>queued</p>");
        editor.bold();
        assertFalse(editor.isEditorReady());
        Form f = new Form("rt", new BorderLayout());
        f.add(BorderLayout.CENTER, editor);
        f.show();
        pump();
        assertTrue(editor.isEditorReady());
        assertTrue(cmds().contains("setHtml:<p>queued</p>"));
        assertTrue(cmds().contains("bold:"));
    }

    @FormTest
    void testConstructorWithHtml() {
        implementation.setEditorNativePeerSupported(true);
        RichTextArea editor = new RichTextArea("<b>hello</b>");
        Form f = new Form("rt", new BorderLayout());
        f.add(BorderLayout.CENTER, editor);
        f.show();
        pump();
        assertTrue(cmds().contains("setHtml:<b>hello</b>"));
    }

    @FormTest
    void testInlineFormattingCommands() {
        RichTextArea editor = showNativeEditor();
        editor.bold();
        editor.italic();
        editor.underline();
        editor.strikeThrough();
        editor.removeFormat();
        editor.undo();
        editor.redo();
        List<String> c = cmds();
        assertTrue(c.contains("bold:"));
        assertTrue(c.contains("italic:"));
        assertTrue(c.contains("underline:"));
        assertTrue(c.contains("strikeThrough:"));
        assertTrue(c.contains("removeFormat:"));
        assertTrue(c.contains("undo:"));
        assertTrue(c.contains("redo:"));
    }

    @FormTest
    void testListsIndentAndAlignment() {
        RichTextArea editor = showNativeEditor();
        editor.insertOrderedList();
        editor.insertUnorderedList();
        editor.indent();
        editor.outdent();
        editor.justifyLeft();
        editor.justifyCenter();
        editor.justifyRight();
        List<String> c = cmds();
        assertTrue(c.contains("insertOrderedList:"));
        assertTrue(c.contains("insertUnorderedList:"));
        assertTrue(c.contains("indent:"));
        assertTrue(c.contains("outdent:"));
        assertTrue(c.contains("justifyLeft:"));
        assertTrue(c.contains("justifyCenter:"));
        assertTrue(c.contains("justifyRight:"));
    }

    @FormTest
    void testLinkCommands() {
        RichTextArea editor = showNativeEditor();
        editor.createLink("https://www.codenameone.com/");
        editor.removeLink();
        assertTrue(cmds().contains("createLink:https://www.codenameone.com/"));
        assertTrue(cmds().contains("unlink:"));
    }

    @FormTest
    void testColorConversionPadsToSixHexDigits() {
        RichTextArea editor = showNativeEditor();
        editor.setForegroundColor(0xff0000);
        editor.setForegroundColor(0x0000ff);
        editor.setForegroundColor(0x000000);
        editor.setHighlightColor(0x00ff00);
        List<String> c = cmds();
        assertTrue(c.contains("foreColor:#ff0000"));
        assertTrue(c.contains("foreColor:#0000ff"));
        assertTrue(c.contains("foreColor:#000000"));
        assertTrue(c.contains("hiliteColor:#00ff00"));
    }

    @FormTest
    void testBlockFormatAndFontSize() {
        RichTextArea editor = showNativeEditor();
        editor.setBlockFormat("h1");
        editor.setFontSize(5);
        assertTrue(cmds().contains("formatBlock:h1"));
        assertTrue(cmds().contains("fontSize:5"));
    }

    @FormTest
    void testInsertHtmlAndImage() {
        RichTextArea editor = showNativeEditor();
        editor.insertHtml("<span>x</span>");
        editor.insertImage("https://example.com/a.png");
        assertTrue(cmds().contains("insertHtml:<span>x</span>"));
        assertTrue(cmds().contains("insertImage:https://example.com/a.png"));
    }

    @FormTest
    void testPlaceholder() {
        RichTextArea editor = showNativeEditor();
        editor.setPlaceholder("Write something...");
        assertEquals("Write something...", editor.getPlaceholder());
        assertTrue(cmds().contains("setPlaceholder:Write something..."));
    }

    @FormTest
    void testEditableToggle() {
        RichTextArea editor = showNativeEditor();
        assertTrue(editor.isEditable());
        editor.setEditable(false);
        assertFalse(editor.isEditable());
        assertTrue(cmds().contains("setEditable:0"));
        editor.setEditable(true);
        assertTrue(editor.isEditable());
        assertTrue(cmds().contains("setEditable:1"));
    }

    @FormTest
    void testFocusAndBlur() {
        RichTextArea editor = showNativeEditor();
        editor.focusEditor();
        editor.blurEditor();
        assertTrue(cmds().contains("focus:"));
        assertTrue(cmds().contains("blur:"));
    }

    @FormTest
    void testGetHtmlQueryRoundTrip() {
        implementation.setEditorNativePeerSupported(true);
        implementation.setEditorQueryResponder(q -> q.equals("getHtml:") ? "<p>stored</p>" : null);
        RichTextArea editor = showNativeEditor();
        AtomicReference<String> result = new AtomicReference<>();
        editor.getHtml(result::set);
        assertEquals("<p>stored</p>", result.get());
    }

    @FormTest
    void testGetTextQueryRoundTrip() {
        implementation.setEditorNativePeerSupported(true);
        implementation.setEditorQueryResponder(q -> q.equals("getText:") ? "plain text" : null);
        RichTextArea editor = showNativeEditor();
        AtomicReference<String> result = new AtomicReference<>();
        editor.getText(result::set);
        assertEquals("plain text", result.get());
    }

    @FormTest
    void testChangeListenerFiresAndDetaches() {
        RichTextArea editor = showNativeEditor();
        AtomicInteger count = new AtomicInteger();
        ActionListener l = e -> count.incrementAndGet();
        editor.addChangeListener(l);
        editor.fireEditorEvent("change", null);
        editor.fireEditorEvent("change", null);
        assertEquals(2, count.get());
        editor.removeChangeListener(l);
        editor.fireEditorEvent("change", null);
        assertEquals(2, count.get());
    }

    @FormTest
    void testReadyListenerFiresImmediatelyWhenAlreadyReady() {
        RichTextArea editor = showNativeEditor();
        AtomicInteger count = new AtomicInteger();
        editor.addReadyListener(e -> count.incrementAndGet());
        assertEquals(1, count.get());
    }

    @FormTest
    void testReadyListenerFiresAfterInitialization() {
        implementation.setEditorNativePeerSupported(true);
        RichTextArea editor = new RichTextArea();
        AtomicInteger count = new AtomicInteger();
        editor.addReadyListener(e -> count.incrementAndGet());
        assertEquals(0, count.get());
        Form f = new Form("rt", new BorderLayout());
        f.add(BorderLayout.CENTER, editor);
        f.show();
        pump();
        assertEquals(1, count.get());
    }

    // --- cross platform BrowserComponent fallback ---

    @FormTest
    void testBrowserFallbackSelectedWhenNoNativePeer() {
        // editorNativePeerSupported defaults to false -> no native peer -> browser fallback
        RichTextArea editor = new RichTextArea();
        Form f = new Form("rt", new BorderLayout());
        f.add(BorderLayout.CENTER, editor);
        f.show();
        pump();
        assertFalse(editor.isNativeEditor());
        assertNotNull(editor.getInternalBrowser());
    }

    @FormTest
    void testBrowserFallbackBecomesReadyOnLoadAndRoutesCommands() {
        RichTextArea editor = new RichTextArea();
        Form f = new Form("rt", new BorderLayout());
        f.add(BorderLayout.CENTER, editor);
        f.show();
        pump();
        assertFalse(editor.isEditorReady());
        // simulate the web view finishing load -> editor becomes ready
        editor.getInternalBrowser().fireWebEvent(BrowserComponent.onLoad, new com.codename1.ui.events.ActionEvent(editor));
        pump();
        assertTrue(editor.isEditorReady());
        editor.bold();
        pump();
        boolean routed = false;
        for (String s : implementation.getBrowserExecuted()) {
            if (s.contains("cn1editor.cmd") && s.contains("bold")) {
                routed = true;
                break;
            }
        }
        assertTrue(routed, "bold() should be routed to the browser backend as a cn1editor.cmd call");
    }
}
