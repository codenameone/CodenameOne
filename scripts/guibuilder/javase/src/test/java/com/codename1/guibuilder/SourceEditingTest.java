package com.codename1.guibuilder;

import com.codename1.ui.TextInputClient;
import com.codename1.ui.TextInputConfig;
import com.codename1.ui.TextInputState;
import com.codename1.ui.editor.CodePureEditor;
import com.codename1.ui.editor.CodeView;
import com.codename1.ui.editor.EditorHost;
import javax.swing.JPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SourceEditingTest {
    @BeforeAll static void init() {
        if (!com.codename1.ui.Display.isInitialized()) com.codename1.ui.Display.init(new JPanel());
    }

    private static final String SOURCE =
            "// <gui-builder-generated>\n"
            + "package com.example;\n"
            + "public class F {\n"
            + "// </gui-builder-generated>\n"
            + "// <gui-builder-user-code>\n"
            + "\n"
            + "// </gui-builder-user-code>\n"
            + "// <gui-builder-generated>\n}\n// </gui-builder-generated>\n";

    @Test void typingInsideTheUserRegionIsAccepted() {
        CodePureEditor editor = new CodePureEditor(new Host(), "java");
        CodeView view = (CodeView) editor.getView();
        editor.cmd("setText", SOURCE);
        editor.cmd("setProtectedMarkers", "// <gui-builder-generated>\n// </gui-builder-generated>");
        int userOffset = SOURCE.indexOf("// <gui-builder-user-code>") + "// <gui-builder-user-code>".length() + 1;
        view.replaceRange(userOffset, userOffset, "int typed = 1;");
        assertTrue(editor.query("getText", null).contains("int typed = 1;"),
                "typing in the user region must work");
    }

    @Test void typingWhereTheCaretStartsIsAccepted() {
        CodePureEditor editor = new CodePureEditor(new Host(), "java");
        CodeView view = (CodeView) editor.getView();
        editor.cmd("setText", SOURCE);
        editor.cmd("setProtectedMarkers", "// <gui-builder-generated>\n// </gui-builder-generated>");
        // Offset 0 is where the caret sits until something moves it, and it is inside the first
        // generated block. Typing there is silently dropped, which reads as a dead editor.
        view.replaceRange(0, 0, "X");
        assertFalse(editor.query("getText", null).startsWith("X"),
                "the generated region must stay protected");
        System.out.println("PROTECTED-AT-CARET-ZERO: generated region correctly rejects typing at offset 0");
    }

    private static final class Host implements EditorHost {
        @Override
        public boolean isTextInputSupported() {
            return false;
        }

        @Override
        public Object startTextInput(TextInputClient client, TextInputConfig config) {
            return null;
        }

        @Override
        public void updateTextInputState(Object handle, TextInputState state) {
        }

        @Override
        public void stopTextInput(Object handle) {
        }

        @Override
        public void editorChanged() {
        }

        @Override
        public void fireEditorEvent(String type, String value) {
        }
    }
}
